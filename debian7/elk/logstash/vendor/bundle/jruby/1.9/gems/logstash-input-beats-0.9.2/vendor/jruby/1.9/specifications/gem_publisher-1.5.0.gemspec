# -*- encoding: utf-8 -*-
# stub: gem_publisher 1.5.0 ruby lib

Gem::Specification.new do |s|
  s.name = "gem_publisher"
  s.version = "1.5.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Government Digital Service"]
  s.date = "2014-10-30"
  s.description = "Automatically build, tag, and push a gem when its version has been updated."
  s.homepage = "http://github.com/alphagov/gem_publisher"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Automatically build, tag, and push gems"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<mocha>, ["= 0.14.0"])
      s.add_development_dependency(%q<minitest>, ["~> 2.5.1"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<mocha>, ["= 0.14.0"])
      s.add_dependency(%q<minitest>, ["~> 2.5.1"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<mocha>, ["= 0.14.0"])
    s.add_dependency(%q<minitest>, ["~> 2.5.1"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
