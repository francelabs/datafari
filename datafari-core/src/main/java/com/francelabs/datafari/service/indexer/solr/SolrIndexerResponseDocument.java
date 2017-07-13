package com.francelabs.datafari.service.indexer.solr;

import org.apache.solr.common.SolrDocument;

import com.francelabs.datafari.service.indexer.IndexerResponseDocument;

/**
 * Response document that is retrieved in a query response
 * 
 * @author francelabs
 *
 */
public class SolrIndexerResponseDocument implements IndexerResponseDocument {

  private final SolrDocument document;

  protected SolrIndexerResponseDocument(final SolrDocument document) {
    this.document = document;
  }

  @Override
  public Object getFieldValue(final String fieldName) {
    return document.getFieldValue(fieldName);
  }

}
