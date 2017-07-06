package com.francelabs.datafari.service.indexer;

import org.json.JSONArray;

public interface IndexerQueryResponse {

  public long getNumFound();

  public int getQTime();

  public String getStrJSONResponse();

  public JSONArray getResults();

}
