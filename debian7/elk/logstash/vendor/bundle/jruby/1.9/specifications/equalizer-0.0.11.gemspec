# -*- encoding: utf-8 -*-
# stub: equalizer 0.0.11 ruby lib

Gem::Specification.new do |s|
  s.name = "equalizer"
  s.version = "0.0.11"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Dan Kubb", "Markus Schirp"]
  s.date = "2015-03-23"
  s.description = "Module to define equality, equivalence and inspection methods"
  s.email = ["dan.kubb@gmail.com", "mbj@schirp-dso.com"]
  s.extra_rdoc_files = ["LICENSE", "README.md", "CONTRIBUTING.md"]
  s.files = ["CONTRIBUTING.md", "LICENSE", "README.md"]
  s.homepage = "https://github.com/dkubb/equalizer"
  s.licenses = ["MIT"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.7")
  s.rubygems_version = "2.4.8"
  s.summary = "Module to define equality, equivalence and inspection methods"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, [">= 1.3.5", "~> 1.3"])
    else
      s.add_dependency(%q<bundler>, [">= 1.3.5", "~> 1.3"])
    end
  else
    s.add_dependency(%q<bundler>, [">= 1.3.5", "~> 1.3"])
  end
end
