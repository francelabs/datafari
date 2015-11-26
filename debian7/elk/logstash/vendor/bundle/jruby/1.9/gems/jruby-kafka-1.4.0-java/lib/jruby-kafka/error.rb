require 'jruby-kafka/namespace'

class KafkaError < StandardError
  attr_reader :object

  def initialize(object)
    @object = object
  end
end