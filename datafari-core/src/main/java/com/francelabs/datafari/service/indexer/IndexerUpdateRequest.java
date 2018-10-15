package com.francelabs.datafari.service.indexer;

import java.util.Map;
import java.util.Set;

public interface IndexerUpdateRequest {

  public void setParam(final String paramName, final String... val);

  public void addParams(final Map<String, String[]> params);

  public void removeParam(final String paramName);

  public String getParamValue(final String paramName);

  public String[] getParamValues(final String paramName);

  public Map<String, String[]> getParams();

  public Set<String> getParamNames();

  public void setContent(final String contentFilePath);

}
