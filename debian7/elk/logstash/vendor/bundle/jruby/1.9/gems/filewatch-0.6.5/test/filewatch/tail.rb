$:.unshift(File.join(File.dirname(__FILE__), "..", "..", "lib"))
require "minitest/unit"
require "minitest/autorun"
require "filewatch/tail"

class TailTest < MiniTest::Unit::TestCase
  def test_quit
    require "timeout"
    tail = FileWatch::Tail.new
    #Thread.new(tail) { |t| sleep(1); t.quit }

    #Timeout.timeout(5) do
      #tail.subscribe { |e| }
    #end
    tail.quit
  end
end # class TailTest
