#!/usr/bin/ruby


$:.unshift File::dirname(__FILE__) + '/../../lib'

require 'test/unit'
require File::dirname(__FILE__) + '/../lib/clienttester'
require 'xmpp4r/muc'
require 'xmpp4r/semaphore'
include Jabber

class MUCClientTest < Test::Unit::TestCase
  include ClientTester

  def test_new1
    m = MUC::MUCClient.new(@client)
    assert_equal(nil, m.jid)
    assert_equal(nil, m.my_jid)
    assert_equal({}, m.roster)
    assert(!m.active?)
  end

  # JEP-0045: 6.3 Entering a Room
  def test_enter_room
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/thirdwitch'), pres.to)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='400' type='modify'>" +
           "<jid-malformed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
           "</error></presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/thirdwitch'), pres.to)
      send("<presence from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda'>" +
          "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='owner' role='moderator'/></x>" +
          "</presence>" +
          "<presence from='darkcave@macbeth.shakespeare.lit/secondwitch' to='hag66@shakespeare.lit/pda'>" +
          "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='admin' role='moderator'/></x>" +
          "</presence>" +
          "<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
          "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
          "</presence>")
    }


    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert(!m.active?)
    assert_nil(m.room)

    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    wait_state
    assert(!m.active?)
    assert_nil(m.room)

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    wait_state
    assert(m.active?)
    assert_equal('darkcave', m.room)
    assert_equal(3, m.roster.size)
    m.roster.each { |resource,pres|
      assert_equal(resource, pres.from.resource)
      assert_equal('darkcave', pres.from.node)
      assert_equal('macbeth.shakespeare.lit', pres.from.domain)
      assert_kind_of(String, resource)
      assert_kind_of(Presence, pres)
      assert(%w(firstwitch secondwitch thirdwitch).include?(resource))
      assert_kind_of(MUC::XMUCUser, pres.x)
      assert_kind_of(Array, pres.x.items)
      assert_equal(1, pres.x.items.size)
    }
    assert_equal(:owner, m.roster['firstwitch'].x.items[0].affiliation)
    assert_equal(:moderator, m.roster['firstwitch'].x.items[0].role)
    assert_equal(:admin, m.roster['secondwitch'].x.items[0].affiliation)
    assert_equal(:moderator, m.roster['secondwitch'].x.items[0].role)
    assert_equal(:member, m.roster['thirdwitch'].x.items[0].affiliation)
    assert_equal(:participant, m.roster['thirdwitch'].x.items[0].role)
    assert_nil(m.roster['thirdwitch'].x.items[0].jid)

    send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='crone1@shakespeare.lit/desktop'>" +
         "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='none' jid='hag66@shakespeare.lit/pda' role='participant'/></x>" +
         "</presence>")
    n = 0
    while m.roster.size != 3 and n < 1000
      Thread::pass
      n += 1
    end
    assert_equal(3, m.roster.size)
    assert_equal(:none, m.roster['thirdwitch'].x.items[0].affiliation)
    assert_equal(:participant, m.roster['thirdwitch'].x.items[0].role)
    assert_equal(JID.new('hag66@shakespeare.lit/pda'), m.roster['thirdwitch'].x.items[0].jid)
  end

  def test_enter_room_password
    state { |pres|
      assert_kind_of(Presence, pres)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='401' type='auth'><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal('cauldron', pres.x.password)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    wait_state
    assert(!m.active?)

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch', 'cauldron'))
    wait_state
    assert(m.active?)
  end

  def test_members_only_room
    state { |pres|
      assert_kind_of(Presence, pres)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='407' type='auth'><registration-required xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    assert(!m.active?)

    wait_state
  end

  def test_banned_users
    state { |pres|
      assert_kind_of(Presence, pres)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='403' type='auth'><forbidden xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    assert(!m.active?)

    wait_state
  end

  def test_nickname_conflict
    state { |pres|
      assert_kind_of(Presence, pres)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='409' type='cancel'><conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    assert(!m.active?)

    wait_state
  end

  def test_max_users
    state { |pres|
      assert_kind_of(Presence, pres)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='503' type='wait'><service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    assert(!m.active?)

    wait_state
  end

  def test_locked_room
    state { |pres|
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='404' type='cancel'><item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_raises(ServerError) {
      m.join('darkcave@macbeth.shakespeare.lit/thirdwitch')
    }
    assert(!m.active?)
    wait_state
  end

  def test_exit_room
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.type)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(:unavailable, pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_nil(pres.status)
      send("<presence from='darkcave@macbeth.shakespeare.lit/secondwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda' type='unavailable'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='none'/></x>" +
           "</presence>")
    }

    ignored_stanzas = 0
    @client.add_stanza_callback { |stanza|
      ignored_stanzas += 1
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_equal(0, ignored_stanzas)
    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    wait_state
    assert(m.active?)

    assert_equal(0, ignored_stanzas)
    assert_equal(m, m.exit)
    wait_state
    assert(!m.active?)
    assert_equal(1, ignored_stanzas)
  end

  def test_custom_exit_message
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.type)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(:unavailable, pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal('gone where the goblins go', pres.status)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda' type='unavailable'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='none'/></x>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    assert(m.active?)
    wait_state

    assert_equal(m, m.exit('gone where the goblins go'))
    assert(!m.active?)
    wait_state
  end

  def test_joins
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/thirdwitch'), pres.to)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(:unavailable, pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/thirdwitch'), pres.to)
      assert_nil(pres.status)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda' type='unavailable'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='none'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/fourthwitch'), pres.to)
      send("<presence from='darkcave@macbeth.shakespeare.lit/fourthwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(:unavailable, pres.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/fourthwitch'), pres.to)
      assert_equal(pres.status, 'Exiting one last time')
      send("<presence from='darkcave@macbeth.shakespeare.lit/fourthwitch' to='hag66@shakespeare.lit/pda' type='unavailable'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='none'/></x>" +
           "</presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    wait_state
    assert(m.active?)

    assert_raises(RuntimeError) { m.join('darkcave@macbeth.shakespeare.lit/thirdwitch') }
    assert_raises(RuntimeError) { m.join('darkcave@macbeth.shakespeare.lit/fourthwitch') }
    assert(m.active?)

    assert_equal(m, m.exit)
    wait_state
    assert(!m.active?)
    assert_raises(RuntimeError) { m.exit }
    assert(!m.active?)

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/fourthwitch'))
    wait_state
    assert(m.active?)

    assert_raises(RuntimeError) { m.join('darkcave@macbeth.shakespeare.lit/thirdwitch') }
    assert_raises(RuntimeError) { m.join('darkcave@macbeth.shakespeare.lit/fourthwitch') }
    assert(m.active?)

    assert_equal(m, m.exit('Exiting one last time'))
    wait_state
    assert(!m.active?)
    assert_raises(RuntimeError) { m.exit }
    assert(!m.active?)
  end

  def test_message_callback
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal('cauldron', pres.x.password)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }

    message_lock = Semaphore.new

    messages_client = 0
    @client.add_message_callback { |msg|
      messages_client += 1
      message_lock.run
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    messages_muc = 0
    m.add_message_callback { |msg|
      messages_muc += 1
      message_lock.run
    }
    messages_muc_private = 0
    m.add_private_message_callback { |msg|
      messages_muc_private += 1
      message_lock.run
    }

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch', 'cauldron'))
    assert(m.active?)

    assert_equal(0, messages_client)
    assert_equal(0, messages_muc)
    assert_equal(0, messages_muc_private)

    send("<message from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda'><body>Hello</body></message>")
    message_lock.wait

    assert_equal(0, messages_client)
    assert_equal(1, messages_muc)
    assert_equal(0, messages_muc_private)

    send("<message from='user@domain/resource' to='hag66@shakespeare.lit/pda'><body>Hello</body></message>")
    message_lock.wait

    assert_equal(1, messages_client)
    assert_equal(1, messages_muc)
    assert_equal(0, messages_muc_private)

    send("<message type='chat' from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda'><body>Hello</body></message>")
    message_lock.wait

    assert_equal(1, messages_client)
    assert_equal(1, messages_muc)
    assert_equal(1, messages_muc_private)

    wait_state
  end

  def test_presence_callbacks
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.x.password)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }

    presence_lock = Semaphore.new

    presences_client = 0
    @client.add_presence_callback { |pres|
      presences_client += 1
      presence_lock.run
    }
    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'
    presences_join = 0
    m.add_join_callback { |pres|
      presences_join += 1
      presence_lock.run
    }
    presences_leave = 0
    m.add_leave_callback { |pres|
      presences_leave += 1
      presence_lock.run
    }
    presences_muc = 0
    m.add_presence_callback { |pres|
      presences_muc += 1
      presence_lock.run
    }

    assert_equal(0, presences_client)
    assert_equal(0, presences_join)
    assert_equal(0, presences_leave)
    assert_equal(0, presences_muc)

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    assert(m.active?)

    assert_equal(0, presences_client)
    assert_equal(0, presences_join) # Joins before own join won't be called back
    assert_equal(0, presences_leave)
    assert_equal(0, presences_muc)

    send("<presence from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda'>" +
         "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
         "</presence>")
    presence_lock.wait
    assert_equal(0, presences_client)
    assert_equal(1, presences_join)
    assert_equal(0, presences_leave)
    assert_equal(0, presences_muc)

    send("<presence from='user@domain/resource' to='hag66@shakespeare.lit/pda'>" +
         "<show>chat</show>" +
         "</presence>")
    presence_lock.wait
    assert_equal(1, presences_client)
    assert_equal(1, presences_join)
    assert_equal(0, presences_leave)
    assert_equal(0, presences_muc)

    send("<presence from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda'>" +
         "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
         "<show>away</show></presence>")
    presence_lock.wait
    assert_equal(1, presences_client)
    assert_equal(1, presences_join)
    assert_equal(0, presences_leave)
    assert_equal(1, presences_muc)

    send("<presence from='darkcave@macbeth.shakespeare.lit/firstwitch' to='hag66@shakespeare.lit/pda' type='unavailable'/>")
    presence_lock.wait
    assert_equal(1, presences_client)
    assert_equal(1, presences_join)
    assert_equal(1, presences_leave)
    assert_equal(1, presences_muc)
    wait_state
  end

  def test_send
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.x.password)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |stanza|
      assert_kind_of(Message, stanza)
      assert(:groupchat, stanza.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), stanza.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit'), stanza.to)
      assert_equal('First message', stanza.body)
    }
    state { |stanza|
      assert_kind_of(Message, stanza)
      assert(:chat, stanza.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), stanza.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/secondwitch'), stanza.to)
      assert_equal('Second message', stanza.body)
    }
    state { |stanza|
      assert_kind_of(Message, stanza)
      assert(:chat, stanza.type)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), stanza.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/firstwitch'), stanza.to)
      assert_equal('Third message', stanza.body)
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    wait_state
    assert(m.active?)

    m.send(Jabber::Message.new(nil, 'First message'))
    wait_state
    m.send(Jabber::Message.new(nil, 'Second message'), 'secondwitch')
    wait_state
    m.send(Jabber::Message.new('secondwitch', 'Third message'), 'firstwitch')
    wait_state
  end

  def test_nick
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_nil(pres.x.password)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member' role='participant'/></x>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/secondwitch'), pres.to)
      assert_nil(pres.type)
      send("<presence from='darkcave@macbeth.shakespeare.lit' to='hag66@shakespeare.lit/pda' type='error'>" +
           "<error code='409' type='cancel'><conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>" +
           "</presence>")
    }
    state { |pres|
      assert_kind_of(Presence, pres)
      assert_equal(JID.new('hag66@shakespeare.lit/pda'), pres.from)
      assert_equal(JID.new('darkcave@macbeth.shakespeare.lit/oldhag'), pres.to)
      assert_nil(pres.type)
      send("<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda' type='unavailable'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'>" +
           "<item affiliation='member' jid='hag66@shakespeare.lit/pda' nick='oldhag' role='participant'/>" +
           "<status code='303'/>" +
           "</x></presence>" +
           "<presence from='darkcave@macbeth.shakespeare.lit/oldhag' to='hag66@shakespeare.lit/pda'>" +
           "<x xmlns='http://jabber.org/protocol/muc#user'>" +
           "<item affiliation='member' jid='hag66@shakespeare.lit/pda' role='participant'/>" +
           "</x></presence>")
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = 'hag66@shakespeare.lit/pda'

    assert_equal(m, m.join('darkcave@macbeth.shakespeare.lit/thirdwitch'))
    wait_state
    assert(m.active?)
    assert_equal('thirdwitch', m.nick)

    assert_raises(ServerError) {
      m.nick = 'secondwitch'
    }
    wait_state
    assert(m.active?)
    assert_equal('thirdwitch', m.nick)

    m.nick = 'oldhag'
    wait_state
    assert(m.active?)
    assert_equal('oldhag', m.nick)
  end

  # JEP-0045: 10.2 Room Configuration
  def test_configuration
    room = JID.new('darkcave@macbeth.shakespeare.lit/thirdwitch')
    jid = JID.new('hag66@shakespeare.lit/pda')

    state { |pres|
      send(
          "<presence from='darkcave@macbeth.shakespeare.lit/thirdwitch' to='hag66@shakespeare.lit/pda'>" +
          "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='owner' role='moderator'/></x>" +
          "</presence>"
      )
    }

    state { |iq|
      assert_kind_of(Jabber::Iq,iq)
      assert_equal(jid, iq.from)
      assert_equal(room.strip, iq.to)
      assert_equal(:get, iq.type)

      assert_kind_of(Jabber::MUC::IqQueryMUCOwner, iq.first_element('query'))

      send(muc_config_form.sub("id='config1'","id='#{iq.id}'"))
    }

    state { |room_config|
      assert_kind_of(Jabber::Iq, room_config)
      assert_equal(room.strip, room_config.to)
      assert_equal(:set, room_config.type)

      assert_kind_of(Jabber::MUC::IqQueryMUCOwner, room_config.first_element('query'))

      form = room_config.first_element('query/x')
      assert_kind_of(Dataforms::XData, form)
      assert_equal(:submit, form.type)
      assert_equal(1, form.elements.size)
      assert_equal('muc#roomconfig_roomname', form.first_element('field').var)
      assert_equal(['Dunsinane'], form.first_element('field').values)

      send(muc_config_acknowledgement.sub("id='config1'","id='#{room_config.id}'"))
    }

    m = MUC::MUCClient.new(@client)
    m.my_jid = jid
    m.join(room)
    wait_state
    assert_equal(true, m.owner?)

    assert_equal(%w{muc#roomconfig_roomname muc#roomconfig_roomdesc
      muc#roomconfig_enablelogging muc#roomconfig_changesubject muc#roomconfig_allowinvites
      muc#roomconfig_maxusers muc#roomconfig_presencebroadcast muc#roomconfig_getmemberlist
      muc#roomconfig_publicroom muc#roomconfig_persistentroom muc#roomconfig_moderatedroom
      muc#roomconfig_membersonly muc#roomconfig_passwordprotectedroom muc#roomconfig_roomsecret
      muc#roomconfig_whois muc#roomconfig_roomadmins muc#roomconfig_roomowners}, m.get_room_configuration)
    wait_state

    m.submit_room_configuration( 'muc#roomconfig_roomname' => 'Dunsinane' )
    wait_state
  end

  # example 150 from XEP-0045
  def muc_config_form
      "<iq from='darkcave@macbeth.shakespeare.lit'
          id='config1'
          to='crone1@shakespeare.lit/desktop'
          type='result'>
        <query xmlns='http://jabber.org/protocol/muc#owner'>
          <x xmlns='jabber:x:data' type='form'>
            <title>Configuration for \"darkcave\" Room</title>
            <instructions>
              Complete this form to make changes to
              the configuration of your room.
            </instructions>
            <field
                type='hidden'
                var='FORM_TYPE'>
              <value>http://jabber.org/protocol/muc#roomconfig</value>
            </field>
            <field
                label='Natural-Language Room Name'
                type='text-single'
                var='muc#roomconfig_roomname'>
              <value>A Dark Cave</value>
            </field>
            <field
                label='Short Description of Room'
                type='text-single'
                var='muc#roomconfig_roomdesc'>
              <value>The place for all good witches!</value>
            </field>
            <field
                label='Enable Public Logging?'
                type='boolean'
                var='muc#roomconfig_enablelogging'>
              <value>0</value>
            </field>
            <field
                label='Allow Occupants to Change Subject?'
                type='boolean'
                var='muc#roomconfig_changesubject'>
              <value>0</value>
            </field>
            <field
                label='Allow Occupants to Invite Others?'
                type='boolean'
                var='muc#roomconfig_allowinvites'>
              <value>0</value>
            </field>
            <field
                label='Maximum Number of Occupants'
                type='list-single'
                var='muc#roomconfig_maxusers'>
              <value>10</value>
              <option label='10'><value>10</value></option>
              <option label='20'><value>20</value></option>
              <option label='30'><value>30</value></option>
              <option label='50'><value>50</value></option>
              <option label='100'><value>100</value></option>
              <option label='None'><value>none</value></option>
            </field>
            <field
                label='Roles for which Presence is Broadcast'
                type='list-multi'
                var='muc#roomconfig_presencebroadcast'>
              <value>moderator</value>
              <value>participant</value>
              <value>visitor</value>
              <option label='Moderator'><value>moderator</value></option>
              <option label='Participant'><value>participant</value></option>
              <option label='Visitor'><value>visitor</value></option>
            </field>
            <field
                label='Roles and Affiliations that May Retrieve Member List'
                type='list-multi'
                var='muc#roomconfig_getmemberlist'>
              <value>moderator</value>
              <value>participant</value>
              <value>visitor</value>
              <option label='Moderator'><value>moderator</value></option>
              <option label='Participant'><value>participant</value></option>
              <option label='Visitor'><value>visitor</value></option>
            </field>
            <field
                label='Make Room Publicly Searchable?'
                type='boolean'
                var='muc#roomconfig_publicroom'>
              <value>0</value>
            </field>
            <field
                label='Make Room Persistent?'
                type='boolean'
                var='muc#roomconfig_persistentroom'>
              <value>0</value>
            </field>
            <field
                label='Make Room Moderated?'
                type='boolean'
                var='muc#roomconfig_moderatedroom'>
              <value>0</value>
            </field>
            <field
                label='Make Room Members Only?'
                type='boolean'
                var='muc#roomconfig_membersonly'>
              <value>0</value>
            </field>
            <field
                label='Password Required for Entry?'
                type='boolean'
                var='muc#roomconfig_passwordprotectedroom'>
              <value>1</value>
            </field>
            <field type='fixed'>
              <value>
                If a password is required to enter this room,
                you must specify the password below.
              </value>
            </field>
            <field
                label='Password'
                type='text-private'
                var='muc#roomconfig_roomsecret'>
              <value>cauldronburn</value>
            </field>
            <field
                label='Who May Discover Real JIDs?'
                type='list-single'
                var='muc#roomconfig_whois'>
              <value>moderators</value>
              <option label='Moderators Only'>
                <value>moderators</value>
              </option>
              <option label='Anyone'>
                <value>anyone</value>
              </option>
            </field>
            <field type='fixed'>
              <value>
                You may specify additional people who have
                administrative privileges in the room. Please
                provide one Jabber ID per line.
              </value>
            </field>
            <field
                label='Room Admins'
                type='jid-multi'
                var='muc#roomconfig_roomadmins'>
              <value>wiccarocks@shakespeare.lit</value>
              <value>hecate@shakespeare.lit</value>
            </field>
            <field type='fixed'>
              <value>
                You may specify additional owners for this
                room. Please provide one Jabber ID per line.
              </value>
            </field>
            <field
                label='Room Owners'
                type='jid-multi'
                var='muc#roomconfig_roomowners'/>
          </x>
        </query>
      </iq>"
  end

  def muc_config_acknowledgement
      "<iq from='darkcave@macbeth.shakespeare.lit'
          id='config1'
          to='crone1@shakespeare.lit/desktop'>
        <query xmlns='http://jabber.org/protocol/muc#owner'>
          <x xmlns='jabber:x:data' type='result'>
            <field var='muc#roomconfig_roomname'><value>Dunsinane</value></field>
          </x>
        </query>
      </iq>"
  end
end
