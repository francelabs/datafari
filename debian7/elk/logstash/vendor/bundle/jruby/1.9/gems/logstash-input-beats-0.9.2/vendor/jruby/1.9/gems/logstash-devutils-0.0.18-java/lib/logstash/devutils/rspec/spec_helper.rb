if ENV['COVERAGE']
  require 'simplecov'
  require 'coveralls'

  SimpleCov.formatter = SimpleCov::Formatter::MultiFormatter[
    SimpleCov::Formatter::HTMLFormatter,
    Coveralls::SimpleCov::Formatter
  ]
  SimpleCov.start do
    add_filter 'spec/'
    add_filter 'vendor/'
  end
end

require "logstash/logging"
require "logstash/environment"
require "logstash/devutils/rspec/logstash_helpers"
require "logstash/devutils/rspec/shared_examples"
require "insist"

Thread.abort_on_exception = true

$TESTING = true
if RUBY_VERSION < "1.9.2"
  $stderr.puts "Ruby 1.9.2 or later is required. (You are running: " + RUBY_VERSION + ")"
  raise LoadError
end

if ENV["TEST_DEBUG"]
  $logger = LogStash::Logger.new(STDOUT)
  $logger.level = :debug
end

puts("Using Accessor#strict_set for specs")
# mokey path LogStash::Event to use strict_set in tests
# ugly, I know, but this avoids adding conditionals in performance critical section
class LogStash::Event
  alias_method :setval, :[]=
  def []=(str, value)
    if str == TIMESTAMP && !value.is_a?(LogStash::Timestamp)
      raise TypeError, "The field '@timestamp' must be a LogStash::Timestamp, not a #{value.class} (#{value})"
    end
    LogStash::Event.validate_value(value)
    setval(str, value)
  end # def []=
end

RSpec.configure do |config|
  # for now both include and extend are required because the newly refactored "input" helper method need to be visible in a "it" block
  # and this is only possible by calling include on LogStashHelper
  config.include LogStashHelper
  config.extend LogStashHelper

  exclude_tags = { :redis => true, :socket => true, :performance => true, :couchdb => true, :elasticsearch => true, :elasticsearch_secure => true, :export_cypher => true, :integration => true }

  if LogStash::Environment.windows?
    exclude_tags[:unix] = true
  else
    exclude_tags[:windows] = true
  end

  config.filter_run_excluding exclude_tags

  # Run specs in random order to surface order dependencies. If you find an
  # order dependency and want to debug it, you can fix the order by providing
  # the seed, which is printed after each run.
  #     --seed 1234
  config.order = :random
end

