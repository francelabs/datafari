# -*- encoding: utf-8 -*-
# stub: aws-sdk-v1 1.66.0 ruby lib

Gem::Specification.new do |s|
  s.name = "aws-sdk-v1"
  s.version = "1.66.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Amazon Web Services"]
  s.date = "2015-09-05"
  s.description = "Version 1 of the AWS SDK for Ruby. Available as both `aws-sdk` and `aws-sdk-v1`.\nUse `aws-sdk-v1` if you want to load v1 and v2 of the Ruby SDK in the same\napplication."
  s.executables = ["aws-rb"]
  s.files = ["bin/aws-rb"]
  s.homepage = "http://aws.amazon.com/sdkforruby"
  s.licenses = ["Apache 2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "AWS SDK for Ruby V1"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<nokogiri>, [">= 1.4.4"])
      s.add_runtime_dependency(%q<json>, ["~> 1.4"])
    else
      s.add_dependency(%q<nokogiri>, [">= 1.4.4"])
      s.add_dependency(%q<json>, ["~> 1.4"])
    end
  else
    s.add_dependency(%q<nokogiri>, [">= 1.4.4"])
    s.add_dependency(%q<json>, ["~> 1.4"])
  end
end
