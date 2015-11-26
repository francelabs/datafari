require "thread"
require "cabin"

module LogStash
  # Largely inspired by Martin's fowler circuit breaker
  class CircuitBreaker
    # Raised when too many errors has occured and we refuse to execute the block
    class OpenBreaker < StandardError; end

    # Raised when we catch an error that we count
    class HalfOpenBreaker < StandardError; end

    # Error threshold before opening the breaker,
    # if the breaker is open it wont execute the code.
    DEFAULT_ERROR_THRESHOLD = 5

    # Recover time after the breaker is open to start
    # executing the method again.
    DEFAULT_TIME_BEFORE_RETRY = 30

    # Exceptions catched by the circuit breaker,
    # too much errors and the breaker will trip.
    DEFAULT_EXCEPTION_RESCUED = [StandardError]

    def initialize(name, options = {}, &block)
      @exceptions = Array(options.fetch(:exceptions, [StandardError]))
      @error_threshold = options.fetch(:error_threshold, DEFAULT_ERROR_THRESHOLD)
      @time_before_retry = options.fetch(:time_before_retry, DEFAULT_TIME_BEFORE_RETRY)
      @block = block
      @name = name
      @mutex = Mutex.new
      reset
    end

    def execute(args = nil)
      case state
      when :open
        logger.warn("CircuitBreaker::Open", :name => @name)
        raise OpenBreaker, "for #{@name}"
      when :close, :half_open
        if block_given?
          yield args
        else
          @block.call(args)
        end

        if state == :half_open
          logger.warn("CircuitBreaker::Close", :name => @name)
          reset
        end
      end
    rescue *@exceptions => e
      logger.warn("CircuitBreaker::rescuing exceptions", :name => @name, :exception => e.class)
      increment_errors(e)

      raise HalfOpenBreaker
    end

    def closed?
      state == :close || state == :half_open
    end

    private
    def logger
      @logger ||= Cabin::Channel.get(LogStash)
    end

    def reset
      @mutex.synchronize do
        @errors_count = 0
        @last_failure_time = nil
      end
    end

    def increment_errors(exception)
      @mutex.synchronize do
        @errors_count += 1
        @last_failure_time = Time.now

        logger.debug("CircuitBreaker increment errors", 
                     :errors_count => @errors_count, 
                     :error_threshold => @error_threshold,
                     :exception => exception.class,
                     :message => exception.message) if logger.debug?
      end
    end

    def state
      @mutex.synchronize do
        if @errors_count >= @error_threshold
          if Time.now - @last_failure_time > @time_before_retry
            :half_open
          else
            :open
          end
        else
          :close
        end
      end
    end
  end
end
