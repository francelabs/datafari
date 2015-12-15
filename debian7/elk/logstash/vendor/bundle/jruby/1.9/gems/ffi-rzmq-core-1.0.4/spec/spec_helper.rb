require File.expand_path(
File.join(File.dirname(__FILE__), %w[.. lib ffi-rzmq-core]))

require 'rspec'
require 'rspec/autorun'

Dir[File.join(File.dirname(__FILE__), "support", "*.rb")].each do |support|
  require support
end

RSpec.configure do |config|
  config.include VersionChecking
end

