package com.francelabs.datafari.service.indexer.solr;

import org.apache.solr.client.solrj.response.FacetField.Count;

import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;

public class SolrIndexerFacetFieldCount implements IndexerFacetFieldCount {

  private final Count count;

  protected SolrIndexerFacetFieldCount(final Count count) {
    this.count = count;
  }

  @Override
  public String getName() {
    return count.getName();
  }

  @Override
  public long getCount() {
    return count.getCount();
  }

}
