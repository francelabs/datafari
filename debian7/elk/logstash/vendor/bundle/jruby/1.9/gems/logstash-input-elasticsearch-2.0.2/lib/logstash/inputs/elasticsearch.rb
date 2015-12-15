# encoding: utf-8
require "logstash/inputs/base"
require "logstash/namespace"
require "base64"

# Read from an Elasticsearch cluster, based on search query results.
# This is useful for replaying test logs, reindexing, etc.
#
# Example:
# [source,ruby]
#     input {
#       # Read all documents from Elasticsearch matching the given query
#       elasticsearch {
#         hosts => "localhost"
#         query => '{ "query": { "match": { "statuscode": 200 } } }'
#       }
#     }
#
# This would create an Elasticsearch query with the following format:
# [source,json]
#     curl 'http://localhost:9200/logstash-*/_search?&scroll=1m&size=1000' -d '{
#       "query": {
#         "match": {
#           "statuscode": 200
#         }
#       }
#     }'
#
class LogStash::Inputs::Elasticsearch < LogStash::Inputs::Base
  config_name "elasticsearch"

  default :codec, "json"

  # List of elasticsearch hosts to use for querying.
  # each host can be either IP, HOST, IP:port or HOST:port
  # port defaults to 9200
  config :hosts, :validate => :array

  # The index or alias to search.
  config :index, :validate => :string, :default => "logstash-*"

  # The query to be executed.
  config :query, :validate => :string, :default => '{"query": { "match_all": {} } }'

  # Enable the Elasticsearch "scan" search type.  This will disable
  # sorting but increase speed and performance.
  config :scan, :validate => :boolean, :default => true

  # This allows you to set the maximum number of hits returned per scroll.
  config :size, :validate => :number, :default => 1000

  # This parameter controls the keepalive time in seconds of the scrolling
  # request and initiates the scrolling process. The timeout applies per
  # round trip (i.e. between the previous scan scroll request, to the next).
  config :scroll, :validate => :string, :default => "1m"

  # If set, include Elasticsearch document information such as index, type, and
  # the id in the event.
  #
  # It might be important to note, with regards to metadata, that if you're
  # ingesting documents with the intent to re-index them (or just update them)
  # that the `action` option in the elasticsearch output want's to know how to
  # handle those things. It can be dynamically assigned with a field 
  # added to the metadata.
  #
  # Example
  # [source, ruby]
  #     input {
  #       elasticsearch {
  #         hosts => "es.production.mysite.org"
  #         index => "mydata-2018.09.*"
  #         query => "*"
  #         size => 500
  #         scroll => "5m"
  #         docinfo => true
  #       }
  #     }
  #     output {
  #       elasticsearch {
  #         index => "copy-of-production.%{[@metadata][_index]}"
  #         index_type => "%{[@metadata][_type]}"
  #         document_id => "%{[@metadata][_id]}"
  #       }
  #     }
  #
  config :docinfo, :validate => :boolean, :default => false

  # Where to move the Elasticsearch document information by default we use the @metadata field.
  config :docinfo_target, :validate=> :string, :default => "@metadata"

  # List of document metadata to move to the `docinfo_target` field
  # To learn more about Elasticsearch metadata fields read
  # http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_document_metadata.html
  config :docinfo_fields, :validate => :array, :default => ['_index', '_type', '_id']

  # Basic Auth - username
  config :user, :validate => :string

  # Basic Auth - password
  config :password, :validate => :password

  # SSL
  config :ssl, :validate => :boolean, :default => false

  # SSL Certificate Authority file
  config :ca_file, :validate => :path

  public
  def register
    require "elasticsearch"

    @options = {
      :index => @index,
      :body => @query,
      :scroll => @scroll,
      :size => @size
    }

    @options[:search_type] = 'scan' if @scan

    transport_options = {}

    if @user && @password
      token = Base64.strict_encode64("#{@user}:#{@password.value}")
      transport_options[:headers] = { :Authorization => "Basic #{token}" }
    end

    hosts = if @ssl then
      @hosts.map do |h|
        host, port = h.split(":")
        { :host => host, :scheme => 'https', :port => port }
      end
    else
      @hosts
    end

    if @ssl && @ca_file
      transport_options[:ssl] = { :ca_file => @ca_file }
    end

    @client = Elasticsearch::Client.new(:hosts => hosts, :transport_options => transport_options)
  end

  public
  def run(output_queue)

    # get first wave of data
    r = @client.search(@options)

    # since 'scan' doesn't return data on the search call, do an extra scroll
    if @scan
      r = process_next_scroll(output_queue, r['_scroll_id'])
      has_hits = r['has_hits']
    else # not a scan, process the response
      r['hits']['hits'].each { |hit| push_hit(hit, output_queue) }
      has_hits = r['hits']['hits'].any?
    end

    while has_hits && !stop?
      r = process_next_scroll(output_queue, r['_scroll_id'])
      has_hits = r['has_hits']
    end
  end # def run

  private
  def process_next_scroll(output_queue, scroll_id)
    r = scroll_request(scroll_id)
    r['hits']['hits'].each { |hit| push_hit(hit, output_queue) }
    {'has_hits' => r['hits']['hits'].any?, '_scroll_id' => r['_scroll_id']}
  end

  private
  def push_hit(hit, output_queue)
    event = LogStash::Event.new(hit['_source'])
    decorate(event)

    if @docinfo
      event[@docinfo_target] ||= {}

      unless event[@docinfo_target].is_a?(Hash)
        @logger.error("Elasticsearch Input: Incompatible Event, incompatible type for the `@metadata` field in the `_source` document, expected a hash got:", :metadata_type => event[@docinfo_target].class)

        raise Exception.new("Elasticsearch input: incompatible event")
      end

      @docinfo_fields.each do |field|
        event[@docinfo_target][field] = hit[field]
      end
    end
    output_queue << event
  end

  private
  def scroll_request scroll_id
    @client.scroll(:body => scroll_id, :scroll => @scroll)
  end
end # class LogStash::Inputs::Elasticsearch
