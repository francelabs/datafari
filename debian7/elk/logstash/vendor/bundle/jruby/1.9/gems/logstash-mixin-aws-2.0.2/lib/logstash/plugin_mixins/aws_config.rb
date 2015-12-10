# encoding: utf-8
require "logstash/config/mixin"

# This module provides helper for the `AWS-SDK` v1,
# and it will be deprecated in the near future, please use the V2 module
# for any new development.
module LogStash::PluginMixins::AwsConfig
  require "logstash/plugin_mixins/aws_config/v1"
  require "logstash/plugin_mixins/aws_config/v2"

  US_EAST_1 = "us-east-1"
  REGIONS_ENDPOINT = [US_EAST_1, "us-west-1", "us-west-2", "eu-central-1",
                      "eu-west-1", "ap-southeast-1", "ap-southeast-2",
                      "ap-northeast-1", "sa-east-1", "us-gov-west-1", "cn-north-1"]

  def self.included(base)
    # Add these methods to the 'base' given.
    base.send(:include, V1)
  end
end
