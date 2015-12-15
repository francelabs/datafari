require 'spec_assist'
require 'webmock/rspec'
require 'coveralls'
require 'codeclimate-test-reporter'

CodeClimate::TestReporter.start
Coveralls.wear!

RSpec.configure do |config|
  config.extend Assist
  config.order = :random
end
