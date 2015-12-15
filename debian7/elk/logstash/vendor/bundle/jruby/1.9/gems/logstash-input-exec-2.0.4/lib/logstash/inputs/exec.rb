# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"
require "socket" # for Socket.gethostname
require "stud/interval"

# Periodically run a shell command and capture the whole output as an event.
#
# Notes:
#
# * The `command` field of this event will be the command run.
# * The `message` field of this event will be the entire stdout of the command.
#
class LogStash::Inputs::Exec < LogStash::Inputs::Base

  config_name "exec"

  default :codec, "plain"

  # Set this to true to enable debugging on an input.
  config :debug, :validate => :boolean, :default => false, :deprecated => "This setting was never used by this plugin. It will be removed soon."

  # Command to run. For example, `uptime`
  config :command, :validate => :string, :required => true

  # Interval to run the command. Value is in seconds.
  config :interval, :validate => :number, :required => true

  def register
    @logger.info("Registering Exec Input", :type => @type, :command => @command, :interval => @interval)
    @hostname = Socket.gethostname
    @io       = nil
  end # def register

  def run(queue)
    while !stop?
      inner_run(queue)
    end # loop
  end # def run

  def inner_run(queue)
    start = Time.now
    execute(@command, queue)
    duration = Time.now - start

    @logger.info? && @logger.info("Command completed", :command => @command, :duration => duration)

    wait_until_end_of_interval(duration)
  end

  def stop
    return if @io.nil? || @io.closed?
    @io.close
    @io = nil
  end

  private

  # Wait until the end of the interval
  # @param [Integer] the duration of the last command executed
  def wait_until_end_of_interval(duration)
    # Sleep for the remainder of the interval, or 0 if the duration ran
    # longer than the interval.
    sleeptime = [0, @interval - duration].max
    if sleeptime > 0
      Stud.stoppable_sleep(sleeptime) { stop? }
    else
      @logger.warn("Execution ran longer than the interval. Skipping sleep.",
                   :command => @command, :duration => duration, :interval => @interval)
    end
  end

  # Execute a given command
  # @param [String] A command string
  # @param [Array or Queue] A queue to append events to
  def execute(command, queue)
    @logger.info? && @logger.info("Running exec", :command => command)
    begin
      @io = IO.popen(command)
      @codec.decode(@io.read) do |event|
        decorate(event)
        event["host"]    = @hostname
        event["command"] = command
        queue << event
      end
    rescue StandardError => e
      @logger.error("Error while running command",
        :command => command, :e => e, :backtrace => e.backtrace)
    rescue Exception => e
      @logger.error("Exception while running command",
        :command => command, :e => e, :backtrace => e.backtrace)
    ensure
      stop
    end
  end
end # class LogStash::Inputs::Exec
