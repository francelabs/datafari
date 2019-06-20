/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.cloud.ZkMaintenanceUtils;

public interface IndexerServer {

  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception;

  public void executeUpdateRequest(final IndexerUpdateRequest ur) throws Exception;

  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception;

  public void pushDoc(final IndexerInputDocument document) throws Exception;

  public void commit() throws Exception;

  public IndexerResponseDocument getDocById(final String id) throws Exception;

  public void deleteById(final String id) throws Exception;

  public void processStatsResponse(final IndexerQueryResponse queryResponse);

  public void uploadConfig(Path configPath, String configName) throws IOException;
  
  public void uploadFile(final String localDirectory,final String fileToUpload,final String collection) throws IOException;

  public void downloadConfig(Path configPath, String configName) throws IOException;
  
  public void downloadFile(final String localDirectory, final String fileToUpload,final String collection) throws IOException ;

  public void reloadCollection(String collectionName) throws SolrServerException, IOException;

  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception;

  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value) throws Exception;

}
