# -*- encoding: utf-8 -*-
# stub: edn 1.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "edn"
  s.version = "1.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Clinton N. Dreisbach & Russ Olsen"]
  s.date = "2015-08-01"
  s.description = "'edn implements a reader for Extensible Data Notation by Rich Hickey.'"
  s.email = ["russ@russolsen.com"]
  s.homepage = "https://github.com/relevance/edn-ruby"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "'edn implements a reader for Extensible Data Notation by Rich Hickey.'"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<pry>, ["~> 0.9.10"])
      s.add_development_dependency(%q<rspec>, ["~> 2.11.0"])
      s.add_development_dependency(%q<rantly>, ["~> 0.3.1"])
      s.add_development_dependency(%q<rake>, ["~> 10.3"])
    else
      s.add_dependency(%q<pry>, ["~> 0.9.10"])
      s.add_dependency(%q<rspec>, ["~> 2.11.0"])
      s.add_dependency(%q<rantly>, ["~> 0.3.1"])
      s.add_dependency(%q<rake>, ["~> 10.3"])
    end
  else
    s.add_dependency(%q<pry>, ["~> 0.9.10"])
    s.add_dependency(%q<rspec>, ["~> 2.11.0"])
    s.add_dependency(%q<rantly>, ["~> 0.3.1"])
    s.add_dependency(%q<rake>, ["~> 10.3"])
  end
end
