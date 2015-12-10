require "helper"

describe Ganglia::GMetric do

  describe Ganglia::XDRPacket do
    def hex(data)
      [data].pack("H")
    end

    it "should pack an int & uint into XDR format" do
      xdr = Ganglia::XDRPacket.new
      xdr.pack_int(1)
      xdr.data.should == "\000\000\000\001"

      xdr = Ganglia::XDRPacket.new
      xdr.pack_uint(8)
      xdr.data.should == "\000\000\000\b"
    end

    it "should pack string" do
      xdr = Ganglia::XDRPacket.new
      xdr.pack_string("test")
      xdr.data.should == "\000\000\000\004test"
    end
  end

  it "should pack GMetric into XDR format from Ruby hash" do
    data = {
      :slope => 'both',
      :name => 'foo',
      :value => 'bar',
      :tmax => 60,
      :units => '',
      :dmax => 0,
      :type => 'string'
    }

    g = Ganglia::GMetric.pack(data)
    g.size.should == 2
    g[0].should == "\000\000\000\200\000\000\000\000\000\000\000\003foo\000\000\000\000\000\000\000\000\006string\000\000\000\000\000\003foo\000\000\000\000\000\000\000\000\003\000\000\000<\000\000\000\000\000\000\000\001\000\000\000\005GROUP\000\000\000\000\000\000\000"
    g[1].should == "\000\000\000\205\000\000\000\000\000\000\000\003foo\000\000\000\000\000\000\000\000\002%s\000\000\000\000\000\003bar\000"
  end

  it "should raise an error on missing name, value, type" do
    %w(name value type).each do |key|
      lambda {
        data = {:name => 'a', :type => 'b', :value => 'c'}
        data.delete key.to_sym
        Ganglia::GMetric.pack(data)
      }.should raise_error
    end
  end

  it "should verify type and raise error on invalid type" do
    %w(string int8 uint8 int16 uint16 int32 uint32 float double).each do |type|
      lambda {
        data = {:name => 'a', :type => type, :value => 'c'}
        Ganglia::GMetric.pack(data)
      }.should_not raise_error
    end

    lambda {
      data = {:name => 'a', :type => 'int', :value => 'c'}
      Ganglia::GMetric.pack(data)
    }.should raise_error
  end

  it "should allow host spoofing" do
    lambda {
      data = {:name => 'a', :type => 'uint8', :value => 'c', :spoof => 1, :host => 'host'}
      Ganglia::GMetric.pack(data)

      data = {:name => 'a', :type => 'uint8', :value => 'c', :spoof => true, :host => 'host'}
      Ganglia::GMetric.pack(data)
    }.should_not raise_error

  end

  it "should allow group meta data" do
    lambda {
      data = {:name => 'a', :type => 'uint8', :value => 'c', :spoof => 1, :host => 'host', :group => 'test'}
      g = Ganglia::GMetric.pack(data)
      g[0].should == "\000\000\000\200\000\000\000\000\000\000\000\001a\000\000\000\000\000\000\001\000\000\000\005uint8\000\000\000\000\000\000\001a\000\000\000\000\000\000\000\000\000\000\003\000\000\000<\000\000\000\000\000\000\000\001\000\000\000\005GROUP\000\000\000\000\000\000\004test"

    }.should_not raise_error
  end

  it "should use EM reactor if used within event loop" do
    pending 'stub out connection class'

    require 'rubygems'
    require 'eventmachine'
    EventMachine.run do
      Ganglia::GMetric.send("127.0.0.1", 1111, {
                              :name => 'pageviews',
                              :units => 'req/min',
                              :type => 'uint8',
                              :value => 7000,
                              :tmax => 60,
                              :dmax => 300
      })

      EM.stop
    end
  end
end
