# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"

require "pathname"
require "socket" # for Socket.gethostname

# Stream events from files, normally by tailing them in a manner
# similar to `tail -0F` but optionally reading them from the
# beginning.
#
# By default, each event is assumed to be one line. If you would like
# to join multiple log lines into one event, you'll want to use the
# multiline codec or filter.
#
# The plugin aims to track changing files and emit new content as it's
# appended to each file. It's not well-suited for reading a file from
# beginning to end and storing all of it in a single event (not even
# with the multiline codec or filter).
#
# ==== Tracking of current position in watched files
#
# The plugin keeps track of the current position in each file by
# recording it in a separate file named sincedb. This makes it
# possible to stop and restart Logstash and have it pick up where it
# left off without missing the lines that were added to the file while
# Logstash was stopped.
#
# By default, the sincedb file is placed in the home directory of the
# user running Logstash with a filename based on the filename patterns
# being watched (i.e. the `path` option). Thus, changing the filename
# patterns will result in a new sincedb file being used and any
# existing current position state will be lost. If you change your
# patterns with any frequency it might make sense to explicitly choose
# a sincedb path with the `sincedb_path` option.
#
# Sincedb files are text files with four columns:
#
# . The inode number (or equivalent).
# . The major device number of the file system (or equivalent).
# . The minor device number of the file system (or equivalent).
# . The current byte offset within the file.
#
# On non-Windows systems you can obtain the inode number of a file
# with e.g. `ls -li`.
#
# ==== File rotation
#
# File rotation is detected and handled by this input, regardless of
# whether the file is rotated via a rename or a copy operation. To
# support programs that write to the rotated file for some time after
# the rotation has taken place, include both the original filename and
# the rotated filename (e.g. /var/log/syslog and /var/log/syslog.1) in
# the filename patterns to watch (the `path` option). Note that the
# rotated filename will be treated as a new file so if
# `start_position` is set to 'beginning' the rotated file will be
# reprocessed.
#
# With the default value of `start_position` ('end') any messages
# written to the end of the file between the last read operation prior
# to the rotation and its reopening under the new name (an interval
# determined by the `stat_interval` and `discover_interval` options)
# will not get picked up.
class LogStash::Inputs::File < LogStash::Inputs::Base
  config_name "file"

  # TODO(sissel): This should switch to use the `line` codec by default
  # once file following
  default :codec, "plain"

  # The path(s) to the file(s) to use as an input.
  # You can use filename patterns here, such as `/var/log/*.log`.
  # Paths must be absolute and cannot be relative.
  #
  # You may also configure multiple paths. See an example
  # on the <<array,Logstash configuration page>>.
  config :path, :validate => :array, :required => true

  # Exclusions (matched against the filename, not full path). Filename
  # patterns are valid here, too. For example, if you have
  # [source,ruby]
  #     path => "/var/log/*"
  #
  # You might want to exclude gzipped files:
  # [source,ruby]
  #     exclude => "*.gz"
  config :exclude, :validate => :array

  # How often (in seconds) we stat files to see if they have been modified.
  # Increasing this interval will decrease the number of system calls we make,
  # but increase the time to detect new log lines.
  config :stat_interval, :validate => :number, :default => 1

  # How often (in seconds) we expand the filename patterns in the
  # `path` option to discover new files to watch.
  config :discover_interval, :validate => :number, :default => 15

  # Path of the sincedb database file (keeps track of the current
  # position of monitored log files) that will be written to disk.
  # The default will write sincedb files to some path matching `$HOME/.sincedb*`
  # NOTE: it must be a file path and not a directory path
  config :sincedb_path, :validate => :string

  # How often (in seconds) to write a since database with the current position of
  # monitored log files.
  config :sincedb_write_interval, :validate => :number, :default => 15

  # Choose where Logstash starts initially reading files: at the beginning or
  # at the end. The default behavior treats files like live streams and thus
  # starts at the end. If you have old data you want to import, set this
  # to 'beginning'.
  #
  # This option only modifies "first contact" situations where a file
  # is new and not seen before, i.e. files that don't have a current
  # position recorded in a sincedb file read by Logstash. If a file
  # has already been seen before, this option has no effect and the
  # position recorded in the sincedb file will be used.
  config :start_position, :validate => [ "beginning", "end"], :default => "end"

  # set the new line delimiter, defaults to "\n"
  config :delimiter, :validate => :string, :default => "\n"

  public
  def register
    require "addressable/uri"
    require "filewatch/tail"
    require "digest/md5"
    @logger.info("Registering file input", :path => @path)
    @host = Socket.gethostname.force_encoding(Encoding::UTF_8)

    @tail_config = {
      :exclude => @exclude,
      :stat_interval => @stat_interval,
      :discover_interval => @discover_interval,
      :sincedb_write_interval => @sincedb_write_interval,
      :delimiter => @delimiter,
      :logger => @logger,
    }

    @path.each do |path|
      if Pathname.new(path).relative?
        raise ArgumentError.new("File paths must be absolute, relative path specified: #{path}")
      end
    end

    if @sincedb_path.nil?
      if ENV["SINCEDB_DIR"].nil? && ENV["HOME"].nil?
        @logger.error("No SINCEDB_DIR or HOME environment variable set, I don't know where " \
                      "to keep track of the files I'm watching. Either set " \
                      "HOME or SINCEDB_DIR in your environment, or set sincedb_path in " \
                      "in your Logstash config for the file input with " \
                      "path '#{@path.inspect}'")
        raise # TODO(sissel): HOW DO I FAIL PROPERLY YO
      end

      #pick SINCEDB_DIR if available, otherwise use HOME
      sincedb_dir = ENV["SINCEDB_DIR"] || ENV["HOME"]

      # Join by ',' to make it easy for folks to know their own sincedb
      # generated path (vs, say, inspecting the @path array)
      @sincedb_path = File.join(sincedb_dir, ".sincedb_" + Digest::MD5.hexdigest(@path.join(",")))

      # Migrate any old .sincedb to the new file (this is for version <=1.1.1 compatibility)
      old_sincedb = File.join(sincedb_dir, ".sincedb")
      if File.exists?(old_sincedb)
        @logger.info("Renaming old ~/.sincedb to new one", :old => old_sincedb,
                     :new => @sincedb_path)
        File.rename(old_sincedb, @sincedb_path)
      end

      @logger.info("No sincedb_path set, generating one based on the file path",
                   :sincedb_path => @sincedb_path, :path => @path)
    end

    if File.directory?(@sincedb_path)
      raise ArgumentError.new("The \"sincedb_path\" argument must point to a file, received a directory: \"#{@sincedb_path}\"")
    end

    @tail_config[:sincedb_path] = @sincedb_path

    if @start_position == "beginning"
      @tail_config[:start_new_files_at] = :beginning
    end
  end # def register

  public
  def run(queue)
    @tail = FileWatch::Tail.new(@tail_config)
    @tail.logger = @logger
    @path.each { |path| @tail.tail(path) }

    @tail.subscribe do |path, line|
      @logger.debug? && @logger.debug("Received line", :path => path, :text => line)
      @codec.decode(line) do |event|
        event["[@metadata][path]"] = path
        event["host"] = @host if !event.include?("host")
        event["path"] = path if !event.include?("path")
        decorate(event)
        queue << event
      end
    end
  end # def run

  public
  def stop
    @tail.quit if @tail # _sincedb_write is called implicitly
  end
end # class LogStash::Inputs::File
