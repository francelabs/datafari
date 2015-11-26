
module LibZMQ

  # Returns a hash of the form {:major => X, :minor => Y, :patch => Z} to represent the
  # version of libzmq.
  #
  # Class method. 
  #
  # Invoke as:  ZMQ::LibZMQ.version
  #  
  def self.version
    unless @version
      major = FFI::MemoryPointer.new :int
      minor = FFI::MemoryPointer.new :int
      patch = FFI::MemoryPointer.new :int
      LibZMQ.zmq_version major, minor, patch
      @version = {:major => major.read_int, :minor => minor.read_int, :patch => patch.read_int}
    end

    @version
  end
  
  def self.version3?
    version[:major] == 3 && version[:minor] >= 2
  end
  
  def self.version4?
    version[:major] == 4
  end

  # Sanity check; print an error and exit if we are trying to load an unsupported
  # version of libzmq.
  #
  hash = LibZMQ.version
  major, minor = hash[:major], hash[:minor]
  if major < 3 || (major == 3 && minor < 2)
    version = "#{hash[:major]}.#{hash[:minor]}.#{hash[:patch]}"
    raise LoadError, "The libzmq version #{version} is incompatible with this version of ffi-rzmq-core."
  end
end