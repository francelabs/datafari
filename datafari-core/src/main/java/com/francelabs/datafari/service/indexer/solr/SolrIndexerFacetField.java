package com.francelabs.datafari.service.indexer.solr;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;

public class SolrIndexerFacetField implements IndexerFacetField {

  private final FacetField facetField;

  protected SolrIndexerFacetField(final FacetField facetField) {
    this.facetField = facetField;
  }

  @Override
  public String getName() {
    return this.facetField.getName();
  }

  @Override
  public List<IndexerFacetFieldCount> getValues() {
    final List<IndexerFacetFieldCount> listFacetFieldCount = new ArrayList<>();
    for (final Count count : facetField.getValues()) {
      listFacetFieldCount.add(new SolrIndexerFacetFieldCount(count));
    }
    return listFacetFieldCount;
  }

}
