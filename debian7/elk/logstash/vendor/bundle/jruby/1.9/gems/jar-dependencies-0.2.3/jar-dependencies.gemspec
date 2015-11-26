#-*- mode: ruby -*-

Gem::Specification.new do |s|
  s.name = 'jar-dependencies'

  path = File.expand_path('lib/jars/version.rb', File.dirname(__FILE__))
  s.version = File.read(path).match( /\s*VERSION\s*=\s*['"](.*)['"]/ )[1]
  
  s.author = 'christian meier'
  s.email = [ 'mkristian@web.de' ]
  s.summary = 'manage jar dependencies for gems'
  s.homepage = 'https://github.com/mkristian/jar-dependencies'

  s.bindir = "bin"
  LOCK_JARS = 'lock_jars'
  s.executables = [LOCK_JARS]

  s.license = 'MIT'

  s.files = `git ls-files`.split($/).select do |file|
    file =~ /^lib\// ||
    [ 'Mavenfile', 'Rakefile' ].include?(file) ||
    [ 'Readme.md', 'jar-dependencies.gemspec', 'MIT-LICENSE' ].include?(file)
  end

  s.description = 'manage jar dependencies for gems and keep track which jar was already loaded using maven artifact coordinates. it warns on version conflicts and loads only ONE jar assuming the first one is compatible to the second one otherwise your project needs to lock down the right version by providing a Jars.lock file.'

  s.add_development_dependency 'minitest', '~> 5.3'
  s.add_development_dependency 'rake', '~> 10.2'
  RUBY_MAVEN_VERSION = '~> 3.3.3'
  s.add_development_dependency 'ruby-maven', RUBY_MAVEN_VERSION

  s.post_install_message = <<EOF

if you want to use the executable #{LOCK_JARS} then install ruby-maven gem before using #{LOCK_JARS} 

   $ gem install ruby-maven -v '#{RUBY_MAVEN_VERSION}'

or add it as deveopment dependency to your Gemfile

   gem 'ruby-maven', '#{RUBY_MAVEN_VERSION}'

EOF
end

# vim: syntax=Ruby
