require 'jar_dependencies'
require 'jars/maven_exec'

module Jars
  class Installer

    class Dependency

      attr_reader :path, :file, :gav, :scope, :type, :coord

      def self.new( line )
        if line.match /:jar:|:pom:/
          super
        end
      end

      def setup_type( line )
        if line.index(':pom:')
          @type = :pom
        elsif line.index(':jar:')
          @type = :jar
        end
      end
      private :setup_type

      def setup_scope( line )
        @scope =
          case line
          when /:provided:/
            :provided
          when /:test:/
            :test
          else
            :runtime
          end
      end
      private :setup_scope

      def initialize( line )
        setup_type( line )

        line.sub!( /^\s+/, empty = '' )
        @coord = line.sub( /:[^:]+:([A-Z]:\\)?[^:]+$/, empty )
        first, second = @coord.split( /:#{type}:/ )
        group_id, artifact_id = first.split( /:/ )
        parts = group_id.split( '.' )
        parts << artifact_id
        parts << second.split( ':' )[ -1 ]
        parts << File.basename( line.sub( /.:/, empty ) )
        @path = File.join( parts ).strip

        setup_scope( line )

        reg = /:jar:|:pom:|:test:|:compile:|:runtime:|:provided:|:system:/
        @file = line.slice(@coord.length, line.length).sub(reg, empty).strip
        @system = line.index(':system:') != nil
        @gav = @coord.sub(reg, ':')
      end

      def system?
        @system
      end
    end

    def self.install_jars( write_require_file = false )
      new.install_jars( write_require_file )
    end

    def self.vendor_jars( write_require_file = false )
      new.vendor_jars( write_require_file )
    end

    def self.load_from_maven( file )
      result = []
      File.read( file ).each_line do |line|
        dep = Dependency.new( line )
        result << dep if dep && dep.scope == :runtime
      end
      result
    end

    def self.write_require_file( require_filename )
      warn "deprecated"
      if needs_to_write?(require_filename)
        FileUtils.mkdir_p( File.dirname( require_filename ) )
        f = File.open( require_filename, 'w' )
        f.puts COMMENT
        f.puts "require 'jar_dependencies'"
        f.puts
        f
      end
    end

    def self.vendor_file( dir, dep )
      if !dep.system? && dep.type == :jar && dep.scope == :runtime
        vendored = File.join( dir, dep.path )
        FileUtils.mkdir_p( File.dirname( vendored ) )
        FileUtils.cp( dep.file, vendored )
      end
    end

    def self.write_dep( file, dir, dep, vendor )
      warn "deprecated"
      print_require_jar( file, dep )
    end

    def self.print_require_jar( file, dep )
      return if dep.type != :jar || dep.scope != :runtime
      if dep.system?
        file.puts( "require( '#{dep.file}' )" ) if file
      elsif dep.scope == :runtime
        file.puts( "require_jar( '#{dep.gav.gsub( ':', "', '" )}' )" ) if file
      end
    end

    COMMENT = '# this is a generated file, to avoid over-writing it just delete this comment'
    def self.needs_to_write?(require_filename)
      ( require_filename and not File.exists?( require_filename ) ) or
        File.read( require_filename ).match( COMMENT)
    end

    def self.install_deps( deps, dir, require_filename, vendor )
      warn "deprecated"
      write_require_jars( deps, require_filename )
      vendor_jars( deps, dir ) if dir && vendor
    end

    def self.write_require_jars( deps, require_filename )
      if needs_to_write?(require_filename)
        FileUtils.mkdir_p( File.dirname( require_filename ) )
        File.open( require_filename, 'w' ) do |f|
          f.puts COMMENT
          f.puts "require 'jar_dependencies'"
          f.puts
          deps.each do |dep|
            print_require_jar( f, dep )
          end
          yield f if block_given?
        end
      end
    end

    def self.vendor_jars( deps, dir )
      deps.each do |dep|
        vendor_file( dir, dep )
      end
    end

    def initialize( spec = nil )
      @mvn = MavenExec.new( spec )
    end

    def spec; @mvn.spec end

    def vendor_jars( write_require_file = true )
      return unless has_jars?
      case Jars.to_prop( Jars::VENDOR )
      when 'true'
        do_vendor = true
      when 'false'
        do_vendor = false
      else
        # if the spec_file does not exists this means it is a local gem
        # coming via bundle :path or :git
        do_vendor = File.exists?( spec.spec_file )
      end
      do_install( do_vendor, write_require_file )
    end

    def install_jars( write_require_file = true )
      return unless has_jars?
      do_install( false, write_require_file )
    end

    def ruby_maven_install_options=( options )
      @mvn.ruby_maven_install_options=( options )
    end

    def has_jars?
      # first look if there are any requirements in the spec
      # and then if gem depends on jar-dependencies for runtime.
      # only then install the jars declared in the requirements
      result = ( spec = self.spec ) && ! spec.requirements.empty? &&
        spec.dependencies.detect { |d| d.name == 'jar-dependencies' && d.type == :runtime } != nil
      if result && spec.platform.to_s != 'java'
        Jars.warn "\njar dependencies found on non-java platform gem - do not install jars\n"
        false
      else
        result
      end
    end
    alias_method :jars?, :has_jars?

    private

    def do_install( vendor, write_require_file )
      target_dir = File.join( @mvn.basedir, spec.require_path )
      jars_file = File.join( target_dir, "#{spec.name}_jars.rb" )

      # write out new jars_file it write_require_file is true or
      # check timestamps:
      # do not generate file if specfile is older then the generated file
      if ! write_require_file &&
          File.exists?( jars_file ) &&
          File.mtime( @mvn.specfile ) < File.mtime( jars_file )
        # leave jars_file as is
        jars_file = nil
      end
      deps = install_dependencies()
      self.class.write_require_jars( deps, jars_file )
      if vendor
        self.class.vendor_jars( deps, target_dir )
      end
    end

    def install_dependencies
      deps = File.join( @mvn.basedir, 'deps.lst' )

      puts "  jar dependencies for #{spec.spec_name} . . ." unless Jars.quiet?
      @mvn.resolve_dependencies_list( deps )

      self.class.load_from_maven( deps )
    ensure
      FileUtils.rm_f( deps ) if deps
    end
  end
  # to stay backward compatible
  JarInstaller = Installer unless defined? JarInstaller
end
