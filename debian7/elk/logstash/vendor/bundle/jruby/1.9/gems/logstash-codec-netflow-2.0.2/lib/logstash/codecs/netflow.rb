# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"
require "logstash/timestamp"

# The "netflow" codec is for decoding Netflow v5/v9 flows.
class LogStash::Codecs::Netflow < LogStash::Codecs::Base
  config_name "netflow"

  # Netflow v9 template cache TTL (minutes)
  config :cache_ttl, :validate => :number, :default => 4000

  # Specify into what field you want the Netflow data.
  config :target, :validate => :string, :default => "netflow"

  # Specify which Netflow versions you will accept.
  config :versions, :validate => :array, :default => [5, 9]

  # Override YAML file containing Netflow field definitions
  #
  # Each Netflow field is defined like so:
  #
  #    ---
  #    id:
  #    - default length in bytes
  #    - :name
  #    id:
  #    - :uintN or :ip4_addr or :ip6_addr or :mac_addr or :string
  #    - :name
  #    id:
  #    - :skip
  #
  # See <https://github.com/logstash-plugins/logstash-codec-netflow/blob/master/lib/logstash/codecs/netflow/netflow.yaml> for the base set.
  config :definitions, :validate => :path

  NETFLOW5_FIELDS = ['version', 'flow_seq_num', 'engine_type', 'engine_id', 'sampling_algorithm', 'sampling_interval', 'flow_records']
  NETFLOW9_FIELDS = ['version', 'flow_seq_num']
  SWITCHED = /_switched$/
  FLOWSET_ID = "flowset_id"

  def initialize(params = {})
    super(params)
    @threadsafe = false
  end

  def register
    require "logstash/codecs/netflow/util"
    @templates = Vash.new()

    # Path to default Netflow v9 field definitions
    filename = ::File.expand_path('netflow/netflow.yaml', ::File.dirname(__FILE__))

    begin
      @fields = YAML.load_file(filename)
    rescue Exception => e
      raise "#{self.class.name}: Bad syntax in definitions file #{filename}"
    end

    # Allow the user to augment/override/rename the supported Netflow fields
    if @definitions
      raise "#{self.class.name}: definitions file #{@definitions} does not exists" unless File.exists?(@definitions)
      begin
        @fields.merge!(YAML.load_file(@definitions))
      rescue Exception => e
        raise "#{self.class.name}: Bad syntax in definitions file #{@definitions}"
      end
    end
  end # def register

  def decode(payload, &block)
    header = Header.read(payload)

    unless @versions.include?(header.version)
      @logger.warn("Ignoring Netflow version v#{header.version}")
      return
    end

    if header.version == 5
      flowset = Netflow5PDU.read(payload)
      flowset.records.each do |record|
        yield(decode_netflow5(flowset, record))
      end
    elsif header.version == 9
      flowset = Netflow9PDU.read(payload)
      flowset.records.each do |record|
        decode_netflow9(flowset, record).each{|event| yield(event)}
      end
    else
      @logger.warn("Unsupported Netflow version v#{header.version}")
    end
  end

  private

  def decode_netflow5(flowset, record)
    event = {
      LogStash::Event::TIMESTAMP => LogStash::Timestamp.at(flowset.unix_sec.snapshot, flowset.unix_nsec.snapshot / 1000),
      @target => {}
    }

    # Copy some of the pertinent fields in the header to the event
    NETFLOW5_FIELDS.each do |f|
      event[@target][f] = flowset[f].snapshot
    end

    # Create fields in the event from each field in the flow record
    record.each_pair do |k, v|
      case k.to_s
      when SWITCHED
        # The flow record sets the first and last times to the device
        # uptime in milliseconds. Given the actual uptime is provided
        # in the flowset header along with the epoch seconds we can
        # convert these into absolute times
        millis = flowset.uptime - v
        seconds = flowset.unix_sec - (millis / 1000)
        micros = (flowset.unix_nsec / 1000) - (millis % 1000)
        if micros < 0
          seconds--
          micros += 1000000
        end
        event[@target][k.to_s] = LogStash::Timestamp.at(seconds, micros).to_iso8601
      else
        event[@target][k.to_s] = v.snapshot
      end
    end

    LogStash::Event.new(event)
  end

  def decode_netflow9(flowset, record)
    events = []

    case record.flowset_id
    when 0
      # Template flowset
      record.flowset_data.templates.each do |template|
        catch (:field) do
          fields = []
          template.fields.each do |field|
            entry = netflow_field_for(field.field_type, field.field_length)
            throw :field unless entry
            fields += entry
          end
          # We get this far, we have a list of fields
          #key = "#{flowset.source_id}|#{event["source"]}|#{template.template_id}"
          key = "#{flowset.source_id}|#{template.template_id}"
          @templates[key, @cache_ttl] = BinData::Struct.new(:endian => :big, :fields => fields)
          # Purge any expired templates
          @templates.cleanup!
        end
      end
    when 1
      # Options template flowset
      record.flowset_data.templates.each do |template|
        catch (:field) do
          fields = []
          template.option_fields.each do |field|
            entry = netflow_field_for(field.field_type, field.field_length)
            throw :field unless entry
            fields += entry
          end
          # We get this far, we have a list of fields
          #key = "#{flowset.source_id}|#{event["source"]}|#{template.template_id}"
          key = "#{flowset.source_id}|#{template.template_id}"
          @templates[key, @cache_ttl] = BinData::Struct.new(:endian => :big, :fields => fields)
          # Purge any expired templates
          @templates.cleanup!
        end
      end
    when 256..65535
      # Data flowset
      #key = "#{flowset.source_id}|#{event["source"]}|#{record.flowset_id}"
      key = "#{flowset.source_id}|#{record.flowset_id}"
      template = @templates[key]

      unless template
        #@logger.warn("No matching template for flow id #{record.flowset_id} from #{event["source"]}")
        @logger.warn("No matching template for flow id #{record.flowset_id}")
        next
      end

      length = record.flowset_length - 4

      # Template shouldn't be longer than the record and there should
      # be at most 3 padding bytes
      if template.num_bytes > length or ! (length % template.num_bytes).between?(0, 3)
        @logger.warn("Template length doesn't fit cleanly into flowset", :template_id => record.flowset_id, :template_length => template.num_bytes, :record_length => length)
        next
      end

      array = BinData::Array.new(:type => template, :initial_length => length / template.num_bytes)
      records = array.read(record.flowset_data)

      records.each do |r|
        event = {
          LogStash::Event::TIMESTAMP => LogStash::Timestamp.at(flowset.unix_sec),
          @target => {}
        }

        # Fewer fields in the v9 header
        NETFLOW9_FIELDS.each do |f|
          event[@target][f] = flowset[f].snapshot
        end

        event[@target][FLOWSET_ID] = record.flowset_id.snapshot

        r.each_pair do |k, v|
          case k.to_s
          when SWITCHED
            millis = flowset.uptime - v
            seconds = flowset.unix_sec - (millis / 1000)
            # v9 did away with the nanosecs field
            micros = 1000000 - (millis % 1000)
            event[@target][k.to_s] = LogStash::Timestamp.at(seconds, micros).to_iso8601
          else
            event[@target][k.to_s] = v.snapshot
          end
        end

        events << LogStash::Event.new(event)
      end
    else
      @logger.warn("Unsupported flowset id #{record.flowset_id}")
    end

    events
  end

  def uint_field(length, default)
    # If length is 4, return :uint32, etc. and use default if length is 0
    ("uint" + (((length > 0) ? length : default) * 8).to_s).to_sym
  end # def uint_field

  def netflow_field_for(type, length)
    if @fields.include?(type)
      field = @fields[type]
      if field.is_a?(Array)

        field[0] = uint_field(length, field[0]) if field[0].is_a?(Integer)

        # Small bit of fixup for skip or string field types where the length
        # is dynamic
        case field[0]
        when :skip
          field += [nil, {:length => length}]
        when :string
          field += [{:length => length, :trim_padding => true}]
        end

        @logger.debug? and @logger.debug("Definition complete", :field => field)

        [field]
      else
        @logger.warn("Definition should be an array", :field => field)
        nil
      end
    else
      @logger.warn("Unsupported field", :type => type, :length => length)
      nil
    end
  end # def netflow_field_for
end # class LogStash::Filters::Netflow
