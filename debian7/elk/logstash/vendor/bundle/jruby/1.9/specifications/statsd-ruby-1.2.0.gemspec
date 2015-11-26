# -*- encoding: utf-8 -*-
# stub: statsd-ruby 1.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "statsd-ruby"
  s.version = "1.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Rein Henrichs"]
  s.date = "2013-01-04"
  s.description = "A Ruby StatsD client (https://github.com/etsy/statsd)"
  s.email = "reinh@reinh.com"
  s.extra_rdoc_files = ["LICENSE.txt", "README.rdoc"]
  s.files = ["LICENSE.txt", "README.rdoc"]
  s.homepage = "https://github.com/reinh/statsd"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "A Ruby StatsD client"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<minitest>, [">= 3.2.0"])
      s.add_development_dependency(%q<yard>, [">= 0"])
      s.add_development_dependency(%q<simplecov>, [">= 0.6.4"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<minitest>, [">= 3.2.0"])
      s.add_dependency(%q<yard>, [">= 0"])
      s.add_dependency(%q<simplecov>, [">= 0.6.4"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<minitest>, [">= 3.2.0"])
    s.add_dependency(%q<yard>, [">= 0"])
    s.add_dependency(%q<simplecov>, [">= 0.6.4"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
