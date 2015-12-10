require 'stringio'
require 'minitest/autorun'
require File.dirname(__FILE__) + '/../lib/geoip'

if Minitest.const_defined?('Test')
  # We're on Minitest 5+. Nothing to do here.
else
  # Minitest 4 doesn't have Minitest::Test yet.
  Minitest::Test = MiniTest::Unit::TestCase
end
