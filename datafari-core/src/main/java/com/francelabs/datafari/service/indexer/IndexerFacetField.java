package com.francelabs.datafari.service.indexer;

import java.util.List;

public interface IndexerFacetField {

  public String getName();

  public List<IndexerFacetFieldCount> getValues();

}
