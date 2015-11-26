# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"
require "thread"
require "stud/task"
require "stud/interval"
# Read events from an IRC Server.
#
class LogStash::Inputs::Irc < LogStash::Inputs::Base

  config_name "irc"

  default :codec, "plain"

  # Host of the IRC Server to connect to.
  config :host, :validate => :string, :required => true

  # Port for the IRC Server
  config :port, :validate => :number, :default => 6667

  # Set this to true to enable SSL.
  config :secure, :validate => :boolean, :default => false

  # IRC Nickname
  config :nick, :validate => :string, :default => "logstash"

  # IRC Username
  config :user, :validate => :string, :default => "logstash"

  # IRC Real name
  config :real, :validate => :string, :default => "logstash"

  # IRC Server password
  config :password, :validate => :password

  # Catch all IRC channel/user events not just channel messages
  config :catch_all, :validate => :boolean, :default => false

  # Gather and send user counts for channels - this requires catch_all and will force it
  config :get_stats, :validate => :boolean, :default => false

  # How often in minutes to get the user count stats
  config :stats_interval, :validate => :number, :default => 5

  # Channels to join and read messages from.
  #
  # These should be full channel names including the '#' symbol, such as
  # "#logstash".
  #
  # For passworded channels, add a space and the channel password, such as
  # "#logstash password".
  #
  config :channels, :validate => :array, :required => true

  public

  def inject_bot(bot)
    @bot = bot
    self
  end

  def bot
    @bot
  end

  def register
    require "cinch"
    @user_stats = Hash.new
    @irc_queue = Queue.new
    @catch_all = true if  @get_stats
    @logger.info("Connecting to irc server", :host => @host, :port => @port, :nick => @nick, :channels => @channels)

    @bot ||= Cinch::Bot.new
    @bot.loggers.clear
    @bot.configure do |c|
      c.server = @host
      c.port = @port
      c.nick = @nick
      c.user = @user
      c.realname = @real
      c.channels = @channels
      c.password = @password.value rescue nil
      c.ssl.use = @secure
    end
    queue = @irc_queue
    if @catch_all
        @bot.on :catchall  do |m|
          queue << m
        end
    else
      @bot.on :channel  do |m|
        queue << m
      end
    end
  end # def register

  public
  def run(output_queue)
    @bot_thread = Stud::Task.new(@bot) do |bot|
      bot.start
    end
    if @get_stats
      @request_names_thread = Stud::Task.new do
        while !stop?
          Stud.stoppable_sleep (@stats_interval * 60) do
            stop?
          end
          request_names
        end
      end
    end
    while !stop?
      begin
        msg = @irc_queue.pop(true)
        handle_response(msg, output_queue)
      rescue ThreadError
        # Empty queue
      end
    end
  end # def run

  RPL_NAMREPLY = "353"
  RPL_ENDOFNAMES = "366"

  def handle_response (msg, output_queue)
      # Set some constant variables based on https://www.alien.net.au/irc/irc2numerics.html

      if @get_stats and msg.command.to_s == RPL_NAMREPLY
        # Got a names list event
        # Count the users returned in msg.params[3] split by " "
        users = msg.params[3].split(" ")
        @user_stats[msg.channel.to_s] = (@user_stats[msg.channel.to_s] || 0)  + users.length
      end
      if @get_stats and msg.command.to_s == RPL_ENDOFNAMES
        # Got an end of names event, now we can send the info down the pipe.
        event = LogStash::Event.new()
        decorate(event)
        event["channel"] = msg.channel.to_s
        event["users"] = @user_stats[msg.channel.to_s]
        event["server"] = "#{@host}:#{@port}"
        output_queue << event
      end
      if msg.command and msg.user
        @logger.debug("IRC Message", :data => msg)
        @codec.decode(msg.message) do |event|
          decorate(event)
          event["user"] = msg.prefix.to_s
          event["command"] = msg.command.to_s
          event["channel"] = msg.channel.to_s
          event["nick"] = msg.user.nick
          event["server"] = "#{@host}:#{@port}"
          event["host"] = msg.user.host
          output_queue << event
        end
      end
  end

  def request_names
    # Go though list of channels, and request a NAMES for them
    # Note : Logstash channel list can have passwords ie : "channel password"
    # Need to account for that
    @channels.each do |channel|
        channel = channel.split(' ').first if channel.include?(' ')
        @user_stats[channel] = 0
        @bot.irc.send("NAMES #{channel}")
    end
  end

  def stop
    @request_names_thread.stop! if @request_names_thread
    @bot_thread.stop!
  end
end # class LogStash::Inputs::Irc
