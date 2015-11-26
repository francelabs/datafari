# -*- encoding: utf-8 -*-
# stub: ffi-rzmq-core 1.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "ffi-rzmq-core"
  s.version = "1.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Chuck Remes"]
  s.date = "2015-09-28"
  s.description = "This gem provides only the FFI wrapper for the ZeroMQ (0mq) networking library.\n    Project can be used by any other zeromq gems that want to provide their own high-level Ruby API."
  s.email = ["git@chuckremes.com"]
  s.homepage = "http://github.com/chuckremes/ffi-rzmq-core"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "This gem provides only the FFI wrapper for the ZeroMQ (0mq) networking library."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ffi>, ["~> 1.9"])
      s.add_development_dependency(%q<rspec>, ["~> 2.14"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<ffi>, ["~> 1.9"])
      s.add_dependency(%q<rspec>, ["~> 2.14"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<ffi>, ["~> 1.9"])
    s.add_dependency(%q<rspec>, ["~> 2.14"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
