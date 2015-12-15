# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"

# This output allows you to write events to https://www.hipchat.com/[HipChat].
#
# Make sure your API token have the appropriate permissions and support
# sending  messages.
class LogStash::Outputs::HipChat < LogStash::Outputs::Base
  config_name "hipchat"

  # The HipChat authentication token.
  config :token, :validate => :string, :required => true

  # The ID or name of the room, support fieldref
  config :room_id, :validate => :string, :required => true

  # The name the message will appear be sent from, you can use fieldref
  config :from, :validate => :string, :default => "logstash"

  # Whether or not this message should trigger a notification for people in the room.
  config :trigger_notify, :validate => :boolean, :default => false

  # Background color for message.
  # HipChat currently supports one of "yellow", "red", "green", "purple",
  # "gray", or "random". (default: yellow), support fieldref
  config :color, :validate => :string, :default => "yellow"

  # Message format to send, event tokens are usable here.
  config :format, :validate => :string, :default => "%{message}"

  # Specify `Message Format`
  config :message_format, :validate => ["html", "text"], :default => "html"

  # HipChat host to use
  config :host, :validate => :string

  public
  def register
    require "hipchat"
  end

  def client
    @client ||= if @host.nil? || @host.empty? 
                  HipChat::Client.new(@token, :api_version => "v2")
                else
                  HipChat::Client.new(@token, :api_version => "v2", :server_url => server_url)
                end
  end

  def server_url
    "https://#{@host}"
  end

  def receive(event)
    

    message = event.sprintf(@format)
    from = event.sprintf(@from)
    color = event.sprintf(@color)
    room = event.sprintf(@room_id)

    @logger.debug("HipChat data", :from => from , :message => message, :notify => trigger_notify, :color => color, :message_format => @message_format) if @logger.debug?

    begin
      client[room].send(from, message, :notify => trigger_notify, :color => color, :message_format => @message_format)
    rescue Exception => e
      logger.warn("Unhandled exception", :exception => e, :stacktrace => e.backtrace)
    end
  end
end
