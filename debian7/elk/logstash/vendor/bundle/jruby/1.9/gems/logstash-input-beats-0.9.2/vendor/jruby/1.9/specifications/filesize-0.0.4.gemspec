# -*- encoding: utf-8 -*-
# stub: filesize 0.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "filesize"
  s.version = "0.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Dominik Honnef"]
  s.date = "2009-07-26"
  s.description = "filesize is a small class for handling filesizes with both the SI and binary prefixes, allowing conversion from any size to any other size."
  s.email = "dominikh@fork-bomb.org"
  s.extra_rdoc_files = ["README.markdown", "CHANGELOG", "lib/filesize.rb"]
  s.files = ["CHANGELOG", "README.markdown", "lib/filesize.rb"]
  s.homepage = "http://filesize.rubyforge.org/"
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.6")
  s.rubyforge_project = "filesize"
  s.rubygems_version = "2.4.8"
  s.summary = "filesize is a small class for handling filesizes with both the SI and binary prefixes, allowing conversion from any size to any other size."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rspec>, ["~> 3.0"])
    else
      s.add_dependency(%q<rspec>, ["~> 3.0"])
    end
  else
    s.add_dependency(%q<rspec>, ["~> 3.0"])
  end
end
