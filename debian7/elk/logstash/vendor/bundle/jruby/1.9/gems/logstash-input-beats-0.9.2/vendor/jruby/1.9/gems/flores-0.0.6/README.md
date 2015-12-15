# Flores - a stress testing library

This library is named in loving memory of Carlo Flores.

---

When writing tests, it is often good to test a wide variety of inputs to ensure
your entire input range behaves correctly.

Further, adding a bit of randomness in your tests can help find bugs.

## Why Flores?

Randomization helps you cover a wider range of inputs to your tests to find bugs. Stress
testing (run a test repeatedly) helps you find bugs faster. We can use stress testing results
to find common patterns in failures!

Let's look at a sample situation. Ruby's TCPServer. Let's write a spec to cover a spec covering port binding:

```ruby
require "flores/rspec"
RSpec.configure do |config|
  Flores::RSpec.configure(config)
end

describe TCPServer do
  subject(:socket) { Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0) }
  let(:port) { 5000 }
  let(:sockaddr) { Socket.sockaddr_in(port, "127.0.0.1") }

  after { socket.close }

  it "should bind successfully" do
    socket.bind(sockaddr)
    expect(socket.local_address.ip_port).to(be == port)
  end
end
```

Running it:

```
% rspec tcpserver_spec.rb
.

Finished in 0.00248 seconds (files took 0.16294 seconds to load)
1 example, 0 failures
```

That's cool. We now have some confidence that TCPServer on port 5000 will bind successfully.

What about the other ports? What ranges of values should work? What shouldn't?

Let's assume I don't know anything about tcp port ranges and test randomly in the range -100,000 to +100,000:

```ruby
describe TCPServer do
  let(:port) { Flores::Random.integer(-100_000..100_000) }
  ...
end
```

Running it:

```
% rspec tcpserver_spec.rb
F

Failures:

  1) TCPServer should bind successfully
     Failure/Error: expect(socket.local_address.ip_port).to(be == port)
       expected: == 70144
            got:    4608
     # ./tcpserver_spec.rb:18:in `block (2 levels) in <top (required)>'

Finished in 0.00163 seconds (files took 0.09982 seconds to load)
1 example, 1 failure

Failed examples:

rspec ./tcpserver_spec.rb:16 # TCPServer should bind successfully
```

Well that's weird. Binding port 70144 actually made it bind on port 4608!

If we run it more times, we'll see all kinds of different results:

* Run 1:
  ```
     Failure/Error: expect(socket.local_address.ip_port).to(be == port)
       expected: == 83359
            got:    17823
  ```
* Run 2:
  ```
     Failure/Error: let(:sockaddr) { Socket.sockaddr_in(port, "127.0.0.1") }
     SocketError:
       getaddrinfo: nodename nor servname provided, or not known
  ```
* Run 3:
  ```
     Errno::EACCES:
       Permission denied - bind(2) for 127.0.0.1:615
  ```
* Run 4:
  ```
     Finished in 0.00161 seconds (files took 0.10356 seconds to load)
     1 example, 0 failures
  ```

## Analyze the results

The above example showed that there were many different kinds of failures when
we introduced randomness to our test inputs.

We can go further and run a given spec example many times and group the
failures by similarity and include context (what the inputs were, etc)

This library provides an `stress_it` helper which behaves similarly to rspec's
`it` except that the spec is copied (and run) many times.

The result is grouped by failure and includes context (`let` and `subject`).
Let's see how it works:

We'll change `it` to use `stress_it` instead, and also add `analyze_results`:

```diff
- it "should bind successfully" do
+ analyze_results # track the `let` and `subject` values in our tests.
+ stress_it "should bind successfully" do
```

The `analyze_results` method just adds an `after` hook to capture the `let` and
`subject` values used in each example.

The final step is to use a custom formatter provided with this library to do the analysis.

Now rerunning the test. With barely any spec changes from the original, we have
now enough randomness and stress testing to identify many different failure cases
and input ranges for those failures.

```
% rspec -f Flores::RSpec::Formatters::Analyze tcpserver_spec.rb

TCPServer should bind successfully
  33.96% (of 742 total) tests are successful
  Failure analysis:
    46.90% -> [348] SocketError
      Sample exception for {:socket=>#<Socket:(closed)>, :port=>-74235}
        getaddrinfo: nodename nor servname provided, or not known
      Samples causing SocketError:
        {:socket=>#<Socket:(closed)>, :port=>-60170}
        {:socket=>#<Socket:(closed)>, :port=>-73159}
        {:socket=>#<Socket:(closed)>, :port=>-84648}
        {:socket=>#<Socket:(closed)>, :port=>-5936}
        {:socket=>#<Socket:(closed)>, :port=>-78195}
    18.33% -> [136] RSpec::Expectations::ExpectationNotMetError
      Sample exception for {:socket=>#<Socket:(closed)>, :port=>72849, :sockaddr=>"\x10\x02\x1C\x91\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        expected: == 72849
             got:    7313
      Samples causing RSpec::Expectations::ExpectationNotMetError:
        {:socket=>#<Socket:(closed)>, :port=>74072, :sockaddr=>"\x10\x02!X\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>77973, :sockaddr=>"\x10\x020\x95\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>88867, :sockaddr=>"\x10\x02[#\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>87710, :sockaddr=>"\x10\x02V\x9E\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>95690, :sockaddr=>"\x10\x02u\xCA\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
    0.81% -> [6] Errno::EACCES
      Sample exception for {:socket=>#<Socket:(closed)>, :port=>65897, :sockaddr=>"\x10\x02\x01i\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        Permission denied - bind(2) for 127.0.0.1:361
      Samples causing Errno::EACCES:
        {:socket=>#<Socket:(closed)>, :port=>879, :sockaddr=>"\x10\x02\x03o\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>66258, :sockaddr=>"\x10\x02\x02\xD2\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>65829, :sockaddr=>"\x10\x02\x01%\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>66044, :sockaddr=>"\x10\x02\x01\xFC\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}
        {:socket=>#<Socket:(closed)>, :port=>65897, :sockaddr=>"\x10\x02\x01i\x7F\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00"}

Finished in 0.10509 seconds
742 examples, 490 failures
```

Now we can see a wide variety of failure cases all found through randomization. Nice!
