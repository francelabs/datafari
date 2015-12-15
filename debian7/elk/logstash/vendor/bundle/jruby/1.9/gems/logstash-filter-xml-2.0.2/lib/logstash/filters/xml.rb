# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"

# XML filter. Takes a field that contains XML and expands it into
# an actual datastructure.
class LogStash::Filters::Xml < LogStash::Filters::Base

  config_name "xml"

  # Config for xml to hash is:
  # [source,ruby]
  #     source => source_field
  #
  # For example, if you have the whole XML document in your `message` field:
  # [source,ruby]
  #     filter {
  #       xml {
  #         source => "message"
  #       }
  #     }
  #
  # The above would parse the XML from the `message` field.
  config :source, :validate => :string, :required => true

  # Define target for placing the data
  #
  # For example if you want the data to be put in the `doc` field:
  # [source,ruby]
  #     filter {
  #       xml {
  #         target => "doc"
  #       }
  #     }
  #
  # XML in the value of the source field will be expanded into a
  # datastructure in the `target` field.
  # Note: if the `target` field already exists, it will be overridden.
  # Required if `store_xml` is true (which is the default).
  config :target, :validate => :string

  # xpath will additionally select string values (non-strings will be
  # converted to strings with Ruby's `to_s` function) from parsed XML
  # (using each source field defined using the method above) and place
  # those values in the destination fields. Configuration:
  # [source,ruby]
  # xpath => [ "xpath-syntax", "destination-field" ]
  #
  # Values returned by XPath parsing from `xpath-syntax` will be put in the
  # destination field. Multiple values returned will be pushed onto the
  # destination field as an array. As such, multiple matches across
  # multiple source fields will produce duplicate entries in the field.
  #
  # More on XPath: http://www.w3schools.com/xpath/
  #
  # The XPath functions are particularly powerful:
  # http://www.w3schools.com/xpath/xpath_functions.asp
  #
  config :xpath, :validate => :hash, :default => {}

  # By default the filter will store the whole parsed XML in the destination
  # field as described above. Setting this to false will prevent that.
  config :store_xml, :validate => :boolean, :default => true

  # Remove all namespaces from all nodes in the document.
  # Of course, if the document had nodes with the same names but different namespaces, they will now be ambiguous.
  config :remove_namespaces, :validate => :boolean, :default => false

  public
  def register
    require "nokogiri"
    require "xmlsimple"

  end # def register

  public
  def filter(event)
    
    matched = false

    @logger.debug("Running xml filter", :event => event)

    return unless event.include?(@source)

    value = event[@source]

    if value.is_a?(Array) && value.length > 1
      @logger.warn("XML filter only works on fields of length 1",
                   :source => @source, :value => value)
      return
    end

    # Do nothing with an empty string.
    return if value.strip.length == 0

    if @xpath
      begin
        doc = Nokogiri::XML(value, nil, value.encoding.to_s)
      rescue => e
        event.tag("_xmlparsefailure")
        @logger.warn("Trouble parsing xml", :source => @source, :value => value,
                     :exception => e, :backtrace => e.backtrace)
        return
      end
      doc.remove_namespaces! if @remove_namespaces
      @xpath.each do |xpath_src, xpath_dest|
        nodeset = doc.xpath(xpath_src)

        # If asking xpath for a String, like "name(/*)", we get back a
        # String instead of a NodeSet.  We normalize that here.
        normalized_nodeset = nodeset.kind_of?(Nokogiri::XML::NodeSet) ? nodeset : [nodeset]

        normalized_nodeset.each do |value|
          # some XPath functions return empty arrays as string
          if value.is_a?(Array)
            return if value.length == 0
          end

          unless value.nil?
            matched = true
            event[xpath_dest] ||= []
            event[xpath_dest] << value.to_s
          end
        end # XPath.each
      end # @xpath.each
    end # if @xpath

    if @store_xml
      begin
        event[@target] = XmlSimple.xml_in(value)
        matched = true
      rescue => e
        event.tag("_xmlparsefailure")
        @logger.warn("Trouble parsing xml with XmlSimple", :source => @source,
                     :value => value, :exception => e, :backtrace => e.backtrace)
        return
      end
    end # if @store_xml

    filter_matched(event) if matched
    @logger.debug("Event after xml filter", :event => event)
  end # def filter
end # class LogStash::Filters::Xml
