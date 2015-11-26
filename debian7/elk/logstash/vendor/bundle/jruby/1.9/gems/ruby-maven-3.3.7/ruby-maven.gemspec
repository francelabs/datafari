# -*- mode:ruby -*-

require './lib/maven/ruby/version'

Gem::Specification.new do |s|
  s.name = 'ruby-maven'
  s.version = Maven::Ruby::VERSION

  s.authors = ["Christian Meier"]
  s.description = %q{maven support for ruby DSL pom files. MRI needs java/javac installed.}
  s.summary = %q{maven support for ruby projects}
  s.email = ["m.kristian@web.de"]

  s.homepage = %q{https://github.com/takari/ruby-maven}

  s.license = 'EPL'

  s.executable = 'rmvn'

  s.files = `git ls-files`.split($/)

  s.rdoc_options = ["--main", "README.md"]

  s.add_dependency 'ruby-maven-libs', "~> 3.3.1"
  s.add_development_dependency 'minitest', '~> 5.3'
  s.add_development_dependency 'rake', '~> 10.3'
end

# vim: syntax=Ruby
