# -*- encoding: utf-8 -*-
# stub: jruby-kafka 1.4.0 java lib

Gem::Specification.new do |s|
  s.name = "jruby-kafka"
  s.version = "1.4.0"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Joseph Lawson"]
  s.date = "2015-03-22"
  s.description = "this is primarily to be used as an interface for logstash"
  s.email = ["joe@joekiller.com"]
  s.homepage = "https://github.com/joekiller/jruby-kafka"
  s.licenses = ["Apache 2.0"]
  s.requirements = ["jar 'org.apache.kafka:kafka_2.10', '0.8.2.1'", "jar 'org.slf4j:slf4j-log4j12', '1.7.10'"]
  s.rubygems_version = "2.4.8"
  s.summary = "jruby Kafka wrapper"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<jar-dependencies>, ["~> 0"])
      s.add_runtime_dependency(%q<ruby-maven>, ["~> 3.1"])
      s.add_development_dependency(%q<rake>, ["~> 10.4"])
    else
      s.add_dependency(%q<jar-dependencies>, ["~> 0"])
      s.add_dependency(%q<ruby-maven>, ["~> 3.1"])
      s.add_dependency(%q<rake>, ["~> 10.4"])
    end
  else
    s.add_dependency(%q<jar-dependencies>, ["~> 0"])
    s.add_dependency(%q<ruby-maven>, ["~> 3.1"])
    s.add_dependency(%q<rake>, ["~> 10.4"])
  end
end
