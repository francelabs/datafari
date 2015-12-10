# -*- encoding: utf-8 -*-
# stub: simple_oauth 0.3.1 ruby lib

Gem::Specification.new do |s|
  s.name = "simple_oauth"
  s.version = "0.3.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Steve Richert", "Erik Michaels-Ober"]
  s.date = "2014-12-28"
  s.description = "Simply builds and verifies OAuth headers"
  s.email = ["steve.richert@gmail.com", "sferik@gmail.com"]
  s.homepage = "https://github.com/laserlemon/simple_oauth"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Simply builds and verifies OAuth headers"

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
