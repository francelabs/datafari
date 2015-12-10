## 2.0.3
 - removed usage of RSpec.configure, see https://github.com/logstash-plugins/logstash-input-tcp/pull/21
## 2.0.2
 - refactored & cleaned up plugin structure, see https://github.com/logstash-plugins/logstash-input-tcp/pull/18
## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully,
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0
