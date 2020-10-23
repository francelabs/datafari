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
