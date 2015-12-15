if RUBY_PLATFORM != "java"
  raise "Only JRuby is supported"
end

Gem::Specification.new do |spec|
  files = %x{git ls-files}.split("\n")

  spec.name = "logstash-devutils"
  spec.version = "0.0.18"
  spec.summary = "logstash-devutils"
  spec.description = "logstash-devutils"
  spec.license = "Apache 2.0"

  spec.files = files
  spec.require_paths << "lib"

  spec.authors = ["Jordan Sissel"]
  spec.email = ["jls@semicomplete.com"]
  spec.homepage = "https://github.com/elasticsearch/logstash-devutils"

  spec.add_runtime_dependency "rspec", "~> 3.1.0" # MIT License
  spec.add_runtime_dependency "rspec-wait" # MIT License
  spec.platform = "java"
  spec.add_runtime_dependency "rake" # MIT License
  spec.add_runtime_dependency "gem_publisher" # MIT License
  spec.add_runtime_dependency "minitar" # GPL2|Ruby License

  # Should be removed as soon as the plugins are using insist by their
  # own, and not relying on being required by the spec helper.
  # (some plugins does it, some use insist throw spec_helper)
  spec.add_runtime_dependency "insist", "1.0.0" # (Apache 2.0 license)
  spec.add_runtime_dependency "kramdown"
  spec.add_runtime_dependency "stud", " >= 0.0.20"
end
