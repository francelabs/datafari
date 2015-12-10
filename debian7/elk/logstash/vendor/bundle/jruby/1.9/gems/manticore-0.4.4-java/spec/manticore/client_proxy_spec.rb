require 'spec_helper'
describe Manticore::Client do
  let(:client) { Manticore::Client.new }

  describe Manticore::Client::StubProxy do
    describe "#respond_with" do
      it "responds with a stubbed response" do
        client.respond_with(body: "body", code: 200).get(local_server).on_success do |response|
          expect(response).to be_a Manticore::StubbedResponse
          expect(response.body).to eq "body"
          expect(response.code).to eq 200
        end
      end

      context "for synchronous requests" do
        it "responds only stub the next subsequent response" do
          stub = client.respond_with(body: "body", code: 200)

          stub.get(local_server) do |response|
            expect(response).to be_a Manticore::StubbedResponse
          end

          stub.get(local_server) do |response|
            expect(response).to be_a Manticore::Response
          end
        end
      end

      context "for synchronous requests" do
        it "responds only stub the next subsequent response" do
          stub = client.respond_with(body: "body", code: 200)

          stub.async.get(local_server).on_success do |response|
            expect(response).to be_a Manticore::StubbedResponse
          end

          stub.async.get(local_server).on_success do |response|
            expect(response).to be_a Manticore::Response
          end

          client.execute!
        end
      end
    end
  end

  describe Manticore::Client::AsyncProxy do
    it "does not make a request until execute is called" do
      anchor = Time.now.to_f
      client.async.get("http://localhost:55441/?sleep=1.6")
      expect(Time.now.to_f - anchor).to be < 1.0

      anchor = Time.now.to_f
      client.execute!
      expect(Time.now.to_f - anchor).to be > 1.0
    end

    it "returns the response object, which may then have handlers attached" do
      response = client.async.get("http://localhost:55441/")
      success = false
      response.on_success do
        success = true
      end

      client.execute!
      expect(success).to eq true
    end

    it "can chain handlers" do
      client.async.get("http://localhost:55441/").on_success {|r| r.code }
      expect(client.execute!.map(&:callback_result)).to eq [200]
    end
  end

  describe Manticore::Client::BackgroundProxy do
    it "does not block execution" do
      anchor = Time.now.to_f
      future = client.background.get("http://localhost:55441/?sleep=1.5")
      expect(Time.now.to_f - anchor).to be < 1.0

      response = future.get
      expect(Time.now.to_f - anchor).to be > 1.0
      expect(response.body).to match(/sleep=1.5/)
    end

    it "returns a future" do
      response = client.background.get("http://localhost:55441/")
      expect(response).to be_a Java::JavaUtilConcurrent::FutureTask
      response.get
    end
  end
end