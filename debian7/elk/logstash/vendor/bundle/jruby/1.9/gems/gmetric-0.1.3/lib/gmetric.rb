require "stringio"
require "socket"

module Ganglia
  class GMetric

    SLOPE = {
      'zero'        => 0,
      'positive'    => 1,
      'negative'    => 2,
      'both'        => 3,
      'unspecified' => 4
    }

    def self.send(host, port, metric)
      gmetric = self.pack(metric)

      if defined?(EventMachine) and EventMachine.reactor_running?
        # open an ephemereal UDP socket since
        # we do not plan on recieving any data
        conn = EM.open_datagram_socket('', 0)

        conn.send_datagram gmetric[0], host, port
        conn.send_datagram gmetric[1], host, port
        conn.close_connection_after_writing
      else
        conn = UDPSocket.new
        conn.connect(host, port)

        conn.send gmetric[0], 0
        conn.send gmetric[1], 0
        conn.close
      end
    end

    def self.pack(metric)
      metric = {
        :hostname => '',
        :group    => '',
        :spoof    => 0,
        :units    => '',
        :slope    => 'both',
        :tmax     => 60,
        :dmax     => 0
      }.merge(metric)

      # convert bools to ints
      metric[:spoof] = 1 if metric[:spoof].is_a? TrueClass
      metric[:spoof] = 0 if metric[:spoof].is_a? FalseClass

      raise "Missing key, value, type" if not metric.key? :name or not metric.key? :value or not metric.key? :type
      raise "Invalid metric type" if not %w(string int8 uint8 int16 uint16 int32 uint32 float double).include? metric[:type]

      meta = XDRPacket.new
      data = XDRPacket.new

      # METADATA payload
      meta.pack_int(128)                            # gmetadata_full
      meta.pack_string(metric[:hostname])           # hostname
      meta.pack_string(metric[:name].to_s)          # name of the metric
      meta.pack_int(metric[:spoof].to_i)            # spoof hostname flag

      meta.pack_string(metric[:type].to_s)          # one of: string, int8, uint8, int16, uint16, int32, uint32, float, double
      meta.pack_string(metric[:name].to_s)          # name of the metric
      meta.pack_string(metric[:units].to_s)         # units for the value, e.g. 'kb/sec'
      meta.pack_int(SLOPE[metric[:slope]])          # sign of the derivative of the value over time, one of zero, positive, negative, both, default both
      meta.pack_uint(metric[:tmax].to_i)            # maximum time in seconds between gmetric calls, default 60
      meta.pack_uint(metric[:dmax].to_i)            # lifetime in seconds of this metric, default=0, meaning unlimited

      ## MAGIC NUMBER: equals the elements of extra data, here it's 1 because I added Group.
      meta.pack_int(1)

      ## METADATA EXTRA DATA: functionally key/value
      meta.pack_string("GROUP")
      meta.pack_string(metric[:group].to_s)

      # DATA payload
      data.pack_int(128+5)                          # string message
      data.pack_string(metric[:hostname].to_s)      # hostname
      data.pack_string(metric[:name].to_s)          # name of the metric
      data.pack_int(metric[:spoof].to_i)            # spoof hostname flag
      data.pack_string("%s")                        #
      data.pack_string(metric[:value].to_s)         # value of the metric

      [meta.data, data.data]
    end
  end

  class XDRPacket
    def initialize
      @data = StringIO.new
    end

    def pack_uint(data)
      # big endian unsigned long
      @data << [data].pack("N")
    end
    alias :pack_int  :pack_uint

    def pack_string(data)
      len = data.size
      pack_uint(len)

      # pad the string
      len = ((len+3) / 4) * 4
      data = data + ("\0" * (len - data.size))
      @data << data
    end

    def data
      @data.string
    end
  end
end
