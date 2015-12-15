# encoding: utf-8
# This file is part of ruby-flores.
# Copyright (C) 2015 Jordan Sissel
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
require "flores/rspec"
require "flores/random"
require "socket"

RSpec.configure do |config|
  Flores::RSpec.configure(config)
  Kernel.srand(config.seed)

  # Demonstrate the wonderful Analyze formatter
  config.add_formatter("Flores::RSpec::Formatters::Analyze")
end

describe TCPServer do
  analyze_results
  subject(:socket) { Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0) }
  let(:sockaddr) { Socket.sockaddr_in(port, "127.0.0.1") }

  after do
    socket.close unless socket.closed?
  end

  context "on a random port" do
    let(:port) { Flores::Random.integer(-100_000..100_000) }
    stress_it "should bind successfully", [:port] do
      socket.bind(sockaddr)
      expect(socket.local_address.ip_port).to(be == port)
    end
  end

  context "on privileged ports" do
    let(:port) { Flores::Random.integer(1..1023) }
    stress_it "should raise Errno::EACCESS" do
      expect { socket.bind(sockaddr) }.to(raise_error(Errno::EACCES))
    end
  end

  context "on unprivileged ports" do
    let(:port) { Flores::Random.integer(1025..65535) }
    stress_it "should bind on a port" do
      # EADDRINUSE is expected since we are picking ports at random
      # Let's ignore this specific exception
      allow(socket).to(receive(:bind).and_wrap_original do |original, *args|
        begin
          original.call(*args)
        rescue Errno::EADDRINUSE # rubocop:disable Lint/HandleExceptions
          # Ignore
        end
      end)
      expect { socket.bind(sockaddr) }.to_not(raise_error)
    end
  end

  context "on port 0" do
    let(:port) { 0 }
    stress_it "should bind successfully" do
      expect { socket.bind(sockaddr) }.to_not(raise_error)
    end
  end
end
