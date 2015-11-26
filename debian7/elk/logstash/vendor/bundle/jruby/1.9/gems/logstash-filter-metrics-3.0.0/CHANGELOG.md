## 3.0.0
 - Elasticsearch 2.0 does not allow for dots in field names.  This change changes to use sub-field syntax instead of
 dotted syntax.  This is a breaking change.

## 2.0.2
 - Fix test that used deprecated "tags" syntax

## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully,
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0
