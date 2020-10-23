package com.francelabs.datafari.service.indexer;

import java.util.Map;

public interface IndexerFieldStatsInfo {

  public String getName();

  public IndexerStatsInfo getStatsInfo();

  public Map<String, IndexerFacetStatsInfo> getFacets();

}
