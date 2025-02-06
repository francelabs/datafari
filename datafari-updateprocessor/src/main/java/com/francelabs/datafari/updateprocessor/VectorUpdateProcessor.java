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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class VectorUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessor.class.getName());
  boolean enabled = false;
  CloudSolrClient client;

  private static final int CHUNK_SIZE = 4000;
  private static final int MAX_OVERLAP_SIZE = 100;

  public VectorUpdateProcessor(CloudSolrClient client, final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      this.enabled = params.getBool("enabled", false);
      this.client = client;
    }
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    if (enabled) {
      final SolrInputDocument parentDoc = cmd.getSolrInputDocument();

      String parentId = (String) parentDoc.get("id").getValue();
      String content = extractContent(parentDoc);

      deleteExistingChildren(parentId);

      if (content != null && !content.isEmpty()) {

        // Chunking
        List<TextSegment> chunks = chunkDocument(content);

        List<SolrInputDocument> batchDocs = new ArrayList<>();

        for (TextSegment chunk : chunks) {
          if(chunk != null && !chunk.text().isEmpty()) {

            // Sub-document creation
            if (chunk.text() != null) {

              SolrInputDocument vectorDocument = parentDoc.deepCopy();

              String id = parentId + "_" + chunks.indexOf(chunk);
              vectorDocument.removeField("id");
              vectorDocument.setField("id", id);
              vectorDocument.addField("parent_doc", parentId);

              // Remove all existing "content" fields:
              vectorDocument.removeField("content_en");
              vectorDocument.removeField("content_fr");
              vectorDocument.removeField("preview_content");
              vectorDocument.removeField("exactContent");

              // ExactContent should ony contain the chunk content:
              vectorDocument.addField("exactContent", chunk.text());
              vectorDocument.addField("embedded_content", chunk.text());

              // URL
              String url;
              if (parentDoc.containsKey("url")) {
                url = (String) parentDoc.getFieldValue("url");
                vectorDocument.addField("url", url);
              } else {
                url = (String) parentDoc.getFieldValue("id");
                vectorDocument.addField("url", url);
              }

              batchDocs.add(vectorDocument);
            }
          } else {
            LOGGER.warn("The file {} appears to be empty and has been ignored during vector embeddings.", parentDoc.get("id").getValue());
          }

          // Send chunks to VectorMain
          try {
            client.add(batchDocs);
            client.commit();
          } catch (SolrServerException e) {
            LOGGER.warn("Warning : chunks from document {} could not be added.", parentDoc.get("id"));
          }
        }

      }

      // VERY IMPORTANT ! without this line of code any other Update Processor declared AFTER this one in the conf WILL NOT EXECUTE
      super.processAdd(cmd);
    }
  }

  private static List<TextSegment> chunkDocument(String content) {
    Tokenizer tokenizer = new OpenAiTokenizer();
    DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, MAX_OVERLAP_SIZE, tokenizer);
    Document document = new Document(content);
    return splitter.split(document);
  }

  private static String extractContent(SolrInputDocument parentDoc) {
    String content = "";
    if (parentDoc.get("exactContent") != null) {
      content = (String) parentDoc.get("exactContent").getFirstValue();
    }
    return content;
  }

  @Override
  public void processDelete(DeleteUpdateCommand cmd) throws IOException {
    final String id = cmd.getId();
    deleteExistingChildren(id);
    super.processDelete(cmd);
  }

  /**
   * Delete all the children of the parent document in VectorMain collection
   * @param parentId : The ID of the parent
   */
  private void deleteExistingChildren(String parentId) {
    try {
      client.deleteByQuery("parent_doc:\"" + parentId + "\"");
      client.commit();
    } catch (SolrServerException|IOException e) {
      LOGGER.error("Could not delete existing children for this document");
    }
  }

}
