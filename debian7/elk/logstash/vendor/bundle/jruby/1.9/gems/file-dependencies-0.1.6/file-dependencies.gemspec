#-*- mode: ruby -*-
require File.expand_path('../lib/file-dependencies/version', __FILE__)

Gem::Specification.new do |s|
  s.name = 'file-dependencies'
  s.version = FileDependencies::VERSION
  s.author = 'Richard Pijnenburg'
  s.email = ['richard.pijnenburg@elasticsearch.com']
  s.summary = 'manage file dependencies for gems'
  s.homepage = 'https://github.com/electrical/file-dependencies'

  s.license = 'APACHE 2.0'

  s.executable = 'file-deps'

  s.files = `git ls-files`.split($ORS)

  s.description = 'manage file dependencies for gems'

  s.add_runtime_dependency 'minitar'

  s.add_development_dependency 'rake', '~> 10.2'
  s.add_development_dependency 'rspec'
  s.add_development_dependency 'stud'
  s.add_development_dependency 'webmock'
  s.add_development_dependency 'coveralls'
  s.add_development_dependency 'codeclimate-test-reporter'
end

# vim: syntax=Ruby
