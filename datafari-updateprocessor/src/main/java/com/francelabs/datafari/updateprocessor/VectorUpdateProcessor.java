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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class VectorUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessor.class.getName());
  boolean enabled = false;
  CloudSolrClient client;

  private static final int CHUNK_SIZE = 4000;
  private static final int MAX_OVERLAP_SIZE = 100;
  private static final int VECTOR_DIMENSION = 4;

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

      String content = "";
      String parentId = (String) parentDoc.get("id").getValue();
      final SolrInputField contentFieldFr = parentDoc.get("content_fr");
      final SolrInputField contentFieldEn = parentDoc.get("content_en");
      if (contentFieldFr != null) {
        content = (String) contentFieldFr.getFirstValue();
      } else if (contentFieldEn != null) {
        content = (String) contentFieldEn.getFirstValue();
      }

      deleteExistingChildren(parentId);

      if (content != null && !content.isEmpty()) {

        // Chunking
        Tokenizer tokenizer = new OpenAiTokenizer();
        DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, MAX_OVERLAP_SIZE, tokenizer);
        Document document = new Document(content);
        List<TextSegment> chunks = splitter.split(document);

        for (TextSegment chunk : chunks) {
          if(chunk != null && !chunk.text().isEmpty()) {

            // Vector embeddings
            List<Float> vector = vectorEmbeddings(chunk.text());

            // Sub-document creation
            if (chunk.text() != null && vector.size() == VECTOR_DIMENSION) {

              SolrInputDocument vectorDocument = parentDoc.deepCopy();
              String vectorField = "vector_1536";

              String id = parentId + "_" + chunks.indexOf(chunk);
              vectorDocument.removeField("id");
              vectorDocument.addField("id", id);
              //vectorDocument.addField(vectorField, vector);
              vectorDocument.addField("parent_doc", parentId);
              vectorDocument.addField("exactContent", chunk.text());
              vectorDocument.removeField("content_en");
              vectorDocument.removeField("content_fr");

              // URL
              String url;
              if (parentDoc.containsKey("url")) {
                url = (String) parentDoc.getFieldValue("url");
                vectorDocument.addField("url", url);
              } else {
                url = (String) parentDoc.getFieldValue("id");
                vectorDocument.addField("url", url);
              }

              // TITLES
              if (parentDoc.getFieldValue("ignored_dc_title") != null) {
                parentDoc.getFieldValues("ignored_dc_title").forEach(value ->
                        vectorDocument.addField("title", value));
              }

              // keep the jsoup or filename as the first title for the searchView of Datafari
              String jsouptitle = "";
              String filename = "";
              if (parentDoc.getField("jsoup_title") != null) {
                jsouptitle = (String) parentDoc.getFieldValue("jsoup_title");
              }

              final SolrInputField streamNameField = parentDoc.get("ignored_stream_name");
              if (streamNameField != null && !streamNameField.getFirstValue().toString().isEmpty() && !streamNameField.getFirstValue().toString().toLowerCase().contentEquals("docname")) {
                filename = (String) streamNameField.getFirstValue();
              } else {
                final Pattern pattern = Pattern.compile("[^/]*$");
                final Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                  filename = matcher.group();
                }
              }
              if (!jsouptitle.isEmpty()) {
                vectorDocument.addField("title", jsouptitle);
              } else if (!filename.isEmpty()) {
                vectorDocument.addField("title", filename);
              }
              // The title field has lost its original value(s) after the LangDetectLanguageIdentifierUpdateProcessorFactory
              // Need to set it back from the exactTitle field
              if (parentDoc.get("exactTitle") != null) {
                for (final Object value : parentDoc.getFieldValues("exactTitle")) {
                  vectorDocument.addField("title", value);
                }
              }

              try {
                client.add(vectorDocument);
                client.commit();
              } catch (SolrServerException e) {
                LOGGER.warn("Warning : the document associated to the chunk {} could not be added.", id);
              }
            }
          } else {
            LOGGER.warn("The file {} appears to be empty and has been ignored during vector embeddings.", parentDoc.get("id").getValue());
          }
        }

      }



      // VERY IMPORTANT ! without this line of code any other Update Processor declared AFTER this one in the conf WILL NOT EXECUTE
      super.processAdd(cmd);
    }
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

  /**
   *
   * @param content : The text content to embbed
   * @return The generated vector
   */
  private List<Float> vectorEmbeddings(String content) {
    // TODO : This method should be edited to implement the Solr Embeddings Model
/*
    SolrEmbeddingModel embedder = modelStore.getModel(embeddingModelName);
    float[] vectorToSearch = embedder.vectorise(qstr);*/


    /*
    List<Float> vector = new ArrayList<>();
    for (float f : vectorToSearch) {
      vector.add(f);
    }*/
    List<Float> vector = Arrays.asList(0.25f , -0.1f, 0.7f, -0.1f);

    return vector;
  }

}
