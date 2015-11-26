# -*- encoding: utf-8 -*-
# stub: ffi-rzmq 2.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "ffi-rzmq"
  s.version = "2.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Chuck Remes"]
  s.date = "2015-01-28"
  s.description = "This gem wraps the ZeroMQ networking library using the ruby FFI (foreign\nfunction interface). It's a pure ruby wrapper so this gem can be loaded\nand run by any ruby runtime that supports FFI. That's all of the major ones - MRI, Rubinius and JRuby."
  s.email = ["git@chuckremes.com"]
  s.homepage = "http://github.com/chuckremes/ffi-rzmq"
  s.licenses = ["MIT"]
  s.rubyforge_project = "ffi-rzmq"
  s.rubygems_version = "2.4.8"
  s.summary = "This gem wraps the ZeroMQ (0mq) networking library using Ruby FFI (foreign function interface)."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ffi-rzmq-core>, [">= 1.0.1"])
      s.add_development_dependency(%q<rspec>, ["~> 2.14"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<ffi-rzmq-core>, [">= 1.0.1"])
      s.add_dependency(%q<rspec>, ["~> 2.14"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<ffi-rzmq-core>, [">= 1.0.1"])
    s.add_dependency(%q<rspec>, ["~> 2.14"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
