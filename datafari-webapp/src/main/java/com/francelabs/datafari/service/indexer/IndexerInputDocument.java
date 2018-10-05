package com.francelabs.datafari.service.indexer;

public interface IndexerInputDocument {

  public void addField(final String fieldName, final Object value);

  public Object getFieldValue(final String fieldName);
}
