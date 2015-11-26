require "gelfd/version"

module Gelfd
  CHUNKED_MAGIC = [0x1e,0x0f].pack('C*').freeze
  ZLIB_MAGIC = [0x78,0x9c].pack('C*').freeze
  GZIP_MAGIC = [0x1f,0x8b].pack('C*').freeze
  HEADER_LENGTH = 12
  DATA_LENGTH = 8192 - HEADER_LENGTH
  MAX_CHUNKS = 128
end

require File.join(File.dirname(__FILE__), 'gelfd', 'exceptions')
require File.join(File.dirname(__FILE__), 'gelfd', 'zlib_parser')
require File.join(File.dirname(__FILE__), 'gelfd', 'gzip_parser')
require File.join(File.dirname(__FILE__), 'gelfd', 'chunked_parser')
require File.join(File.dirname(__FILE__), 'gelfd', 'parser')
