package com.francelabs.datafari.external.search.api;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ResultDocument {

  private final String id;
  private final String previewContent;
  private final String title;
  private final String url;
  private final String extension;
  private final String highlighting;
  private final Map<String, Object> metadata;

  /**
   * ResultDocument
   *
   * @param id
   *          unique id for the doc (usually its url)
   * @param previewContent
   *          a short extract of the beginning of the document that do not exceed 1000 chars. can be null
   * @param title
   *          the title of the document
   * @param url
   *          url of the document (to reach it through a web browser)
   * @param extension
   *          the extension of the document
   * @param highlighting
   *          a short extract of the document where the search has matched. Can be null
   */
  public ResultDocument(final String id, final String previewContent, final String title, final String url, final String extension, final String highlighting) {
    this.id = id;
    this.previewContent = previewContent;
    this.title = title;
    this.url = url;
    this.extension = extension;
    this.highlighting = highlighting;
    metadata = new HashMap<>();
  }

  public void addMetadata(final String key, final Object value) {
    metadata.put(key, value);
  }

  public void removeMetadata(final String key) {
    metadata.remove(key);
  }

  public JSONObject getAsJSON() {
    final JSONObject resultDoc = new JSONObject();
    resultDoc.put("id", id);
    resultDoc.put("url", url);
    resultDoc.put("extension", extension);
    final JSONArray jpreviewContent = new JSONArray();
    if (previewContent != null) {
      jpreviewContent.add(previewContent);
    } else {
      jpreviewContent.add("");
    }
    resultDoc.put("preview_content", jpreviewContent);
    final JSONArray jtitle = new JSONArray();
    jtitle.add(title);
    resultDoc.put("title", jtitle);
    metadata.forEach((key, value) -> {
      resultDoc.put(key, value);
    });
    return resultDoc;
  }

  public JSONObject getHighlighting() {
    final JSONObject jHighlighting = new JSONObject();
    final JSONArray highlightingValue = new JSONArray();
    if (highlighting != null) {
      highlightingValue.add(highlighting);
    } else {
      highlightingValue.add("");
    }
    jHighlighting.put("default", highlightingValue);
    return jHighlighting;
  }

  public String getId() {
    return id;
  }

}
