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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.json.simple.JSONObject;

public class ElasticsearchUpdateProcessorFactory extends UpdateRequestProcessorFactory {

  private static final String ELASTICSEARCH_HOST = "host";
  private static final String ELASTICSEARCH_PORT = "port";
  private static final String ELASTICSEARCH_INDEX = "index";
  private static final String ELASTICSEARCH_INDEX_TYPE = "type";

  private static final String ELASTICSEARCH_HOST_DEFAULT = "localhost";
  private static final String ELASTICSEARCH_PORT_DEFAULT = "9200";
  private static final String ELASTICSEARCH_INDEX_DEFAULT = "datafari";
  private static final String ELASTICSEARCH_INDEX_TYPE_DEFAULT = "fileshare";

  private String elasticsearchHost;
  private String elasticsearchPort;
  private String elasticsearchIndex;
  private String elasticsearchIndexType;

  @Override
  public void init(@SuppressWarnings("rawtypes") final NamedList args) {
    if (args != null) {
      final SolrParams params = args.toSolrParams();
      this.elasticsearchHost = params.get(ELASTICSEARCH_HOST, ELASTICSEARCH_HOST_DEFAULT);
      this.elasticsearchPort = params.get(ELASTICSEARCH_PORT, ELASTICSEARCH_PORT_DEFAULT);
      this.elasticsearchIndex = params.get(ELASTICSEARCH_INDEX, ELASTICSEARCH_INDEX_DEFAULT);
      this.elasticsearchIndexType = params.get(ELASTICSEARCH_INDEX_TYPE, ELASTICSEARCH_INDEX_TYPE_DEFAULT);
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
    return new ElasticsearchUpdateProcessor(next);
  }

  private class ElasticsearchUpdateProcessor extends UpdateRequestProcessor {

    private boolean mappingExists = false;

    public ElasticsearchUpdateProcessor(final UpdateRequestProcessor next) {
      super(next);
      // TODO Auto-generated constructor stub
    }

    private String readFile(final String fileName) throws IOException {
      final BufferedReader br = new BufferedReader(new FileReader(fileName));
      try {
        final StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
          sb.append(line);
          sb.append("\n");
          line = br.readLine();
        }
        return sb.toString();
      } finally {
        br.close();
      }
    }

    /**
     * Check if the mapping for FileShare exists in Elasticsearch, if not try to
     * create it and return the status
     *
     * @return true if the mapping exists or has been successfully created,
     *         false otherwise
     *
     * @throws ClientProtocolException
     * @throws IOException
     */
    private synchronized boolean mappingExists() throws ClientProtocolException, IOException {

      if (mappingExists) {
        return mappingExists;
      } else {

        final HttpClient client = HttpClients.createDefault();
        final HttpGet get = new HttpGet("http://" + elasticsearchHost + ":" + elasticsearchPort + "/" + elasticsearchIndex + "/" + elasticsearchIndexType + "/_mapping");

        // Check if mapping exists
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() != 200) {

          // Get the mapping
          String mapping = readFile("../conf/elasticsearch_datafari_mapping.txt");

          // Configure the mapping with the provided IndexType
          mapping = mapping.replace(ELASTICSEARCH_INDEX_TYPE_DEFAULT, elasticsearchIndexType);

          // Put the mapping to Elasticsearch
          final HttpEntity putEntity = new StringEntity(mapping, "UTF-8");
          final HttpPut put = new HttpPut("http://" + elasticsearchHost + ":" + elasticsearchPort + "/" + elasticsearchIndex);
          put.setEntity(putEntity);
          response = client.execute(put);
          if (response.getStatusLine().getStatusCode() != 200) {
            mappingExists = false;
          } else {
            mappingExists = true;
          }
        } else {
          mappingExists = true;
        }
      }

      return mappingExists;
    }

    @Override
    public void processAdd(final AddUpdateCommand cmd) throws IOException {

      // Get the Solr document
      final SolrInputDocument doc = cmd.getSolrInputDocument();

      // Get the id
      final String id = (String) doc.getFieldValue("id");

      // Create the Elasticsearch document
      final JSONObject elasticDoc = new JSONObject();

      // Set the Elasticsearch document fields based on the Solr document
      // ones
      final String titleEn = (String) doc.getFieldValue("title_en");
      String title = "";
      if (titleEn != null && !titleEn.equals("")) {
        title = titleEn;
      } else {
        final String titleFr = (String) doc.getFieldValue("title_fr");
        if (titleFr != null && !titleFr.equals("")) {
          title = titleFr;
        }
      }

      elasticDoc.put("url", doc.getFieldValue("url"));
      elasticDoc.put("title", title);
      elasticDoc.put("source", doc.getFieldValue("source"));
      elasticDoc.put("extension", doc.getFieldValue("extension"));
      elasticDoc.put("title_en", doc.getFieldValue("title_en"));
      elasticDoc.put("title_fr", doc.getFieldValue("title_fr"));
      elasticDoc.put("language", doc.getFieldValue("language"));
      elasticDoc.put("last_modified", doc.getFieldValue("last_modified"));
      elasticDoc.put("content", doc.getFieldValue("content"));
      elasticDoc.put("content_en", doc.getFieldValue("content_en"));
      elasticDoc.put("content_fr", doc.getFieldValue("content_fr"));
      elasticDoc.put("suggest", doc.getFieldValue("suggest"));
      elasticDoc.put("spell", doc.getFieldValue("spell"));
      elasticDoc.put("signature", doc.getFieldValue("signature"));
      elasticDoc.put("allow_token_document", doc.getFieldValue("allow_token_document"));
      elasticDoc.put("allow_token_parent", doc.getFieldValue("allow_token_parent"));
      elasticDoc.put("allow_token_share", doc.getFieldValue("allow_token_share"));
      elasticDoc.put("deny_token_document", doc.getFieldValue("deny_token_document"));
      elasticDoc.put("deny_token_parent", doc.getFieldValue("deny_token_parent"));
      elasticDoc.put("deny_token_share", doc.getFieldValue("deny_token_share"));

      // Check if Elasticsearch is ready for indexing
      if (mappingExists()) {

        // Prepare the POST request
        final HttpClient client = HttpClients.createDefault();
        final String idB64 = Base64.getEncoder().encodeToString(id.getBytes());
        final HttpPost post = new HttpPost("http://" + elasticsearchHost + ":" + elasticsearchPort + "/" + elasticsearchIndex + "/" + elasticsearchIndexType + "/" + idB64);

        // Index in Elasticsearch thanks to the POST request
        final String jsonString = elasticDoc.toJSONString();
        final HttpEntity entity = new StringEntity(jsonString, "UTF-8");
        post.setEntity(entity);
        client.execute(post);
      }

      // Run the next processor in the chain
      if (next != null) {
        next.processAdd(cmd);
      }

    }

  }
}
