package com.francelabs.datafari.service.indexer;

public interface IndexerResponse {

  public long getNumFound();

  public int getQTime();

  public String getStrJSONResponse();

}
