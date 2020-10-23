package com.francelabs.datafari.service.indexer.solr;

import java.util.Collection;

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

  @Override
  public void removeField(final String field) {
    document.remove(field);
  }

  @Override
  public void addField(final String field, final Object value) {
    document.addField(field, value);

  }

  @Override
  public Collection<Object> getFieldValues(final String fieldname) {
    return document.getFieldValues(fieldname);
  }

}
