# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"

# A simple output which prints to the STDOUT of the shell running
# Logstash. This output can be quite convenient when debugging
# plugin configurations, by allowing instant access to the event
# data after it has passed through the inputs and filters.
#
# For example, the following output configuration, in conjunction with the
# Logstash `-e` command-line flag, will allow you to see the results
# of your event pipeline for quick iteration. 
# [source,ruby]
#     output {
#       stdout {}
#     }
# 
# Useful codecs include:
#
# `rubydebug`: outputs event data using the ruby "awesome_print"
# http://rubygems.org/gems/awesome_print[library]
#
# [source,ruby]
#     output {
#       stdout { codec => rubydebug }
#     }
#
# `json`: outputs event data in structured JSON format
# [source,ruby]
#     output {
#       stdout { codec => json }
#     }
#
class LogStash::Outputs::Stdout < LogStash::Outputs::Base
  begin
     require "awesome_print"
  rescue LoadError
  end

  config_name "stdout"
  
  default :codec, "line"

  public
  def register
    @codec.on_event do |event, data|
      $stdout.write(data)
    end
  end

  def receive(event)
    
    return if event == LogStash::SHUTDOWN
    @codec.encode(event)
  end

end # class LogStash::Outputs::Stdout
