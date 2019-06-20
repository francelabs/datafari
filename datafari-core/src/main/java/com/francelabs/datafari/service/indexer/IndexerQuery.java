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
