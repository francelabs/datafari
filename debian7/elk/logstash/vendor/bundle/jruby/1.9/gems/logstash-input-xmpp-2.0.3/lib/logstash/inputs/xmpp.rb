# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"

require 'xmpp4r' # xmpp4r gem
# load the MUC Client anyway, its mocked in testing
require 'xmpp4r/muc/helper/simplemucclient'

# This input allows you to receive events over XMPP/Jabber.
#
# This plugin can be used for accepting events from humans or applications
# XMPP, or you can use it for PubSub or general message passing for logstash to
# logstash.
class LogStash::Inputs::Xmpp < LogStash::Inputs::Base

  config_name "xmpp"

  default :codec, "plain"

  # The user or resource ID, like `foo@example.com`.
  config :user, :validate => :string, :required => :true

  # The xmpp password for the user/identity.
  config :password, :validate => :password, :required => :true

  # if muc/multi-user-chat required, give the name of the room that
  # you want to join: `room@conference.domain/nick`
  config :rooms, :validate => :array

  # The xmpp server to connect to. This is optional. If you omit this setting,
  # the host on the user/identity is used. (`foo.com` for `user@foo.com`)
  config :host, :validate => :string

  # Set to true to enable greater debugging in XMPP. Useful for debugging
  # network/authentication erros.
  config :debug, :validate => :boolean, :default => false, :deprecated => "Use the logstash --debug flag for this instead."

  public

  def initialize(config)
    super
    @client = Jabber::Client.new(Jabber::JID.new(@user))
    @muc_clients = []
  end

  # and for testing access
  attr_reader :client, :muc_clients

  def register
    Jabber::debug = true if @debug || @logger.debug?
    client.connect(@host) # it is ok if host is nil
    client.auth(@password.value)
    client.send(Jabber::Presence.new.set_type(:available))
  end # def register


  def run(queue)
    if using_rooms?
      @rooms.each do |room| # handle muc messages in different rooms
        muc = Jabber::MUC::SimpleMUCClient.new(client)
        muc.join(room)
        muc.on_message do |time, from, body|
          @codec.decode(body) do |event|
            decorate(event)
            event["room"] = room
            event["from"] = from
            queue << event
          end
        end # @muc.on_message
        # we need to hold a reference to the muc
        # otherwise it will be GC'd
        muc_clients.push(muc)
      end # @rooms.each
    end # if @rooms

    client.add_message_callback do |msg| # handle direct/private messages
      # accept normal msgs (skip presence updates, etc)
      if msg.body != nil
        @codec.decode(msg.body) do |event|
          decorate(event)
          # Maybe "from" should just be a hash:
          # { "node" => ..., "domain" => ..., "resource" => ... }
          event["from"] = "#{msg.from.node}@#{msg.from.domain}/#{msg.from.resource}"
          queue << event
        end
      end
    end # @client.add_message_callback

    # [GUY] the two clients: muc and client maintain their own threads
    # and will call the blocks in those threads so this one should sleep
    # this is how the xmpp4r examples do it (not saying it is correct)
    Stud.stoppable_sleep(Float::INFINITY) { stop? }

    client.close
  end # def run

  private

  def using_rooms?
    @rooms && !@rooms.empty?
  end
end # class LogStash::Inputs::Xmpp
