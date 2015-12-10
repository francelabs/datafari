require 'java'
require 'jruby-kafka/namespace'

# noinspection JRubyStringImportInspection
class Kafka::Consumer
  java_import 'kafka.consumer.ConsumerIterator'
  java_import 'kafka.consumer.KafkaStream'
  java_import 'kafka.common.ConsumerRebalanceFailedException'
  java_import 'kafka.consumer.ConsumerTimeoutException'

  include Java::JavaLang::Runnable
  java_signature 'void run()'

  def initialize(a_stream, a_thread_number, a_queue, restart_on_exception, a_sleep_ms)
    @m_thread_number = a_thread_number
    @m_stream = a_stream
    @m_queue = a_queue
    @m_restart_on_exception = restart_on_exception
    @m_sleep_ms = 1.0 / 1000.0 * Float(a_sleep_ms)
  end

  def run
    it = @m_stream.iterator
    begin
      while it.hasNext
        begin
          @m_queue << it.next
        end
      end
    rescue Exception => e
      puts("#{self.class.name} caught exception: #{e.class.name}")
      puts(e.message) if e.message != ''
      if @m_restart_on_exception
        sleep(@m_sleep_ms)
        retry
      else
        raise e
      end
    end
  end
end
