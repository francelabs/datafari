## 3.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# 1.0.1
  * Properly trim messages for AWS without breaking unicode byte boundaries

# 1.0.0
  * Full refactor. 
  * This plugin now uses codecs for all formatting. The 'format' option has now been removed. Please use a codec.
# 0.1.5
  * If no `subject` are specified fallback to the %{host} key (https://github.com/logstash-plugins/logstash-output-sns/pull/2)
  * Migrate the SNS Api to use the AWS-SDK v2
