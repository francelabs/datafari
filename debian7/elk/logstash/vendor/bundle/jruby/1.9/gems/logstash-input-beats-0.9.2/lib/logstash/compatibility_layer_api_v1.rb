# encoding: utf-8
require "logstash/version"
require "gems"

# This module allow this plugin to work with the v1 API.
module LogStash::CompatibilityLayerApiV1
  LOGSTASH_CORE_VERSION = Gem::Version.new(LOGSTASH_VERSION)
  V2_VERSION = Gem::Version.new("2.0.0.beta2")

  def self.included(base)
    base.send(:include, InstanceMethods) if self.is_v1?
  end

  def self.is_v1?
    LOGSTASH_CORE_VERSION < V2_VERSION
  end
  
  # This allow this plugin to work both in V1 and v2 of logstash-core
  module InstanceMethods
    def stop?
      false
    end

    def teardown
      stop
    end
  end
end
