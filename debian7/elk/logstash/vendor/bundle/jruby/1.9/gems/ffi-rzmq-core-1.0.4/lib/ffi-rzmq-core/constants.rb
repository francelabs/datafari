
module ZMQ
  # Set up all of the constants that are *common* to all API
  # versions

  #  Socket types
  PAIR   = 0
  PUB    = 1
  SUB    = 2
  REQ    = 3
  REP    = 4
  XREQ   = 5
  XREP   = 6
  PULL   = 7
  PUSH   = 8
  XPUB   = 9
  XSUB   = 10
  DEALER = XREQ
  ROUTER = XREP
  STREAM = 11

  SocketTypeNameMap = {
    PAIR   => "PAIR",
    PUB    => "PUB",
    SUB    => "SUB",
    REQ    => "REQ",
    REP    => "REP",
    PULL   => "PULL",
    PUSH   => "PUSH",
    XREQ   => "XREQ",
    XREP   => "XREP",
    ROUTER => "ROUTER",
    DEALER => "DEALER",
    XPUB   => "XPUB",
    XSUB   => "XSUB",
    STREAM => "STREAM",
  }

  #  Socket options
  AFFINITY                 = 4
  IDENTITY                 = 5
  SUBSCRIBE                = 6
  UNSUBSCRIBE              = 7
  RATE                     = 8
  RECOVERY_IVL             = 9
  SNDBUF                   = 11
  RCVBUF                   = 12
  RCVMORE                  = 13
  FD                       = 14
  EVENTS                   = 15
  TYPE                     = 16
  LINGER                   = 17
  RECONNECT_IVL            = 18
  BACKLOG                  = 19
  RECONNECT_IVL_MAX        = 21
  MAXMSGSIZE               = 22
  SNDHWM                   = 23
  RCVHWM                   = 24
  MULTICAST_HOPS           = 25
  RCVTIMEO                 = 27
  SNDTIMEO                 = 28
  IPV4ONLY                 = 31
  LAST_ENDPOINT            = 32
  ROUTER_MANDATORY         = 33
  TCP_KEEPALIVE            = 34
  TCP_KEEPALIVE_CNT        = 35
  TCP_KEEPALIVE_IDLE       = 36
  TCP_KEEPALIVE_INTVL      = 37
  TCP_ACCEPT_FILTER        = 38
  DELAY_ATTACH_ON_CONNECT  = 39
  XPUB_VERBOSE             = 40
  ROUTER_RAW               = 41
  IPV6                     = 42
  MECHANISM                = 43
  PLAIN_SERVER             = 44
  PLAIN_USERNAME           = 45
  PLAIN_PASSWORD           = 46
  CURVE_SERVER             = 47
  CURVE_PUBLICKEY          = 48
  CURVE_SECRETKEY          = 49
  CURVE_SERVERKEY          = 50
  PROBE_ROUTER             = 51
  REQ_CORRELATE            = 52
  REQ_RELAXED              = 53
  CONFLATE                 = 54
  ZAP_DOMAIN               = 55
  ROUTER_HANDOVER          = 56
  TOS                      = 57
  CONNECT_RID              = 61
  GSSAPI_SERVER            = 62
  GSSAPI_PRINCIPAL         = 63
  GSSAPI_SERVICE_PRINCIPAL = 64
  GSSAPI_PLAINTEXT         = 65
  HANDSHAKE_IVL            = 66
  SOCKS_PROXY              = 68
  XPUB_NODROP              = 69
  BLOCKY                   = 70
  XPUB_MANUAL              = 71
  XPUB_WELCOME_MSG         = 72
  STREAM_NOTIFY            = 73
  INVERT_MATCHING          = 74
  HEARTBEAT_IVL            = 75
  HEARTBEAT_TTL            = 76
  HEARTBEAT_TIMEOUT        = 77
  XPUB_VERBOSE_UNSUBSCRIBE = 78

  IMMEDIATE       = DELAY_ATTACH_ON_CONNECT
  FAIL_UNROUTABLE = ROUTER_MANDATORY
  ROUTER_BEHAVIOR = ROUTER_MANDATORY

  # Socket Security Types
  NULL  = 0
  PLAIN = 1
  CURVE = 2

  #  Send/recv options
  DONTWAIT = 1
  SNDMORE  = 2
  SNDLABEL = 4
  NOBLOCK  = DONTWAIT

  # Message options
  MORE = 1

  # Context options
  IO_THREADS       = 1
  MAX_SOCKETS      = 2
  IO_THREADS_DFLT  = 1
  MAX_SOCKETS_DFLT = 1023

  #  I/O multiplexing
  POLL    = 1
  POLLIN  = 1
  POLLOUT = 2
  POLLERR = 4

  #  Socket errors
  EAGAIN = Errno::EAGAIN::Errno
  EINVAL = Errno::EINVAL::Errno
  ENOMEM = Errno::ENOMEM::Errno
  ENODEV = Errno::ENODEV::Errno
  EFAULT = Errno::EFAULT::Errno
  EINTR  = Errno::EINTR::Errno
  EMFILE = Errno::EMFILE::Errno

  # ZMQ errors
  HAUSNUMERO     = 156384712
  EFSM           = (HAUSNUMERO + 51)
  ENOCOMPATPROTO = (HAUSNUMERO + 52)
  ETERM          = (HAUSNUMERO + 53)
  EMTHREAD       = (HAUSNUMERO + 54)

  # Rescue unknown constants and use the ZeroMQ defined values
  # Usually only happens on Windows though some don't resolve on
  # OSX too (ENOTSUP)
  ENOTSUP         = Errno::ENOTSUP::Errno rescue (HAUSNUMERO + 1)
  EPROTONOSUPPORT = Errno::EPROTONOSUPPORT::Errno rescue (HAUSNUMERO + 2)
  ENOBUFS         = Errno::ENOBUFS::Errno rescue (HAUSNUMERO + 3)
  ENETDOWN        = Errno::ENETDOWN::Errno rescue (HAUSNUMERO + 4)
  EADDRINUSE      = Errno::EADDRINUSE::Errno rescue (HAUSNUMERO + 5)
  EADDRNOTAVAIL   = Errno::EADDRNOTAVAIL::Errno rescue (HAUSNUMERO + 6)
  ECONNREFUSED    = Errno::ECONNREFUSED::Errno rescue (HAUSNUMERO + 7)
  EINPROGRESS     = Errno::EINPROGRESS::Errno rescue (HAUSNUMERO + 8)
  ENOTSOCK        = Errno::ENOTSOCK::Errno rescue (HAUSNUMERO + 9)
  EMSGSIZE        = Errno::EMSGSIZE::Errno rescue (HAUSNUMERO + 10)
  EAFNOSUPPORT    = Errno::EAFNOSUPPORT::Errno rescue (HAUSNUMERO + 11)
  ENETUNREACH     = Errno::ENETUNREACH::Errno rescue (HAUSNUMERO + 12)
  ECONNABORTED    = Errno::ECONNABORTED::Errno rescue (HAUSNUMERO + 13)
  ECONNRESET      = Errno::ECONNRESET::Errno rescue (HAUSNUMERO + 14)
  ENOTCONN        = Errno::ENOTCONN::Errno rescue (HAUSNUMERO + 15)
  ETIMEDOUT       = Errno::ETIMEDOUT::Errno rescue (HAUSNUMERO + 16)
  EHOSTUNREACH    = Errno::EHOSTUNREACH::Errno rescue (HAUSNUMERO + 17)
  ENETRESET       = Errno::ENETRESET::Errno rescue (HAUSNUMERO + 18)

  #  Device Types
  STREAMER  = 1
  FORWARDER = 2
  QUEUE     = 3

  # Socket events and monitoring
  EVENT_CONNECTED       = 1
  EVENT_CONNECT_DELAYED = 2
  EVENT_CONNECT_RETRIED = 4
  EVENT_LISTENING       = 8
  EVENT_BIND_FAILED     = 16
  EVENT_ACCEPTED        = 32
  EVENT_ACCEPT_FAILED   = 64
  EVENT_CLOSED          = 128
  EVENT_CLOSE_FAILED    = 256
  EVENT_DISCONNECTED    = 512
  EVENT_MONITOR_STOPPED = 1024
  EVENT_ALL             = (EVENT_CONNECTED | EVENT_CONNECT_DELAYED | EVENT_CONNECT_RETRIED |
                           EVENT_LISTENING | EVENT_BIND_FAILED | EVENT_ACCEPTED |
                           EVENT_ACCEPT_FAILED | EVENT_CLOSED | EVENT_CLOSE_FAILED |
                           EVENT_DISCONNECTED | EVENT_MONITOR_STOPPED)

  # version checking
  # This gem supports both libzmq 3.2+ and 4.x. However, not all socket options are visible between versions.
  # To make support easier for consumers of this gem, we do all version checking here and only
  # expose the appropriate socket options. Note that *all* options are defined above even if the library
  # version doesn't support it. Consumers of this gem can enable support at runtime.
  integer_socket_options = [
    EVENTS, LINGER, RCVTIMEO, SNDTIMEO, RECONNECT_IVL, FD, TYPE, BACKLOG, RECONNECT_IVL_MAX, RCVHWM,
    SNDHWM, RATE, RECOVERY_IVL, SNDBUF, RCVBUF, IPV4ONLY, ROUTER_BEHAVIOR, TCP_KEEPALIVE,
    TCP_KEEPALIVE_CNT, TCP_KEEPALIVE_IDLE, TCP_KEEPALIVE_INTVL, TCP_ACCEPT_FILTER, MULTICAST_HOPS,
    IMMEDIATE,
  ]

  long_long_socket_options = [
    RCVMORE, AFFINITY, MAXMSGSIZE,
  ]

  string_socket_options = [
    IDENTITY, SUBSCRIBE, UNSUBSCRIBE, LAST_ENDPOINT,
  ]

  if LibZMQ.version4?
    integer_socket_options += [
      IPV6, MECHANISM, PLAIN_SERVER, CURVE_SERVER, PROBE_ROUTER, REQ_CORRELATE, REQ_RELAXED, CONFLATE,
    ]

    string_socket_options += [
      ZAP_DOMAIN, PLAIN_USERNAME, PLAIN_PASSWORD, CURVE_PUBLICKEY, CURVE_SERVERKEY, CURVE_SECRETKEY,
    ]
  end

  IntegerSocketOptions = integer_socket_options.sort
  LongLongSocketOptions = long_long_socket_options.sort
  StringSocketOptions = string_socket_options.sort
  SocketOptions = (IntegerSocketOptions + LongLongSocketOptions + StringSocketOptions).sort
end
