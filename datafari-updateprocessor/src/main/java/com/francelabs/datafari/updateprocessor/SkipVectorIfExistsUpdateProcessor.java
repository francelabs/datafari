/*******************************************************************************
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

import java.io.IOException;

public class SkipVectorIfExistsUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(SkipVectorIfExistsUpdateProcessor.class.getName());
  boolean enabled = false;
  String vectorField;
  CloudSolrClient client;

  public SkipVectorIfExistsUpdateProcessor(CloudSolrClient client, final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      this.enabled = params.getBool("enabled", false);
      this.vectorField = params.get("outputField", "");
      this.client = client;
    }
  }

  @Override
  public void processAdd(AddUpdateCommand cmd) throws IOException {
    SolrInputDocument solrInputDoc = cmd.getSolrInputDocument();
    String docId = (String) solrInputDoc.getFieldValue("id");

    if (docId != null && !docId.isEmpty() && !vectorField.isEmpty()) {
      try {
        SolrQuery query = new SolrQuery("id:\"" + docId + "\"");
        query.setFields(vectorField);
        query.setRows(1);

        QueryResponse response = client.query(query);

        if (!response.getResults().isEmpty()) {
          SolrDocument existingDoc = response.getResults().get(0);
          if (existingDoc.containsKey(vectorField)) {
            // vectorField already set. Ignoring this document.
            LOGGER.debug("Existing embedding for {} -> document skipped", docId);
            return;
          }
        }

      } catch (SolrServerException | SolrException e) {
        throw new IOException("Erreur lors de la requête Solr pour l'ID " + docId, e);
      }
    } else {
      // docId or vectorField is empty
      LOGGER.debug("Missing docId or outputField-> document skipped", docId);
      return;
    }

    // If the chunk doesn't have an existing vectorField, keep going for embeddings
    LOGGER.debug("No existing vector for {} ({})  -> document ready for embeddings", docId, vectorField);
    super.processAdd(cmd);
  }

}
