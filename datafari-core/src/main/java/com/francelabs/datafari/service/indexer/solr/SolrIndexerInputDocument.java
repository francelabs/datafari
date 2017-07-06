package com.francelabs.datafari.service.indexer.solr;

import org.apache.solr.common.SolrInputDocument;

import com.francelabs.datafari.service.indexer.IndexerInputDocument;

public class SolrIndexerInputDocument implements IndexerInputDocument {

  private final SolrInputDocument inputDocument;

  protected SolrIndexerInputDocument() {
    inputDocument = new SolrInputDocument();
  }

  @Override
  public void addField(final String fieldName, final Object value) {
    inputDocument.addField(fieldName, value);
  }

  protected SolrInputDocument getSolrInputDocument() {
    return inputDocument;
  }

  @Override
  public Object getFieldValue(final String fieldName) {
    return inputDocument.getFieldValue(fieldName);
  }

}
