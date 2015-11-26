require "logger"
if RbConfig::CONFIG['host_os'] =~ /mswin|mingw|cygwin/
  require "filewatch/winhelper"
end

module FileWatch
  class Watch
    attr_accessor :logger

    public
    def initialize(opts={})
      @iswindows = ((RbConfig::CONFIG['host_os'] =~ /mswin|mingw|cygwin/) != nil)
      if opts[:logger]
        @logger = opts[:logger]
      else
        @logger = Logger.new(STDERR)
        @logger.level = Logger::INFO
      end
      @watching = []
      @exclude = []
      @files = Hash.new { |h, k| h[k] = Hash.new }
    end # def initialize

    public
    def logger=(logger)
      @logger = logger
    end

    public
    def exclude(path)
      path.to_a.each { |p| @exclude << p }
    end

    public
    def watch(path)
      if ! @watching.member?(path)
        @watching << path
        _discover_file(path, true)
      end

      return true
    end # def watch

    public
    def inode(path,stat)
      if @iswindows
        fileId = Winhelper.GetWindowsUniqueFileIdentifier(path)
        inode = [fileId, stat.dev_major, stat.dev_minor]
      else
        inode = [stat.ino.to_s, stat.dev_major, stat.dev_minor]
      end
      return inode
    end

    # Calls &block with params [event_type, path]
    # event_type can be one of:
    #   :create_initial - initially present file (so start at end for tail)
    #   :create - file is created (new file after initial globs, start at 0)
    #   :modify - file is modified (size increases)
    #   :delete - file is deleted
    public
    def each(&block)
      # Send any creates.
      @files.keys.each do |path|
        if ! @files[path][:create_sent]
          if @files[path][:initial]
            yield(:create_initial, path)
          else
            yield(:create, path)
          end
          @files[path][:create_sent] = true
        end
      end

      @files.keys.each do |path|
        begin
          stat = File::Stat.new(path)
        rescue Errno::ENOENT
          # file has gone away or we can't read it anymore.
          @files.delete(path)
          @logger.debug? && @logger.debug("#{path}: stat failed (#{$!}), deleting from @files")
          yield(:delete, path)
          next
        end

        inode = inode(path,stat)
        if inode != @files[path][:inode]
          @logger.debug? && @logger.debug("#{path}: old inode was #{@files[path][:inode].inspect}, new is #{inode.inspect}")
          yield(:delete, path)
          yield(:create, path)
        elsif stat.size < @files[path][:size]
          @logger.debug? && @logger.debug("#{path}: file rolled, new size is #{stat.size}, old size #{@files[path][:size]}")
          yield(:delete, path)
          yield(:create, path)
        elsif stat.size > @files[path][:size]
          @logger.debug? && @logger.debug("#{path}: file grew, old size #{@files[path][:size]}, new size #{stat.size}")
          yield(:modify, path)
        end

        @files[path][:size] = stat.size
        @files[path][:inode] = inode
      end # @files.keys.each
    end # def each

    public
    def discover
      @watching.each do |path|
        _discover_file(path)
      end
    end

    public
    def subscribe(stat_interval = 1, discover_interval = 5, &block)
      glob = 0
      @quit = false
      while !@quit
        each(&block)

        glob += 1
        if glob == discover_interval
          discover
          glob = 0
        end

        sleep(stat_interval)
      end
    end # def subscribe

    private
    def _discover_file(path, initial=false)
      globbed_dirs = Dir.glob(path)
      @logger.debug? && @logger.debug("_discover_file_glob: #{path}: glob is: #{globbed_dirs}")
      if globbed_dirs.empty? && File.file?(path)
        globbed_dirs = [path]
        @logger.debug? && @logger.debug("_discover_file_glob: #{path}: glob is: #{globbed_dirs} because glob did not work")
      end
      globbed_dirs.each do |file|
        next if @files.member?(file)
        next unless File.file?(file)

        @logger.debug? && @logger.debug("_discover_file: #{path}: new: #{file} (exclude is #{@exclude.inspect})")

        skip = false
        @exclude.each do |pattern|
          if File.fnmatch?(pattern, File.basename(file))
            @logger.debug? && @logger.debug("_discover_file: #{file}: skipping because it " +
                          "matches exclude #{pattern}")
            skip = true
            break
          end
        end
        next if skip

        stat = File::Stat.new(file)
        @files[file] = {
          :size => 0,
          :inode => inode(file,stat),
          :create_sent => false,
          :initial => initial
        }
      end
    end # def _discover_file

    public
    def quit
      @quit = true
    end # def quit
  end # class Watch
end # module FileWatch
