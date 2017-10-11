package com.francelabs.datafari.handler.parsed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.apache.solr.handler.extraction.ExtractionDateUtil;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.NumberType;
import org.apache.solr.schema.SchemaField;
import org.apache.tika.metadata.TikaMetadataKeys;

public class ParsedContentHandler implements ExtractingParams {

  private final SolrInputDocument document;
  private final SolrParams params;
  private final boolean lowerNames;
  private final String unknownFieldPrefix;
  public static final String contentFieldName = "content";
  private final IndexSchema schema;
  private final InputStream is;
  private final String defaultField;
  private final Collection<String> dateFormats;

  public ParsedContentHandler(final SolrParams params, final IndexSchema schema, final InputStream is, final Collection<String> dateFormats) {
    this.params = params;
    this.schema = schema;
    this.dateFormats = dateFormats;
    this.is = is;
    this.lowerNames = params.getBool(LOWERNAMES, false);
    this.defaultField = params.get(DEFAULT_FIELD, "");
    this.unknownFieldPrefix = params.get(UNKNOWN_FIELD_PREFIX, "");
    this.document = new SolrInputDocument();
  }

  public SolrInputDocument newDocument() throws IOException {
    addLiterals();
    addContent();
    return document;
  }

  /**
   * Add in the literals to the document using the {@link #params} and the
   * {@link #LITERALS_PREFIX}.
   */
  private void addLiterals() {
    final Iterator<String> paramNames = params.getParameterNamesIterator();
    while (paramNames.hasNext()) {
      final String pname = paramNames.next();
      if (!pname.startsWith(LITERALS_PREFIX))
        continue;

      final String name = pname.substring(LITERALS_PREFIX.length());
      addField(name, null, params.getParams(pname));
    }
  }

  private void addContent() throws IOException {
    addField(contentFieldName, IOUtils.toString(is, StandardCharsets.UTF_8), null);
  }

  // Naming rules:
  // 1) optionally map names to nicenames (lowercase+underscores)
  // 2) execute "map" commands
  // 3) if resulting field is unknown, map it to a common prefix
  private void addField(String fname, String fval, String[] vals) {
    if (lowerNames) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fname.length(); i++) {
        char ch = fname.charAt(i);
        if (!Character.isLetterOrDigit(ch))
          ch = '_';
        else
          ch = Character.toLowerCase(ch);
        sb.append(ch);
      }
      fname = sb.toString();
    }

    String name = findMappedName(fname);
    SchemaField sf = schema.getFieldOrNull(name);
    if (sf == null && unknownFieldPrefix.length() > 0) {
      name = unknownFieldPrefix + name;
      sf = schema.getFieldOrNull(name);
    } else if (sf == null && defaultField.length() > 0 && name.equals(
        TikaMetadataKeys.RESOURCE_NAME_KEY) == false /*
                                                      * let the fall through
                                                      * below handle this
                                                      */) {
      name = defaultField;
      sf = schema.getFieldOrNull(name);
    }

    // Arguably we should handle this as a special case. Why? Because unlike
    // basically
    // all the other fields in metadata, this one was probably set not by Tika
    // by in
    // ExtractingDocumentLoader.load(). You shouldn't have to define a mapping
    // for this
    // field just because you specified a resource.name parameter to the
    // handler, should
    // you?
    if (sf == null && unknownFieldPrefix.length() == 0 && name == TikaMetadataKeys.RESOURCE_NAME_KEY) {
      return;
    }

    // normalize val params so vals.length>1
    if (vals != null && vals.length == 1) {
      fval = vals[0];
      vals = null;
    }

    // single valued field with multiple values... catenate them.
    if (sf != null && !sf.multiValued() && vals != null) {
      final StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (final String val : vals) {
        if (first) {
          first = false;
        } else {
          builder.append(' ');
        }
        builder.append(val);
      }
      fval = builder.toString();
      vals = null;
    }

    if (fval != null) {
      document.addField(name, transformValue(fval, sf));
    }

    if (vals != null) {
      for (final String val : vals) {
        document.addField(name, transformValue(val, sf));
      }
    }
  }

  /**
   * Can be used to transform input values based on their
   * {@link org.apache.solr.schema.SchemaField}
   * <p>
   * This implementation only formats dates using the
   * {@link ExtractionDateUtil}.
   *
   * @param val
   *          The value to transform
   * @param schFld
   *          The {@link org.apache.solr.schema.SchemaField}
   * @return The potentially new value.
   */
  protected String transformValue(final String val, final SchemaField schFld) {
    String result = val;
    if (schFld != null && NumberType.DATE.equals(schFld.getType().getNumberType())) {
      // try to transform the date
      try {
        final Date date = ExtractionDateUtil.parseDate(val, dateFormats); // may
                                                                          // throw
        result = date.toInstant().toString();// ISO format
      } catch (final Exception e) {
        // Let the specific fieldType handle errors
        // throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
        // "Invalid value: " + val + " for field: " + schFld, e);
      }
    }
    return result;
  }

  /**
   * Get the name mapping
   *
   * @param name
   *          The name to check to see if there is a mapping
   * @return The new name, if there is one, else <code>name</code>
   */
  protected String findMappedName(final String name) {
    return params.get(MAP_PREFIX + name, name);
  }
}
