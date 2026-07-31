package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

/**
 * Compatibility update processor used to restore the metadata normalization
 * behavior that was previously provided by Datafari's ParsedRequestHandler /
 * Solr ExtractingRequestHandler.
 *
 * <p>In Datafari 6, documents commonly went through a request handler derived
 * from Solr's ExtractingRequestHandler. That handler applied SolrCell-like
 * parameters such as:
 *
 * <ul>
 *   <li>lowernames=true</li>
 *   <li>fmap.* mappings</li>
 *   <li>uprefix=ignored_</li>
 * </ul>
 *
 * <p>In Datafari 7, documents can be sent through a standard UpdateRequestHandler.
 * In that case, the parameters above are no longer applied by the request handler,
 * which means incoming metadata may now arrive as fields such as {@code dc_creator},
 * {@code dc:creator}, {@code stream_size}, etc., instead of the former
 * {@code ignored_dc_creator}, {@code ignored_stream_size}, and so on.
 *
 * <p>This processor must be placed at the beginning of the Datafari update chain.
 * Its goal is to normalize incoming metadata as early as possible, before clone
 * fields, date parsing, language detection, signature computation, vectorization,
 * and Datafari-specific processing.
 *
 * <p>For simple cases, this processor maps incoming metadata directly to Datafari's
 * canonical Solr fields, for example:
 *
 * <ul>
 *   <li>{@code dc_creator -> author}</li>
 *   <li>{@code dc_title -> title}</li>
 *   <li>{@code dc_description -> description}</li>
 *   <li>{@code stream_size -> original_file_size}</li>
 * </ul>
 *
 * <p>Some temporary compatibility bridges are still kept for fields that are
 * currently consumed by {@link DatafariUpdateProcessor}, such as
 * {@code ignored_stream_name} and {@code ignored_content_type}.
 */
public class DatafariMetadataCompatibilityUpdateProcessor extends UpdateRequestProcessor {

  /**
   * Matches characters that should not be kept in a normalized metadata field name.
   *
   * <p>The former lowernames behavior made metadata names less sensitive to case
   * and separators. This processor reproduces that behavior by normalizing names
   * such as:
   *
   * <ul>
   *   <li>{@code dc:creator -> dc_creator}</li>
   *   <li>{@code Content-Type -> content_type}</li>
   *   <li>{@code resource.name -> resource_name}</li>
   * </ul>
   */
  private static final Pattern NON_FIELD_CHARS = Pattern.compile("[^a-z0-9_]+");

  /**
   * Collapses repeated underscores generated during field name normalization.
   */
  private static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("_+");

  public DatafariMetadataCompatibilityUpdateProcessor(final UpdateRequestProcessor next) {
    super(next);
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    final SolrInputDocument doc = cmd.getSolrInputDocument();

    /*
     * Build a lookup index once per document.
     *
     * The lookup allows us to find fields in a case-insensitive and separator-insensitive
     * way without renaming all fields in the input document.
     */
    final FieldLookup fields = new FieldLookup(doc);

    /*
     * Author metadata.
     *
     * Former behavior:
     *   ignored_dc_creator -> author
     *
     * New possible incoming forms:
     *   dc_creator, dc:creator, meta_author, jsoup_author
     */
    copyValues(doc, fields, "author",
        "ignored_dc_creator",
        "dc_creator",
        "dc:creator",
        "meta_author",
        "author_meta",
        "jsoup_author");

    /*
     * Last author metadata.
     *
     * Former behavior:
     *   ignored_meta_last_author -> last_author
     */
    copyValues(doc, fields, "last_author",
        "ignored_meta_last_author",
        "meta_last_author",
        "cp_last_author");

    /*
     * Title metadata.
     *
     * Former behavior:
     *   ignored_dc_title -> title
     *
     * jsoup_title is also included because HTML Extractor can provide a better
     * title for web pages than the raw filename or URL.
     */
    copyValues(doc, fields, "title",
        "ignored_dc_title",
        "dc_title",
        "dc:title",
        "meta_title");

    /*
     * Creation date metadata.
     *
     * Date parsing itself should remain the responsibility of Solr's date parsing
     * update processor. This processor only restores / canonicalizes the field.
     */
    copyValues(doc, fields, "creation_date",
        "ignored_dcterms_created",
        "dcterms_created",
        "dcterms:created",
        "created",
        "meta_creation_date",
        "xmp_create_date");

    /*
     * Last modification date metadata.
     *
     * filelastmodified is kept for compatibility with file-oriented connectors.
     */
    copyValues(doc, fields, "last_modified",
        "ignored_dcterms_modified",
        "dcterms_modified",
        "dcterms:modified",
        "modified",
        "filelastmodified",
        "file_last_modified",
        "ignored_filelastmodified",
        "meta_save_date",
        "last_save_date");

    /*
     * Character count.
     *
     * This field is single-valued in the schema, so we only keep the first valid
     * numeric value.
     */
    copyFirstValidLong(doc, fields, "character_count",
        "ignored_meta_character_count",
        "meta_character_count");

    /*
     * Keywords metadata.
     *
     * jsoup_keywords comes from ManifoldCF HTML Extractor.
     */
    copyValues(doc, fields, "keywords",
        "ignored_meta_keyword",
        "meta_keyword",
        "jsoup_keywords");

    /*
     * Subject metadata.
     */
    copyValues(doc, fields, "subject",
        "ignored_dc_subject",
        "dc_subject",
        "dc:subject");

    /*
     * Page count metadata.
     *
     * Several Tika / PDF metadata variants can be observed depending on the parser
     * and version.
     */
    copyFirstValidLong(doc, fields, "page_count",
        "ignored_meta_page_count",
        "meta_page_count",
        "xmp_tpg_npages",
        "xmptpg_npages",
        "numpages");

    /*
     * Revision number metadata.
     */
    copyValues(doc, fields, "revision_number",
        "ignored_cp_revision",
        "cp_revision");

    /*
     * Word count metadata.
     */
    copyFirstValidLong(doc, fields, "word_count",
        "ignored_meta_word_count",
        "meta_word_count");

    /*
     * Publisher metadata.
     */
    copyValues(doc, fields, "publisher",
        "ignored_dc_publisher",
        "dc_publisher",
        "dc:publisher");

    /*
     * Description metadata.
     *
     * jsoup_description comes from ManifoldCF HTML Extractor.
     */
    copyValues(doc, fields, "description",
        "ignored_dc_description",
        "dc_description",
        "dc:description",
        "meta_description",
        "jsoup_description");

    /*
     * Total editing time metadata.
     *
     * The canonical field expects a long-compatible value.
     */
    copyFirstValidLong(doc, fields, "total_time",
        "ignored_extended_properties_totaltime",
        "extended_properties_totaltime",
        "extended_properties_total_time");

    /*
     * Original file size.
     *
     * Former behavior:
     *   ignored_stream_size -> original_file_size
     *
     * In Datafari 7, some connectors may send stream_size directly.
     */
    copyFirstValidLong(doc, fields, "original_file_size",
        "ignored_stream_size",
        "stream_size",
        "streamsize",
        "content_length",
        "content-length",
        "file_size",
        "filesize");

    /*
     * Temporary compatibility bridge for stream name.
     *
     * DatafariUpdateProcessor still uses ignored_stream_name to compute the filename
     * fallback used in title handling. Once that logic is moved to this processor,
     * this bridge can be removed.
     */
    copyValues(doc, fields, "ignored_stream_name",
        "stream_name",
        "streamname",
        "resource_name",
        "resource.name",
        "resourcename");

    /*
     * Temporary compatibility bridge for MIME type.
     *
     * DatafariUpdateProcessor currently checks ignored_content_type, Content_Type,
     * then content_type when computing extension and mime. Keeping this bridge avoids
     * changing that logic immediately.
     */
    copyValues(doc, fields, "ignored_content_type",
        "content_type",
        "content-type",
        "Content_Type",
        "mime_type",
        "mimetype");

    super.processAdd(cmd);
  }

  /**
   * Copies all values found in any alias field to the destination field.
   *
   * <p>The method does not remove source fields. Unknown / non-schema fields are
   * expected to be ignored by the schema-level catch-all dynamicField:
   *
   * <pre>
   * &lt;dynamicField name="*" type="ignored" indexed="false" stored="false"/&gt;
   * </pre>
   */
  private void copyValues(
      final SolrInputDocument doc,
      final FieldLookup fields,
      final String destination,
      final String... aliases) {

    final List<Object> values = fields.valuesOf(aliases);
    if (values.isEmpty()) {
      return;
    }

    for (final Object value : values) {
      addIfNotBlankAndNotDuplicate(doc, destination, value);
    }
  }

  /**
   * Copies the first valid long value found in any alias field to the destination.
   *
   * <p>This is intended for numeric single-valued fields such as:
   *
   * <ul>
   *   <li>original_file_size</li>
   *   <li>page_count</li>
   *   <li>word_count</li>
   *   <li>character_count</li>
   * </ul>
   *
   * <p>If the destination already contains a non-blank value, nothing is changed.
   */
  private void copyFirstValidLong(
      final SolrInputDocument doc,
      final FieldLookup fields,
      final String destination,
      final String... aliases) {

    if (hasNonBlankValue(doc, destination)) {
      return;
    }

    for (final Object value : fields.valuesOf(aliases)) {
      if (value == null) {
        continue;
      }

      final String stringValue = value.toString().trim();
      if (stringValue.isEmpty()) {
        continue;
      }

      try {
        final long longValue = Long.parseLong(stringValue);
        doc.addField(destination, longValue);
        return;
      } catch (final NumberFormatException e) {
        // Ignore non-long metadata values.
      }
    }
  }

  /**
   * Adds a value to a destination field unless the value is null, blank, or already
   * present in the destination field.
   */
  private void addIfNotBlankAndNotDuplicate(
      final SolrInputDocument doc,
      final String destination,
      final Object value) {

    if (value == null) {
      return;
    }

    final String stringValue = value.toString().trim();
    if (stringValue.isEmpty()) {
      return;
    }

    final Collection<Object> existingValues = doc.getFieldValues(destination);
    if (existingValues != null) {
      for (final Object existingValue : existingValues) {
        if (existingValue != null && stringValue.equals(existingValue.toString())) {
          return;
        }
      }
    }

    doc.addField(destination, value);
  }

  /**
   * Returns true when a field already contains at least one non-blank value.
   */
  private boolean hasNonBlankValue(final SolrInputDocument doc, final String fieldName) {
    final Collection<Object> values = doc.getFieldValues(fieldName);
    if (values == null) {
      return false;
    }

    for (final Object value : values) {
      if (value != null && !value.toString().trim().isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Normalizes a metadata field name in a way that is close to the former
   * lowernames behavior.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code dc:creator -> dc_creator}</li>
   *   <li>{@code Content-Type -> content_type}</li>
   *   <li>{@code resource.name -> resource_name}</li>
   *   <li>{@code Meta Last Author -> meta_last_author}</li>
   * </ul>
   */
  private static String normalizeFieldName(final String fieldName) {
    if (fieldName == null) {
      return "";
    }

    String normalized = fieldName.trim().toLowerCase(Locale.ROOT);
    normalized = normalized
        .replace(':', '_')
        .replace('-', '_')
        .replace('.', '_')
        .replace(' ', '_');

    normalized = NON_FIELD_CHARS.matcher(normalized).replaceAll("_");
    normalized = MULTIPLE_UNDERSCORES.matcher(normalized).replaceAll("_");

    if (normalized.startsWith("_")) {
      normalized = normalized.substring(1);
    }

    if (normalized.endsWith("_")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    return normalized;
  }

  /**
   * Helper class used to resolve metadata aliases in a case-insensitive and
   * separator-insensitive way.
   *
   * <p>The document itself is not modified during lookup construction. This is
   * important because this processor should normalize useful metadata without
   * unexpectedly renaming every incoming field.
   */
  private static class FieldLookup {

    private final SolrInputDocument doc;
    private final Map<String, List<String>> normalizedToActualFieldNames = new HashMap<>();

    private FieldLookup(final SolrInputDocument doc) {
      this.doc = doc;

      for (final String fieldName : doc.getFieldNames()) {
        final String normalizedFieldName = normalizeFieldName(fieldName);
        normalizedToActualFieldNames
            .computeIfAbsent(normalizedFieldName, key -> new ArrayList<>())
            .add(fieldName);
      }
    }

    /**
     * Returns all values found for the provided aliases.
     *
     * <p>If several aliases resolve to the same actual field, that field is read
     * only once.
     */
    private List<Object> valuesOf(final String... aliases) {
      final List<Object> values = new ArrayList<>();
      final Set<String> visitedActualFields = new HashSet<>();

      for (final String alias : aliases) {
        final String normalizedAlias = normalizeFieldName(alias);
        final List<String> actualFieldNames = normalizedToActualFieldNames.get(normalizedAlias);

        if (actualFieldNames == null) {
          continue;
        }

        for (final String actualFieldName : actualFieldNames) {
          if (!visitedActualFields.add(actualFieldName)) {
            continue;
          }

          final Collection<Object> fieldValues = doc.getFieldValues(actualFieldName);
          if (fieldValues != null) {
            values.addAll(fieldValues);
          }
        }
      }

      return values;
    }
  }
}