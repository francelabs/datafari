# -*- encoding: utf-8 -*-
# stub: http 0.6.4 ruby lib

Gem::Specification.new do |s|
  s.name = "http"
  s.version = "0.6.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Tony", "Arcieri"]
  s.date = "2015-03-25"
  s.description = "An easy-to-use client library for making requests from Ruby. It uses a simple method chaining system for building requests, similar to Python's Requests."
  s.email = ["tony.arcieri@gmail.com"]
  s.homepage = "https://github.com/tarcieri/http"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "HTTP should be easy"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<http_parser.rb>, ["~> 0.6.0"])
      s.add_development_dependency(%q<bundler>, ["~> 1.0"])
    else
      s.add_dependency(%q<http_parser.rb>, ["~> 0.6.0"])
      s.add_dependency(%q<bundler>, ["~> 1.0"])
    end
  else
    s.add_dependency(%q<http_parser.rb>, ["~> 0.6.0"])
    s.add_dependency(%q<bundler>, ["~> 1.0"])
  end
end
