# -*- encoding: utf-8 -*-
# stub: elasticsearch 1.0.14 ruby lib

Gem::Specification.new do |s|
  s.name = "elasticsearch"
  s.version = "1.0.14"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Karel Minarik"]
  s.date = "2015-10-14"
  s.description = "Ruby integrations for Elasticsearch (client, API, etc.)\n"
  s.email = ["karel.minarik@elasticsearch.org"]
  s.extra_rdoc_files = ["README.md", "LICENSE.txt"]
  s.files = ["LICENSE.txt", "README.md"]
  s.homepage = "http://github.com/elasticsearch/elasticsearch-ruby"
  s.licenses = ["Apache 2"]
  s.rdoc_options = ["--charset=UTF-8"]
  s.rubygems_version = "2.4.8"
  s.summary = "Ruby integrations for Elasticsearch"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<elasticsearch-transport>, ["= 1.0.14"])
      s.add_runtime_dependency(%q<elasticsearch-api>, ["= 1.0.14"])
      s.add_development_dependency(%q<bundler>, ["> 1"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<elasticsearch-extensions>, [">= 0"])
      s.add_development_dependency(%q<ansi>, [">= 0"])
      s.add_development_dependency(%q<shoulda-context>, [">= 0"])
      s.add_development_dependency(%q<mocha>, [">= 0"])
      s.add_development_dependency(%q<turn>, [">= 0"])
      s.add_development_dependency(%q<yard>, [">= 0"])
      s.add_development_dependency(%q<pry>, [">= 0"])
      s.add_development_dependency(%q<ci_reporter>, ["~> 1.9"])
      s.add_development_dependency(%q<minitest>, ["~> 4.0"])
      s.add_development_dependency(%q<ruby-prof>, [">= 0"])
      s.add_development_dependency(%q<require-prof>, [">= 0"])
      s.add_development_dependency(%q<simplecov>, [">= 0"])
      s.add_development_dependency(%q<simplecov-rcov>, [">= 0"])
      s.add_development_dependency(%q<cane>, [">= 0"])
    else
      s.add_dependency(%q<elasticsearch-transport>, ["= 1.0.14"])
      s.add_dependency(%q<elasticsearch-api>, ["= 1.0.14"])
      s.add_dependency(%q<bundler>, ["> 1"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<elasticsearch-extensions>, [">= 0"])
      s.add_dependency(%q<ansi>, [">= 0"])
      s.add_dependency(%q<shoulda-context>, [">= 0"])
      s.add_dependency(%q<mocha>, [">= 0"])
      s.add_dependency(%q<turn>, [">= 0"])
      s.add_dependency(%q<yard>, [">= 0"])
      s.add_dependency(%q<pry>, [">= 0"])
      s.add_dependency(%q<ci_reporter>, ["~> 1.9"])
      s.add_dependency(%q<minitest>, ["~> 4.0"])
      s.add_dependency(%q<ruby-prof>, [">= 0"])
      s.add_dependency(%q<require-prof>, [">= 0"])
      s.add_dependency(%q<simplecov>, [">= 0"])
      s.add_dependency(%q<simplecov-rcov>, [">= 0"])
      s.add_dependency(%q<cane>, [">= 0"])
    end
  else
    s.add_dependency(%q<elasticsearch-transport>, ["= 1.0.14"])
    s.add_dependency(%q<elasticsearch-api>, ["= 1.0.14"])
    s.add_dependency(%q<bundler>, ["> 1"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<elasticsearch-extensions>, [">= 0"])
    s.add_dependency(%q<ansi>, [">= 0"])
    s.add_dependency(%q<shoulda-context>, [">= 0"])
    s.add_dependency(%q<mocha>, [">= 0"])
    s.add_dependency(%q<turn>, [">= 0"])
    s.add_dependency(%q<yard>, [">= 0"])
    s.add_dependency(%q<pry>, [">= 0"])
    s.add_dependency(%q<ci_reporter>, ["~> 1.9"])
    s.add_dependency(%q<minitest>, ["~> 4.0"])
    s.add_dependency(%q<ruby-prof>, [">= 0"])
    s.add_dependency(%q<require-prof>, [">= 0"])
    s.add_dependency(%q<simplecov>, [">= 0"])
    s.add_dependency(%q<simplecov-rcov>, [">= 0"])
    s.add_dependency(%q<cane>, [">= 0"])
  end
end
