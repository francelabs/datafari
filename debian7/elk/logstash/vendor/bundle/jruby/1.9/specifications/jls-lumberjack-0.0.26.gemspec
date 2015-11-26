# -*- encoding: utf-8 -*-
# stub: jls-lumberjack 0.0.26 ruby lib

Gem::Specification.new do |s|
  s.name = "jls-lumberjack"
  s.version = "0.0.26"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Jordan Sissel"]
  s.date = "2015-10-19"
  s.description = "lumberjack log transport library"
  s.email = ["jls@semicomplete.com"]
  s.homepage = "https://github.com/jordansissel/lumberjack"
  s.rubygems_version = "2.4.8"
  s.summary = "lumberjack log transport library"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<concurrent-ruby>, [">= 0"])
      s.add_development_dependency(%q<flores>, ["~> 0.0.6"])
      s.add_development_dependency(%q<rspec>, [">= 0"])
      s.add_development_dependency(%q<stud>, [">= 0"])
      s.add_development_dependency(%q<pry>, [">= 0"])
      s.add_development_dependency(%q<rspec-wait>, [">= 0"])
    else
      s.add_dependency(%q<concurrent-ruby>, [">= 0"])
      s.add_dependency(%q<flores>, ["~> 0.0.6"])
      s.add_dependency(%q<rspec>, [">= 0"])
      s.add_dependency(%q<stud>, [">= 0"])
      s.add_dependency(%q<pry>, [">= 0"])
      s.add_dependency(%q<rspec-wait>, [">= 0"])
    end
  else
    s.add_dependency(%q<concurrent-ruby>, [">= 0"])
    s.add_dependency(%q<flores>, ["~> 0.0.6"])
    s.add_dependency(%q<rspec>, [">= 0"])
    s.add_dependency(%q<stud>, [">= 0"])
    s.add_dependency(%q<pry>, [">= 0"])
    s.add_dependency(%q<rspec-wait>, [">= 0"])
  end
end
