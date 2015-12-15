# encoding: UTF-8

$LOAD_PATH << File.expand_path('../../lib', __FILE__)

require "java"

require 'test/unit'
require 'thread'
require 'bigdecimal'
require 'jrjackson'
require 'stringio'
require 'time'

class JrJacksonTest < Test::Unit::TestCase

  class CustomToH
    attr_accessor :one, :two, :six
    def initialize(a,b,c)
      @one, @two, @six = a,b,c
    end
    def to_h
      {'one' => one, 'two' => two, 'six' => six}
    end
  end

  class CustomToHash
    attr_accessor :one, :two, :six
    def initialize(a,b,c)
      @one, @two, @six = a,b,c
    end
    def to_hash
      {'one' => one, 'two' => two, 'six' => six}
    end
  end

  class CustomToJson
    attr_accessor :one, :two, :six
    def initialize(a,b,c)
      @one, @two, @six = a,b,c
    end
    def to_json
      %Q|{"one":#{one},"two":#{two},"six":#{six}}|
    end
  end

  class CustomToTime
    def initialize(tm = Time.now)
      @now = tm
    end
    def to_time
      @now
    end
  end

  CustomStruct = Struct.new(:one, :two, :six)

  def test_threading
    q1, q2, q3 = Queue.new, Queue.new, Queue.new

    s1 = %Q|{"a":2.5, "b":0.214, "c":3.4567, "d":-3.4567}|
    th1 = Thread.new(s1) do |string|
      q1 << JrJackson::Json.load(string, {use_bigdecimal: true, raw: true})
    end
    th2 = Thread.new(s1) do |string|
      q2 << JrJackson::Json.load(string, {use_bigdecimal: true, symbolize_keys: true})
    end
    th3 = Thread.new(s1) do |string|
      q3 << JrJackson::Json.load(string, {use_bigdecimal: false, symbolize_keys: true})
    end
    a1, a2, a3 = q1.pop, q2.pop, q3.pop
    assert_equal ["a", "b", "c", "d"], a1.keys
    assert a1.values.all? {|v| v.is_a?(Java::JavaMath::BigDecimal)}, "Expected all values to be Java::JavaMath::BigDecimal"
    assert_equal [:a, :b, :c, :d], a2.keys
    assert a2.values.all? {|v| v.is_a?(BigDecimal)}, "Expected all values to be BigDecimal"
    assert a3.values.all? {|v| v.is_a?(Float)}, "Expected all values to be Float"
  end

  def test_deserialize_JSON_with_UTF8_characters
    json_string = JrJackson::Json.dump({"utf8" => "żółć"})
    expected = {utf8: "żółć"}
    actual = JrJackson::Json.load(json_string, :symbolize_keys => true)
    assert_equal expected, actual
  end

  def test_serialize_non_json_datatypes_as_values
    dt = Time.now
    co1 = CustomToH.new("uno", :two, 6.0)
    co2 = CustomToHash.new("uno", :two, 6.0)
    co3 = CustomToJson.new(1.0, 2, 6.0)
    co4 = CustomStruct.new(1, 2, 6)
    co5 = CustomToTime.new(dt)
    source = {'sym' => :a_symbol, 'dt' => dt, 'co1' => co1, 'co2' => co2, 'co3' => co3, 'co4' => co4, 'co5' => co5}
    json_string = JrJackson::Json.dump(source)
    expected = {
      :sym => "a_symbol",
      :dt => dt.strftime("%F %T %z"),
      :co1 => {:one => "uno", :two => "two", :six => 6.0 },
      :co2 => {:one => "uno", :two => "two", :six => 6.0 },
      :co3 => {:one => 1.0, :two => 2.0, :six => 6.0 },
      :co4 => [1, 2, 6],
      :co5 => dt.strftime("%F %T %z")
    }
    actual = JrJackson::Json.load(json_string, :symbolize_keys => true)
    assert_equal expected, actual
  end

  def test_raw_serialize_base_classes
    # String
    assert_equal JrJackson::Json.dump("foo"), "\"foo\""

    # Hash and implementations of the Java Hash interface
    assert_equal JrJackson::Json.dump({"foo" => 1}), "{\"foo\":1}"
    assert_equal JrJackson::Json.dump(Java::JavaUtil::HashMap.new({"foo" => 1})), "{\"foo\":1}"
    assert_equal JrJackson::Json.dump(Java::JavaUtil::LinkedHashMap.new({"foo" => 1})), "{\"foo\":1}"

    # Array and implementations of the Java List interface
    assert_equal JrJackson::Json.dump(["foo", 1]), "[\"foo\",1]"
    assert_equal JrJackson::Json.dump(Java::JavaUtil::ArrayList.new(["foo", 1])), "[\"foo\",1]"
    assert_equal JrJackson::Json.dump(Java::JavaUtil::LinkedList.new(["foo", 1])), "[\"foo\",1]"
    assert_equal JrJackson::Json.dump(Java::JavaUtil::Vector.new(["foo", 1])), "[\"foo\",1]"

    # true/false
    assert_equal JrJackson::Json.dump(true), "true"
    assert_equal JrJackson::Json.dump(false), "false"

    # nil
    assert_equal JrJackson::Json.dump(nil), "null"
  end

  def test_serialize_date
    # default date format
    time_string = "2014-06-10 18:18:40 EDT"
    source_time = Time.parse(time_string)
    serialized_output = JrJackson::Json.dump({"time" => source_time})
    other_time = Time.parse(serialized_output.split('"')[-2])
    assert_equal other_time.to_f, source_time.to_f
  end

  def test_serialize_date_date_format

    time = Time.new(2014,6,10,18,18,40, "-04:00")
    # using date_format option
    assert_equal "{\"time\":\"2014-06-10\"}", JrJackson::Json.dump({"time" => time}, :date_format => "yyyy-MM-dd")
    assert_match /\{"time"\:"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}[+-]\d{4}"\}/, JrJackson::Json.dump({"time" => time}, :date_format => "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  end

  def test_serialize_date_date_format_timezone

    time = Time.new(2014,6,10,18,18,40, "-04:00")
    # using date_format and timezone options
    assert_equal "{\"time\":\"2014-06-10T22:18:40.000+0000\"}", JrJackson::Json.dump({"time" => time}, :date_format => "yyyy-MM-dd'T'HH:mm:ss.SSSZ", :timezone => "UTC")
    # iso8601 date_format and timezone
    assert_equal "{\"time\":\"2014-06-10T22:18:40.000Z\"}", JrJackson::Json.dump({"time" => time}, :date_format => "yyyy-MM-dd'T'HH:mm:ss.SSSX", :timezone => "UTC")
  end

  def test_can_parse_nulls
    expected = {"foo" => nil}
    json = '{"foo":null}'
    actual = JrJackson::Json.parse(json)
    assert_equal expected, actual
  end

  def test_stringio
    expected = {"foo" => 5, "utf8" => "żółć"}
    json = ::StringIO.new('{"foo":5, "utf8":"żółć"}')
    actual = JrJackson::Json.load(json)
    assert_equal expected, actual
  end

  def test_ruby_io
    expected = {"foo" => 5, "bar" => 6, "utf8" => "żółć"}
    json, w = IO.pipe
    w.write('{"foo":5, "bar":6, "utf8":"żółć"}')
    w.close
    actual = JrJackson::Json.load(json)
    assert_equal expected, actual
  end

  def test_bad_utf
    assert_raise JrJackson::ParseError do
      JrJackson::Json.load("\x82\xAC\xEF")
    end
  end

  def test_can_parse_bignum
    expected = 12345678901234567890123456789
    json = '{"foo":12345678901234567890123456789}'

    actual = JrJackson::Json.parse(json)['foo']
    assert_equal expected, actual
  end

  def test_can_parse_big_decimals
    expected = BigDecimal.new '0.12345678901234567890123456789'
    json = '{"foo":0.12345678901234567890123456789}'

    actual = JrJackson::Json.parse(json, :use_bigdecimal => true)['foo']
    assert_bigdecimal_equal expected, actual

    actual = JrJackson::Json.parse(json, :use_bigdecimal => true, :symbolize_keys => true)[:foo]
    assert_bigdecimal_equal expected, actual

    actual = JrJackson::Json.parse(json, :use_bigdecimal => true, :raw => true)['foo']
    assert_bigdecimal_similar expected, actual
  end

  def test_cannot_serialize_object
    err = assert_raises(JrJackson::ParseError) { JrJackson::Json.dump({"foo" => Object.new}) }
    assert_match /Cannot find Serializer for class: org.jruby.RubyObject/, err.message
  end

  def test_cannot_serialize_basic_object
    err = assert_raises(JrJackson::ParseError) { JrJackson::Json.dump({"foo" => BasicObject.new}) }
    assert_match /Cannot find Serializer for class: org.jruby.RubyBasicObject/, err.message
  end

  def assert_bigdecimal_equal(expected, actual)
    assert_equal expected, actual
    assert_equal expected.class, actual.class
    assert_equal BigDecimal, actual.class
  end

  def assert_bigdecimal_similar(expected, actual)
    assert_equal BigDecimal.new(expected.to_s), BigDecimal.new(actual.to_s)
    assert_equal Java::JavaMath::BigDecimal, actual.class
  end

end
