package com.francelabs.datafari.service.indexer;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerInputDocument;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerQuery;

public class IndexerQueryManager {

  public static IndexerQuery createQuery() {
    return new SolrIndexerQuery();
  }

  public static IndexerInputDocument createDocument() {
    return new SolrIndexerInputDocument();
  }

}
