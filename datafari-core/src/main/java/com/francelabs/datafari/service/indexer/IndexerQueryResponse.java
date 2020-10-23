package com.francelabs.datafari.service.indexer;

import java.util.Map;

import org.json.simple.JSONArray;

public interface IndexerQueryResponse {

  public long getNumFound();

  public int getQTime();

  public String getStrJSONResponse();

  public JSONArray getResults();

  /**
   * Get the facet fields
   *
   * @return A map with facet name as key and count infos as value
   */
  public Map<String, IndexerFacetField> getFacetFields();

  public Map<String, IndexerFieldStatsInfo> getFieldStatsInfo();

}
