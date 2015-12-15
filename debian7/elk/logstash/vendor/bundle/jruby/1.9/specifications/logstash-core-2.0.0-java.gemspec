# -*- encoding: utf-8 -*-
# stub: logstash-core 2.0.0 java lib

Gem::Specification.new do |s|
  s.name = "logstash-core"
  s.version = "2.0.0"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Jordan Sissel", "Pete Fritchman", "Elasticsearch"]
  s.date = "2015-10-27"
  s.description = "The core components of logstash, the scalable log and event management tool"
  s.email = ["jls@semicomplete.com", "petef@databits.net", "info@elasticsearch.com"]
  s.homepage = "http://www.elastic.co/guide/en/logstash/current/index.html"
  s.licenses = ["Apache License (2.0)"]
  s.rubygems_version = "2.4.8"
  s.summary = "logstash-core - The core components of logstash"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<cabin>, ["~> 0.7.0"])
      s.add_runtime_dependency(%q<pry>, ["~> 0.10.1"])
      s.add_runtime_dependency(%q<stud>, ["~> 0.0.19"])
      s.add_runtime_dependency(%q<clamp>, ["~> 0.6.5"])
      s.add_runtime_dependency(%q<filesize>, ["= 0.0.4"])
      s.add_runtime_dependency(%q<gems>, ["~> 0.8.3"])
      s.add_runtime_dependency(%q<concurrent-ruby>, ["~> 0.9.1"])
      s.add_runtime_dependency(%q<jruby-openssl>, [">= 0.9.11"])
      s.add_runtime_dependency(%q<treetop>, ["< 1.5.0"])
      s.add_runtime_dependency(%q<i18n>, ["= 0.6.9"])
      s.add_runtime_dependency(%q<minitar>, ["~> 0.5.4"])
      s.add_runtime_dependency(%q<thread_safe>, ["~> 0.3.5"])
      s.add_runtime_dependency(%q<jrjackson>, ["~> 0.3.6"])
    else
      s.add_dependency(%q<cabin>, ["~> 0.7.0"])
      s.add_dependency(%q<pry>, ["~> 0.10.1"])
      s.add_dependency(%q<stud>, ["~> 0.0.19"])
      s.add_dependency(%q<clamp>, ["~> 0.6.5"])
      s.add_dependency(%q<filesize>, ["= 0.0.4"])
      s.add_dependency(%q<gems>, ["~> 0.8.3"])
      s.add_dependency(%q<concurrent-ruby>, ["~> 0.9.1"])
      s.add_dependency(%q<jruby-openssl>, [">= 0.9.11"])
      s.add_dependency(%q<treetop>, ["< 1.5.0"])
      s.add_dependency(%q<i18n>, ["= 0.6.9"])
      s.add_dependency(%q<minitar>, ["~> 0.5.4"])
      s.add_dependency(%q<thread_safe>, ["~> 0.3.5"])
      s.add_dependency(%q<jrjackson>, ["~> 0.3.6"])
    end
  else
    s.add_dependency(%q<cabin>, ["~> 0.7.0"])
    s.add_dependency(%q<pry>, ["~> 0.10.1"])
    s.add_dependency(%q<stud>, ["~> 0.0.19"])
    s.add_dependency(%q<clamp>, ["~> 0.6.5"])
    s.add_dependency(%q<filesize>, ["= 0.0.4"])
    s.add_dependency(%q<gems>, ["~> 0.8.3"])
    s.add_dependency(%q<concurrent-ruby>, ["~> 0.9.1"])
    s.add_dependency(%q<jruby-openssl>, [">= 0.9.11"])
    s.add_dependency(%q<treetop>, ["< 1.5.0"])
    s.add_dependency(%q<i18n>, ["= 0.6.9"])
    s.add_dependency(%q<minitar>, ["~> 0.5.4"])
    s.add_dependency(%q<thread_safe>, ["~> 0.3.5"])
    s.add_dependency(%q<jrjackson>, ["~> 0.3.6"])
  end
end
