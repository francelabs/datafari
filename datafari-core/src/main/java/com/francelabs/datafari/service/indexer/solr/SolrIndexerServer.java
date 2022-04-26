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
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.FieldTypes;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.ReplaceFieldType;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.FieldTypesResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkMaintenanceUtils;
import org.apache.solr.common.util.NamedList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.noggit.JSONUtil;

import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;
import com.francelabs.datafari.service.indexer.IndexerFacetStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerFieldStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
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
  private static final String defaultSolrServer = "localhost";
  private static final String defaultSolrPort = "8983";
  private static final String defaultSolrProtocol = "http";
  private static final String defaultLocation = "/solr/";
  private final Logger LOGGER = LogManager.getLogger(SolrIndexerServer.class.getName());
  /**
   * queryClient must only be used for executeQuery method as it is configured not to treat the solr response and get a raw response (json) instead of a SolrJ object
   */
  private CloudSolrClient queryClient;
  /**
   * standardClient must be used for any request BUT the executeQuery method
   */
  private CloudSolrClient standardClient;
  private ModifiedHttpSolrClient httpClient;
  private ZkClientClusterStateProvider zkManager;
  private SolrZkClient zkClient;
  private final String indexCore;

  public SolrIndexerServer(final String core) throws Exception {
    indexCore = core;
    String protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);
    if (protocol == null) {
      LOGGER.warn("Unable to get Solr protocol from Solr properties, switching to default value: " + defaultSolrProtocol);
      protocol = defaultSolrProtocol;
    }

    // If protocol is https ensure the trustStore is correctly set
    if (protocol.toLowerCase().equals("https") && System.getProperty("javax.net.ssl.trustStore") == null) {
      System.setProperty("javax.net.ssl.trustStore", System.getenv("TRUSTSTORE_PATH"));
      System.setProperty("javax.net.ssl.trustStorePassword", System.getenv("TRUSTSTORE_PASSWORD"));
      System.setProperty("solr.ssl.checkPeerName", System.getenv("false"));
    }

    // Zookeeper Hosts
    final List<String> zkHosts = DatafariMainConfiguration.getInstance().getZkHosts();

    try {
      queryClient = new CloudSolrClient.Builder(zkHosts, Optional.empty()).build();
      standardClient = new CloudSolrClient.Builder(zkHosts, Optional.empty()).build();
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
      final String location = defaultLocation + indexCore;
      final String solrUrl = protocol + "://" + server + ":" + port + location;
      httpClient = new ModifiedHttpSolrClient(solrUrl, HttpClientBuilder.create().build(), new XMLResponseParser(), true);
      httpClient.setUseMultiPartPost(true);
      zkManager = new ZkClientClusterStateProvider(zkHosts, null);
      zkClient = new SolrZkClient(zkHosts.get(0), 60000);
      queryClient.setDefaultCollection(core);
      standardClient.setDefaultCollection(core);
      final NoOpResponseParser jsonWriter = new NoOpResponseParser();
      jsonWriter.setWriterType("json");
      queryClient.setParser(jsonWriter);
      final SolrPing ping = new SolrPing();
      queryClient.request(ping);
    } catch (final Exception e) {
      // test default param
      try {
        queryClient = new CloudSolrClient.Builder(defaultZkHosts, Optional.empty()).build();
        standardClient = new CloudSolrClient.Builder(defaultZkHosts, Optional.empty()).build();
        final String solrUrl = defaultSolrProtocol + "://" + defaultSolrServer + ":" + defaultSolrPort + defaultLocation + indexCore;
        httpClient = new ModifiedHttpSolrClient(solrUrl, HttpClientBuilder.create().build(), new XMLResponseParser(), true);
        httpClient.setUseMultiPartPost(true);
        zkManager = new ZkClientClusterStateProvider(defaultZkHosts, null);
        zkClient = new SolrZkClient(defaultZkHosts.get(0), 60000);
        queryClient.setDefaultCollection(core);
        standardClient.setDefaultCollection(core);
        final NoOpResponseParser jsonWriter = new NoOpResponseParser();
        jsonWriter.setWriterType("json");
        queryClient.setParser(jsonWriter);
        final SolrPing ping = new SolrPing();
        queryClient.request(ping);
      } catch (final Exception e2) {
        LOGGER.error("Cannot instanciate Solr Client for core : " + core, e);
        throw new Exception("Cannot instanciate Solr Client for core : " + core);
      }
    }

  }

  @Override
  public long getNumberOfIndexedDocuments() {
    try {
      final IndexerQuery query = IndexerServerManager.createQuery();
      query.setRequestHandler("/opensearch");
      query.setQuery("*:*");
      query.addParam("fl", "id");
      final IndexerQueryResponse response = executeQuery(query);
      return response.getNumFound();
    } catch (final Exception e) {
      LOGGER.warn("Unable to retrieve the number of indexed documents", e);
      return -1L;
    }
  }

  @Override
  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception {
    final SolrQuery solrQuery = ((SolrIndexerQuery) query).prepareQuery();
    final QueryRequest req = new QueryRequest(solrQuery);
    final NamedList<Object> response = queryClient.request(req);
    final JSONParser parser = new JSONParser();

    // the response may be encapsulated in a jQuery wrapper function, so remove it
    String cleanedJson = response.get("response").toString();
    if (!cleanedJson.startsWith("{")) {
      cleanedJson = cleanedJson.substring(cleanedJson.indexOf("{"), cleanedJson.lastIndexOf(")"));
    }
    final JSONObject responseJSON = (JSONObject) parser.parse(cleanedJson);
    final SolrIndexerQueryResponse sir = new SolrIndexerQueryResponse(responseJSON);
    return sir;
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
    standardClient.add(solrDoc, commitWithinMs);
  }

  @Override
  public IndexerResponseDocument getDocById(final String id) throws Exception {
    final SolrDocument document = standardClient.getById(id);
    return new SolrIndexerResponseDocument(document);
  }

  @Override
  public void uploadConfig(final Path configPath, final String configName) throws IOException {
    zkManager.uploadConfig(configPath, configName);
  }

  @Override
  public void downloadConfig(final Path configPath, final String configName) throws IOException {
    zkManager.downloadConfig(configName, configPath);
  }

  @Override
  public void uploadFile(final String localDirectory, final String fileToUpload, final String collection, final String distantDirectory) throws IOException {
    final Path localPath = Paths.get(localDirectory + "/" + fileToUpload);
    LOGGER.debug("zkpath : " + "/configs/" + collection + "/" + fileToUpload);
    LOGGER.debug("localPath : " + localDirectory + "/" + fileToUpload);
    if (distantDirectory != null && !distantDirectory.isEmpty())
      ZkMaintenanceUtils.uploadToZK(zkClient, localPath, "/configs/" + collection + "/" + distantDirectory + "/" + fileToUpload, null);
    else
      ZkMaintenanceUtils.uploadToZK(zkClient, localPath, "/configs/" + collection + "/" + fileToUpload, null);

  }

  @Override
  public void downloadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException {
    final Path localPath = Paths.get(localDirectory + "/" + fileToUpload);
    ZkMaintenanceUtils.downloadFromZK(zkClient, "/configs/" + collection + "/" + fileToUpload, localPath);
    LOGGER.debug("zkpath : " + "/configs/" + collection + "/" + fileToUpload);
    LOGGER.debug("localPath : " + localDirectory + fileToUpload);
  }

  @Override
  public void reloadCollection(final String collectionName) throws SolrServerException, IOException {
    CollectionAdminRequest.reloadCollection(collectionName).process(standardClient);
  }

  @Override
  public void processStatsResponse(final IndexerQueryResponse queryResponse) {
    final SolrIndexerQueryResponse solrResponse = (SolrIndexerQueryResponse) queryResponse;
    final JSONObject solrJSONResp = solrResponse.getQueryResponse();
    final JSONObject responseHeader = (JSONObject) solrJSONResp.get("responseHeader");
    final SolrIndexerFacetField QFacet = (SolrIndexerFacetField) solrResponse.getFacetFields().get("q");

    final Long numTot = queryResponse.getNumFound();

    final SolrDocumentList solrDocumentList = new SolrDocumentList();

    if (numTot != 0) {
      final Map<String, IndexerFieldStatsInfo> stats = solrResponse.getFieldStatsInfo();
      final IndexerFacetStatsInfo noHitsStats = stats.get("noHits").getFacets().get("q");
      final IndexerFacetStatsInfo QTimeStats = stats.get("QTime").getFacets().get("q");
      IndexerFacetStatsInfo positionClickTotStats = null;
      try {
        positionClickTotStats = stats.get("positionClickTot").getFacets().get("q");
      } catch (final Exception e) {

      }
      final IndexerFacetStatsInfo clickStats = stats.get("click").getFacets().get("q");
      final IndexerFacetStatsInfo numClicksStats = stats.get("numClicks").getFacets().get("q");
      final IndexerFacetStatsInfo numFoundStats = stats.get("numFound").getFacets().get("q");

      final List<IndexerFacetFieldCount> QFacetValues = QFacet.getValues();

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

      for (int i = 0; i < QTimeStats.getValuesStatsInfo().size(); i++) {
        final String query = QTimeStats.getValuesStatsInfo().get(i).getName();
        final SolrDocument doc = mapDocuments.get(query);

        final int AVGHits = new Double(numFoundStats.getValuesStatsInfo().get(i).getMean()).intValue();
        final Double noHits = new Double(noHitsStats.getValuesStatsInfo().get(i).getSum());
        final int AVGQTime = new Double(QTimeStats.getValuesStatsInfo().get(i).getMean()).intValue();
        final int MAXQTime = new Double(QTimeStats.getValuesStatsInfo().get(i).getMax()).intValue();
        final double click = new Double(clickStats.getValuesStatsInfo().get(i).getSum());
        final double clickRatio = StatsUtils.round(click * 100 / (Double) doc.getFirstValue("count"), 2, BigDecimal.ROUND_HALF_UP);
        if (click > 0) {
          final double AVGClickPosition = new Double(positionClickTotStats.getValuesStatsInfo().get(i).getSum() / numClicksStats.getValuesStatsInfo().get(i).getSum()).intValue();

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

    final JSONObject newRawResponse = new JSONObject();
    newRawResponse.put("responseHeader", responseHeader.clone());
    final JSONObject response = new JSONObject();
    newRawResponse.put("response", response);
    response.put("numFound", QFacet.getValues().size());
    response.put("start", 0);
    final JSONParser parser = new JSONParser();
    JSONArray docs = new JSONArray();
    try {
      docs = (JSONArray) parser.parse(JSONUtil.toJSON(solrDocumentList));
    } catch (final ParseException e) {
      LOGGER.error("Impossible to convert solrDocumentList to JSON string", e);
    }
    response.put("docs", docs);
    solrResponse.setResponse(newRawResponse);

  }

  @Override
  public void pushDoc(final IndexerInputDocument document) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    standardClient.add(solrDoc);

  }

  @Override
  public void commit() throws Exception {
    standardClient.commit();

  }

  @Override
  public void deleteById(final String id) throws Exception {
    // Normalize/clean the id
    String cleanId = id.toLowerCase();
    cleanId = Normalizer.normalize(cleanId, Normalizer.Form.NFD);
    cleanId = cleanId.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    standardClient.deleteById(cleanId);

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
    solrResponse.setResponse(standardClient.request(solrRequest));

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
    solrResponse.setResponse(standardClient.request(solrRequest));
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
    standardClient.request(multiUpdateRequest);

  }

  @Override
  public void close() throws Exception {
    LOGGER.debug("Closing Solr connections for core " + indexCore);
    queryClient.close();
    standardClient.close();
    zkClient.close();
    zkManager.close();
    httpClient.close();
    LOGGER.debug("Solr connections for core " + indexCore + " closed !");
  }
}
