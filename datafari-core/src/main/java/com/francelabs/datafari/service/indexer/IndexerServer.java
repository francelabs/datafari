package com.francelabs.datafari.service.indexer;

public interface IndexerServer {

  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception;

  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception;

  public void pushDoc(final IndexerInputDocument document) throws Exception;

  public void commit() throws Exception;

  public IndexerResponseDocument getDocById(final String id) throws Exception;

  public void deleteById(final String id) throws Exception;

  public void processStatsResponse(final IndexerQueryResponse queryResponse);

  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception;

  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value)
      throws Exception;

}
