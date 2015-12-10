## 2.0.2
 - Fix the test to work properly within the context of the LS core
   defaults plugin test.

## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# 1.1.0
 - Add basic tests for the project
 - Refactor the plugin to include the option to add an option to inject
   a dummy bot.
 - Refactor the plugin to have a cleaner shutdown methodology, so the
   thread is not stuck waiting in the queue if there is no reason.
