# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"
require "logstash/plugin_mixins/aws_config"
require "stud/temporary"
require "stud/task"
require "socket" # for Socket.gethostname
require "thread"
require "tmpdir"
require "fileutils"


# INFORMATION:
#
# This plugin sends logstash events to Amazon Simple Storage Service (Amazon S3).
# To use it you need to have the proper write premissions and a valid s3 bucket.
# Make sure you have permissions to write files on S3's bucket.  Also be sure to run logstash as super user to establish a connection.
#
#
# This plugin outputs temporary files to "/opt/logstash/S3_temp/". If you want, you can change the path at the start of register method.
# These files have a special name, for example:
#
# ls.s3.ip-10-228-27-95.2013-04-18T10.00.tag_hello.part0.txt
#
# ls.s3 : indicate logstash plugin s3
#
# "ip-10-228-27-95" : indicates the ip of your machine.
# "2013-04-18T10.00" : represents the time whenever you specify time_file.
# "tag_hello" : this indicates the event's tag.
# "part0" : this means if you indicate size_file then it will generate more parts if you file.size > size_file.
#           When a file is full it will be pushed to a bucket and will be deleted from the temporary directory.
#           If a file is empty is not pushed, it is not deleted.
#
# This plugin have a system to restore the previous temporary files if something crash.
#
##[Note] :
#
## If you specify size_file and time_file then it will create file for each tag (if specified), when time_file or
## their size > size_file, it will be triggered then they will be pushed on s3's bucket and will delete from local disk.
## If you don't specify size_file, but time_file then it will create only one file for each tag (if specified).
## When time_file it will be triggered then the files will be pushed on s3's bucket and delete from local disk.
#
## If you don't specify time_file, but size_file  then it will create files for each tag (if specified),
## that will be triggered when their size > size_file, then they will be pushed on s3's bucket and will delete from local disk.
#
## If you don't specific size_file and time_file you have a curios mode. It will create only one file for each tag (if specified).
## Then the file will be rest on temporary directory and don't will be pushed on bucket until we will restart logstash.
#
#
# #### Usage:
# This is an example of logstash config:
# [source,ruby]
# output {
#    s3{
#      access_key_id => "crazy_key"             (required)
#      secret_access_key => "monkey_access_key" (required)
#      endpoint_region => "eu-west-1"           (required)
#      bucket => "boss_please_open_your_bucket" (required)
#      size_file => 2048                        (optional)
#      time_file => 5                           (optional)
#      format => "plain"                        (optional)
#      canned_acl => "private"                  (optional. Options are "private", "public_read", "public_read_write", "authenticated_read". Defaults to "private" )
#    }
#
class LogStash::Outputs::S3 < LogStash::Outputs::Base
  include LogStash::PluginMixins::AwsConfig

  TEMPFILE_EXTENSION = "txt"
  S3_INVALID_CHARACTERS = /[\^`><]/

  config_name "s3"
  default :codec, 'line'

  # S3 bucket
  config :bucket, :validate => :string

  # AWS endpoint_region
  config :endpoint_region, :validate => ["us-east-1", "us-west-1", "us-west-2",
                                         "eu-west-1", "ap-southeast-1", "ap-southeast-2",
                                        "ap-northeast-1", "sa-east-1", "us-gov-west-1"], :deprecated => 'Deprecated, use region instead.'

  # Set the size of file in bytes, this means that files on bucket when have dimension > file_size, they are stored in two or more file.
  # If you have tags then it will generate a specific size file for every tags
  ##NOTE: define size of file is the better thing, because generate a local temporary file on disk and then put it in bucket.
  config :size_file, :validate => :number, :default => 0

  # Set the time, in minutes, to close the current sub_time_section of bucket.
  # If you define file_size you have a number of files in consideration of the section and the current tag.
  # 0 stay all time on listerner, beware if you specific 0 and size_file 0, because you will not put the file on bucket,
  # for now the only thing this plugin can do is to put the file when logstash restart.
  config :time_file, :validate => :number, :default => 0

  ## IMPORTANT: if you use multiple instance of s3, you should specify on one of them the "restore=> true" and on the others "restore => false".
  ## This is hack for not destroy the new files after restoring the initial files.
  ## If you do not specify "restore => true" when logstash crashes or is restarted, the files are not sent into the bucket,
  ## for example if you have single Instance.
  config :restore, :validate => :boolean, :default => false

  # The S3 canned ACL to use when putting the file. Defaults to "private".
  config :canned_acl, :validate => ["private", "public_read", "public_read_write", "authenticated_read"],
         :default => "private"

  # Set the directory where logstash will store the tmp files before sending it to S3
  # default to the current OS temporary directory in linux /tmp/logstash
  config :temporary_directory, :validate => :string, :default => File.join(Dir.tmpdir, "logstash")

  # Specify a prefix to the uploaded filename, this can simulate directories on S3
  config :prefix, :validate => :string, :default => ''

  # Specify how many workers to use to upload the files to S3
  config :upload_workers_count, :validate => :number, :default => 1

  # Define tags to be appended to the file on the S3 bucket.
  #
  # Example:
  # tags => ["elasticsearch", "logstash", "kibana"]
  #
  # Will generate this file:
  # "ls.s3.logstash.local.2015-01-01T00.00.tag_elasticsearch.logstash.kibana.part0.txt"
  # 
  config :tags, :validate => :array, :default => []

  # Exposed attributes for testing purpose.
  attr_accessor :tempfile
  attr_reader :page_counter
  attr_reader :s3

  def aws_s3_config
    @logger.info("Registering s3 output", :bucket => @bucket, :endpoint_region => @region)
    @s3 = AWS::S3.new(aws_options_hash)
  end

  def aws_service_endpoint(region)
    # Make the deprecated endpoint_region work
    # TODO: (ph) Remove this after deprecation.
    
    if @endpoint_region
      region_to_use = @endpoint_region
    else
      region_to_use = @region
    end

    return {
      :s3_endpoint => region_to_use == 'us-east-1' ? 's3.amazonaws.com' : "s3-#{region_to_use}.amazonaws.com"
    }
  end

  public
  def write_on_bucket(file)
    # find and use the bucket
    bucket = @s3.buckets[@bucket]

    remote_filename = "#{@prefix}#{File.basename(file)}"

    @logger.debug("S3: ready to write file in bucket", :remote_filename => remote_filename, :bucket => @bucket)

    File.open(file, 'r') do |fileIO|
      begin
        # prepare for write the file
        object = bucket.objects[remote_filename]
        object.write(fileIO, :acl => @canned_acl)
      rescue AWS::Errors::Base => error
        @logger.error("S3: AWS error", :error => error)
        raise LogStash::Error, "AWS Configuration Error, #{error}"
      end
    end

    @logger.debug("S3: has written remote file in bucket with canned ACL", :remote_filename => remote_filename, :bucket  => @bucket, :canned_acl => @canned_acl)
  end

  # This method is used for create new empty temporary files for use. Flag is needed for indicate new subsection time_file.
  public
  def create_temporary_file
    filename = File.join(@temporary_directory, get_temporary_filename(@page_counter))

    @logger.debug("S3: Creating a new temporary file", :filename => filename)

    @file_rotation_lock.synchronize do
      unless @tempfile.nil?
        @tempfile.close
      end

      @tempfile = File.open(filename, "a")
    end
  end

  public
  def register
    require "aws-sdk"
    # required if using ruby version < 2.0
    # http://ruby.awsblog.com/post/Tx16QY1CI5GVBFT/Threading-with-the-AWS-SDK-for-Ruby
    AWS.eager_autoload!(AWS::S3)

    workers_not_supported

    @s3 = aws_s3_config
    @upload_queue = Queue.new
    @file_rotation_lock = Mutex.new

    if @prefix && @prefix =~ S3_INVALID_CHARACTERS
      @logger.error("S3: prefix contains invalid characters", :prefix => @prefix, :contains => S3_INVALID_CHARACTERS)
      raise LogStash::ConfigurationError, "S3: prefix contains invalid characters"
    end

    if !Dir.exist?(@temporary_directory)
      FileUtils.mkdir_p(@temporary_directory)
    end

    test_s3_write

    restore_from_crashes if @restore == true
    reset_page_counter
    create_temporary_file
    configure_periodic_rotation if time_file != 0
    configure_upload_workers

    @codec.on_event do |event, encoded_event|
      handle_event(encoded_event)
    end
  end


  # Use the same method that Amazon use to check
  # permission on the user bucket by creating a small file
  public
  def test_s3_write
    @logger.debug("S3: Creating a test file on S3")

    test_filename = File.join(@temporary_directory,
                              "logstash-programmatic-access-test-object-#{Time.now.to_i}")

    File.open(test_filename, 'a') do |file|
      file.write('test')
    end

    begin
      write_on_bucket(test_filename)
      delete_on_bucket(test_filename)
    ensure
      File.delete(test_filename)
    end
  end
  
  public
  def restore_from_crashes
    @logger.debug("S3: is attempting to verify previous crashes...")

    Dir[File.join(@temporary_directory, "*.#{TEMPFILE_EXTENSION}")].each do |file|
      name_file = File.basename(file)
      @logger.warn("S3: have found temporary file the upload process crashed, uploading file to S3.", :filename => name_file)
      move_file_to_bucket_async(file)
    end
  end

  public
  def move_file_to_bucket(file)
    if !File.zero?(file)
      write_on_bucket(file)
      @logger.debug("S3: file was put on the upload thread", :filename => File.basename(file), :bucket => @bucket)
    end

    begin
      File.delete(file)
    rescue Errno::ENOENT
      # Something else deleted the file, logging but not raising the issue
      @logger.warn("S3: Cannot delete the temporary file since it doesn't exist on disk", :filename => File.basename(file))
    rescue Errno::EACCES
      @logger.error("S3: Logstash doesnt have the permission to delete the file in the temporary directory.", :filename => File.basename(file), :temporary_directory => @temporary_directory)
    end
  end

  public
  def periodic_interval
    @time_file * 60
  end

  public
  def get_temporary_filename(page_counter = 0)
    current_time = Time.now
    filename = "ls.s3.#{Socket.gethostname}.#{current_time.strftime("%Y-%m-%dT%H.%M")}"

    if @tags.size > 0
      return "#{filename}.tag_#{@tags.join('.')}.part#{page_counter}.#{TEMPFILE_EXTENSION}"
    else
      return "#{filename}.part#{page_counter}.#{TEMPFILE_EXTENSION}"
    end
  end

  public
  def receive(event)
    
    @codec.encode(event)
  end

  public
  def rotate_events_log?
    @file_rotation_lock.synchronize do
      @tempfile.size > @size_file
    end
  end

  public
  def write_events_to_multiple_files?
    @size_file > 0
  end

  public
  def write_to_tempfile(event)
    begin
      @logger.debug("S3: put event into tempfile ", :tempfile => File.basename(@tempfile))

      @file_rotation_lock.synchronize do
        @tempfile.syswrite(event)
      end
    rescue Errno::ENOSPC
      @logger.error("S3: No space left in temporary directory", :temporary_directory => @temporary_directory)
      close
    end
  end

  public
  def close
    shutdown_upload_workers
    @periodic_rotation_thread.stop! if @periodic_rotation_thread

    @file_rotation_lock.synchronize do
      @tempfile.close unless @tempfile.nil? && @tempfile.closed?
    end
  end

  private
  def shutdown_upload_workers
    @logger.debug("S3: Gracefully shutdown the upload workers")
    @upload_queue << LogStash::ShutdownEvent
  end

  private
  def handle_event(encoded_event)
    if write_events_to_multiple_files?
      if rotate_events_log?
        @logger.debug("S3: tempfile is too large, let's bucket it and create new file", :tempfile => File.basename(@tempfile))

        move_file_to_bucket_async(@tempfile.path)
        next_page
        create_temporary_file
      else
        @logger.debug("S3: tempfile file size report.", :tempfile_size => @tempfile.size, :size_file => @size_file)
      end
    end 

    write_to_tempfile(encoded_event)
  end

  private
  def configure_periodic_rotation
    @periodic_rotation_thread = Stud::Task.new do
      LogStash::Util::set_thread_name("<S3 periodic uploader")

      Stud.interval(periodic_interval, :sleep_then_run => true) do
        @logger.debug("S3: time_file triggered, bucketing the file", :filename => @tempfile.path)

        move_file_to_bucket_async(@tempfile.path)
        next_page
        create_temporary_file
      end
    end
  end

  private
  def configure_upload_workers
    @logger.debug("S3: Configure upload workers")

    @upload_workers = @upload_workers_count.times.map do |worker_id|
      Stud::Task.new do
        LogStash::Util::set_thread_name("<S3 upload worker #{worker_id}")

        while true do
          @logger.debug("S3: upload worker is waiting for a new file to upload.", :worker_id => worker_id)

          upload_worker
        end
      end
    end
  end

  private
  def upload_worker
    file = @upload_queue.deq

    case file
      when LogStash::ShutdownEvent
        @logger.debug("S3: upload worker is shutting down gracefuly")
        @upload_queue.enq(LogStash::ShutdownEvent)
      else
        @logger.debug("S3: upload working is uploading a new file", :filename => File.basename(file))
        move_file_to_bucket(file)
    end
  end

  private
  def next_page
    @page_counter += 1
  end

  private
  def reset_page_counter
    @page_counter = 0
  end

  private
  def delete_on_bucket(filename)
    bucket = @s3.buckets[@bucket]

    remote_filename = "#{@prefix}#{File.basename(filename)}"

    @logger.debug("S3: delete file from bucket", :remote_filename => remote_filename, :bucket => @bucket)

    begin
      # prepare for write the file
      object = bucket.objects[remote_filename]
      object.delete
    rescue AWS::Errors::Base => e
      @logger.error("S3: AWS error", :error => e)
      raise LogStash::ConfigurationError, "AWS Configuration Error"
    end
  end

  private
  def move_file_to_bucket_async(file)
    @logger.debug("S3: Sending the file to the upload queue.", :filename => File.basename(file))
    @upload_queue.enq(file)
  end
end
