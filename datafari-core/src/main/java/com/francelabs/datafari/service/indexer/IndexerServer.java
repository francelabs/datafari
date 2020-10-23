package com.francelabs.datafari.service.indexer;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.solr.client.solrj.SolrServerException;

public interface IndexerServer {

  /**
   * Return the number of currently indexed documents in the server
   *
   * @return the number of indexed documents, -1 if there is a problem to retrieve this info
   */
  public int getNumberOfIndexedDocuments();

  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception;

  public void executeUpdateRequest(final IndexerUpdateRequest ur) throws Exception;

  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception;

  public void pushDoc(final IndexerInputDocument document) throws Exception;

  public void commit() throws Exception;

  public IndexerResponseDocument getDocById(final String id) throws Exception;

  public void deleteById(final String id) throws Exception;

  public void processStatsResponse(final IndexerQueryResponse queryResponse);

  public void uploadConfig(Path configPath, String configName) throws IOException;

  public void uploadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException;

  public void downloadConfig(Path configPath, String configName) throws IOException;

  public void downloadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException;

  public void reloadCollection(String collectionName) throws SolrServerException, IOException;

  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception;

  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value) throws Exception;

  public void close() throws Exception;

}
