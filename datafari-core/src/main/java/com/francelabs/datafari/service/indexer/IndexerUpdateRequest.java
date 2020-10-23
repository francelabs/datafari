package com.francelabs.datafari.service.indexer;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.util.ContentStreamBase;

public interface IndexerUpdateRequest {

  public void setParam(final String paramName, final String... val);

  /**
   * Add the provided paramValue(s) to any existing paramName's value(s)
   *
   * @param paramName  the parameter name
   * @param paramValue the value(s) to add to the parameter name's values
   */
  public void addParam(final String paramName, final String... paramValue);

  public void addParams(final Map<String, String[]> params);

  public void removeParam(final String paramName);

  public String getParamValue(final String paramName);

  public String[] getParamValues(final String paramName);

  public Map<String, String[]> getParams();

  public Set<String> getParamNames();

  public void setContent(final File contentFile);

  public void setContent(final String contentString);

}
