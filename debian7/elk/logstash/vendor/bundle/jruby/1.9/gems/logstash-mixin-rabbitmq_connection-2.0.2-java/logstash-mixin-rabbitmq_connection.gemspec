Gem::Specification.new do |s|

  s.name            = 'logstash-mixin-rabbitmq_connection'
  s.version         = '2.0.2'
  s.licenses        = ['Apache License (2.0)']
  s.summary         = "Common functionality for RabbitMQ plugins"
  s.description     = "This is used to provide configuration options and connection settings for logstash plugins working with RabbitMQ"
  s.authors         = ["Elastic"]
  s.email           = 'info@elastic.co'
  s.homepage        = "http://www.elastic.co/guide/en/logstash/current/index.html"
  s.require_paths = ["lib"]

  # Files
  s.files = `git ls-files`.split($\)+::Dir.glob('vendor/*')

  # Tests
  s.test_files = s.files.grep(%r{^(test|spec|features)/})

  # Gem dependencies
  s.add_runtime_dependency "logstash-core", ">= 2.0.0.beta2", "< 3.0.0"

  s.platform = RUBY_PLATFORM
  s.add_runtime_dependency 'march_hare', ['~> 2.11.0'] #(MIT license)
  s.add_runtime_dependency 'stud', '~> 0.0.22'

  s.add_development_dependency 'logstash-devutils'
  s.add_development_dependency 'logstash-input-generator'
  s.add_development_dependency 'logstash-codec-json'
end

