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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  private final String REGEX_PHONE_DEFAULT = "\\(?\\+[0-9]{1,3}\\)? ?-?[0-9]{1,3} ?-?[0-9]{3,5} ?-?[0-9]{4}( ?-?[0-9]{3})? ?(\\w{1,10}\\s?\\d{1,6})?";
  private final String REGEX_SPECIAL_DEFAULT = ".*resume*";

  private boolean simpleEntityExtraction = false;
  private boolean simpleNameExtraction = false;
  private boolean simplePhoneExtraction = false;
  private boolean simpleSpecialExtraction = false;
  private Pattern specialExtractionPattern = Pattern.compile(REGEX_SPECIAL_DEFAULT);
  private Pattern phoneExtractionPattern = Pattern.compile(REGEX_PHONE_DEFAULT);

  public DatafariUpdateProcessor(final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      simpleEntityExtraction = params.getBool(SIMPLE_ENTITY_EXTRACTION_PARAM, false);
      simpleNameExtraction = params.getBool(SIMPLE_NAME_EXTRACTION, false);
      simplePhoneExtraction = params.getBool(SIMPLE_PHONE_EXTRACTION, false);
      simpleSpecialExtraction = params.getBool(SIMPLE_SPECIAL_EXTRACTION, false);
      final String specialExtractionRegex = params.get(SIMPLE_SPECIAL_EXTRACTION_REGEX, REGEX_SPECIAL_DEFAULT);
      final String phoneExtractionRegex = params.get(SIMPLE_PHONE_EXTRACTION_REGEX, REGEX_PHONE_DEFAULT);
      specialExtractionPattern = Pattern.compile(specialExtractionRegex);
      phoneExtractionPattern = Pattern.compile(phoneExtractionRegex);
    }
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    final SolrInputDocument doc = cmd.getSolrInputDocument();

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

    final String url = (String) doc.getFieldValue("id");

    final String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
    doc.addField("url_search", decodedUrl);

    // Create path hierarchy for facet
    final List<String> urlHierarchy = new ArrayList<>();

    /*
     * // Create path hierarchy for facet
     *
     * final List<String> urlHierarchy = new ArrayList<String>();
     *
     * final String path = url.replace("file:", ""); int previousIndex = 1; int depth = 0; // Tokenize the path and add the depth as first character for each token // (like: 0/home, 1/home/project
     * ...) for (int i = 0; i < path.split("/").length - 2; i++) { int endIndex = path.indexOf('/', previousIndex); if (endIndex == -1) { endIndex = path.length() - 1; } urlHierarchy.add(depth +
     * path.substring(0, endIndex)); depth++; previousIndex = endIndex + 1; }
     *
     * // Add the tokens to the urlHierarchy field doc.addField("urlHierarchy", urlHierarchy);
     */

    doc.remove("url");
    doc.addField("url", url);

    String filename = "";
    String jsouptitle = "";
    if (doc.getField("jsoup_title") != null) {
      jsouptitle = (String) doc.getFieldValue("jsoup_title");
    }

    final SolrInputField streamNameField = doc.get("ignored_stream_name");
    if (streamNameField != null && !streamNameField.getFirstValue().toString().isEmpty()) {
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

    // The title field has lost its original value(s) after the LangDetectLanguageIdentifierUpdateProcessorFactory
    // Need to set it back from the exactTitle field
    if (doc.get("exactTitle") != null) {
      for (final Object value : doc.getFieldValues("exactTitle")) {
        doc.addField("title", value);
      }
    }
    // keep the filename as the first title for the searchView of Datafari
    if (doc.get("title") != null) {
      final List<Object> titleValues = new ArrayList<>();
      titleValues.addAll(doc.getFieldValues("title"));
      doc.removeField("title");
      if (!jsouptitle.isEmpty()) {
        doc.addField("title", jsouptitle);
      }
      if (!filename.isEmpty()) {
        doc.addField("title", filename);
      }
      for (final Object value : titleValues) {
        doc.addField("title", value);
      }
    } else {
      if (!jsouptitle.isEmpty()) {
        doc.addField("title", jsouptitle);
      }
      if (!filename.isEmpty()) {
        doc.addField("title", filename);
      }
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

    extension = nameExtension.length() > 1 && nameExtension.length() < 5 ? nameExtension : tikaExtension;
    mime = tikaExtension.length() > 1 && tikaExtension.length() < 5 ? tikaExtension : nameExtension;
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

    super.processAdd(cmd);
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
}
