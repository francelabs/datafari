# -*- encoding: utf-8 -*-
# stub: bindata 2.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "bindata"
  s.version = "2.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Dion Mendel"]
  s.date = "2014-04-16"
  s.description = "BinData is a declarative way to read and write binary file formats.\n\nThis means the programmer specifies *what* the format of the binary\ndata is, and BinData works out *how* to read and write data in this\nformat.  It is an easier ( and more readable ) alternative to\nruby's #pack and #unpack methods.\n"
  s.email = "dion@lostrealm.com"
  s.extra_rdoc_files = ["NEWS.rdoc"]
  s.files = ["NEWS.rdoc"]
  s.homepage = "http://github.com/dmendel/bindata"
  s.licenses = ["Ruby"]
  s.rdoc_options = ["--main", "NEWS.rdoc"]
  s.rubyforge_project = "bindata"
  s.rubygems_version = "2.4.8"
  s.summary = "A declarative way to read and write binary file formats"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<minitest>, ["> 5.0.0"])
      s.add_development_dependency(%q<coveralls>, [">= 0"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<minitest>, ["> 5.0.0"])
      s.add_dependency(%q<coveralls>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<minitest>, ["> 5.0.0"])
    s.add_dependency(%q<coveralls>, [">= 0"])
  end
end
