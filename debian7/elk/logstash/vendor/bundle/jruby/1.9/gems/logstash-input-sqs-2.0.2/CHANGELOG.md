## 2.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# 1.1.0
- AWS ruby SDK v2 upgrade
- Replaces aws-sdk dependencies with mixin-aws
- Removes unnecessary de-allocation
- Move the code into smaller methods to allow easier mocking and testing
- Add the option to configure polling frequency
- Adding a monkey patch to make sure `LogStash::ShutdownSignal` doesn't get catch by AWS RetryError.
