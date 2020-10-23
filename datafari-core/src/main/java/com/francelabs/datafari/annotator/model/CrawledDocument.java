package com.francelabs.datafari.annotator.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawledDocument {

  private final String id;
  private Map<String, List<String>> fields;
  private Instant lastCheck;
  private final String solrCore;
  private final String solrUpdateHandler;
  private List<String> annotators;
  /** docPath used by annotators and that can be modified by them */
  private String docPath;
  /** originalDocPath that must not be changed */
  private final String originalDocPath;
  private final List<String> tempFilesToDelete = new ArrayList<>();

  public CrawledDocument(final String id, final String solrCore, final String solrUpdateHandler, final String originalDocPath) {
    this.id = id;
    this.solrCore = solrCore;
    this.solrUpdateHandler = solrUpdateHandler;
    this.originalDocPath = originalDocPath;
    this.docPath = originalDocPath;
  }

  /**
   * If an annotator generates additional temporary files for a document, use this method to declare them in order that they will be deleted
   * at the end of an annotate process (whether it succeeds of fails)
   *
   * @param filePath
   *          the temp file absolute path
   */
  public void addTempFileToDelete(final String filePath) {
    tempFilesToDelete.add(filePath);
  }

  public List<String> getTempFilesToDelete() {
    return tempFilesToDelete;
  }

  public String getId() {
    return id;
  }

  public String getDocPath() {
    return docPath;
  }

  public void setDocPath(final String docPath) {
    this.docPath = docPath;
  }

  public String getOriginalDocPath() {
    return originalDocPath;
  }

  public Map<String, List<String>> getFields() {
    return fields;
  }

  public void setFields(final Map<String, List<String>> fields) {
    this.fields = new HashMap<>(fields);
  }

  public Instant getLastCheck() {
    return lastCheck;
  }

  public void setLastCheck(final Instant lastCheck) {
    this.lastCheck = lastCheck;
  }

  public String getSolrCore() {
    return solrCore;
  }

  public String getSolrUpdateHandler() {
    return solrUpdateHandler;
  }

  public List<String> getAnnotators() {
    return annotators;
  }

  public void setAnnotators(final List<String> annotatorType) {
    this.annotators = annotatorType;
  }
}
