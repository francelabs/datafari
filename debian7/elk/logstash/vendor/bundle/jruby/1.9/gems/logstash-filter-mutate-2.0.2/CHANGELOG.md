## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

## 1.0.2
 - Fix for uppercase and lowercase fail when value is already desired case
 - Modify tests to prove bug and verify fix.

## 1.0.1
 - Fix for uppercase and lowercase malfunction
 - Specific test to prove bug and fix.
