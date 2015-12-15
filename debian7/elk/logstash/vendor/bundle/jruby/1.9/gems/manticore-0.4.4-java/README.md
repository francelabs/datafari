# Manticore

[![Build Status](https://travis-ci.org/cheald/manticore.png?branch=master)](https://travis-ci.org/cheald/manticore)

Manticore is a fast, robust HTTP client built on the Apache HTTPClient libraries. It is only compatible with JRuby.

## Installation

Add this line to your application's Gemfile:

    gem 'manticore'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install manticore

## Documentation

  Documentation is available [at rubydoc.info](http://www.rubydoc.info/github/cheald/manticore/master/Manticore/Client).

## Performance

  Manticore is [very fast](https://github.com/cheald/manticore/wiki/Performance).

## Major Features

  As it's built on the Apache Commons HTTP components, Manticore is very rich. It includes support for:

  * Keepalive connections (and connection pooling)
  * Transparent gzip and deflate handling
  * Transparent cookie handling
  * Both synchronous and asynchronous execution models
  * Lazy evaluation
  * Authentication
  * Proxy support
  * SSL

## Usage

### Quick Start

If you don't want to worry about setting up and maintaining client pools, Manticore comes with a facade that you can use to start making requests right away:

```ruby
get_body  = Manticore.get("http://www.google.com/",  query:  {q: "kittens"}).body
post_body = Manticore.post("http://www.google.com/", params: {q: "kittens"}).body

# Or

get_body = Manticore.http(:get, "http://www.google.com/").body
```

This is threadsafe and automatically backed with a pool, so you can execute `Manticore.get` in multiple threads without harming performance.

Alternately, you can mix the `Manticore::Facade` into your own class for similar behavior:

```ruby
class MyClient
  include Manticore::Facade
  include_http_client user_agent: "MyClient/1.0"
end

response_code = MyClient.get("http://www.google.com/").code
```

Mixing the client into a class will create a new pool. If you want to share a single pool between clients, specify the `shared_pool` option:

```ruby
class MyClient
  include Manticore::Facade
  include_http_client shared_pool: true
end

class MyOtherClient
  include Manticore::Facade
  include_http_client shared_pool: true
end
```

For detailed documentation, see the [full Manticore::Client documentation](http://www.rubydoc.info/github/cheald/manticore/master/Manticore/Client).

### Configuring clients

Rather than using the Facade, you can create your own standalone Client instances. When you create a `Client`, you will pass various parameters that it will use to set up the pool.

```ruby
client = Manticore::Client.new(request_timeout: 5, connect_timeout: 5, socket_timeout: 5, pool_max: 10, pool_max_per_route: 2)
```

Then, you can make requests from the client. Pooling and route maximum constraints are automatically managed:

```ruby
response = client.get("http://www.google.com/")
body = response.body
```

It is recommend that you instantiate a client once, then re-use it, rather than instantiating a new client per request.

Additionally, if you pass a block to the initializer, the underlying [HttpClientBuilder](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html) and [RequestConfig.Builder](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html) will be yielded so that you can operate on them directly:

```ruby
client = Manticore::Client.new(socket_timeout: 5) do |http_client_builder, request_builder|
  http_client_builder.disable_redirect_handling
end
```

### Pools

You've seen "pools" mentioned a few times. Manticore creates and configures a [PoolingHttpClientConnectionManager](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/conn/PoolingHttpClientConnectionManager.html)
which all requests are run through. The advantage here is that configuration and setup is performed once, and this lets clients take advantage of things like keepalive,
per-route concurrency limits, and other neat things. In general, you should create one `Manticore::Client` instance per unique configuration needed. For example, you might have an app that performs 2 functions:

1. General HTTP requesting from the internet-at-large
2. Communication with a backend service over SSL, using a custom trust store

To set this up, you might create 2 pools, each configured for the task:

```ruby
general_http_client    = Manticore::Client.new connect_timeout: 10, socket_timeout: 10, request_timeout: 10, follow_redirects: true, max_per_route: 2
# With an OpenSSL CA store
proxied_backend_client = Manticore::Client.new proxy: "https://backend.internal:4242", ssl: {ca_file: "my_certs.pem"}
# Or with a .jks truststore
# proxied_backend_client = Manticore::Client.new proxy: "https://backend.internal:4242", ssl: {truststore: "./truststore.jks", truststore_password: "s3cr3t"}
```

This would create 2 separate request pools; the first would be configured with generous timeouts and redirect following, and would use the system
default trust stores (ie, the normal certs used to verify SSL certificates with the normal certificate authorities). Additionally, it will only permit
2 concurrent requests to a given domain ("route") at a time; this can be nice for web crawling or fetching against rate-limited APIs, to help you stay
under your rate limits even when executing in a parallel context. The second client would use a custom trust store to recognize certs signed with your
internal CA, and would proxy all requests through an internal server.

Creating pools is expensive, so you don't want to be doing it for each request. Instead, you should set up your pools once and then re-use them.
Clients and their backing pools are thread-safe, so feel free to set them up once before you start performing parallel operations.


### Parallel execution

Manticore can perform concurrent execution of multiple requests.

```ruby
client = Manticore::Client.new

# These aren't actually executed until #execute! is called.
# You can define response handlers in a block when you queue the request:
client.async.get("http://www.google.com") {|req|
  req.on_success do |response|
    puts response.body
  end

  req.on_failure do |exception|
    puts "Boom! #{exception.message}"
  end
}

# ...or by invoking the method on the queued response returned:
response = client.async.get("http://www.yahoo.com")
response.on_success do |response|
  puts "The length of the Yahoo! homepage is #{response.body.length}"
end

# ...or even by chaining them onto the call
client.async.get("http://bing.com").
  on_success {|r| puts r.code }.
  on_failure {|e| puts "on noes!"}

client.execute!
```

### Lazy Evaluation

Manticore attempts to avoid doing any actual work until right before you need results. As a result,
responses are lazy-evaluated as late as possible. The following rules apply:

1. Synchronous responses are evaluted when you call an accessor on them, like `#body` or `#headers`.
2. Synchronous responses which pass a handler block are evaluated immediately.
3. Asynchronous responses are always evaluated when you call `Client#execute!`
4. Background responses are always immediately evaluated, but return a `Future`.

As a result, with the exception of background requests, this allows you to attach handlers to synchronous
and asynchronous responses in the same fashion:

```ruby
# Response doesn't evaluate when you call get, since you don't need any results from it yet
response = client.get("http://google.com").on_success {|r| "Success handler!" }
# As soon as you request #body, the response will evaluate to a result.
body = response.body

response = client.async.get("http://google.com").on_success {|r| "Success handler!" }
client.execute!
body = response.body
```

If you want to make a response that is not lazy-evaluated, you can either pass a handler block to it, or you can
call `#call` on the resulting response:

```ruby
# This will evaluate immediately
client.get("http://google.com") {r| r.body }

# As will this, via explicit invocation of #call
client.get("http://google.com").call
```

### Stubbing

Manticore provides a stubbing interface somewhat similar to Typhoeus'

```ruby
client.stub("http://google.com", body: "response body", code: 200)
client.get("http://google.com") do |response|
  response.body.should == "response body"
end
client.clear_stubs!
```

This works for async requests as well:

```ruby
client.stub("http://google.com", body: "response body", code: 200)

# The request to google.com returns a stub as expected
client.async.get("http://google.com").on_success do |response|
  response.should be_a Manticore::ResponseStub
end

# Since yahoo.com isn't stubbed, a full request will be performed
client.async.get("http://yahoo.com").on_success do |response|
  response.should be_a Manticore::Response
end
client.clear_stubs!
```

If you don't want to worry about stub teardown, you can just use `#respond_with`, which will stub the next
response the client makes with a ResponseStub rather than permitting it to execute a remote request.

```ruby
client.respond_with(body: "body").get("http://google.com") do |response|
  response.body.should == "body"
end
```

You can also chain proxies to, say, stub an async request:

```ruby
response = client.async.respond_with(body: "response body").get("http://google.com")
client.execute!

response.body.should == "response body"
```

Additionally, you can stub and unstub individual URLs as desired:
```ruby
client.stub("http://google.com", body: "response body", code: 200)
client.stub("http://yahoo.com",  body: "response body", code: 200)

# The request to google.com returns a stub as expected
client.get("http://google.com") do |response|
  response.should be_a Manticore::ResponseStub
end

# After this point, yahoo will remain stubbed, while google will not.
client.unstub("http://google.com")
```

### Background requests

You might want to fire-and-forget requests without blocking your calling thread. You can do this with `Client#background`:

```ruby
future = client.background.get("http://google.com")
# The request is now running, but the calling thread isn't blocked
# Do whatever stuff you need to right now. At some point, if you want the result of the request, you can call `Future#get`:
response = future.get
```

### Faraday Adapter

Manticore includes a Faraday adapter. To use it:

    require 'faraday/adapter/manticore'
    Faraday.new(...) do |faraday|
      faraday.adapter :manticore
    end

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
