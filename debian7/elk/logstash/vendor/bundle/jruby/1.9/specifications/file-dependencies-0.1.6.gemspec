# -*- encoding: utf-8 -*-
# stub: file-dependencies 0.1.6 ruby lib

Gem::Specification.new do |s|
  s.name = "file-dependencies"
  s.version = "0.1.6"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Richard Pijnenburg"]
  s.date = "2015-02-18"
  s.description = "manage file dependencies for gems"
  s.email = ["richard.pijnenburg@elasticsearch.com"]
  s.executables = ["file-deps"]
  s.files = ["bin/file-deps"]
  s.homepage = "https://github.com/electrical/file-dependencies"
  s.licenses = ["APACHE 2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "manage file dependencies for gems"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<minitar>, [">= 0"])
      s.add_development_dependency(%q<rake>, ["~> 10.2"])
      s.add_development_dependency(%q<rspec>, [">= 0"])
      s.add_development_dependency(%q<stud>, [">= 0"])
      s.add_development_dependency(%q<webmock>, [">= 0"])
      s.add_development_dependency(%q<coveralls>, [">= 0"])
      s.add_development_dependency(%q<codeclimate-test-reporter>, [">= 0"])
    else
      s.add_dependency(%q<minitar>, [">= 0"])
      s.add_dependency(%q<rake>, ["~> 10.2"])
      s.add_dependency(%q<rspec>, [">= 0"])
      s.add_dependency(%q<stud>, [">= 0"])
      s.add_dependency(%q<webmock>, [">= 0"])
      s.add_dependency(%q<coveralls>, [">= 0"])
      s.add_dependency(%q<codeclimate-test-reporter>, [">= 0"])
    end
  else
    s.add_dependency(%q<minitar>, [">= 0"])
    s.add_dependency(%q<rake>, ["~> 10.2"])
    s.add_dependency(%q<rspec>, [">= 0"])
    s.add_dependency(%q<stud>, [">= 0"])
    s.add_dependency(%q<webmock>, [">= 0"])
    s.add_dependency(%q<coveralls>, [">= 0"])
    s.add_dependency(%q<codeclimate-test-reporter>, [">= 0"])
  end
end
