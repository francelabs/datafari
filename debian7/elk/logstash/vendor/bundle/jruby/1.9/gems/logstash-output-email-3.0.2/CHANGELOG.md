## 3.0.0
 - Plugins were updated to follow the new shutdown semantic, this mainly allows Logstash to instruct input plugins to terminate gracefully, 
   instead of using Thread.raise on the plugins' threads. Ref: https://github.com/elastic/logstash/pull/3895
 - Dependency on logstash-core update to 2.0

# 2.0.0
  - Introduced new configuration options for the smtp server, the
    options option is gone and now you need to specify each option
    independetly. This require you to change your previous configuration
    when updating.
  - Removed the deprecated option match. This option was deprecatred in
    favor of using conditionals. This change also require you to change
    your current cnofiguration if using this option.

# 1.1.0
  - Make the delivery method more reliable to failure by catching and
    logging exceptions when happen, like this LS is not going to break
    if something wrong happen, but is going to log it. Fixes #26 and #7
  - Randomize port in specs so they can run in parallel.
