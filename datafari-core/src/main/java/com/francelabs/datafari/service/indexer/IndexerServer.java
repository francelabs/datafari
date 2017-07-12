package com.francelabs.datafari.service.indexer;

public interface IndexerServer {

  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception;

  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception;

  public IndexerResponseDocument getDocById(final String id) throws Exception;

  public void processStatsResponse(final IndexerQueryResponse queryResponse);

}
