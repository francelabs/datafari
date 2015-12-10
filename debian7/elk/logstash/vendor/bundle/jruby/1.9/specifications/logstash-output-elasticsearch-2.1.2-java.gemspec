# -*- encoding: utf-8 -*-
# stub: logstash-output-elasticsearch 2.1.2 java lib

Gem::Specification.new do |s|
  s.name = "logstash-output-elasticsearch"
  s.version = "2.1.2"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.metadata = { "logstash_group" => "output", "logstash_plugin" => "true" } if s.respond_to? :metadata=
  s.require_paths = ["lib"]
  s.authors = ["Elastic"]
  s.date = "2015-10-22"
  s.description = "Output events to elasticsearch"
  s.email = "info@elastic.co"
  s.homepage = "http://logstash.net/"
  s.licenses = ["apache-2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "Logstash Output to Elasticsearch"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<concurrent-ruby>, [">= 0"])
      s.add_runtime_dependency(%q<elasticsearch>, [">= 1.0.13", "~> 1.0"])
      s.add_runtime_dependency(%q<stud>, [">= 0.0.17", "~> 0.0"])
      s.add_runtime_dependency(%q<cabin>, ["~> 0.6"])
      s.add_runtime_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
      s.add_development_dependency(%q<ftw>, ["~> 0.0.42"])
      s.add_development_dependency(%q<logstash-codec-plain>, [">= 0"])
      s.add_runtime_dependency(%q<manticore>, ["~> 0.4.2"])
      s.add_development_dependency(%q<logstash-devutils>, [">= 0"])
      s.add_development_dependency(%q<longshoreman>, [">= 0"])
      s.add_development_dependency(%q<flores>, [">= 0"])
    else
      s.add_dependency(%q<concurrent-ruby>, [">= 0"])
      s.add_dependency(%q<elasticsearch>, [">= 1.0.13", "~> 1.0"])
      s.add_dependency(%q<stud>, [">= 0.0.17", "~> 0.0"])
      s.add_dependency(%q<cabin>, ["~> 0.6"])
      s.add_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
      s.add_dependency(%q<ftw>, ["~> 0.0.42"])
      s.add_dependency(%q<logstash-codec-plain>, [">= 0"])
      s.add_dependency(%q<manticore>, ["~> 0.4.2"])
      s.add_dependency(%q<logstash-devutils>, [">= 0"])
      s.add_dependency(%q<longshoreman>, [">= 0"])
      s.add_dependency(%q<flores>, [">= 0"])
    end
  else
    s.add_dependency(%q<concurrent-ruby>, [">= 0"])
    s.add_dependency(%q<elasticsearch>, [">= 1.0.13", "~> 1.0"])
    s.add_dependency(%q<stud>, [">= 0.0.17", "~> 0.0"])
    s.add_dependency(%q<cabin>, ["~> 0.6"])
    s.add_dependency(%q<logstash-core>, ["< 3.0.0", ">= 2.0.0.beta2"])
    s.add_dependency(%q<ftw>, ["~> 0.0.42"])
    s.add_dependency(%q<logstash-codec-plain>, [">= 0"])
    s.add_dependency(%q<manticore>, ["~> 0.4.2"])
    s.add_dependency(%q<logstash-devutils>, [">= 0"])
    s.add_dependency(%q<longshoreman>, [">= 0"])
    s.add_dependency(%q<flores>, [">= 0"])
  end
end
