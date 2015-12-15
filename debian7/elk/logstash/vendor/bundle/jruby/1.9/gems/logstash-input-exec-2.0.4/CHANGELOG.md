## 2.0.3
 - fix masked errors due to rescue Exception
 - fix random race condition on closing io object
 - refactor code for more reliable tests
## 2.0.1
 - Replace non-whitespace character in code

## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully,
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0
