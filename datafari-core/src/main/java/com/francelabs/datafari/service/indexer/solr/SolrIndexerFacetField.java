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

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;

public class SolrIndexerFacetField implements IndexerFacetField {

  private final String facetField;
  private final List<IndexerFacetFieldCount> counts;

  protected SolrIndexerFacetField(final String fieldName, final JSONArray values) {
    facetField = fieldName;
    this.counts = new ArrayList<IndexerFacetFieldCount>();
    for (int i = 0; i < values.size(); i += 2) {
      final String value = values.get(i).toString();
      final long count = Long.parseLong(values.get(i + 1).toString());
      counts.add(new SolrIndexerFacetFieldCount(value, count));
    }
  }

  @Override
  public String getName() {
    return facetField;
  }

  @Override
  public List<IndexerFacetFieldCount> getValues() {
    return counts;
  }

}
