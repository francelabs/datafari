# -*- encoding: utf-8 -*-
# stub: spoon 0.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "spoon"
  s.version = "0.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Charles Oliver Nutter"]
  s.date = "2013-03-29"
  s.description = "Spoon is an FFI binding of the posix_spawn function (and Windows equivalent), providing fork+exec functionality in a single shot."
  s.rubygems_version = "2.4.8"
  s.summary = "Spoon is an FFI binding of the posix_spawn function (and Windows equivalent), providing fork+exec functionality in a single shot."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ffi>, [">= 0"])
    else
      s.add_dependency(%q<ffi>, [">= 0"])
    end
  else
    s.add_dependency(%q<ffi>, [">= 0"])
  end
end
