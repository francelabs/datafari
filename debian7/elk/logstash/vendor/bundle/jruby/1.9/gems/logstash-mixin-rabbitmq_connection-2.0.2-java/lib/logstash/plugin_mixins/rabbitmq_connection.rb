# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"
require "march_hare"
require "java"

# Common functionality for the rabbitmq input/output
module LogStash
  module PluginMixins
    module RabbitMQConnection
      EXCHANGE_TYPES = ["fanout", "direct", "topic"]

      HareInfo = Struct.new(:connection, :channel, :exchange, :queue)

      def self.included(base)
        base.extend(self)
        base.setup_rabbitmq_connection_config
      end

      def setup_rabbitmq_connection_config
        # RabbitMQ server address
        config :host, :validate => :string, :required => true

        # RabbitMQ port to connect on
        config :port, :validate => :number, :default => 5672

        # RabbitMQ username
        config :user, :validate => :string, :default => "guest"

        # RabbitMQ password
        config :password, :validate => :password, :default => "guest"

        # The vhost to use. If you don't know what this is, leave the default.
        config :vhost, :validate => :string, :default => "/"

        # Enable or disable SSL
        config :ssl, :validate => :boolean, :default => false

        # Validate SSL certificate
        config :verify_ssl, :validate => :boolean, :default => false

        # Enable or disable logging
        config :debug, :validate => :boolean, :default => false, :deprecated => "Use the logstash --debug flag for this instead."

        # Set this to automatically recover from a broken connection. You almost certainly don't want to override this!!!
        config :automatic_recovery, :validate => :boolean, :default => true

        # Time in seconds to wait before retrying a connection
        config :connect_retry_interval, :validate => :number, :default => 1

        # Passive queue creation? Useful for checking queue existance without modifying server state
        config :passive, :validate => :boolean, :default => false

        # Extra queue arguments as an array.
        # To make a RabbitMQ queue mirrored, use: `{"x-ha-policy" => "all"}`
        config :arguments, :validate => :array, :default => {}
      end

      def conn_str
        "amqp://#{@user}@#{@host}:#{@port}#{@vhost}"
      end

      def close_connection
        @rabbitmq_connection_stopping = true
        @hare_info.channel.close if channel_open?
        @hare_info.connection.close if connection_open?
      end

      def rabbitmq_settings
        return @rabbitmq_settings if @rabbitmq_settings

        s = {
          :vhost => @vhost,
          :host  => @host,
          :port  => @port,
          :user  => @user,
          :automatic_recovery => @automatic_recovery,
          :pass => @password ? @password.value : "guest",
        }
        s[:tls] = @ssl if @ssl
        @rabbitmq_settings = s
      end

      def connect!
        @hare_info = connect() unless @hare_info # Don't duplicate the conn!
      rescue MarchHare::Exception => e
        @logger.error("RabbitMQ connection error, will retry.",
                      :message => e.message,
                      :exception => e.class.name,
                      :backtrace => e.backtrace)

        sleep_for_retry
        retry
      end

      def channel_open?
        @hare_info && @hare_info.channel && @hare_info.channel.open?
      end

      def connection_open?
        @hare_info && @hare_info.connection && @hare_info.connection.open?
      end

      def connected?
        return nil unless @hare_info && @hare_info.connection
        @hare_info.connection.connected?
      end

      private

      def declare_exchange!(channel, exchange, exchange_type, durable)
        @logger.debug? && @logger.debug("Declaring an exchange", :name => exchange,
                      :type => exchange_type, :durable => durable)
        exchange = channel.exchange(exchange, :type => exchange_type.to_sym, :durable => durable)
        @logger.debug? && @logger.debug("Exchange declared")
        exchange
      rescue StandardError => e
        @logger.error("Could not declare exchange!",
                      :exchange => exchange, :type => exchange_type,
                      :durable => durable, :error_class => e.class.name,
                      :error_message => e.message, :backtrace => e.backtrace)
        raise e
      end

      def connect
        @logger.debug? && @logger.debug("Connecting to RabbitMQ. Settings: #{rabbitmq_settings.inspect}")

        connection = MarchHare.connect(rabbitmq_settings)
        connection.on_blocked { @logger.warn("RabbitMQ output blocked! Check your RabbitMQ instance!") }
        connection.on_unblocked { @logger.warn("RabbitMQ output unblocked!") }

        channel = connection.create_channel
        @logger.info("Connected to RabbitMQ at #{rabbitmq_settings[:host]}")

        HareInfo.new(connection, channel)
      end

      def sleep_for_retry
        Stud.stoppable_sleep(@connect_retry_interval) { @rabbitmq_connection_stopping }
      end
    end
  end
end