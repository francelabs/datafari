package com.francelabs.datafari.service.indexer.solr;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.service.indexer.IndexerQuery;

public class SolrIndexerQuery implements IndexerQuery {
  private static final List<String> FORBIDDEN_PREFIXES = List.of(
      "org.springframework.",
      "javax.",
      "jakarta."
  );

  private static boolean isAllowed(String key) {
    return FORBIDDEN_PREFIXES.stream().noneMatch(key::startsWith);
  }

  private final ModifiableSolrParams parameters;
  private final SolrQuery query = new SolrQuery();

  public SolrIndexerQuery() {
    parameters = new ModifiableSolrParams();
  }

  protected ModifiableSolrParams getParameters() {
    return parameters;
  }

  @Override
  public void setParam(final String paramName, final String... val) {
    if (isAllowed(paramName)){
      parameters.set(paramName, val);
    }
  }

  @Override
  public void addParam(final String paramName, final String... val) {
    if (isAllowed(paramName)) {
      parameters.add(paramName, val);
    }
  }

  @Override
  public void addParams(final Map<String, String[]> params) {
    params.forEach((key, values) -> {
      if (isAllowed(key)){
        parameters.add(key, values);
      }
    });
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

  @Override
  public void setQuery(final String query) {
    this.query.setQuery(query);
  }

  @Override
  public void setFilterQueries(final String... queries) {
    this.query.setFilterQueries(queries);
  }

  public SolrQuery prepareQuery() {
    query.add(parameters);
    return query;
  }

  @Override
  public void setRequestHandler(final String requestHandler) {
    this.query.setRequestHandler(requestHandler);

  }

  @Override
  public Map<String, String[]> getParams() {
    return parameters.getMap();
  }

  @Override
  public Set<String> getParamNames() {
    return parameters.getParameterNames();
  }

  @Override
  public void addFacetField(final String... facetField) {
    query.addFacetField(facetField);

  }

  @Override
  public void addFilterQuery(final String... filterQuery) {
    query.addFilterQuery(filterQuery);

  }

  @Override
  public void addFacetQuery(final String facetQuery) {
    query.addFacetQuery(facetQuery);

  }

}
