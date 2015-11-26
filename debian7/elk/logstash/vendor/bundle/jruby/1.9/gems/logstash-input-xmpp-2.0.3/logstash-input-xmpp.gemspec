Gem::Specification.new do |s|

  s.name            = 'logstash-input-xmpp'
  s.version         = '2.0.3'
  s.licenses        = ['Apache License (2.0)']
  s.summary         = "This input allows you to receive events over XMPP/Jabber."
  s.description     = "This gem is a logstash plugin required to be installed on top of the Logstash core pipeline using $LS_HOME/bin/plugin install gemname. This gem is not a stand-alone program"
  s.authors         = ["Elastic"]
  s.email           = 'info@elastic.co'
  s.homepage        = "http://www.elastic.co/guide/en/logstash/current/index.html"
  s.require_paths = ["lib"]

  # Files
  s.files = Dir['lib/**/*','spec/**/*','vendor/**/*','*.gemspec','*.md','CONTRIBUTORS','Gemfile','LICENSE','NOTICE.TXT']

  # Tests
  s.test_files = s.files.grep(%r{^(test|spec|features)/})

  # Special flag to let us know this is actually a logstash plugin
  s.metadata = { "logstash_plugin" => "true", "logstash_group" => "input" }

  # Gem dependencies
  s.add_runtime_dependency "logstash-core", ">= 2.0.0.beta2", "< 3.0.0"

  s.add_runtime_dependency 'logstash-codec-plain'
  s.add_runtime_dependency 'xmpp4r', ['0.5']

  s.add_development_dependency 'logstash-devutils'
end

