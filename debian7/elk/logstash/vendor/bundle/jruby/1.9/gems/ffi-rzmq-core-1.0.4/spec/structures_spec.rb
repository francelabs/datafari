require 'spec_helper'

describe LibZMQ do

  if LibZMQ.version4? && LibZMQ.version[:minor] > 0

    it "the msg_t struct wrapped in Message is 64 bytes" do
      LibZMQ::Message.size == 64
    end
    
  else
    
    it "wraps the msg_t struct as Message" do
      message = LibZMQ::Message.new
  
      expect(message[:content]).to_not be_nil
      expect(message[:flags]).to_not be_nil
      expect(message[:vsm_size]).to_not be_nil
      expect(message[:vsm_data]).to_not be_nil
    end
    
  end

  it "wraps poll_item_t in a PollItem" do
    poll_item = LibZMQ::PollItem.new
    expect(poll_item.socket).to_not be_nil
    expect(poll_item.fd).to_not be_nil
    expect(poll_item.readable?).to_not be_nil
    expect(poll_item.writable?).to_not be_nil
    expect(poll_item.inspect).to_not be_nil
  end

  it "wraps zmq_event_t in an EventData" do
    event_data = LibZMQ::EventData.new

    expect(event_data.event).to_not be_nil
    expect(event_data.value).to_not be_nil
    expect(event_data.inspect).to_not be_nil
  end

end
