require 'spec_helper'

describe LibC do

  it "exposes the malloc function" do
    expect(LibC).to respond_to(:malloc)
  end

  it "exposes the free function" do
    expect(LibC).to respond_to(:free)
    expect(LibC::Free).to_not be_nil
  end

  it "exposes the memcpy function" do
    expect(LibC).to respond_to(:memcpy)
  end

end
