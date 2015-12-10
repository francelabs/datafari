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

require "flores/namespace"
autoload :Socket, "socket"

# A collection of methods intended for use in randomized testing.
module Flores::Random
  # A selection of UTF-8 characters
  #
  # I'd love to generate this, but I don't yet know enough about how unicode
  # blocks are allocated to do that. For now, hardcode a set of possible
  # characters.
  CHARACTERS = [
    # Basic Latin
    *(32..126).map(&:chr).map { |c| c.force_encoding(Encoding.default_external) },

    # hand-selected CJK Unified Ideographs Extension A
    "㐤", "㐨", "㐻", "㑐",

    # hand-selected Hebrew
    "א", "ב", "ג", "ד", "ה",

    # hand-selected Cyrillic
    "Є", "Б", "Р", "н", "я"
  ]

  # Generates text with random characters of a given length (or within a length range)
  #
  # * The length can be a number or a range `x..y`. If a range, it must be ascending (x < y)
  # * Negative lengths are not permitted and will raise an ArgumentError
  #
  # @param length [Fixnum or Range] the length of text to generate
  # @return [String] the generated text
  def self.text(length)
    return text_range(length) if length.is_a?(Range)

    raise ArgumentError, "A negative length is not permitted, I received #{length}" if length < 0
    length.times.collect { character }.join
  end # def text

  # Generate text with random characters of a length within the given range.
  #
  # @param range [Range] the range of length to generate, inclusive
  # @return [String] the generated text
  def self.text_range(range)
    raise ArgumentError, "Requires ascending range, you gave #{range}." if range.end < range.begin
    raise ArgumentError, "A negative range values are not permitted, I received range #{range}" if range.begin < 0
    text(integer(range))
  end

  # Generates a random character (A string of length 1)
  #
  # @return [String]
  def self.character
    return CHARACTERS[integer(0...CHARACTERS.length)]
  end # def character

  # Return a random integer value within a given range.
  #
  # @param range [Range]
  def self.integer(range)
    raise ArgumentError, "Range not given, got #{range.class}: #{range.inspect}" if !range.is_a?(Range)
    rand(range)
  end # def integer

  # Return a random number within a given range.
  #
  # @param range [Range]
  def self.number(range)
    raise ArgumentError, "Range not given, got #{range.class}: #{range.inspect}" if !range.is_a?(Range)
    # Ruby 1.9.3 and below do not have Enumerable#size, so we have to compute the size of the range
    # ourselves.
    rand * (range.end - range.begin) + range.begin
  end # def number
   
  # Run a block a random number of times.
  #
  # @param range [Fixnum of Range] same meaning as #integer(range)
  def self.iterations(range, &block)
    range = 0..range if range.is_a?(Numeric)
    if block_given?
      integer(range).times(&block)
      nil
    else
      integer(range).times
    end
  end # def iterations

  # Return a random element from an array
  def self.item(array)
    array[integer(0...array.size)]
  end

  # Return a random IPv4 address as a string
  def self.ipv4
    # TODO(sissel): Support CIDR range restriction?
    # TODO(sissel): Support netmask restriction?
    [integer(0..IPV4_MAX)].pack("N").unpack("C4").join(".")
  end

  # Return a random IPv6 address as a string
  #
  # The address may be in abbreviated form (ABCD::01EF):w
  def self.ipv6
    # TODO(sissel): Support CIDR range restriction?
    # TODO(sissel): Support netmask restriction?
    length = integer(2..8)
    if length == 8
      # Full address; nothing to abbreviate
      ipv6_pack(length)
    else
      abbreviation = ipv6_abbreviation(length)
      if length == 2
        first = 1
        second = 1
      else
        first = integer(2...length)
        second = length - first
      end
      ipv6_pack(first) + abbreviation + ipv6_pack(second)
    end
  end

  # Get a TCP socket bound and listening on a random port.
  #
  # You are responsible for closing the socket.
  #
  # Returns [socket, address, port]
  def self.tcp_listener(host = "127.0.0.1")
    socket_listener(Socket::SOCK_STREAM, host)
  end

  # Get a UDP socket bound and listening on a random port.
  #
  # You are responsible for closing the socket.
  #
  # Returns [socket, address, port]
  def self.udp_listener(host = "127.0.0.1")
    socket_listener(Socket::SOCK_DGRAM, host)
  end

  private

  IPV4_MAX = 1 << 32
  IPV6_SEGMENT = 1 << 16

  def self.ipv6_pack(length)
    length.times.collect { integer(0..IPV6_SEGMENT).to_s(16) }.join(":")
  end

  def self.ipv6_abbreviation(length)
    abbreviate = (integer(0..1) == 0)
    if abbreviate
      "::"
    else
      ":" + (8 - length).times.collect { "0" }.join(":") + ":"
    end
  end

  LISTEN_BACKLOG = 5
  def self.socket_listener(type, host)
    socket = server_socket_class.new(Socket::AF_INET, type)
    socket.bind(Socket.pack_sockaddr_in(0, host))
    if type == Socket::SOCK_STREAM || type == Socket::SOCK_SEQPACKET
      socket.listen(LISTEN_BACKLOG)
    end

    port = socket.local_address.ip_port
    address = socket.local_address.ip_address
    [socket, address, port]
  end

  def self.server_socket_class
    if RUBY_ENGINE == 'jruby'
      # https://github.com/jruby/jruby/wiki/ServerSocket
      ServerSocket
    else
      Socket
    end
  end
end # module Flores::Random
