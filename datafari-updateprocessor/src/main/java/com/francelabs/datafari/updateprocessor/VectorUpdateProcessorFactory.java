/*******************************************************************************
 *  * Copyright 2020 France Labs
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public class VectorUpdateProcessorFactory extends UpdateRequestProcessorFactory {


  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessorFactory.class.getName());

  private static final String DEFAULT_COLLECTION = "VectorMain";
  private static final String DEFAULT_HOST = "localhost:2181";
  private static final String COLLECTION = "collection";
  private static final String HOST = "host";

  private SolrParams params = null;
  private CloudSolrClient client;

  @Override
  public void init(final NamedList args) {
    // Retrieve the parameters if any
    if (args != null) {
      params = args.toSolrParams();
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    // Pass the parameters retrieved in the init (if any) to the update processor
    if (params.getBool("enabled", false) && tryInitClient()) {
      return new VectorUpdateProcessor(client, params, next);
    } else {
      // Dummy request processor forwarding the processing to the rest of the pipe when text tagger is disabled.
      return new UpdateRequestProcessor(next) {
      };
    }
  }

  /**
   * Initiate the SolrClient if it does not exist yet
   * @return true if the operation succeeded
   */
  private boolean tryInitClient() {
    if (client == null) {
      try {
        client = new CloudSolrClient.Builder(new ArrayList<>(Collections.singletonList(getParam(HOST, DEFAULT_HOST))), Optional.empty())
                .withDefaultCollection(getParam(COLLECTION, DEFAULT_COLLECTION))
                .build();
        final SolrPing ping = new SolrPing();
        client.request(ping);
        return true;
      } catch (final Exception e) {
        LOGGER.warn("{} error initializing the solr client to get tags, disabling text tagger.", e.getMessage());
        client = null;
        return false;
      }
    }
    return true;
  }

  /**
   * Extracts a String parameter from the SolrParams and return it if it exists.
   * Sets it to its default value if it doesn't exist
   *
   * @param paramName the name of the parameter to set
   * @param def       the default value to use
   * @return the new value for the parameter, the default value is returned when the parameter is malformed and the tagger is disabled.
   */
  private String getParam(final String paramName, final String def) {
    String result = params.get(paramName);
    return (result != null && !result.isEmpty()) ? result : def;
  }

}
