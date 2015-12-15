# -*- encoding: utf-8 -*-
# stub: logstash-mixin-rabbitmq_connection 2.0.2 java lib

Gem::Specification.new do |s|
  s.name = "logstash-mixin-rabbitmq_connection"
  s.version = "2.0.2"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Elastic"]
  s.date = "2015-10-14"
  s.description = "This is used to provide configuration options and connection settings for logstash plugins working with RabbitMQ"
  s.email = "info@elastic.co"
  s.homepage = "http://www.elastic.co/guide/en/logstash/current/index.html"
  s.licenses = ["Apache License (2.0)"]
  s.rubygems_version = "2.4.8"
  s.summary = "Common functionality for RabbitMQ plugins"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
      s.add_runtime_dependency(%q<march_hare>, ["~> 2.11.0"])
      s.add_runtime_dependency(%q<stud>, ["~> 0.0.22"])
      s.add_development_dependency(%q<logstash-devutils>, [">= 0"])
      s.add_development_dependency(%q<logstash-input-generator>, [">= 0"])
      s.add_development_dependency(%q<logstash-codec-json>, [">= 0"])
    else
      s.add_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
      s.add_dependency(%q<march_hare>, ["~> 2.11.0"])
      s.add_dependency(%q<stud>, ["~> 0.0.22"])
      s.add_dependency(%q<logstash-devutils>, [">= 0"])
      s.add_dependency(%q<logstash-input-generator>, [">= 0"])
      s.add_dependency(%q<logstash-codec-json>, [">= 0"])
    end
  else
    s.add_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
    s.add_dependency(%q<march_hare>, ["~> 2.11.0"])
    s.add_dependency(%q<stud>, ["~> 0.0.22"])
    s.add_dependency(%q<logstash-devutils>, [">= 0"])
    s.add_dependency(%q<logstash-input-generator>, [">= 0"])
    s.add_dependency(%q<logstash-codec-json>, [">= 0"])
  end
end
