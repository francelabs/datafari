## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# ## 2.0.0.beta.1
 - Change to 0.8.2 version of Kafka producer. This will unfortunately break existing configuration, but has a lot of enhancements and fixes.
