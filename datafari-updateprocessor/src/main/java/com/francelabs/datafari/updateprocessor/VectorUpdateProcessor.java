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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.*;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class VectorUpdateProcessor extends UpdateRequestProcessor {
  private static final Logger LOGGER = LogManager.getLogger(VectorUpdateProcessor.class.getName());
  boolean enabled = false;
  String splitterType = "recursiveSplitter";
  int chunksize = 300;
  int maxoverlap = 0;
  static int minChunkLength = 1;
  double minAlphaNumRatio = 0.0;
  CloudSolrClient client;

  public VectorUpdateProcessor(CloudSolrClient client, final SolrParams params, final UpdateRequestProcessor next) {
    super(next);
    if (params != null) {
      this.enabled = params.getBool("enabled", false);
      this.chunksize = params.getInt("chunksize", 300);
      this.maxoverlap = params.getInt("maxoverlap", 0);
      this.splitterType = params.get("splitter", "recursiveSplitter");
      this.minChunkLength = params.getInt("minchunklength", 1);
      this.minAlphaNumRatio = params.getDouble("minalphanumratio", 0.0);
      this.client = client;
    }
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException {
    if (enabled) {
      final SolrInputDocument parentDoc = cmd.getSolrInputDocument();

      String parentId = (String) parentDoc.get("id").getValue();
      LOGGER.debug("Processing chunking for document {}", parentId);
      String content = extractContent(parentDoc);

      deleteExistingChildren(parentId);

      if (content != null && !content.isEmpty()) {

        // Chunking
        List<TextSegment> chunks = chunkDocument(content);

        List<SolrInputDocument> batchDocs = new ArrayList<>();

        for (TextSegment chunk : chunks) {
          if(chunk != null && !chunk.text().isEmpty()) {

            // check if chunk is worth embedding
            if (isChunkTextValid(chunk.text())) {

              // Sub-document creation
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

        }

        // Send chunks to VectorMain
        LOGGER.debug("Chunking - {} chunks for document {}", batchDocs.size(), parentDoc.get("id").getValue());
        if (!batchDocs.isEmpty()){
          UpdateRequest solrRequest = new UpdateRequest();
          // Do not reject all update batch for some version conflicts
          solrRequest.setParam("failOnVersionConflicts", "false");
          solrRequest.add(batchDocs);

          try {
            solrRequest.process(client, "VectorMain");
          } catch (Exception e) {
            LOGGER.warn("Warning : chunks from document {} could not be added.", parentDoc.get("id").getValue());
            LOGGER.warn(e);
          }

        }

      }

      // VERY IMPORTANT ! without this line of code any other Update Processor declared AFTER this one in the conf WILL NOT EXECUTE
      super.processAdd(cmd);
    }
  }

  /**
   * Check if the chunk has a content worth embedding
   * @param text String
   * @return true or false
   */
  private boolean isChunkTextValid(String text) {

    // The content must not be empty
    String cleaned = text.trim();
    if (cleaned.isEmpty()) return false;

    // The chunk must constain at least minChunkLength alphanumerical characters
    long alphaNumCount = cleaned.chars()
            .filter(Character::isLetterOrDigit)
            .count();
    if (alphaNumCount < minChunkLength) return false;

    // Ratio ALPHANUM CHAR / LENGTH should be greater than minAlphaNumRatio
    int length = cleaned.length();
    double alphaNumRatio = (double) alphaNumCount / length;

    return alphaNumRatio >= minAlphaNumRatio;
  }

  /**
   * Chunking uses the following parameters:
   * *** splitter: A String specifying the splitter type
   * *** chunksize: The max size (in tokens) allowed for a chunk.
   * Since we are using OpenAI tokenizer, the chunk size in token may vary depending on the model you use.
   *
   * @param content: The content to be chunked
   * @return a list of TextSegment
   */
  private List<TextSegment> chunkDocument(String content) {
    Tokenizer tokenizer = new OpenAiTokenizer();
    DocumentSplitter splitter;


    // Chunking
    switch (this.splitterType) {
      case "splitterBySentence":
        splitter = new DocumentBySentenceSplitter(this.chunksize, this.maxoverlap, tokenizer);
        break;
      case "splitterByLine":
        splitter = new DocumentByLineSplitter(this.chunksize, this.maxoverlap, tokenizer);
        break;
      case "splitterByCharacter":
        splitter = new DocumentByCharacterSplitter(this.chunksize, this.maxoverlap, tokenizer);
        break;
      case "splitterByParagraph":
        splitter = new DocumentByParagraphSplitter(this.chunksize, this.maxoverlap, tokenizer);
        break;
      case "recursiveSplitter":
      default:
        splitter = DocumentSplitters.recursive(this.chunksize, this.maxoverlap, tokenizer);
    }

    Document document = Document.from(content);
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
    //  client.commit();
    } catch (SolrServerException|IOException e) {
      LOGGER.error("Could not delete existing children for this document");
    }
  }

}
