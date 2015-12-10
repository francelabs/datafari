require 'spec_helper'

describe Manticore::Facade do
  context "when extended into an arbitrary class" do
    let(:extended_class) {
     Class.new do
        include Manticore::Facade
        include_http_client
      end
    }

    let(:extended_shared_class) {
     Class.new do
        include Manticore::Facade
        include_http_client shared_pool: true
      end
    }

    it "gets a response" do
      result = JSON.parse extended_class.get(local_server).body
      expect(result["method"]).to eq "GET"
    end

    it "does not use the shared client by default" do
      expect(extended_class.instance_variable_get("@manticore_facade").object_id).to_not eq \
        Manticore.instance_variable_get("@manticore_facade").object_id
    end

    it "is able to use the shared client" do
      expect(extended_shared_class.instance_variable_get("@manticore_facade").object_id).to eq \
        Manticore.instance_variable_get("@manticore_facade").object_id
    end
  end

  context "from the default Manticore module" do
    it "gets a response" do
      result = JSON.parse Manticore.get(local_server).body
      expect(result["method"]).to eq "GET"
    end
  end
end