require "logstash/agent"
require "logstash/pipeline"
require "logstash/event"
require "stud/try"
require "rspec/expectations"
require "thread"

module LogStashHelper
  DEFAULT_NUMBER_OF_TRY = 5
  DEFAULT_EXCEPTIONS_FOR_TRY = [RSpec::Expectations::ExpectationNotMetError]

  def try(number_of_try = DEFAULT_NUMBER_OF_TRY, &block)
    Stud.try(number_of_try.times, DEFAULT_EXCEPTIONS_FOR_TRY, &block)
  end

  def config(configstr)
    let(:config) { configstr }
  end # def config

  def type(default_type)
    let(:default_type) { default_type }
  end

  def tags(*tags)
    let(:default_tags) { tags }
    puts "Setting default tags: #{tags}"
  end

  def sample(sample_event, &block)
    name = sample_event.is_a?(String) ? sample_event : LogStash::Json.dump(sample_event)
    name = name[0..50] + "..." if name.length > 50

    describe "\"#{name}\"" do
      let(:pipeline) { LogStash::Pipeline.new(config) }
      let(:event) do
        sample_event = [sample_event] unless sample_event.is_a?(Array)
        next sample_event.collect do |e|
          e = { "message" => e } if e.is_a?(String)
          next LogStash::Event.new(e)
        end
      end

      let(:results) do
        results = []
        pipeline.instance_eval { @filters.each(&:register) }

        event.each do |e|
          # filter call the block on all filtered events, included new events added by the filter
          pipeline.filter(e) { |filtered_event| results << filtered_event }
        end

        # flush makes sure to empty any buffered events in the filter
        pipeline.flush_filters(:final => true) { |flushed_event| results << flushed_event }

        results.select { |e| !e.cancelled? }
      end

      subject { results.length > 1 ? results : results.first }

      it("when processed", &block)
    end
  end # def sample

  def input(config, &block)
    pipeline = LogStash::Pipeline.new(config)
    queue = Queue.new

    pipeline.instance_eval do
      # create closure to capture queue
      @output_func = lambda { |event| queue << event }

      # output_func is now a method, call closure
      def output_func(event)
        @output_func.call(event)
      end
    end

    pipeline_thread = Thread.new { pipeline.run }
    sleep 0.1 while !pipeline.ready?

    result = block.call(pipeline, queue)

    pipeline.shutdown
    pipeline_thread.join

    result
  end # def input

  def plugin_input(plugin, &block)
    queue = Queue.new

    input_thread = Thread.new do
      plugin.run(queue)
    end
    result = block.call(queue)

    plugin.do_stop
    input_thread.join
    result
  end

  def agent(&block)

    it("agent(#{caller[0].gsub(/ .*/, "")}) runs") do
      pipeline = LogStash::Pipeline.new(config)
      pipeline.run
      block.call
    end
  end # def agent

end # module LogStash

