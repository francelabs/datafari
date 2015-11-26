require 'open3'

# Wraps the libzmq library and attaches to the functions that are
# common across the 3.2.x+ and 4.x APIs.
#
module LibZMQ
  extend FFI::Library

  begin
    # bias the library discovery to a path inside the gem first, then
    # to the usual system paths
    inside_gem = File.join(File.dirname(__FILE__), '..', '..', 'ext')
    local_path = FFI::Platform::IS_WINDOWS ? ENV['PATH'].split(';') : ENV['PATH'].split(':')
    homebrew_path = nil

    # RUBYOPT set by RVM breaks 'brew' so we need to unset it.
    rubyopt = ENV.delete('RUBYOPT')

    begin
      stdout, stderr, status = Open3.capture3("brew", "--prefix")
      homebrew_path  = if status.success?
                        "#{stdout.chomp}/lib"
                      else
                        '/usr/local/homebrew/lib'
                      end
    rescue
      # Homebrew doesn't exist
    end

    # Restore RUBYOPT after executing 'brew' above.
    ENV['RUBYOPT'] = rubyopt

    # Search for libzmq in the following order...
    ZMQ_LIB_PATHS = ([inside_gem] + local_path + [
                       '/usr/local/lib', '/opt/local/lib', homebrew_path, '/usr/lib64'
    ]).compact.map{|path| "#{path}/libzmq.#{FFI::Platform::LIBSUFFIX}"}
    ffi_lib(ZMQ_LIB_PATHS + %w{libzmq})

  rescue LoadError
    if ZMQ_LIB_PATHS.any? {|path|
      File.file? File.join(path, "libzmq.#{FFI::Platform::LIBSUFFIX}")}
      warn "Unable to load this gem. The libzmq library exists, but cannot be loaded."
      warn "If this is Windows:"
      warn "-  Check that you have MSVC runtime installed or statically linked"
      warn "-  Check that your DLL is compiled for #{FFI::Platform::ADDRESS_SIZE} bit"
    else
      warn "Unable to load this gem. The libzmq library (or DLL) could not be found."
      warn "If this is a Windows platform, make sure libzmq.dll is on the PATH."
      warn "If the DLL was built with mingw, make sure the other two dependent DLLs,"
      warn "libgcc_s_sjlj-1.dll and libstdc++6.dll, are also on the PATH."
      warn "For non-Windows platforms, make sure libzmq is located in this search path:"
      warn ZMQ_LIB_PATHS.inspect
    end
    raise LoadError, "The libzmq library (or DLL) could not be loaded"
  end

  # Size_t not working properly on Windows
  find_type(:size_t) rescue typedef(:ulong, :size_t)

  # Context and misc api
  #
  # The `:blocking` option is a hint to FFI that the following (and only the following)
  # function may block, therefore it should release the GIL before calling it.
  # This can aid in situations where the function call will/may block and another
  # thread within the lib may try to call back into the ruby runtime. Failure to
  # release the GIL will result in a hang; the hint is required for MRI otherwise
  # there are random hangs (which require kill -9 to terminate).
  #
  attach_function :zmq_version, [:pointer, :pointer, :pointer], :void, :blocking => true
  attach_function :zmq_errno, [], :int, :blocking => true
  attach_function :zmq_strerror, [:int], :pointer, :blocking => true

  # Context initialization and destruction
  attach_function :zmq_init, [:int], :pointer, :blocking => true
  attach_function :zmq_term, [:pointer], :int, :blocking => true
  attach_function :zmq_ctx_new, [], :pointer, :blocking => true
  attach_function :zmq_ctx_destroy, [:pointer], :int, :blocking => true
  attach_function :zmq_ctx_set, [:pointer, :int, :int], :int, :blocking => true
  attach_function :zmq_ctx_get, [:pointer, :int], :int, :blocking => true

  # Message API
  attach_function :zmq_msg_init, [:pointer], :int, :blocking => true
  attach_function :zmq_msg_init_size, [:pointer, :size_t], :int, :blocking => true
  attach_function :zmq_msg_init_data, [:pointer, :pointer, :size_t, :pointer, :pointer], :int, :blocking => true
  attach_function :zmq_msg_close, [:pointer], :int, :blocking => true
  attach_function :zmq_msg_data, [:pointer], :pointer, :blocking => true
  attach_function :zmq_msg_size, [:pointer], :size_t, :blocking => true
  attach_function :zmq_msg_copy, [:pointer, :pointer], :int, :blocking => true
  attach_function :zmq_msg_move, [:pointer, :pointer], :int, :blocking => true
  attach_function :zmq_msg_send, [:pointer, :pointer, :int], :int, :blocking => true
  attach_function :zmq_msg_recv, [:pointer, :pointer, :int], :int, :blocking => true
  attach_function :zmq_msg_more, [:pointer], :int, :blocking => true
  attach_function :zmq_msg_get, [:pointer, :int], :int, :blocking => true
  attach_function :zmq_msg_set, [:pointer, :int, :int], :int, :blocking => true

  # Socket API
  attach_function :zmq_socket, [:pointer, :int], :pointer, :blocking => true
  attach_function :zmq_setsockopt, [:pointer, :int, :pointer, :int], :int, :blocking => true
  attach_function :zmq_getsockopt, [:pointer, :int, :pointer, :pointer], :int, :blocking => true
  attach_function :zmq_bind, [:pointer, :string], :int, :blocking => true
  attach_function :zmq_connect, [:pointer, :string], :int, :blocking => true
  attach_function :zmq_close, [:pointer], :int, :blocking => true
  attach_function :zmq_unbind, [:pointer, :string], :int, :blocking => true
  attach_function :zmq_disconnect, [:pointer, :string], :int, :blocking => true
  attach_function :zmq_recvmsg, [:pointer, :pointer, :int], :int, :blocking => true
  attach_function :zmq_recv, [:pointer, :pointer, :size_t, :int], :int, :blocking => true
  attach_function :zmq_sendmsg, [:pointer, :pointer, :int], :int, :blocking => true
  attach_function :zmq_send, [:pointer, :pointer, :size_t, :int], :int, :blocking => true

  # Device API
  attach_function :zmq_proxy, [:pointer, :pointer, :pointer], :int, :blocking => true

  # Poll API
  attach_function :zmq_poll, [:pointer, :int, :long], :int, :blocking => true

  # Monitoring API
  attach_function :zmq_socket_monitor, [:pointer, :pointer, :int], :int, :blocking => true

end
