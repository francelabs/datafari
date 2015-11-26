# basically we are porting this https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+Producer+Example
require 'jruby-kafka/namespace'
require 'jruby-kafka/error'

# noinspection JRubyStringImportInspection
class Kafka::Producer
  extend Gem::Deprecate
  java_import 'kafka.producer.ProducerConfig'
  java_import 'kafka.producer.KeyedMessage'
  KAFKA_PRODUCER = Java::kafka.javaapi.producer.Producer
  java_import 'kafka.message.NoCompressionCodec'
  java_import 'kafka.message.GZIPCompressionCodec'
  java_import 'kafka.message.SnappyCompressionCodec'

  VALIDATIONS = {
    :'request.required.acks' => %w[ 0 1 -1 ],
    :'required.codecs' => [NoCompressionCodec.name, GZIPCompressionCodec.name, SnappyCompressionCodec.name],
    :'producer.type' => %w[ sync async ]
  }

  REQUIRED = %w[
    metadata.broker.list
  ]

  # List of all available options extracted from http://kafka.apache.org/documentation.html#producerconfigs Apr. 27, 2014
  # If new options are added, they should just work. Please add them to the list so that we can get handy warnings.
  KNOWN = %w[
    metadata.broker.list      request.required.acks         request.timeout.ms
    producer.type             serializer.class              key.serializer.class
    partitioner.class         compression.codec             compressed.topics
    message.send.max.retries  retry.backoff.ms              topic.metadata.refresh.interval.ms
    queue.buffering.max.ms    queue.buffering.max.messages  queue.enqueue.timeout.ms
    batch.num.messages        send.buffer.bytes             client.id
    broker.list               serializer.encoding
  ]

  attr_reader :producer, :send_method, :options

  # Create a Kafka Producer
  #
  # options:
  # metadata_broker_list: ["localhost:9092"] - REQUIRED: a seed list of kafka brokers
  def initialize(opts = {})
    @options = opts.reduce({}) do |opts_array, (k, v)|
      unless v.nil?
        opts_array[k.to_s.gsub(/_/, '.')] = v
      end
      opts_array
    end
    if options['broker.list']
      options['metadata.broker.list'] = options.delete 'broker.list'
    end
    if options['metadata.broker.list'].is_a? Array
      options['metadata.broker.list'] = options['metadata.broker.list'].join(',')
    end
    if options['compressed.topics'].is_a? Array
      options['compressed.topics'] = options['compressed.topics'].join(',')
    end
    validate_arguments
    @send_method = proc { throw StandardError.new 'Producer is not connected' }
  end

  def connect
    @producer = KAFKA_PRODUCER.new(create_producer_config)
    @send_method = producer.java_method :send, [KeyedMessage]
  end

  # throws FailedToSendMessageException or if not connected, StandardError.
  def send_msg(topic, key, msg)
    send_method.call(KeyedMessage.new(topic, key, msg))
  end

  def sendMsg(topic, key, msg)
    send_msg(topic, key, msg)
  end
  deprecate :sendMsg, :send_msg, 2015, 01

  def close
    @producer.close
  end

  private

  def validate_arguments
    errors = []
    missing = REQUIRED.reject { |opt| options[opt] }
    errors = ["Required settings: #{ missing.join(', ')}"] if missing.any?
    invalid = VALIDATIONS.reject { |opt, valid| options[opt].nil? or valid.include? options[opt].to_s }
    errors += invalid.map { |opt, valid| "#{ opt } should be one of: [#{ valid.join(', ')}]" }
    fail StandardError.new "Invalid configuration arguments: #{ errors.join('; ') }" if errors.any?
    options.keys.each do |opt|
      STDERR.puts "WARNING: Unknown configuration key: #{opt}" unless KNOWN.include? opt
    end
  end

  def create_producer_config
    properties = java.util.Properties.new
    options.each { |opt, value| properties.put opt, value.to_s }
    ProducerConfig.new(properties)
  end
end
