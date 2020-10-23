package com.francelabs.datafari.service.indexer.solr;

import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;

public class SolrIndexerFacetFieldCount implements IndexerFacetFieldCount {

  private final String name;
  private final long count;

  protected SolrIndexerFacetFieldCount(final String name, final long count) {
    this.name = name;
    this.count = count;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long getCount() {
    return count;
  }

}
