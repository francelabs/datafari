require 'ffi'
require 'ffi-rzmq-core/libc'
require 'ffi-rzmq-core/libzmq'
require 'ffi-rzmq-core/utilities'
require 'ffi-rzmq-core/structures'
require 'ffi-rzmq-core/constants'

if LibZMQ.version4?
  require 'ffi-rzmq-core/libzmq4'
end
