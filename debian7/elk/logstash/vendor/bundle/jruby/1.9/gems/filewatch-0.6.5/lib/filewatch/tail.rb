require "filewatch/helper"
require "filewatch/buftok"
require "filewatch/watch"
if RbConfig::CONFIG['host_os'] =~ /mswin|mingw|cygwin/
  require "filewatch/winhelper"
end
require "logger"
require "rbconfig"

include Java if defined? JRUBY_VERSION
require "JRubyFileExtension.jar" if defined? JRUBY_VERSION

module FileWatch
  class Tail
    # how often (in seconds) we @logger.warn a failed file open, per path.
    OPEN_WARN_INTERVAL = ENV["FILEWATCH_OPEN_WARN_INTERVAL"] ?
                         ENV["FILEWATCH_OPEN_WARN_INTERVAL"].to_i : 300

    attr_accessor :logger

    class NoSinceDBPathGiven < StandardError; end

    public
    def initialize(opts={})
      @iswindows = ((RbConfig::CONFIG['host_os'] =~ /mswin|mingw|cygwin/) != nil)

      if opts[:logger]
        @logger = opts[:logger]
      else
        @logger = Logger.new(STDERR)
        @logger.level = Logger::INFO
      end
      @files = {}
      @lastwarn = Hash.new { |h, k| h[k] = 0 }
      @buffers = {}
      @watch = FileWatch::Watch.new
      @watch.logger = @logger
      @sincedb = {}
      @sincedb_last_write = 0
      @statcache = {}
      @opts = {
        :sincedb_write_interval => 10,
        :stat_interval => 1,
        :discover_interval => 5,
        :exclude => [],
        :start_new_files_at => :end,
        :delimiter => "\n"
      }.merge(opts)
      if !@opts.include?(:sincedb_path)
        @opts[:sincedb_path] = File.join(ENV["HOME"], ".sincedb") if ENV.include?("HOME")
        @opts[:sincedb_path] = ENV["SINCEDB_PATH"] if ENV.include?("SINCEDB_PATH")
      end
      if !@opts.include?(:sincedb_path)
        raise NoSinceDBPathGiven.new("No HOME or SINCEDB_PATH set in environment. I need one of these set so I can keep track of the files I am following.")
      end
      @watch.exclude(@opts[:exclude])

      _sincedb_open
    end # def initialize

    public
    def logger=(logger)
      @logger = logger
      @watch.logger = logger
    end # def logger=

    public
    def tail(path)
      @watch.watch(path)
    end # def tail

    public
    def subscribe(&block)
      # subscribe(stat_interval = 1, discover_interval = 5, &block)
      @watch.subscribe(@opts[:stat_interval],
                       @opts[:discover_interval]) do |event, path|
        case event
        when :create, :create_initial
          if @files.member?(path)
            @logger.debug? && @logger.debug("#{event} for #{path}: already exists in @files")
            next
          end
          if _open_file(path, event)
            _read_file(path, &block)
          end
        when :modify
          if !@files.member?(path)
            @logger.debug? && @logger.debug(":modify for #{path}, does not exist in @files")
            if _open_file(path, event)
              _read_file(path, &block)
            end
          else
            _read_file(path, &block)
          end
        when :delete
          @logger.debug? && @logger.debug(":delete for #{path}, deleted from @files")
          if @files[path]
            _read_file(path, &block)
            @files[path].close
          end
          @files.delete(path)
          @statcache.delete(path)
        else
          @logger.warn("unknown event type #{event} for #{path}")
        end
      end # @watch.subscribe
    end # def subscribe

    public
    def sincedb_record_uid(path, stat)
      inode = @watch.inode(path,stat)
      @statcache[path] = inode
      return inode
    end # def sincedb_record_uid

    private
    def _open_file(path, event)
      @logger.debug? && @logger.debug("_open_file: #{path}: opening")
      begin
        if @iswindows && defined? JRUBY_VERSION
            @files[path] = Java::RubyFileExt::getRubyFile(path)
        else
            @files[path] = File.open(path)
        end
      rescue
        # don't emit this message too often. if a file that we can't
        # read is changing a lot, we'll try to open it more often,
        # and might be spammy.
        now = Time.now.to_i
        if now - @lastwarn[path] > OPEN_WARN_INTERVAL
          @logger.warn("failed to open #{path}: #{$!}")
          @lastwarn[path] = now
        else
          @logger.debug? && @logger.debug("(warn supressed) failed to open #{path}: #{$!}")
        end
        @files.delete(path)
        return false
      end

      stat = File::Stat.new(path)
      sincedb_record_uid = sincedb_record_uid(path, stat)

      if @sincedb.member?(sincedb_record_uid)
        last_size = @sincedb[sincedb_record_uid]
        @logger.debug? && @logger.debug("#{path}: sincedb last value #{@sincedb[sincedb_record_uid]}, cur size #{stat.size}")
        if last_size <= stat.size
          @logger.debug? && @logger.debug("#{path}: sincedb: seeking to #{last_size}")
          @files[path].sysseek(last_size, IO::SEEK_SET)
        else
          @logger.debug? && @logger.debug("#{path}: last value size is greater than current value, starting over")
          @sincedb[sincedb_record_uid] = 0
        end
      elsif event == :create_initial && @files[path]
        if @opts[:start_new_files_at] == :beginning
          @logger.debug? && @logger.debug("#{path}: initial create, no sincedb, seeking to beginning of file")
          @files[path].sysseek(0, IO::SEEK_SET)
          @sincedb[sincedb_record_uid] = 0
        else 
          # seek to end
          @logger.debug? && @logger.debug("#{path}: initial create, no sincedb, seeking to end #{stat.size}")
          @files[path].sysseek(stat.size, IO::SEEK_SET)
          @sincedb[sincedb_record_uid] = stat.size
        end
      elsif event == :create && @files[path]
        @sincedb[sincedb_record_uid] = 0
      else
        @logger.debug? && @logger.debug("#{path}: staying at position 0, no sincedb")
      end

      return true
    end # def _open_file

    private
    def _read_file(path, &block)
      @buffers[path] ||= FileWatch::BufferedTokenizer.new(@opts[:delimiter])
      delimiter_byte_size = @opts[:delimiter].bytesize
      changed = false
      loop do
        begin
          data = @files[path].sysread(32768)
          changed = true
          @buffers[path].extract(data).each do |line|
            yield(path, line)
            @sincedb[@statcache[path]] += (line.bytesize + delimiter_byte_size)
          end
        rescue Errno::EWOULDBLOCK, Errno::EINTR, EOFError
          break
        end
      end

      if changed
        now = Time.now.to_i
        delta = now - @sincedb_last_write
        if delta >= @opts[:sincedb_write_interval]
          @logger.debug? && @logger.debug("writing sincedb (delta since last write = #{delta})")
          _sincedb_write
          @sincedb_last_write = now
        end
      end
    end # def _read_file

    public
    def sincedb_write(reason=nil)
      @logger.debug? && @logger.debug("caller requested sincedb write (#{reason})")
      _sincedb_write
    end

    private
    def _sincedb_open
      path = @opts[:sincedb_path]
      begin
        db = File.open(path)
      rescue
        #No existing sincedb to load
        @logger.debug? && @logger.debug("_sincedb_open: #{path}: #{$!}")
        return
      end

      @logger.debug? && @logger.debug("_sincedb_open: reading from #{path}")
      db.each do |line|
        ino, dev_major, dev_minor, pos = line.split(" ", 4)
        sincedb_record_uid = [ino, dev_major.to_i, dev_minor.to_i]
        @logger.debug? && @logger.debug("_sincedb_open: setting #{sincedb_record_uid.inspect} to #{pos.to_i}")
        @sincedb[sincedb_record_uid] = pos.to_i
      end
      db.close
    end # def _sincedb_open

    private
    def _sincedb_write
      path = @opts[:sincedb_path]
      if File.device?(path)
        IO.write(path, serialize_sincedb, 0)
      else
        File.atomic_write(path) {|file| file.write(serialize_sincedb) }
      end
    end # def _sincedb_write

    public
    def quit
      _sincedb_write
      @watch.quit
    end # def quit

    private
    def serialize_sincedb
      @sincedb.map do |inode, pos|
        [inode, pos].flatten.join(" ")
      end.join("\n") + "\n"
    end
  end # class Tail
end # module FileWatch
