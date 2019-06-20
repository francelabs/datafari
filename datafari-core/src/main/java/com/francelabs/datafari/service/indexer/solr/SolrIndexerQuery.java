/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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

  @Override
  public void addFilterQuery(final String... filterQuery) {
    query.addFilterQuery(filterQuery);

  }

  @Override
  public void addFacetQuery(final String facetQuery) {
    query.addFacetQuery(facetQuery);

  }

}
