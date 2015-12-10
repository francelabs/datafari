# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"
require "tempfile"
require "lru_redux"

# The GeoIP filter adds information about the geographical location of IP addresses,
# based on data from the Maxmind database.
#
# Starting with version 1.3.0 of Logstash, a `[geoip][location]` field is created if
# the GeoIP lookup returns a latitude and longitude. The field is stored in
# http://geojson.org/geojson-spec.html[GeoJSON] format. Additionally,
# the default Elasticsearch template provided with the
# <<plugins-outputs-elasticsearch,`elasticsearch` output>> maps
# the `[geoip][location]` field to an https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_mapping_options[Elasticsearch geo_point].
#
# As this field is a `geo_point` _and_ it is still valid GeoJSON, you get
# the awesomeness of Elasticsearch's geospatial query, facet and filter functions
# and the flexibility of having GeoJSON for all other applications (like Kibana's
# map visualization).
#
# Logstash releases ship with the GeoLiteCity database made available from
# Maxmind with a CCA-ShareAlike 3.0 license. For more details on GeoLite, see
# <http://www.maxmind.com/en/geolite>.
class LogStash::Filters::GeoIP < LogStash::Filters::Base
  LOOKUP_CACHE_INIT_MUTEX = Mutex.new
  # Map of lookup caches, keyed by geoip_type
  LOOKUP_CACHES = {}

  attr_accessor :lookup_cache
  attr_reader :threadkey

  config_name "geoip"

  # The path to the GeoIP database file which Logstash should use. Country, City, ASN, ISP
  # and organization databases are supported.
  #
  # If not specified, this will default to the GeoLiteCity database that ships
  # with Logstash.
  # Up-to-date databases can be downloaded from here: <https://dev.maxmind.com/geoip/legacy/geolite/>
  # Please be sure to download a legacy format database.
  config :database, :validate => :path

  # The field containing the IP address or hostname to map via geoip. If
  # this field is an array, only the first value will be used.
  config :source, :validate => :string, :required => true

  # An array of geoip fields to be included in the event.
  #
  # Possible fields depend on the database type. By default, all geoip fields
  # are included in the event.
  #
  # For the built-in GeoLiteCity database, the following are available:
  # `city_name`, `continent_code`, `country_code2`, `country_code3`, `country_name`,
  # `dma_code`, `ip`, `latitude`, `longitude`, `postal_code`, `region_name` and `timezone`.
  config :fields, :validate => :array

  # Specify the field into which Logstash should store the geoip data.
  # This can be useful, for example, if you have `src\_ip` and `dst\_ip` fields and
  # would like the GeoIP information of both IPs.
  #
  # If you save the data to a target field other than `geoip` and want to use the
  # `geo\_point` related functions in Elasticsearch, you need to alter the template
  # provided with the Elasticsearch output and configure the output to use the
  # new template.
  #
  # Even if you don't use the `geo\_point` mapping, the `[target][location]` field
  # is still valid GeoJSON.
  config :target, :validate => :string, :default => 'geoip'

  # GeoIP lookup is surprisingly expensive. This filter uses an LRU cache to take advantage of the fact that
  # IPs agents are often found adjacent to one another in log files and rarely have a random distribution.
  # The higher you set this the more likely an item is to be in the cache and the faster this filter will run.
  # However, if you set this too high you can use more memory than desired.
  #
  # Experiment with different values for this option to find the best performance for your dataset.
  #
  # This MUST be set to a value > 0. There is really no reason to not want this behavior, the overhead is minimal
  # and the speed gains are large.
  #
  # It is important to note that this config value is global to the geoip_type. That is to say all instances of the geoip filter
  # of the same geoip_type share the same cache. The last declared cache size will 'win'. The reason for this is that there would be no benefit
  # to having multiple caches for different instances at different points in the pipeline, that would just increase the
  # number of cache misses and waste memory.
  config :lru_cache_size, :validate => :number, :default => 1000

  public
  def register
    require "geoip"

    if @database.nil?
      @database = ::Dir.glob(::File.join(::File.expand_path("../../../vendor/", ::File.dirname(__FILE__)),"GeoLiteCity*.dat")).first
      if !File.exists?(@database)
        raise "You must specify 'database => ...' in your geoip filter (I looked for '#{@database}'"
      end
    end
    @logger.info("Using geoip database", :path => @database)
    # For the purpose of initializing this filter, geoip is initialized here but
    # not set as a global. The geoip module imposes a mutex, so the filter needs
    # to re-initialize this later in the filter() thread, and save that access
    # as a thread-local variable.
    geoip_initialize = ::GeoIP.new(@database)

    @geoip_type = case geoip_initialize.database_type
    when GeoIP::GEOIP_CITY_EDITION_REV0, GeoIP::GEOIP_CITY_EDITION_REV1
      :city
    when GeoIP::GEOIP_COUNTRY_EDITION
      :country
    when GeoIP::GEOIP_ASNUM_EDITION
      :asn
    when GeoIP::GEOIP_ISP_EDITION, GeoIP::GEOIP_ORG_EDITION
      :isp
    else
      raise RuntimeException.new "This GeoIP database is not currently supported"
    end

    @threadkey = "geoip-#{self.object_id}"

    # This is wrapped in a mutex to make sure the initialization behavior of LOOKUP_CACHES (see def above) doesn't create a dupe
    LOOKUP_CACHE_INIT_MUTEX.synchronize do
      self.lookup_cache = LOOKUP_CACHES[@geoip_type] ||= LruRedux::ThreadSafeCache.new(1000)
    end
  end # def register

  public
  def filter(event)
    
    geo_data = nil

    geo_data = get_geo_data(event)

    return if geo_data.nil? || !geo_data.respond_to?(:to_hash)

    apply_geodata(geo_data, event)

    filter_matched(event)
  end # def filter

  def apply_geodata(geo_data,event)
    geo_data_hash = geo_data.to_hash
    geo_data_hash.delete(:request)
    event[@target] = {} if event[@target].nil?
    if geo_data_hash.key?(:latitude) && geo_data_hash.key?(:longitude)
      # If we have latitude and longitude values, add the location field as GeoJSON array
      geo_data_hash[:location] = [ geo_data_hash[:longitude].to_f, geo_data_hash[:latitude].to_f ]
    end
    geo_data_hash.each do |key, value|
      next if value.nil? || (value.is_a?(String) && value.empty?)
      if @fields.nil? || @fields.empty? || @fields.include?(key.to_s)
        # convert key to string (normally a Symbol)
        if value.is_a?(String)
          # Some strings from GeoIP don't have the correct encoding...
          value = case value.encoding
                    # I have found strings coming from GeoIP that are ASCII-8BIT are actually
                    # ISO-8859-1...
                    when Encoding::ASCII_8BIT; value.force_encoding(Encoding::ISO_8859_1).encode(Encoding::UTF_8)
                    when Encoding::ISO_8859_1, Encoding::US_ASCII;  value.encode(Encoding::UTF_8)
                    else; value.dup
                  end
        end
        event[@target][key.to_s] = value
      end
    end # geo_data_hash.each
  end

  def get_geo_data(event)
    ip = event[@source]
    ip = ip.first if ip.is_a? Array

    get_geo_data_for_ip(ip)
  rescue SocketError => e
    @logger.error("IP Field contained invalid IP address or hostname", :field => @source, :event => event)
  rescue StandardError => e
    @logger.error("Unknown error while looking up GeoIP data", :exception => e, :field => @source, :event => event)
  end

  def get_geo_data_for_ip(ip)
    ensure_database!
    if (cached = lookup_cache[ip])
      cached
    else
      geo_data = Thread.current[threadkey].send(@geoip_type, ip)
      lookup_cache[ip] = geo_data
      geo_data
    end
  end

  def ensure_database!
    # Use thread-local access to GeoIP. The Ruby GeoIP module forces a mutex
    # around access to the database, which can be overcome with :pread.
    # Unfortunately, :pread requires the io-extra gem, with C extensions that
    # aren't supported on JRuby. If / when :pread becomes available, we can stop
    # needing thread-local access.
    Thread.current[threadkey] ||= ::GeoIP.new(@database)
  end
end # class LogStash::Filters::GeoIP
