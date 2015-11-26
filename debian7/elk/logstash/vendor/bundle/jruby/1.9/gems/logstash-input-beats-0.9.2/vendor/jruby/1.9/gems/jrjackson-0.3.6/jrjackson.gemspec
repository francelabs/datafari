#! /usr/bin/env jruby
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

require 'jrjackson/build_info'

Gem::Specification.new do |s|
  s.name        = 'jrjackson'
  s.version     = JrJackson::BuildInfo.version
  s.date        = JrJackson::BuildInfo.release_date
  s.platform    = Gem::Platform::RUBY
  s.authors     = ['Guy Boertje']
  s.email       = ['guyboertje@gmail.com']
  s.homepage    = "http://github.com/guyboertje/jrjackson"
  s.summary     = %q{A JRuby wrapper for the java jackson json processor jar}
  s.description = %q{A mostly native JRuby wrapper for the java jackson json processor jar}
  s.license     = 'Apache License 2.0'

  s.add_development_dependency 'bundler', '~> 1.0'

  s.files = JrJackson::BuildInfo.files

end
