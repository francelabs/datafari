package com.francelabs.datafari.service.indexer.solr;

import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.service.indexer.IndexerQuery;

public class SolrIndexerQuery implements IndexerQuery {

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
    parameters.set(paramName, val);
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

}
