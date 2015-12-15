## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

## 1.0.2 (September 3 - 2015)
 - fix scan/scroll response handling

## 1.0.1
 - refactor request logic into own method (better memory gc perf)
