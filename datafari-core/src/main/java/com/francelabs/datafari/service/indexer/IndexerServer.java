package com.francelabs.datafari.service.indexer;

public interface IndexerServer {

  public IndexerQuery createQuery();

  public IndexerResponse executeQuery(final IndexerQuery query) throws Exception;

}
