# encoding: utf-8
require "logstash/config/mixin"

# This module makes it easy to add a very fully configured HTTP client to logstash
# based on [Manticore](https://github.com/cheald/manticore).
# For an example of its usage see https://github.com/logstash-plugins/logstash-input-http_poller
module LogStash::PluginMixins::HttpClient
  class InvalidHTTPConfigError < StandardError; end

  def self.included(base)
    require 'manticore'
    base.extend(self)
    base.setup_http_client_config
  end

  public
  def setup_http_client_config
    # Timeout (in seconds) for the entire request
    config :request_timeout, :validate => :number, :default => 60

    # Timeout (in seconds) to wait for data on the socket. Default is `10s`
    config :socket_timeout, :validate => :number, :default => 10

    # Timeout (in seconds) to wait for a connection to be established. Default is `10s`
    config :connect_timeout, :validate => :number, :default => 10

    # Should redirects be followed? Defaults to `true`
    config :follow_redirects, :validate => :boolean, :default => true

    # Max number of concurrent connections. Defaults to `50`
    config :pool_max, :validate => :number, :default => 50

    # Max number of concurrent connections to a single host. Defaults to `25`
    config :pool_max_per_route, :validate => :number, :default => 25

    # Turn this on to enable HTTP keepalive support
    config :keepalive, :validate => :boolean, :default => true

    # How many times should the client retry a failing URL? Default is `0`
    config :automatic_retries, :validate => :number, :default => 0

    # Set this to false to disable SSL/TLS certificate validation
    # Note: setting this to false is generally considered insecure!
    config :ssl_certificate_validation, :validate => :boolean, :default => true

    # If you need to use a custom X.509 CA (.pem certs) specify the path to that here
    config :cacert, :validate => :path

    # If you'd like to use a client certificate (note, most people don't want this) set the path to the x509 cert here
    config :client_cert, :validate => :path
    # If you're using a client certificate specify the path to the encryption key here
    config :client_key, :validate => :path

    # If you need to use a custom keystore (`.jks`) specify that here. This does not work with .pem keys!
    config :keystore, :validate => :path

    # Specify the keystore password here.
    # Note, most .jks files created with keytool require a password!
    config :keystore_password, :validate => :password

    # Specify the keystore type here. One of `JKS` or `PKCS12`. Default is `JKS`
    config :keystore_type, :validate => :string, :default => "JKS"

    # If you need to use a custom truststore (`.jks`) specify that here. This does not work with .pem certs!
    config :truststore, :validate => :path

    # Specify the truststore password here.
    # Note, most .jks files created with keytool require a password!
    config :truststore_password, :validate => :password

    # Specify the truststore type here. One of `JKS` or `PKCS12`. Default is `JKS`
    config :truststore_type, :validate => :string, :default => "JKS"

    # Enable cookie support. With this enabled the client will persist cookies
    # across requests as a normal web browser would. Enabled by default
    config :cookies, :validate => :boolean, :default => true

    # If you'd like to use an HTTP proxy . This supports multiple configuration syntaxes:
    #
    # 1. Proxy host in form: `http://proxy.org:1234`
    # 2. Proxy host in form: `{host => "proxy.org", port => 80, scheme => 'http', user => 'username@host', password => 'password'}`
    # 3. Proxy host in form: `{url =>  'http://proxy.org:1234', user => 'username@host', password => 'password'}`
    config :proxy
  end

  public
  def client_config
    c = {
      connect_timeout: @connect_timeout,
      socket_timeout: @socket_timeout,
      request_timeout: @request_timeout,
      follow_redirects: @follow_redirects,
      automatic_retries: @automatic_retries,
      pool_max: @pool_max,
      pool_max_per_route: @pool_max_per_route,
      cookies: @cookies,
      keepalive: @keepalive,
      verify: @ssl_certificate_validation
    }

    if @proxy
      # Symbolize keys if necessary
      c[:proxy] = @proxy.is_a?(Hash) ?
        @proxy.reduce({}) {|memo,(k,v)| memo[k.to_sym] = v; memo} :
        @proxy
    end

    c[:ssl] = {}
    if @cacert
      c[:ssl][:ca_file] = @cacert
    end

    if @truststore
      c[:ssl].merge!(
        :truststore => @truststore,
        :truststore_type => @truststore_type
      )

      # JKS files have optional passwords if programatically created
      if (@truststore_password)
        c[:ssl].merge!(truststore_password: @truststore_password.value)
      end
    end

    if @keystore
      c[:ssl].merge!(
        :keystore => @keystore,
        :keystore_type => @keystore_type
      )

      # JKS files have optional passwords if programatically created
      if keystore_password
        c[:ssl].merge!(keystore_password: @keystore_password.value)
      end
    end

    if @client_cert && @client_key
      c[:ssl][:client_cert] = @client_cert
      c[:ssl][:client_key] = @client_key
    elsif !!@client_cert ^ !!@client_key
      raise InvalidHTTPConfigError, "You must specify both client_cert and client_key for an HTTP client, or neither!"
    end

    c
  end

  private
  def make_client
    Manticore::Client.new(client_config)
  end

  public
  def client
    @client ||= make_client
  end
end
