## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

## 1.0.3 (September 2, 2015)
* Include remote host address to events (#25)

## 1.0.2 (July 28, 2015)
* Fix for missing base64 require which was crashing Logstash (#17)

## 1.0.0 (July 1, 2015)

* First version: New input to receive HTTP requests
* Added basic authentication and SSL support
