require 'jar_dependencies'
require 'jars/maven_factory'

module Jars
  class MavenExec

    def find_spec( allow_no_file )
      specs = Dir[ '*.gemspec' ]
      case specs.size
      when 0
        raise 'no gemspec found' unless allow_no_file
      when 1
        specs.first
      else
        raise 'more then one gemspec found. please specify a specfile' unless allow_no_file
      end
    end
    private :find_spec

    attr_reader :basedir, :spec, :specfile

    def initialize( spec = nil )
      @options = {}
      setup( spec )
    rescue StandardError, LoadError => e
      # If spec load fails, skip looking for jar-dependencies
      warn "jar-dependencies: " + e.to_s
      warn e.backtrace.join( "\n" ) if Jars.verbose?
    end

    def setup( spec = nil, allow_no_file = false )
      spec ||= find_spec( allow_no_file )

      case spec
      when String
        @specfile = File.expand_path( spec )
        @basedir = File.dirname( @specfile )
        spec =  Dir.chdir( File.dirname(@specfile) ) do
          eval( File.read( @specfile ) )
        end
      when Gem::Specification
        if File.exists?( spec.spec_file )
          @basedir = spec.gem_dir
          @specfile = spec.spec_file
        else
          # this happens with bundle and local gems
          # there the spec_file is "not installed" but inside
          # the gem_dir directory
          Dir.chdir( spec.gem_dir ) do
            setup( nil, true )
          end
        end
      when NilClass
      else
        raise 'spec must be either String or Gem::Specification'
      end
      @spec = spec
    end

    def ruby_maven_install_options=( options )
      @options = options
    end

    def resolve_dependencies_list( file )
      factory = MavenFactory.new( @options )
      maven = factory.maven_new( File.expand_path( '../gemspec_pom.rb', __FILE__ ) )

      maven[ 'outputAbsoluteArtifactFilename' ] = 'true'
      maven[ 'includeTypes' ] = 'jar'
      maven[ 'outputScope' ] = 'true'
      maven[ 'useRepositoryLayout' ] = 'true'
      maven[ 'outputDirectory' ] = "#{Jars.home}"
      maven[ 'jars.specfile' ] = "#{@specfile}"
      maven[ 'outputFile' ] = "#{file}"

      maven.exec( 'dependency:copy-dependencies', 'dependency:list' )
    end
  end
end
