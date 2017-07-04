package com.francelabs.datafari.service.indexer;

import java.util.Map;

public interface IndexerQuery {

  public void setParam(final String paramName, final String... val);

  public void addParams(final Map<String, String[]> params);

  public void removeParam(final String paramName);

  public String[] getParamValues(final String paramName);

  public void setQuery(final String query);

  public void setFilterQueries(final String... queries);

  public void serRequestHandler(final String requestHandler);

}
