# -*- encoding: utf-8 -*-
# stub: aws-sdk-resources 2.1.32 ruby lib

Gem::Specification.new do |s|
  s.name = "aws-sdk-resources"
  s.version = "2.1.32"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Amazon Web Services"]
  s.date = "2015-10-26"
  s.description = "Provides resource oriented interfaces and other higher-level abstractions for many AWS services. This gem is part of the official AWS SDK for Ruby."
  s.email = ["trevrowe@amazon.com"]
  s.homepage = "http://github.com/aws/aws-sdk-ruby"
  s.licenses = ["Apache 2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "AWS SDK for Ruby - Resources"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<aws-sdk-core>, ["= 2.1.32"])
    else
      s.add_dependency(%q<aws-sdk-core>, ["= 2.1.32"])
    end
  else
    s.add_dependency(%q<aws-sdk-core>, ["= 2.1.32"])
  end
end
