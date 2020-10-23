package com.francelabs.datafari.service.indexer;

import java.util.Collection;

public interface IndexerResponseDocument {

  public Object getFieldValue(final String fieldName);

  public Collection<Object> getFieldValues(final String fieldname);

  public void removeField(final String field);

  public void addField(final String field, final Object value);

}
