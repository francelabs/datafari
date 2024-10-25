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

import java.io.IOException;

import java.util.Arrays;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;

import dev.langchain4j.data.segment.TextSegment;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class VectorUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessor.class.getName());
  boolean enabled = false;
  CloudSolrClient client;

  private static final int CHUNK_SIZE = 20000;
  private static final int MAX_OVERLAP_SIZE = 200;
  private static final int VECTOR_DIMENSION = 4;

  public VectorUpdateProcessor(CloudSolrClient client, final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      boolean enabled = params.getBool("enabled", false);
      this.enabled = enabled;
      this.client = client;
    }
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    if (enabled) {
      final SolrInputDocument doc = cmd.getSolrInputDocument();

      LOGGER.info("Vector Update processor");

      String content = "";
      final SolrInputField contentFieldFr = doc.get("content_fr");
      final SolrInputField contentFieldEn = doc.get("content_en");
      if (contentFieldFr != null) {
        content = (String) contentFieldFr.getFirstValue();
      } else if (contentFieldEn != null) {
        content = (String) contentFieldEn.getFirstValue();
      }


      if (content != null && !content.isEmpty()) {
        LOGGER.info(content);

        // Chunking
        DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, MAX_OVERLAP_SIZE);
        Document document = new Document(content);
        List<TextSegment> chunks = splitter.split(document);
        String parentId = (String) doc.get("id").getValue();

        deleteExistingChildren(client, parentId);

        for (TextSegment chunk : chunks) {
          if(chunk != null && !chunk.text().isEmpty()) {
            LOGGER.debug(chunk.text());
            SolrInputDocument vectorDocument = new SolrInputDocument();

            List<Float> vector = vectorEmbeddings(chunk.text());

            if (chunk.text() != null && vector.size() == VECTOR_DIMENSION) {

              String id = doc.get("id") + "_" + chunks.indexOf(chunk);
              vectorDocument.addField("id", id);
              vectorDocument.addField("vector", vector);
              vectorDocument.addField("parent_doc", parentId);
              vectorDocument.addField("content", chunk.text());

              try {
                client.add(vectorDocument);
                client.commit();
              } catch (SolrServerException e) {
                LOGGER.warn("Warning : the document assiciated to the chunk {} could not be added.", id);
              }
            }
          } else {
            LOGGER.warn("The file {} appears to be empty and has been ignored during vector embeddings.", doc.get("id").getValue());
          }
        }

      }



      // VERY IMPORTANT ! without this line of code any other Update Processor declared AFTER this one in the conf WILL NOT EXECUTE
      super.processAdd(cmd);
    }
  }

  private void deleteExistingChildren(CloudSolrClient client, String parentId) {
    try {
      client.deleteByQuery("parent_doc:\"" + parentId + "\"");
      client.commit();
    } catch (SolrServerException|IOException e) {
      LOGGER.error("Could not delete existing children for this document");
    }
  }

  private List<Float> vectorEmbeddings(String content) {
    // TODO : This method should be edited to implement the Solr Embeddings Model

    /*
    SolrEmbeddingModel embedder = modelStore.getModel(embeddingModelName);
    float[] vectorToSearch = embedder.vectorise(qstr);
     */

    /*
    List<Float> vector = new ArrayList<>();
    for (float f : vectorToSearch) {
      vector.add(f);
    }*/
    List<Float> vector = Arrays.asList(0.25f , 4.1f, 3.7f, 5.1f);

    return vector;
  }

}
