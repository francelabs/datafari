## 3.0.2
 - Bump dependency on logstash-mixin-rabbitmq_connection

## 3.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

* 2.0.0
  - Massive refactor
  - Implement Logstash 2.x stop behavior
  - Fix reconnect issues
  - Depend on rabbitmq_connection mixin for most connection functionality
* 1.1.1
  - Bump march hare to 2.12.0 which fixes jar perms on unices
* 1.1.0
  - Bump march hare version to 2.11.0
