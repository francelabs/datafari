# -*- encoding: utf-8 -*-
# stub: avl_tree 1.2.1 ruby lib

Gem::Specification.new do |s|
  s.name = "avl_tree"
  s.version = "1.2.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Hiroshi Nakamura"]
  s.date = "2014-09-28"
  s.email = "nahi@ruby-lang.org"
  s.homepage = "http://github.com/nahi/avl_tree"
  s.rubygems_version = "2.4.8"
  s.summary = "AVL tree, Red black tree and Lock-free Red black tree in Ruby"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<atomic>, ["~> 1.1"])
    else
      s.add_dependency(%q<atomic>, ["~> 1.1"])
    end
  else
    s.add_dependency(%q<atomic>, ["~> 1.1"])
  end
end
