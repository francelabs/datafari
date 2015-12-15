# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"

# Receive events using the lumberjack protocol.
#
# This is mainly to receive events shipped with lumberjack[http://github.com/jordansissel/lumberjack],
# now represented primarily via the
# https://github.com/elasticsearch/logstash-forwarder[Logstash-forwarder].
#
class LogStash::Inputs::Lumberjack < LogStash::Inputs::Base

  config_name "lumberjack"

  default :codec, "plain"

  # The IP address to listen on.
  config :host, :validate => :string, :default => "0.0.0.0"

  # The port to listen on.
  config :port, :validate => :number, :required => true

  # SSL certificate to use.
  config :ssl_certificate, :validate => :path, :required => true

  # SSL key to use.
  config :ssl_key, :validate => :path, :required => true

  # SSL key passphrase to use.
  config :ssl_key_passphrase, :validate => :password

  # This setting no longer has any effect and will be removed in a future release.
  config :max_clients, :validate => :number, :deprecated => "This setting no longer has any effect. See https://github.com/logstash-plugins/logstash-input-lumberjack/pull/12 for the history of this change"
  
  # The number of seconds before we raise a timeout,
  # this option is useful to control how much time to wait if something is blocking the pipeline.
  config :congestion_threshold, :validate => :number, :default => 5

  # TODO(sissel): Add CA to authenticate clients with.
  BUFFERED_QUEUE_SIZE = 1
  RECONNECT_BACKOFF_SLEEP = 0.5
  
  def register
    require "lumberjack/server"
    require "concurrent"
    require "logstash/circuit_breaker"
    require "logstash/sized_queue_timeout"

    @logger.info("Starting lumberjack input listener", :address => "#{@host}:#{@port}")
    @lumberjack = Lumberjack::Server.new(:address => @host, :port => @port,
      :ssl_certificate => @ssl_certificate, :ssl_key => @ssl_key,
      :ssl_key_passphrase => @ssl_key_passphrase)

    # Create a reusable threadpool, we do not limit the number of connections
    # to the input, the circuit breaker with the timeout should take care 
    # of `blocked` threads and prevent logstash to go oom.
    @threadpool = Concurrent::CachedThreadPool.new(:idletime => 15)

    # in 1.5 the main SizeQueue doesnt have the concept of timeout
    # We are using a small plugin buffer to move events to the internal queue
    @buffered_queue = LogStash::SizedQueueTimeout.new(BUFFERED_QUEUE_SIZE)

    @circuit_breaker = LogStash::CircuitBreaker.new("Lumberjack input",
                            :exceptions => [LogStash::SizedQueueTimeout::TimeoutError])

  end # def register

  def run(output_queue)
    start_buffer_broker(output_queue)

    while !stop? do
      # Wrappingu the accept call into a CircuitBreaker
      if @circuit_breaker.closed?
        connection = @lumberjack.accept # call that creates a new connection
        next if connection.nil? # if the connection is nil the connection was close.
        invoke(connection, @codec.clone) do |_codec, line, fields|
          if stop?
            connection.close
            break
          end

          _codec.decode(line) do |event|
            begin
              decorate(event)
              fields.each { |k,v| event[k] = v; v.force_encoding(Encoding::UTF_8) }
              @circuit_breaker.execute { @buffered_queue.push(event, @congestion_threshold) }
            rescue => e
              raise e
            end
          end
        end
      else
        @logger.warn("Lumberjack input: the pipeline is blocked, temporary refusing new connection.")
        sleep(RECONNECT_BACKOFF_SLEEP)
      end
    end
  end # def run

  public
  def stop
    @lumberjack.close
  end

  private
  def invoke(connection, codec, &block)
    @threadpool.post do
      begin
        # If any errors occur in from the events the connection should be closed in the 
        # library ensure block and the exception will be handled here
        connection.run do |fields|
          block.call(codec, fields.delete("line"), fields)
        end

        # When too many errors happen inside the circuit breaker it will throw 
        # this exception and start refusing connection. The bubbling of theses
        # exceptions make sure that the lumberjack library will close the current 
        # connection which will force the client to reconnect and restransmit
        # his payload.
      rescue LogStash::CircuitBreaker::OpenBreaker,
        LogStash::CircuitBreaker::HalfOpenBreaker => e
        logger.warn("Lumberjack input: The circuit breaker has detected a slowdown or stall in the pipeline, the input is closing the current connection and rejecting new connection until the pipeline recover.", :exception => e.class)
      rescue => e # If we have a malformed packet we should handle that so the input doesn't crash completely.
        @logger.error("Lumberjack input: unhandled exception", :exception => e, :backtrace => e.backtrace)
      end
    end
  end

  def start_buffer_broker(output_queue)
    @threadpool.post do
      while !stop?
        output_queue << @buffered_queue.pop_no_timeout
      end
    end
  end
end # class LogStash::Inputs::Lumberjack
