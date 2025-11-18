package com.francelabs.datafari.service.indexer.solr;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

import com.francelabs.datafari.service.indexer.IndexerUpdateRequest;

/**
 * Implementation of IndexerUpdateRequest for Solr.
 * Wraps a SolrJ ContentStreamUpdateRequest with additional parameter management.
 */
public class SolrIndexerUpdateRequest implements IndexerUpdateRequest {

  private final ModifiableSolrParams parameters;
  private final ContentStreamUpdateRequest ur;

  /**
   * Creates a Solr update request targeting the specified update handler.
   *
   * @param updateHandler Solr update handler endpoint (e.g. "/update" or "/update/json")
   */
  public SolrIndexerUpdateRequest(final String updateHandler) {
    parameters = new ModifiableSolrParams();
    ur = new ContentStreamUpdateRequest(updateHandler);
  }

  protected ModifiableSolrParams getParameters() {
    return parameters;
  }

  @Override
  public void setParam(final String paramName, final String... val) {
    parameters.set(paramName, val);
  }

  @Override
  public void addParam(final String paramName, final String... paramValue) {
    parameters.add(paramName, paramValue);
  }

  @Override
  public void addParams(final Map<String, String[]> params) {
    parameters.add(new ModifiableSolrParams(params));
  }

  @Override
  public void removeParam(final String paramName) {
    parameters.remove(paramName);
  }

  @Override
  public String getParamValue(final String paramName) {
    return parameters.get(paramName);
  }

  @Override
  public String[] getParamValues(final String paramName) {
    return parameters.getParams(paramName);
  }

  /**
   * Builds and returns the SolrJ ContentStreamUpdateRequest populated with all parameters and content streams.
   *
   * @return The prepared ContentStreamUpdateRequest instance.
   */
  public ContentStreamUpdateRequest prepareUpdateRequest() {
    ur.setParams(parameters);
    return ur;
  }

  @Override
  public Map<String, String[]> getParams() {
    return parameters.getMap();
  }

  @Override
  public Set<String> getParamNames() {
    return parameters.getParameterNames();
  }

  /**
   * Sets the content of this update request using a file.
   * In SolrJ 10, the FileStream constructor requires a Path instance.
   *
   * @param contentFile The file containing the content to be indexed.
   */
  @Override
  public void setContent(final File contentFile) {
    final ContentStream csb = new ContentStreamBase.FileStream(contentFile.toPath());
    ur.addContentStream(csb);
  }

  /**
   * Sets the content of this update request using a raw string.
   *
   * @param contentString The string content to be indexed.
   */
  @Override
  public void setContent(final String contentString) {
    final ContentStream csb = new ContentStreamBase.StringStream(contentString);
    ur.addContentStream(csb);
  }
}
