require "thread"
require "concurrent"

module LogStash
  # Minimal subset implement of a SizedQueue supporting
  # a timeout option on the lock.
  #
  # This will be part of the main Logstash's sized queue
  class SizedQueueTimeout
    class TimeoutError < StandardError; end

    DEFAULT_TIMEOUT = 5 # in seconds

    def initialize(max_size, options = {})
      # `concurrent-ruby` are deprecating the `Condition`
      # in favor of a Synchonization class that you need to implement.
      # this was bit overkill to only check if the wait did a timeout.
      @condition_in = ConditionVariable.new
      @condition_out = ConditionVariable.new

      @max_size = max_size
      @queue = []
      @mutex = Mutex.new
    end

    def push(obj, timeout = DEFAULT_TIMEOUT)
      @mutex.synchronize do
        while full? # wake up check
          start_time = Concurrent.monotonic_time
          @condition_out.wait(@mutex, timeout) 
          if start_time + timeout - Concurrent.monotonic_time  < 0
            raise TimeoutError
          end
        end

        @queue << obj
        @condition_in.signal

        return obj
      end
    end
    alias_method :<<, :push

    def size
      @mutex.synchronize { @queue.size }
    end

    def pop_no_timeout
      @mutex.synchronize do
        @condition_in.wait(@mutex) while @queue.empty? # Wake up check

        obj = @queue.shift
        @condition_out.signal

        return obj
      end
    end

    private
    def full?
      @queue.size == @max_size
    end
  end
end
