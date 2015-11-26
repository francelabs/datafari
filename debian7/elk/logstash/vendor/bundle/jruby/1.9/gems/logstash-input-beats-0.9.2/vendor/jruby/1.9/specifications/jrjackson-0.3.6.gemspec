# -*- encoding: utf-8 -*-
# stub: jrjackson 0.3.6 ruby lib

Gem::Specification.new do |s|
  s.name = "jrjackson"
  s.version = "0.3.6"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Guy Boertje"]
  s.date = "2015-10-19"
  s.description = "A mostly native JRuby wrapper for the java jackson json processor jar"
  s.email = ["guyboertje@gmail.com"]
  s.homepage = "http://github.com/guyboertje/jrjackson"
  s.licenses = ["Apache License 2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "A JRuby wrapper for the java jackson json processor jar"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.0"])
  end
end
