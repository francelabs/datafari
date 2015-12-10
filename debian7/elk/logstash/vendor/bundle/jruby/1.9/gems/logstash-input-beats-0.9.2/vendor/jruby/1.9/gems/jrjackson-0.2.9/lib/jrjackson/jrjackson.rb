unless RUBY_PLATFORM =~ /java/
  puts "This library is only compatible with a java-based ruby environment like JRuby."
  exit 255
end

require_relative "jars/jrjackson-1.2.12.jar"
# require_relative "linked/jrjackson-1.2.12.jar"

require 'com/jrjackson/jr_jackson'

module JrJackson
  module Json
      class << self
      TIME_REGEX = %r(\A(\d{4}-\d\d-\d\d|(\w{3}\s){2}\d\d)\s\d\d:\d\d:\d\d)

      def load(json_string, options = nil)
        if json_string.is_a?(String) && is_time_string?(json_string)
          return JrJackson::Raw.parse_raw("\"#{json_string}\"")
        end

        if options && !options.empty?
          if options.size == 1 && !!options[:raw]
            return JrJackson::Raw.parse_raw(json_string)
          end
          if options.size == 1 && !!options[:symbolize_keys]
            return JrJackson::Raw.parse_sym(json_string)
          end
          JrJackson::Raw.parse(json_string, options)
        else
          JrJackson::Raw.parse_str(json_string)
        end
      end

      def dump(object, options = {})
        case object
        when Hash, Array, String, Java::JavaUtil::Map, Java::JavaUtil::List
          JrJackson::Raw.generate(object, options)
        when true, false
          object.to_s
        when nil
          'null'
        else
          if object.respond_to?(:to_json)
            object.to_json
          elsif object.respond_to?(:to_s)
            object.to_s
          else
            object
          end
        end
      end

      alias :parse :load
      alias :generate :dump

      private

      def is_time_string?(json_string)
        json_string =~ TIME_REGEX
      rescue => e
        raise JrJackson::ParseError, e.message
      end
    end
  end
end
