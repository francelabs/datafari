/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class DuplicatesDeleteUpdateProcessorFactory extends UpdateRequestProcessorFactory {

  private static final String ENABLED_PARAM = "enabled";
  private static final String SOLR_HOST_PARAM = "solr_host";
  private static final String SOLR_HOST_DEFAULT = "localhost:2181";
  private static final String COLLECTION_PARAM = "collection";
  private static final String COLLECTION_DEFAULT = "Duplicates";
  protected CloudSolrClient standardClient;
  protected boolean enabled;
  protected String collection;
  protected String solr_host;

  @Override
  public void init(final NamedList args) {
    if (args != null) {
      final SolrParams params = args.toSolrParams();
      this.enabled = params.getBool(ENABLED_PARAM, false);
      this.collection = params.get(COLLECTION_PARAM, COLLECTION_DEFAULT);
      this.solr_host = params.get(SOLR_HOST_PARAM, SOLR_HOST_DEFAULT);
      final List<String> zkHosts = new ArrayList<>();
      zkHosts.add(solr_host);
      standardClient = new CloudSolrClient.Builder(zkHosts, Optional.empty()).build();
      standardClient.setDefaultCollection(collection);
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    return new DuplicatesDeleteUpdateProcessor(next);
  }

  class DuplicatesDeleteUpdateProcessor extends UpdateRequestProcessor {

    public DuplicatesDeleteUpdateProcessor(final UpdateRequestProcessor next) {
      super(next);
    }

    @Override
    public void processDelete(final DeleteUpdateCommand cmd) throws IOException {

      if (enabled) {
        // retrieve the Solr doc id
        final String docID = cmd.id;
        try {
          standardClient.deleteById(collection, docID);
        } catch (final SolrServerException e) {
          throw new IOException(e);
        }
      }
      // Run the next processor in the chain
      if (next != null) {
        next.processDelete(cmd);
      }

    }

  }

}
