# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"

# The split filter is for splitting multiline messages into separate events.
#
# An example use case of this filter is for taking output from the `exec` input
# which emits one event for the whole output of a command and splitting that
# output by newline - making each line an event.
#
# The end result of each split is a complete copy of the event
# with only the current split section of the given field changed.
class LogStash::Filters::Split < LogStash::Filters::Base

  config_name "split"

  # The string to split on. This is usually a line terminator, but can be any
  # string.
  config :terminator, :validate => :string, :default => "\n"

  # The field which value is split by the terminator
  config :field, :validate => :string, :default => "message"

  # The field within the new event which the value is split into.
  # If not set, target field defaults to split field name.
  config :target, :validate => :string

  public
  def register
    # Nothing to do
  end # def register

  public
  def filter(event)
    

    original_value = event[@field]

    if original_value.is_a?(Array)
      splits = original_value
    elsif original_value.is_a?(String)
      # Using -1 for 'limit' on String#split makes ruby not drop trailing empty
      # splits.
      splits = original_value.split(@terminator, -1)
    else
      raise LogStash::ConfigurationError, "Only String and Array types are splittable. field:#{@field} is of type = #{original_value.class}"
    end

    # Skip filtering if splitting this event resulted in only one thing found.
    return if splits.length == 1
    #or splits[1].empty?

    splits.each do |value|
      next if value.empty?

      event_split = event.clone
      @logger.debug("Split event", :value => value, :field => @field)
      event_split[(@target || @field)] = value
      filter_matched(event_split)

      # Push this new event onto the stack at the LogStash::FilterWorker
      yield event_split
    end

    # Cancel this event, we'll use the newly generated ones above.
    event.cancel
  end # def filter
end # class LogStash::Filters::Split
