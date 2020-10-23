package com.francelabs.datafari.service.indexer;

import java.util.List;

public interface IndexerFacetStatsInfo {

  public String getName();

  public List<IndexerStatsInfo> getValuesStatsInfo();

}
