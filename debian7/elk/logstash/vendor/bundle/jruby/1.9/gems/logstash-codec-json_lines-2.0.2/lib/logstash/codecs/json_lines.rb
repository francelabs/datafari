# encoding: utf-8
require "logstash/codecs/base"
require "logstash/codecs/line"
require "logstash/json"

# This codec will decode streamed JSON that is newline delimited.
# Encoding will emit a single JSON string ending in a `\n`
# NOTE: Do not use this codec if your source input is line-oriented JSON, for
# example, redis or file inputs. Rather, use the json codec.
# More info: This codec is expecting to receive a stream (string) of newline
# terminated lines. The file input will produce a line string without a newline.
# Therefore this codec cannot work with line oriented inputs.
class LogStash::Codecs::JSONLines < LogStash::Codecs::Base
  config_name "json_lines"


  # The character encoding used in this codec. Examples include `UTF-8` and
  # `CP1252`
  #
  # JSON requires valid `UTF-8` strings, but in some cases, software that
  # emits JSON does so in another encoding (nxlog, for example). In
  # weird cases like this, you can set the charset setting to the
  # actual encoding of the text and logstash will convert it for you.
  #
  # For nxlog users, you'll want to set this to `CP1252`
  config :charset, :validate => ::Encoding.name_list, :default => "UTF-8"

  public

  def initialize(params={})
    super(params)
    @lines = LogStash::Codecs::Line.new
    @lines.charset = @charset
  end

  def decode(data)
    @lines.decode(data) do |event|
      yield guard(event, data)
    end
  end # def decode

  def encode(event)
    # Tack on a \n for now because previously most of logstash's JSON
    # outputs emitted one per line, and whitespace is OK in json.
    @on_event.call(event, "#{event.to_json}#{NL}")
  end # def encode

  private

  def guard(event, data)
    begin
      LogStash::Event.new(LogStash::Json.load(event["message"]))
    rescue LogStash::Json::ParserError => e
      LogStash::Event.new("message" => event["message"], "tags" => ["_jsonparsefailure"])
    end
  end

end # class LogStash::Codecs::JSONLines
