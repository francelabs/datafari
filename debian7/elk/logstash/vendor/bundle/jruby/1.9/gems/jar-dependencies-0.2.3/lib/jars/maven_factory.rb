require 'jar_dependencies'

module Jars
  class MavenFactory

    attr_reader :debug, :verbose

    def initialize( options = nil,  debug = Jars.debug?, verbose = Jars.verbose? )
      @options = (options || {}).dup
      @options.delete( :ignore_dependencies )
      @debug = debug
      @verbose = verbose
      @installed_maven = false
    end

    def maven_new( pom )
      lazy_load_maven
      maven = setup( Maven::Ruby::Maven.new )

      # TODO copy pom to tmp dir in case it is not a real file
      maven.options[ '-f' ] = pom
      maven
    end

    private

    def setup( maven )
      maven.verbose = @verbose
      if @debug
        maven.options[ '-X' ] = nil
      end
      if @verbose
        maven.options[ '-e' ] = nil
      elsif not @debug
        maven.options[ '--quiet' ] = nil
      end
      maven[ 'verbose' ] = (@debug || @verbose) == true

      # TODO what todo with https proxy ?
      # FIX this proxy settings seems not to work
      if (proxy = Gem.configuration[ :http_proxy ]).is_a?( String )
        require 'uri'; uri = URI.parse( proxy )
        maven['proxySet'] = 'true'
        maven['proxyHost'] = "#{uri.host}"
        maven['proxyPort'] = "#{uri.port}"
      end

      if Jars.maven_settings
        maven.options[ '-s' ] = Jars.maven_settings
      end

      maven[ 'maven.repo.local' ] = "#{java.io.File.new( Jars.local_maven_repo ).absolute_path}"

      maven
    end

    private

    def lazy_load_maven
      add_gem_to_load_path( 'ruby-maven' )
      add_gem_to_load_path( 'ruby-maven-libs' )
      if @installed_maven
        puts
        puts 'using maven for the first time results in maven'
        puts 'downloading all its default plugin and can take time.'
        puts 'as those plugins get cached on disk and further execution'
        puts 'of maven is much faster then the first time.'
        puts
      end
      require 'maven/ruby/maven'
    end

    def find_spec_via_rubygems( name, req )
      require 'rubygems/dependency'
      dep = Gem::Dependency.new( name, req )
      dep.matching_specs( true ).last
    end

    def add_gem_to_load_path( name )
      # if the gem is already activated => good
      return if Gem.loaded_specs[ name ]
      # just install gem if needed and add it to the load_path
      # and leave activated gems as they are
      req = requirement( name )
      unless spec = find_spec_via_rubygems( name, req )
        spec = install_gem( name, req )
      end
      unless spec
        raise "failed to resolve gem '#{name}' if you're using Bundler add it as a dependency"
      end
      $LOAD_PATH << File.join( spec.full_gem_path, spec.require_path )
    end

    def requirement( name )
      jars = Gem.loaded_specs[ 'jar-dependencies' ]
      dep = jars.nil? ? nil : jars.dependencies.detect { |d| d.name == name }
      dep.nil? ? Gem::Requirement.create( '>0' ) : dep.requirement
    end

    def install_gem( name, req )
      @installed_maven = true
      puts "Installing gem '#{name}' . . ."
      require 'rubygems/dependency_installer'
      inst = Gem::DependencyInstaller.new( @options ||= {} )
      inst.install( name, req ).first
    rescue => e
      if Jars.verbose?
        warn "#{e.inspect}"
        warn e.backtrace.join( "\n" )
      end
      raise "there was an error installing '#{name} (#{req})' #{@options[:domain]}. please install it manually: #{e.inspect}"
    end
  end
end
