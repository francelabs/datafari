package com.francelabs.datafari.service.indexer;

import java.util.Map;
import java.util.Set;

public interface IndexerQuery {

  public void setParam(final String paramName, final String... val);

  public void addParams(final Map<String, String[]> params);

  public void removeParam(final String paramName);

  public String getParamValue(final String paramName);

  public String[] getParamValues(final String paramName);

  public void setQuery(final String query);

  public void setFilterQueries(final String... queries);

  public void setRequestHandler(final String requestHandler);

  public Map<String, String[]> getParams();

  public Set<String> getParamNames();

  public void addFacetField(final String... facetField);

  public void addFilterQuery(final String... filterQuery);

  public void addFacetQuery(final String facetQuery);

}
