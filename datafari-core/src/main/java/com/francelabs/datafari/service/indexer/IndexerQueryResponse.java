package com.francelabs.datafari.service.indexer;

import java.util.List;

import org.json.simple.JSONArray;

public interface IndexerQueryResponse {

  public long getNumFound();

  public int getQTime();

  public String getStrJSONResponse();

  public JSONArray getResults();

  public List<IndexerFacetField> getFacetFields();

}
