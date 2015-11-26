module LibZMQ

  # Used for casting pointers back to the msg_t struct
  #
  class Message < FFI::Struct
    layout :content,  :pointer,
      :flags,    :uint8,
      :vsm_size, :uint8,
      :vsm_data, [:uint8, 30]
  end
  
  if LibZMQ.version[:major] >= 4 && LibZMQ.version[:minor] > 0
    
    # zmq_msg_t was expanded to 64 bytes as of version 4.1.0
    class Message < FFI::Struct
      layout :content, :ulong_long
    end
    
  end


  # Create the basic mapping for the poll_item_t structure so we can
  # access those fields via Ruby.
  #
  module PollItemLayout
    def self.included(base)
      fd_type = if FFI::Platform::IS_WINDOWS && FFI::Platform::ADDRESS_SIZE == 64
        # On Windows, zmq.h defines fd as a SOCKET, which is 64 bits on x64.
        :uint64
      else
        :int
      end

      base.class_eval do
        layout :socket,  :pointer,
          :fd,    fd_type,
          :events, :short,
          :revents, :short
      end
    end
  end


  # PollItem class includes the PollItemLayout module so that we can use the
  # basic FFI accessors to get at the structure's fields. We also want to provide
  # some higher-level Ruby accessors for convenience.
  #
  class PollItem < FFI::Struct
    include PollItemLayout

    def socket
      self[:socket]
    end

    def fd
      self[:fd]
    end

    def readable?
      (self[:revents] & ZMQ::POLLIN) > 0
    end

    def writable?
      (self[:revents] & ZMQ::POLLOUT) > 0
    end

    def inspect
      "socket [#{socket}], fd [#{fd}], events [#{self[:events]}], revents [#{self[:revents]}]"
    end
  end


  #      /*  Socket event data  */
  #      typedef struct {
  #          uint16_t event;  // id of the event as bitfield
  #          int32_t  value ; // value is either error code, fd or reconnect interval
  #      } zmq_event_t;
  module EventDataLayout
    def self.included(base)
      base.class_eval do
        layout :event, :uint16,
          :value,    :int32
      end
    end
  end # module EventDataLayout


  # Provide a few convenience methods for accessing the event structure.
  #
  class EventData < FFI::Struct
    include EventDataLayout

    def event
      self[:event]
    end

    def value
      self[:value]
    end

    def inspect
      "event [#{event}], value [#{value}]"
    end
  end # class EventData
end
