require 'forwardable'

module Manticore
  # Mix-in that can be used to add Manticore functionality to arbitrary classes.
  # Its primary purpose is to extend the Manticore module for one-shot usage.
  #
  # @example  Simple mix-in usage
  #     class Foo
  #       include Manticore::Facade
  #       include_http_client pool_size: 5
  #
  #       def latest_results
  #         Foo.get "http://some.com/url"
  #       end
  #     end
  module Facade
    # @private
    def self.included(other)
      other.send :extend, ClassMethods
    end

    module ClassMethods
      # Adds basic synchronous Manticore::Client functionality to the receiver.
      # @param  options Hash Options to be passed to the underlying shared client, if it has not been created yet.
      # @return nil
      def include_http_client(options = {}, &block)
        if shared_pool = options.delete(:shared_pool)
          @manticore_facade = Manticore.instance_variable_get("@manticore_facade")
        else
          @manticore_facade = Manticore::Client.new(options, &block)
        end
        class << self
          extend Forwardable
          def_delegators "@manticore_facade", :get, :head, :put, :post, :delete, :options, :patch
        end
        nil
      end
    end
  end
end