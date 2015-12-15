# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"
require "logstash/environment"
require "logstash/patterns/core"
require "set"
#
# This filter will collapse multiline messages from a single source into one Logstash event.
#
# The original goal of this filter was to allow joining of multi-line messages
# from files into a single event. For example - joining java exception and
# stacktrace messages into a single event.
#
# NOTE: This filter will not work with multiple worker threads `-w 2` on the logstash command line.
#
# The config looks like this:
# [source,ruby]
#     filter {
#       multiline {
#         type => "type"
#         pattern => "pattern, a regexp"
#         negate => boolean
#         what => "previous" or "next"
#       }
#     }
#
# The `pattern` should be a regexp which matches what you believe to be an indicator
# that the field is part of an event consisting of multiple lines of log data.
#
# The `what` must be `previous` or `next` and indicates the relation
# to the multi-line event.
#
# The `negate` can be `true` or `false` (defaults to `false`). If `true`, a
# message not matching the pattern will constitute a match of the multiline
# filter and the `what` will be applied. (vice-versa is also true)
#
# For example, Java stack traces are multiline and usually have the message
# starting at the far-left, with each subsequent line indented. Do this:
# [source,ruby]
#     filter {
#       multiline {
#         type => "somefiletype"
#         pattern => "^\s"
#         what => "previous"
#       }
#     }
#
# This says that any line starting with whitespace belongs to the previous line.
#
# Another example is C line continuations (backslash). Here's how to do that:
# [source,ruby]
#     filter {
#       multiline {
#         type => "somefiletype "
#         pattern => "\\$"
#         what => "next"
#       }
#     }
#
# This says that any line ending with a backslash should be combined with the
# following line.
#
class LogStash::Filters::Multiline < LogStash::Filters::Base

  config_name "multiline"

  # The field name to execute the pattern match on.
  config :source, :validate => :string, :default => "message"

  # Allow duplcate values on the source field.
  config :allow_duplicates, :validate => :boolean, :default => true

  # The regular expression to match.
  config :pattern, :validate => :string, :required => true

  # If the pattern matched, does event belong to the next or previous event?
  config :what, :validate => ["previous", "next"], :required => true

  # Negate the regexp pattern ('if not matched')
  config :negate, :validate => :boolean, :default => false

  # The stream identity is how the multiline filter determines which stream an
  # event belongs to. This is generally used for differentiating, say, events
  # coming from multiple files in the same file input, or multiple connections
  # coming from a tcp input.
  #
  # The default value here is usually what you want, but there are some cases
  # where you want to change it. One such example is if you are using a tcp
  # input with only one client connecting at any time. If that client
  # reconnects (due to error or client restart), then logstash will identify
  # the new connection as a new stream and break any multiline goodness that
  # may have occurred between the old and new connection. To solve this use
  # case, you can use `%{@source_host}.%{@type}` instead.
  config :stream_identity , :validate => :string, :default => "%{host}.%{path}.%{type}"

  # Logstash ships by default with a bunch of patterns, so you don't
  # necessarily need to define this yourself unless you are adding additional
  # patterns.
  #
  # Pattern files are plain text with format:
  # [source,ruby]
  #     NAME PATTERN
  #
  # For example:
  # [source,ruby]
  #     NUMBER \d+
  config :patterns_dir, :validate => :array, :default => []

  # The maximum age an event can be (in seconds) before it is automatically
  # flushed.
  config :max_age, :validate => :number, :default => 5

  # Call the filter flush method at regular interval.
  # Optional.
  config :periodic_flush, :validate => :boolean, :default => true

  # Register default pattern paths
  @@patterns_path = Set.new
  @@patterns_path += [LogStash::Patterns::Core.path]

  MULTILINE_TAG = "multiline"

  public
  def initialize(config = {})
    super

    # this filter cannot be parallelized because message order
    # cannot be garanteed across threads, line #2 could be processed
    # before line #1
    @threadsafe = false

    # this filter needs to keep state
    @pending = Hash.new
  end # def initialize

  public
  def register
    require "grok-pure" # rubygem 'jls-grok'

    @grok = Grok.new

    @patterns_dir = @@patterns_path.to_a + @patterns_dir
    @patterns_dir.each do |path|
      path = File.join(path, "*") if File.directory?(path)
      Dir.glob(path).each do |file|
        @logger.info("Grok loading patterns from file", :path => file)
        @grok.add_patterns_from_file(file)
      end
    end

    @grok.compile(@pattern)

    case @what
    when "previous"
      class << self; alias_method :multiline_filter!, :previous_filter!; end
    when "next"
      class << self; alias_method :multiline_filter!, :next_filter!; end
    else
      # we should never get here since @what is validated at config
      raise(ArgumentError, "Unknown multiline 'what' value")
    end # case @what

    @logger.debug("Registered multiline plugin", :type => @type, :config => @config)
  end # def register

  public
  def filter(event)
    

    match = event[@source].is_a?(Array) ? @grok.match(event[@source].first) : @grok.match(event[@source])
    match = (match && !@negate) || (!match && @negate) # add negate option

    @logger.debug? && @logger.debug("Multiline", :pattern => @pattern, :message => event[@source], :match => match, :negate => @negate)

    multiline_filter!(event, match)

    filter_matched(event) unless event.cancelled?
  end # def filter

  # flush any pending messages
  # called at regular interval without options and at pipeline shutdown with the :final => true option
  # @param options [Hash]
  # @option options [Boolean] :final => true to signal a final shutdown flush
  # @return [Array<LogStash::Event>] list of flushed events
  public
  def flush(options = {})
    # note that thread safety concerns are not necessary here because the multiline filter
    # is not thread safe thus cannot be run in multiple filterworker threads and flushing
    # is called by the same thread

    # select all expired events from the @pending hash into a new expired hash
    # if :final flush then select all events
    expired = @pending.inject({}) do |result, (key, events)|
      unless events.empty?
        age = Time.now - events.first["@timestamp"].time
        result[key] = events if (age >= @max_age) || options[:final]
      end
      result
    end

    # return list of uncancelled expired events
    expired.map do |key, events|
      @pending.delete(key)
      event = merge(events)
      event.uncancel
      filter_matched(event)
      event
    end
  end # def flush

  public
  def close
    # nothing to do
  end

  private

  def previous_filter!(event, match)
    key = event.sprintf(@stream_identity)
    pending = @pending[key] ||= []

    if match
      # previous previous line is part of this event. append it to the event and cancel it
      event.tag(MULTILINE_TAG)
      pending << event
      event.cancel
    else
      # this line is not part of the previous event if we have a pending event, it's done, send it.
      # put the current event into pending
      unless pending.empty?
        tmp = event.to_hash
        event.overwrite(merge(pending))
        pending.clear # avoid array creation
        pending << LogStash::Event.new(tmp)
      else
        pending.clear # avoid array creation
        pending << event
        event.cancel
      end
    end # if match
  end

  def next_filter!(event, match)
    key = event.sprintf(@stream_identity)
    pending = @pending[key] ||= []

    if match
      # this line is part of a multiline event, the next line will be part, too, put it into pending.
      event.tag(MULTILINE_TAG)
      pending << event
      event.cancel
    else
      # if we have something in pending, join it with this message and send it.
      # otherwise, this is a new message and not part of multiline, send it.
      unless pending.empty?
        event.overwrite(merge(pending << event))
        pending.clear
      end
    end # if match
  end

  # merge a list of events. @timestamp for the resulting merged event will be from
  # the "oldest" (events.first). all @source fields will be deduplicated depending
  # on @allow_duplicates and joined with \n. all other fields will be deduplicated.
  # @param events [Array<Event>] the list of events to merge
  # @return [Event] the resulting merged event
  def merge(events)
    dups_key = @allow_duplicates ? @source : nil

    data = events.inject({}) do |result, event|
      self.class.event_hash_merge!(result, event.to_hash_with_metadata, dups_key)
    end

    # merged event @timestamp is from first event in sequence
    data["@timestamp"] = Array(data["@timestamp"]).first
    # collapse all @source field values
    data[@source] = Array(data[@source]).join("\n")
    LogStash::Event.new(data)
  end

  # merge two events data hash, src into dst and handle duplicate values for dups_key
  # @param dst [Hash] the event to merge into, dst will be mutated
  # @param src [Hash] the event to merge in dst
  # @param dups_key [String] the field key to keep duplicate values
  # @return [Hash] mutated dst
  def self.event_hash_merge!(dst, src, dups_key = nil)
    src.each do |key, svalue|
      dst[key] = if dst.has_key?(key)
        dvalue = dst[key]

        if dvalue.is_a?(Hash) && svalue.is_a?(Hash)
          event_hash_merge!(dvalue, svalue, dups_key)
        else
          v = (dups_key == key) ? Array(dvalue) + Array(svalue) : Array(dvalue) | Array(svalue)
          # the v result is always an Array, if none of the fields were arrays and there is a
          # single value in the array, return the value, not the array
          dvalue.is_a?(Array) || svalue.is_a?(Array) ? v : (v.size == 1 ? v.first : v)
        end
      else
        svalue
      end
    end

    dst
  end # def self.hash_merge

end # class LogStash::Filters::Multiline
