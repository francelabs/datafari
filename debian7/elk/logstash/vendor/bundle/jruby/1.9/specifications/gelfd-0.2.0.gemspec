# -*- encoding: utf-8 -*-
# stub: gelfd 0.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "gelfd"
  s.version = "0.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["John E. Vincent"]
  s.date = "2011-11-11"
  s.description = "Standalone implementation of the Graylog Extended Log Format"
  s.email = ["lusis.org+github.com@gmail.com"]
  s.executables = ["gelfd"]
  s.files = ["bin/gelfd"]
  s.homepage = ""
  s.rubyforge_project = "gelfd"
  s.rubygems_version = "2.4.8"
  s.summary = "Pure ruby gelf server and decoding library"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<json>, ["~> 1.5.4"])
      s.add_development_dependency(%q<rake>, ["~> 0.9.2"])
    else
      s.add_dependency(%q<json>, ["~> 1.5.4"])
      s.add_dependency(%q<rake>, ["~> 0.9.2"])
    end
  else
    s.add_dependency(%q<json>, ["~> 1.5.4"])
    s.add_dependency(%q<rake>, ["~> 0.9.2"])
  end
end
