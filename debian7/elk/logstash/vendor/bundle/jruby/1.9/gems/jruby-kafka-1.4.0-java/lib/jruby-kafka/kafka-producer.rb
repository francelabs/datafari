require 'jruby-kafka/namespace'
require 'jruby-kafka/error'

# noinspection JRubyStringImportInspection
class Kafka::KafkaProducer
  java_import 'org.apache.kafka.clients.producer.ProducerRecord'
  KAFKA_PRODUCER = Java::org.apache.kafka.clients.producer.KafkaProducer

  VALIDATIONS = {
      :'required.codecs' => %w[
      none gzip snappy lz4
    ]
  }

  REQUIRED = %w[
    bootstrap.servers key.serializer
  ]

  KNOWN = %w[
    acks                      batch.size                block.on.buffer.full
    bootstrap.servers         buffer.memory             client.id
    compression.type          key.serializer            linger.ms
    max.in.flight.requests.per.connection               max.request.size
    metadata.fetch.timeout.ms metadata.max.age.ms       metric.reporters
    metrics.num.samples       metrics.sample.window.ms  receive.buffer.bytes
    reconnect.backoff.ms      retries                   retry.backoff.ms
    send.buffer.bytes         timeout.ms                value.serializer
  ]

  attr_reader :producer, :send_method, :options

  def initialize(opts = {})
    @options = opts.reduce({}) do |opts_array, (k, v)|
      unless v.nil?
        opts_array[k.to_s.gsub(/_/, '.')] = v
      end
      opts_array
    end
    validate_arguments
    @send_method = proc { throw StandardError.new 'Producer is not connected' }
  end

  def connect
    @producer = KAFKA_PRODUCER.new(create_producer_config)
    @send_method = producer.java_method :send, [ProducerRecord]
  end

  # throws FailedToSendMessageException or if not connected, StandardError.
  def send_msg(topic, partition, key, value)
    send_method.call(ProducerRecord.new(topic, partition, key, value))
  end

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
    properties
  end
end
