# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "ffi-rzmq-core/version"

Gem::Specification.new do |s|
  s.name        = "ffi-rzmq-core"
  s.version     = LibZMQ::VERSION
  s.authors     = ["Chuck Remes"]
  s.email       = ["git@chuckremes.com"]
  s.homepage    = "http://github.com/chuckremes/ffi-rzmq-core"
  s.summary     = %q{This gem provides only the FFI wrapper for the ZeroMQ (0mq) networking library.}
  s.description = %q{This gem provides only the FFI wrapper for the ZeroMQ (0mq) networking library.
    Project can be used by any other zeromq gems that want to provide their own high-level Ruby API.}

  s.license = 'MIT'

  s.files         = `git ls-files`.split("\n")
  s.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  s.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  s.require_paths = ["lib"]

  s.add_runtime_dependency "ffi", ["~> 1.9"]
  s.add_development_dependency "rspec", ["~> 2.14"]
  s.add_development_dependency "rake"
end
