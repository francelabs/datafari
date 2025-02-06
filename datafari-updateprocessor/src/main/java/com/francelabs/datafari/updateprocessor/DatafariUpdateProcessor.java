/*******************************************************************************
 /*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

public class DatafariUpdateProcessor extends UpdateRequestProcessor {

  private static final Logger LOGGER = LogManager.getLogger(DatafariUpdateProcessor.class.getName());

  private final String SIMPLE_ENTITY_EXTRACTION_PARAM = "entities.extract.simple";
  private final String SIMPLE_NAME_EXTRACTION = "entities.extract.simple.name";
  private final String SIMPLE_PHONE_EXTRACTION = "entities.extract.simple.phone";
  private final String SIMPLE_SPECIAL_EXTRACTION = "entities.extract.simple.special";
  private final String SIMPLE_SPECIAL_EXTRACTION_REGEX = "entities.extract.simple.special.regex";
  private final String SIMPLE_PHONE_EXTRACTION_REGEX = "entities.extract.simple.phone.regex";
  private final String HIERARCHICAL_PATH_PROCESSING = "hierarchical.path.processing";
  private final String HIERARCHICAL_FIELD = "hierarchical.field";
  private final String HIERARCHICAL_PATH_SEPARATOR = "hierarchical.path.separator";

  private final String REGEX_PHONE_DEFAULT = "\\(?\\+[0-9]{1,3}\\)? ?-?[0-9]{1,3} ?-?[0-9]{3,5} ?-?[0-9]{4}( ?-?[0-9]{3})? ?(\\w{1,10}\\s?\\d{1,6})?";
  private final String REGEX_SPECIAL_DEFAULT = ".*resume*";

  private boolean simpleEntityExtraction = false;
  private boolean simpleNameExtraction = false;
  private boolean simplePhoneExtraction = false;
  private boolean simpleSpecialExtraction = false;
  private Pattern specialExtractionPattern = Pattern.compile(REGEX_SPECIAL_DEFAULT);
  private Pattern phoneExtractionPattern = Pattern.compile(REGEX_PHONE_DEFAULT);
  private boolean hierarchicalPathProcessing = false;
  private String hierarchicalField = "urlHierarchy";
  private char hierarchicalPathSeparator = '/';
  private Pattern hierarchicalRegexPattern;

  public DatafariUpdateProcessor(final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      simpleEntityExtraction = params.getBool(SIMPLE_ENTITY_EXTRACTION_PARAM, false);
      simpleNameExtraction = params.getBool(SIMPLE_NAME_EXTRACTION, false);
      simplePhoneExtraction = params.getBool(SIMPLE_PHONE_EXTRACTION, false);
      simpleSpecialExtraction = params.getBool(SIMPLE_SPECIAL_EXTRACTION, false);
      hierarchicalPathProcessing = params.getBool(HIERARCHICAL_PATH_PROCESSING, false);
      final String specialExtractionRegex = params.get(SIMPLE_SPECIAL_EXTRACTION_REGEX, REGEX_SPECIAL_DEFAULT);
      final String phoneExtractionRegex = params.get(SIMPLE_PHONE_EXTRACTION_REGEX, REGEX_PHONE_DEFAULT);
      specialExtractionPattern = Pattern.compile(specialExtractionRegex);
      phoneExtractionPattern = Pattern.compile(phoneExtractionRegex);
      hierarchicalField = params.get(HIERARCHICAL_FIELD, "urlHierarchy");
      hierarchicalPathSeparator = params.get(HIERARCHICAL_PATH_SEPARATOR, "/").charAt(0);
      final String hierarchicalRegex = "[^\\" + hierarchicalPathSeparator + "]*\\" + hierarchicalPathSeparator + "[^\\" + hierarchicalPathSeparator + "]";
      hierarchicalRegexPattern = Pattern.compile(hierarchicalRegex);
    }
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    final SolrInputDocument doc = cmd.getSolrInputDocument();

    // Tika specific mapping
    if (doc.getFieldValue("ignored_dc_creator") != null) {
      doc.getFieldValues("ignored_dc_creator").forEach(value -> {
        doc.addField("author", value);
      });
    }
    if (doc.getFieldValue("ignored_meta_last_author") != null) {
      doc.getFieldValues("ignored_meta_last_author").forEach(value -> {
        doc.addField("last_author", value);
      });
    }
    if (doc.getFieldValue("ignored_dc_title") != null) {
      doc.getFieldValues("ignored_dc_title").forEach(value -> {
        doc.addField("title", value);
      });
    }
    if (doc.getFieldValue("ignored_dcterms_created") != null) {
      doc.getFieldValues("ignored_dcterms_created").forEach(value -> {
        doc.addField("creation_date", value);
      });
    }
    if (doc.getFieldValue("ignored_dcterms_modified") != null) {
      doc.getFieldValues("ignored_dcterms_modified").forEach(value -> {
        doc.addField("last_modified", value);
      });
    }
    if (doc.getFieldValue("ignored_meta_character_count") != null) {
      doc.getFieldValues("ignored_meta_character_count").forEach(value -> {
        doc.setField("character_count", value);
      });
    }
    if (doc.getFieldValue("ignored_meta_keyword") != null) {
      doc.getFieldValues("ignored_meta_keyword").forEach(value -> {
        doc.addField("keywords", value);
      });
    }
    if (doc.getFieldValue("ignored_dc_subject") != null) {
      doc.getFieldValues("ignored_dc_subject").forEach(value -> {
        doc.addField("subject", value);
      });
    }
    if (doc.getFieldValue("ignored_meta_page_count") != null) {
      doc.getFieldValues("ignored_meta_page_count").forEach(value -> {
        doc.addField("page_count", value);
      });
    }
    if (doc.getFieldValue("ignored_cp_revision") != null) {
      doc.getFieldValues("ignored_cp_revision").forEach(value -> {
        doc.addField("revision_number", value);
      });
    }
    if (doc.getFieldValue("ignored_meta_word_count") != null) {
      doc.getFieldValues("ignored_meta_word_count").forEach(value -> {
        doc.addField("word_count", value);
      });
    }
    if (doc.getFieldValue("ignored_dc_publisher") != null) {
      doc.getFieldValues("ignored_dc_publisher").forEach(value -> {
        doc.addField("publisher", value);
      });
    }
    if (doc.getFieldValue("ignored_dc_description") != null) {
      doc.getFieldValues("ignored_dc_description").forEach(value -> {
        doc.addField("description", value);
      });
    }
    if (doc.getFieldValue("ignored_extended_properties_totaltime") != null) {
      doc.getFieldValues("ignored_extended_properties_totaltime").forEach(value -> {
        if (isValidLong(value)) doc.addField("total_time", value);
      });
    }

    // Sometimes Tika put several ids so we keep the first one which is
    // always the right one
    if (doc.getFieldValues("id").size() > 1) {
      final Object id = doc.getFieldValue("id");
      doc.remove("id");
      doc.addField("id", id);
    }

    // Try to retrieve at the ignored_filelastmodified field to set it's
    // value in the last_modified field
    if (doc.getFieldValue("ignored_filelastmodified") != null) {
      final Object last_modified = doc.getFieldValue("ignored_filelastmodified");
      doc.addField("last_modified", last_modified);
    }

    /*
     * Entity extraction
     */
    if (simpleEntityExtraction) {
      if (simplePhoneExtraction || simpleSpecialExtraction) {
        String content = "";
        final SolrInputField contentFieldFr = doc.get("content_fr");
        final SolrInputField contentFieldEn = doc.get("content_en");
        if (contentFieldFr != null) {
          content = (String) contentFieldFr.getFirstValue();
        } else if (contentFieldEn != null) {
          content = (String) contentFieldEn.getFirstValue();

        }

        if (simplePhoneExtraction) {
          final Matcher matcherPhone = phoneExtractionPattern.matcher(content);
          final Set<String> matches = new HashSet<>();
          while (matcherPhone.find()) {
            matches.add(matcherPhone.group());
          }
          for (final String match : matches) {
            doc.addField("entity_phone", match);
          }
          if (doc.getFieldValue("entity_phone") != null) {
            doc.addField("entity_phone_present", "true");
          } else {
            doc.addField("entity_phone_present", "false");
          }
        }

        if (simpleSpecialExtraction) {
          final Matcher matcherSpecial = specialExtractionPattern.matcher(content);
          final Set<String> matches = new HashSet<>();
          while (matcherSpecial.find()) {
            matches.add(matcherSpecial.group());
          }
          for (final String match : matches) {
            doc.addField("entity_special", match);
          }
          if (doc.getFieldValue("entity_special") != null) {
            doc.addField("entity_special_present", "true");
          } else {
            doc.addField("entity_special_present", "false");
          }
        }
      }

    }

    
    String url;
    if (doc.containsKey("url")) {
      url = (String) doc.getFieldValue("url");
      doc.remove("url");
      doc.addField("url", url);
    } else {    
      url = (String) doc.getFieldValue("id");
      doc.addField("url", url);
    }

    try {
      final String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
      doc.addField("url_search", decodedUrl);
    } catch (final Exception e) {
      // The url is malformed, keep it like it is although the stemming will not be good, it is better than nothing
      doc.addField("url_search", url);
    }

    if (hierarchicalPathProcessing) {
      // Create path hierarchy for facet
      final List<String> urlHierarchy = new ArrayList<>();
      final Matcher regexMatcher = hierarchicalRegexPattern.matcher(url);
      String cleanUrl = url;
      if (regexMatcher.find()) {
        final int endIndex = regexMatcher.end();
        cleanUrl = url.substring(endIndex - 2);
      }

      // Create path hierarchy for facet
      final long separatorCount = cleanUrl.chars().filter(ch -> ch == hierarchicalPathSeparator).count();
      int previousIndex = 0;
      int depth = 0;
      // Tokenize the path and add the depth as first character for each token // (like: 0/home,1/home/project ...)
      for (int i = 0; i < separatorCount; i++) {
        final int endIndex = cleanUrl.indexOf(hierarchicalPathSeparator, previousIndex);
        if (endIndex == -1) {
          break;
        }
        String label = cleanUrl.substring(0, endIndex);
        if (label.isEmpty()) {
          label = String.valueOf(hierarchicalPathSeparator);
        }
        urlHierarchy.add(depth + label);
        depth++;
        previousIndex = endIndex + 1;
      }

      doc.remove(hierarchicalField);
      doc.addField(hierarchicalField, urlHierarchy);
    }

    String filename = "";
    String jsouptitle = "";
    if (doc.getField("jsoup_title") != null) {
      jsouptitle = (String) doc.getFieldValue("jsoup_title");
    }

    final SolrInputField streamNameField = doc.get("ignored_stream_name");
    if (streamNameField != null && !streamNameField.getFirstValue().toString().isEmpty() && !streamNameField.getFirstValue().toString().toLowerCase().contentEquals("docname")) {
      filename = (String) streamNameField.getFirstValue();
    } else {
      final Pattern pattern = Pattern.compile("[^/]*$");
      final Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
        filename = matcher.group();
      }
    }

    final SolrInputField streamSizeField = doc.get("ignored_stream_size");

    if (doc.getFieldValue("original_file_size") == null && streamSizeField != null) {
      final long streamSize = Long.parseLong((String) streamSizeField.getValue());
      doc.addField("original_file_size", streamSize);
    }

    if (url.startsWith("http")) {
      if (!doc.containsKey("source")) {
        doc.addField("source", "web");
      }
    }

    if (url.startsWith("file")) {
      if (!doc.containsKey("source")) {
        doc.addField("source", "file");
      }
    }

    // keep the jsoup or filename as the first title for the searchView of Datafari
    if (!jsouptitle.isEmpty()) {
      doc.addField("title", jsouptitle);
    } else if (!filename.isEmpty()) {
      doc.addField("title", filename);
    }

    // The title field has lost its original value(s) after the LangDetectLanguageIdentifierUpdateProcessorFactory
    // Need to set it back from the exactTitle field
    if (doc.get("exactTitle") != null) {
      for (final Object value : doc.getFieldValues("exactTitle")) {
        doc.addField("title", value);
      }
    }

    // Clean authors (remove duplicates)
    if (doc.containsKey("author")) {
      final Set<String> authors = new HashSet<>();
      for (final Object authorObj : doc.getFieldValues("author")) {
        authors.add(authorObj.toString());
      }
      doc.remove("author");
      doc.addField("author", authors.toArray(new String[0]));
    }

    // Ensure a search-able title
    String language = (String) doc.getFieldValue("language");
    if (language == null) {
      language = "en";
    }
    if (!filename.isEmpty()) {
      doc.addField("title_" + language, filename);
      doc.addField("exactTitle", filename);
    }
    if (!jsouptitle.isEmpty()) {
      doc.addField("title_" + language, jsouptitle);
      doc.addField("exactTitle", jsouptitle);
    }

    String extension = "";
    String mime = "";
    String nameExtension = "";
    try {
      final URL urlObject = new URL(url);
      final String path = urlObject.getPath();
      nameExtension = FilenameUtils.getExtension(path);
    } catch (final MalformedURLException e) {
      // Do nothing
    }
    final SolrInputField mimeTypeField = doc.get("ignored_content_type");
    final String tikaExtension = mimeTypeField == null ? "" : extensionFromMimeTypeField(mimeTypeField);

    mime = tikaExtension.length() > 1 && tikaExtension.length() < 5 ? tikaExtension : nameExtension;
    if (url.startsWith("http") && !mime.isEmpty()) {
      extension = mime;
    } else {
      extension = nameExtension.length() > 1 && nameExtension.length() < 5 ? nameExtension : tikaExtension;
    }

    /*
     * if (extensionFromName || mimeTypeField == null) { if (path.contains(".")){ extension = FilenameUtils.getExtension(path); if (extension.length() > 4 || extension.length() < 1) { // If length is
     * too long, try extracting from tika information if available String tryExtension = mimeTypeField==null ? null : extensionFromMimeTypeField(mimeTypeField); if (tryExtension != null) { extension =
     * tryExtension; } else { // Else default to bin for anything else extension = "bin"; } } } else if (urlObject.getProtocol().equals("http") || urlObject.getProtocol().equals("https")) { extension
     * = null; if (mimeTypeField != null) { extension = extensionFromMimeTypeField(mimeTypeField); } if (extension == null) { extension = "html"; } } } else { extension =
     * extensionFromMimeTypeField(mimeTypeField); if (extension == null) { extension = FilenameUtils.getExtension(path); } }
     */
    if (!doc.containsKey("extension")) {
      doc.addField("extension", extension.toLowerCase());
    }
    doc.addField("mime", mime.toLowerCase());

    // remove useless dates to only keep one for last_modified and creation_date fields
    final String lastmodifiedField = "last_modified";
    final String creationdateField = "creation_date";

    if (doc.containsKey(lastmodifiedField) && doc.getFieldValue(lastmodifiedField) != null && !doc.getFieldValue(lastmodifiedField).toString().isEmpty()) {
      final Collection<Object> lastmodifiedCollection = doc.getFieldValues("last_modified");
      final Object firsLastModified = lastmodifiedCollection.iterator().next();
      doc.remove(lastmodifiedField);
      if (isValidDate(firsLastModified)) doc.addField(lastmodifiedField, firsLastModified);
    }

    if (doc.containsKey(creationdateField) && doc.getFieldValue(creationdateField) != null && !doc.getFieldValue(creationdateField).toString().isEmpty()) {
      final Collection<Object> creationDateCollection = doc.getFieldValues("creation_date");
      final Object firstCreationDate = creationDateCollection.iterator().next();
      doc.remove(creationdateField);
      if (isValidDate(firstCreationDate)) doc.addField(creationdateField, firstCreationDate);
    }

    super.processAdd(cmd);
  }

  private String normalizeUrl(final String url) {
    final int paramIndex = url.indexOf("?");
    if (paramIndex != -1) {
      final String path = url.substring(0, paramIndex);
      final String params = url.substring(paramIndex + 1);
      final String normalizedUrl = path + "?" + URLEncoder.encode(params, StandardCharsets.UTF_8);
      return normalizedUrl;
    } else {
      return url;
    }
  }

  private String extensionFromMimeTypeField(final SolrInputField mimeTypeField) {
    String extension = "";
    final String[] mimeTypeList = mimeTypeField.getValues().toArray(new String[0]);
    for (String mimeType : mimeTypeList) {
      if (mimeType.contains(";")) {
        final String[] parts = mimeType.split(";");
        mimeType = parts[0];
      }
      final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
      MimeType type;

      try {
        type = allTypes.forName(mimeType);
        String currentExtension = type.getExtension();
        if (currentExtension.length() > 1) {
          currentExtension = currentExtension.substring(1);
        }
        if (!currentExtension.equals("bin")) {
          extension = currentExtension;
        }
      } catch (final MimeTypeException e) {
      }
    }
    return extension;
  }

  /**
   * Check if a String is castable to Long
   * @return boolean
   */
  boolean isValidLong(Object str) {
    try {
      Long.parseLong(String.valueOf(str));
    } catch(Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Check if a String is castable to Date and if it is not only digits
   * @return boolean
   */
  boolean isValidDate(Object str) {
    return !String.valueOf(str).matches("[0-9]+"); // This value should not be a stringified timespamp
  }
}
