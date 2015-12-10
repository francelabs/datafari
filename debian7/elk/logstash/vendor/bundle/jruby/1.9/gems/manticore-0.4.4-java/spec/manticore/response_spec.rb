require 'spec_helper'

describe Manticore::Response do
  let(:client) { Manticore::Client.new }
  subject { client.get( local_server ) }

  its(:headers) { is_expected.to be_a Hash }
  its(:body)    { is_expected.to be_a String }
  its(:length)  { is_expected.to be_a Fixnum }

  it "provides response header lookup via #[]" do
    expect(subject["Content-Type"]).to eq "text/plain"
  end

  it "reads the body" do
    expect(subject.body).to match "Manticore"
  end

  it "reads the status code" do
    expect(subject.code).to eq 200
  end

  it "reads the status text" do
    expect(subject.message).to match "OK"
  end

  context "when the client is invoked with a block" do
    it "allows reading the body from a block" do
      response = client.get(local_server) do |response|
        expect(response.body).to match 'Manticore'
      end

      expect(response.body).to match "Manticore"
    end

    it "does not read the body implicitly if called with a block" do
      response = client.get(local_server) {}
      expect { response.body }.to raise_exception(Manticore::StreamClosedException)
    end
  end

  context "when an entity fails to read" do
    it "releases the connection" do
      stats_before = client.pool_stats
      expect_any_instance_of(Manticore::EntityConverter).to receive(:read_entity).and_raise(Manticore::StreamClosedException)
      expect { client.get(local_server).call rescue nil }.to_not change { client.pool_stats[:available] }
    end
  end
end
