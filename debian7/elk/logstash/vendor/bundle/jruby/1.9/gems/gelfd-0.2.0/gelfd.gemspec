# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "gelfd/version"

Gem::Specification.new do |s|
  s.name        = "gelfd"
  s.version     = Gelfd::VERSION
  s.authors     = ["John E. Vincent"]
  s.email       = ["lusis.org+github.com@gmail.com"]
  s.homepage    = ""
  s.summary     = %q{Pure ruby gelf server and decoding library}
  s.description = %q{Standalone implementation of the Graylog Extended Log Format}

  s.rubyforge_project = "gelfd"

  s.files         = `git ls-files`.split("\n")
  s.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  s.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  s.require_paths = ["lib"]

  # specify any dependencies here; for example:
  s.add_development_dependency "json", "~> 1.5.4"
  s.add_development_dependency "rake", "~> 0.9.2"
  # s.add_runtime_dependency "rest-client"
end
