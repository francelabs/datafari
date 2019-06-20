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

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

import com.francelabs.datafari.service.indexer.IndexerUpdateRequest;

public class SolrIndexerUpdateRequest implements IndexerUpdateRequest {

  private final ModifiableSolrParams parameters;
  private final ContentStreamUpdateRequest ur;

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

  @Override
  public void setContent(final String contentFilePath) {
    final ContentStream csb = new ContentStreamBase.FileStream(new File(contentFilePath));
    ur.addContentStream(csb);
  }

}
