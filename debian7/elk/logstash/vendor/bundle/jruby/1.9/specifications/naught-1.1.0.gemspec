# -*- encoding: utf-8 -*-
# stub: naught 1.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "naught"
  s.version = "1.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Avdi Grimm"]
  s.date = "2015-09-08"
  s.description = "Naught is a toolkit for building Null Objects"
  s.email = ["avdi@avdi.org"]
  s.homepage = "https://github.com/avdi/naught"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Naught is a toolkit for building Null Objects"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.3"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.3"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.3"])
  end
end
