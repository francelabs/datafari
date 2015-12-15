require 'spec_helper'

if LibZMQ.version4?
  describe LibZMQ do

    it "exposes new context management methods" do
      [:zmq_ctx_term, :zmq_ctx_shutdown].each do |method|
        expect(LibZMQ).to respond_to(method)
      end
    end

    it "exposes new sending methods" do
      expect(LibZMQ).to respond_to(:zmq_send_const)
    end

    it "exposes the binary encoding / decoding API" do
      [:zmq_z85_encode, :zmq_z85_decode].each do |method|
        expect(LibZMQ).to respond_to(method)
      end
    end

    it "exposes CURVE security methods" do
      expect(LibZMQ).to respond_to(:zmq_curve_keypair)
    end

  end
end
