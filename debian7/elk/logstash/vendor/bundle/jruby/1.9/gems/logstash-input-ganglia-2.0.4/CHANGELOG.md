## 2.0.3
 - Fix test to be able to run withint the LS core default plugins
   integration system.

## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# 2.0.1
   - Fix the test to run without issues of fetching required filed in
     a the logstash integration test.
# 1.0.1
   - Add test to the plugin
   - Fix dependencies and a bug with event generation to make the plugin
     stable again.
