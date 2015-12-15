# -*- encoding: utf-8 -*-
# stub: buftok 0.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "buftok"
  s.version = "0.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.5") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Tony Arcieri", "Martin Emde", "Erik Michaels-Ober"]
  s.date = "2013-11-22"
  s.description = "BufferedTokenizer extracts token delimited entities from a sequence of arbitrary inputs"
  s.email = "sferik@gmail.com"
  s.homepage = "https://github.com/sferik/buftok"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "BufferedTokenizer extracts token delimited entities from a sequence of arbitrary inputs"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.0"])
  end
end
