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
package com.francelabs.datafari.service.indexer.solr;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.FieldTypes;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.ReplaceFieldType;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldTypesResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkMaintenanceUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerUpdateRequest;
import com.francelabs.datafari.solr.custom.ModifiedHttpSolrClient;
import com.francelabs.datafari.statistics.StatsUtils;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;

public class SolrIndexerServer implements IndexerServer {

  private static List<String> defaultZkHosts = new ArrayList<>();
  static {
    defaultZkHosts.add("localhost:2181");
  }
  private static String defaultSolrServer = "localhost";
  private static String defaultSolrPort = "8983";
  private static String defaultSolrProtocol = "http";
  private static String defaultLocation = "/solr/FileShare";
  private final Logger LOGGER = LogManager.getLogger(SolrIndexerServer.class.getName());
  private CloudSolrClient client;
  private ModifiedHttpSolrClient httpClient;
  private ZkClientClusterStateProvider zkManager;
  private SolrZkClient zkClient;

  public SolrIndexerServer(final String core) throws Exception {
    // Zookeeper Hosts
    final List<String> zkHosts = DatafariMainConfiguration.getInstance().getZkHosts();

    try {
      client = new CloudSolrClient.Builder(zkHosts, Optional.empty()).build();
      String protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);
      if (protocol == null) {
        LOGGER.warn("Unable to get Solr protocol from Solr properties, switching to default value: " + defaultSolrProtocol);
        protocol = defaultSolrProtocol;
      }
      String server = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
      if (server == null) {
        LOGGER.warn("Unable to get Solr server from Solr properties, switching to default value: " + defaultSolrServer);
        server = defaultSolrServer;
      }
      String port = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
      if (port == null) {
        LOGGER.warn("Unable to get Solr port from Solr properties, switching to default value: " + defaultSolrPort);
        port = defaultSolrPort;
      }
      final String location = "/solr/" + "FileShare";
      final String solrUrl = protocol + "://" + server + ":" + port + location;
      httpClient = new ModifiedHttpSolrClient(solrUrl, HttpClientBuilder.create().build(), new XMLResponseParser(), true);
      httpClient.setUseMultiPartPost(true);
      zkManager = new ZkClientClusterStateProvider(zkHosts, null);
      zkClient = new SolrZkClient(defaultZkHosts.get(0), 60000);
      client.setDefaultCollection(core);
      final SolrPing ping = new SolrPing();
      client.request(ping);
    } catch (final Exception e) {
      // test default param
      try {
        client = new CloudSolrClient.Builder(defaultZkHosts, Optional.empty()).build();
        final String solrUrl = defaultSolrProtocol + "://" + defaultSolrServer + ":" + defaultSolrPort + defaultLocation;
        httpClient = new ModifiedHttpSolrClient(solrUrl, HttpClientBuilder.create().build(), new XMLResponseParser(), true);
        httpClient.setUseMultiPartPost(true);
        zkManager = new ZkClientClusterStateProvider(defaultZkHosts, null);
        zkClient = new SolrZkClient(defaultZkHosts.get(0), 60000);
        client.setDefaultCollection(core);
        final SolrPing ping = new SolrPing();
        client.request(ping);
      } catch (final Exception e2) {
        LOGGER.error("Cannot instanciate Solr Client for core : " + core, e);
        throw new Exception("Cannot instanciate Solr Client for core : " + core);
      }
    }

  }

  @Override
  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception {
    try {
      final SolrQuery solrQuery = ((SolrIndexerQuery) query).prepareQuery();
      final QueryResponse response = client.query(solrQuery);
      final SolrIndexerQueryResponse sir = new SolrIndexerQueryResponse(solrQuery, response);
      return sir;
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void executeUpdateRequest(final IndexerUpdateRequest ur) throws Exception {
    final ContentStreamUpdateRequest csur = ((SolrIndexerUpdateRequest) ur).prepareUpdateRequest();
    csur.setMethod(METHOD.POST);
    httpClient.request(csur);
  }

  @Override
  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    client.add(solrDoc, commitWithinMs);
  }

  @Override
  public IndexerResponseDocument getDocById(final String id) throws Exception {
    final SolrDocument document = client.getById(id);
    return new SolrIndexerResponseDocument(document);
  }

  @Override
  public void uploadConfig(final Path configPath, final String configName) throws IOException {
    zkManager.uploadConfig(configPath, configName);
  }

  @Override
  public void uploadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException {
    final Path localPath = Paths.get(localDirectory + "/" + fileToUpload);
    LOGGER.error("zkpath : " + "/configs/" + collection + "/" + fileToUpload);
    LOGGER.error("localPath : " + localDirectory + fileToUpload);
    ZkMaintenanceUtils.uploadToZK(zkClient, localPath, "/configs/" + collection + "/" + fileToUpload, null);

  }

  @Override
  public void downloadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException {
    final Path localPath = Paths.get(localDirectory + "/" + fileToUpload);
    ZkMaintenanceUtils.downloadFromZK(zkClient, "/configs/" + collection + "/" + fileToUpload, localPath);
    LOGGER.error("zkpath : " + "/configs/" + collection + "/" + fileToUpload);
    LOGGER.error("localPath : " + localDirectory + fileToUpload);
  }

  @Override
  public void downloadConfig(final Path configPath, final String configName) throws IOException {
    zkManager.downloadConfig(configName, configPath);

  }

  @Override
  public void reloadCollection(final String collectionName) throws SolrServerException, IOException {
    CollectionAdminRequest.reloadCollection(collectionName).process(client);
  }

  @Override
  public void processStatsResponse(final IndexerQueryResponse queryResponse) {
    final QueryResponse solrResponse = ((SolrIndexerQueryResponse) queryResponse).getQueryResponse();
    final NamedList responseHeader = solrResponse.getResponseHeader();
    final FacetField QFacet = solrResponse.getFacetField("q");

    final Long numTot = queryResponse.getNumFound();

    final SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.setNumFound(QFacet.getValueCount());
    solrDocumentList.setStart(0);

    if (numTot != 0) {
      final Map<String, FieldStatsInfo> stats = solrResponse.getFieldStatsInfo();
      final List<FieldStatsInfo> noHitsStats = stats.get("noHits").getFacets().get("q");
      final List<FieldStatsInfo> QTimeStats = stats.get("QTime").getFacets().get("q");
      List<FieldStatsInfo> positionClickTotStats = null;
      try {
        positionClickTotStats = stats.get("positionClickTot").getFacets().get("q");
      } catch (final Exception e) {

      }
      final List<FieldStatsInfo> clickStats = stats.get("click").getFacets().get("q");
      final List<FieldStatsInfo> numClicksStats = stats.get("numClicks").getFacets().get("q");
      final List<FieldStatsInfo> numFoundStats = stats.get("numFound").getFacets().get("q");

      final List<Count> QFacetValues = QFacet.getValues();

      final Map<String, SolrDocument> mapDocuments = new HashMap<>();

      for (int i = 0; i < QFacetValues.size(); i++) {
        final SolrDocument doc = new SolrDocument();
        final String query = QFacetValues.get(i).getName();

        final double count = QFacetValues.get(i).getCount();
        final double frequency = StatsUtils.round(count * 100 / numTot, 2, BigDecimal.ROUND_HALF_UP);

        doc.addField("query", query);

        doc.addField("count", count);
        doc.addField("frequency", frequency);
        mapDocuments.put(query, doc);
        solrDocumentList.add(doc);
      }

      for (int i = 0; i < QTimeStats.size(); i++) {
        final String query = QTimeStats.get(i).getName();
        final SolrDocument doc = mapDocuments.get(query);

        final int AVGHits = new Double((Double) numFoundStats.get(i).getMean()).intValue();
        final Double noHits = new Double((Double) noHitsStats.get(i).getSum());
        final int AVGQTime = new Double((Double) QTimeStats.get(i).getMean()).intValue();
        final int MAXQTime = new Double((Double) QTimeStats.get(i).getMax()).intValue();
        final double click = new Double((Double) clickStats.get(i).getSum());
        final double clickRatio = StatsUtils.round(click * 100 / (Double) doc.getFirstValue("count"), 2, BigDecimal.ROUND_HALF_UP);
        if (click > 0) {
          final double AVGClickPosition = new Double((Double) positionClickTotStats.get(i).getSum() / (Double) numClicksStats.get(i).getSum()).intValue();

          doc.addField("AVGClickPosition", AVGClickPosition);

        } else {
          doc.addField("AVGClickPosition", "-");
        }

        doc.addField("withClickRatio", clickRatio);
        doc.addField("AVGHits", AVGHits);
        doc.addField("numNoHits", noHits);
        doc.addField("withClick", click);
        doc.addField("AVGQTime", AVGQTime);
        doc.addField("MaxQTime", MAXQTime);
      }

    }

    final NamedList<Object> response = new SimpleOrderedMap<>();
    response.add("responseHeader", responseHeader);
    response.add("response", solrDocumentList);
    solrResponse.setResponse(response);

  }

  @Override
  public void pushDoc(final IndexerInputDocument document) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    client.add(solrDoc);

  }

  @Override
  public void commit() throws Exception {
    client.commit();

  }

  @Override
  public void deleteById(final String id) throws Exception {
    // Normalize/clean the id
    String cleanId = id.toLowerCase();
    cleanId = Normalizer.normalize(cleanId, Normalizer.Form.NFD);
    cleanId = cleanId.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    client.deleteById(cleanId);

  }

  /**
   * Get current value for a filter of an analyzer type for text_* fields
   *
   * filterClass = name of the filter filterAttr = attr in filter
   *
   */
  @Override
  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception {
    String value = "";
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(client.request(solrRequest));

    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      // get all analyzers for text_* field
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = new ArrayList<>();
        analyzers.add(fieldType.getAnalyzer());
        analyzers.add(fieldType.getIndexAnalyzer());
        analyzers.add(fieldType.getQueryAnalyzer());
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null) {
            final List<Map<String, Object>> filters = analyzer.getFilters();
            if (filters != null) {
              for (final Map<String, Object> filter : filters) {
                if (filter != null) {
                  final String clazzAttr = (String) filter.get("class");
                  if (clazzAttr != null && clazzAttr.equals(filterClass)) {
                    // get last value
                    value = (String) filter.get(filterAttr);
                  }
                }
              }
            }
          }
        }
      }
    }
    return value;
  }

  /**
   * Set the current value for a filter of an analyzer type
   *
   * filterClass = name of the filter filterAttr = attribute of the filter value = value to change
   *
   */
  @Override
  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value) throws Exception {
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(client.request(solrRequest));
    final List<Update> updates = new ArrayList<>();
    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = new ArrayList<>();
        analyzers.add(fieldType.getAnalyzer());
        analyzers.add(fieldType.getIndexAnalyzer());
        analyzers.add(fieldType.getQueryAnalyzer());
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null) {
            final List<Map<String, Object>> filters = analyzer.getFilters();
            if (filters != null) {
              for (final Map<String, Object> filter : filters) {
                if (filter != null) {
                  final String clazzAttr = (String) filter.get("class");
                  if (clazzAttr != null && clazzAttr.equals(filterClass)) {
                    filter.put(filterAttr, value);
                    // keep each modified fieldType definition
                    updates.add(new ReplaceFieldType(fieldType));
                  }
                }
              }
            }
          }
        }
      }
    }

    // send a bulk update of each modified fieldType definition
    final MultiUpdate multiUpdateRequest = new MultiUpdate(updates);
    client.request(multiUpdateRequest);

  }
}
