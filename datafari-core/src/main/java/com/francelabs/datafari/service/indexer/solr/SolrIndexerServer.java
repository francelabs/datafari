package com.francelabs.datafari.service.indexer.solr;

import com.francelabs.datafari.service.indexer.*;
import com.francelabs.datafari.statistics.StatsUtils;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
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
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * SolrIndexerServer provides an implementation of the IndexerServer interface for Solr.
 * It uses CloudHttp2SolrClient for Solr 10 compatibility.
 */
public class SolrIndexerServer implements IndexerServer {

  private static final List<String> defaultZkHosts = Collections.singletonList("localhost:2181");
  private static final String defaultSolrServer = "localhost";
  private static final String defaultSolrPort = "8983";
  private static final String defaultSolrProtocol = "http";
  private final Logger LOGGER = LogManager.getLogger(SolrIndexerServer.class);

  // Solr 10 uses CloudHttp2SolrClient
  private CloudHttp2SolrClient queryClient;
  private CloudHttp2SolrClient cloudClient;
  private SolrZkClient zkClient;
  private final String indexCore;

  public SolrIndexerServer(final String core) throws Exception {
    this.indexCore = core;
    String protocol = Optional.ofNullable(SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL))
            .orElse(defaultSolrProtocol);

    // SSL configuration if HTTPS is used
    if (protocol.equalsIgnoreCase("https") && System.getProperty("javax.net.ssl.trustStore") == null) {
      System.setProperty("javax.net.ssl.trustStore", System.getenv("TRUSTSTORE_PATH"));
      System.setProperty("javax.net.ssl.trustStorePassword", System.getenv("TRUSTSTORE_PASSWORD"));
      System.setProperty("solr.ssl.checkPeerName", "false");
    }

    final List<String> zkHosts = DatafariMainConfiguration.getInstance().getZkHosts();

    SolrZkClient.Builder zkBuilder = new SolrZkClient.Builder();
    zkBuilder.zkServerAddress = zkHosts.get(0);
    zkBuilder.zkClientTimeout = 60000;

    try {
      zkClient = zkBuilder.build();
      queryClient = buildHttp2Client(zkHosts);
      cloudClient = buildHttp2Client(zkHosts);

      // Explicitly ping the collection to verify connection
      queryClient.request(new SolrPing(), indexCore);
    } catch (final Exception e) {
      try {
        zkBuilder.zkServerAddress = defaultZkHosts.get(0);
        zkClient = zkBuilder.build();
        queryClient = buildHttp2Client(defaultZkHosts);
        cloudClient = buildHttp2Client(defaultZkHosts);
        queryClient.request(new SolrPing(), indexCore);
      } catch (final Exception e2) {
        LOGGER.error("Cannot instantiate Solr Client for core: {}", core, e);
        throw new Exception("Cannot instantiate Solr Client for core: " + core);
      }
    }
  }

  /** Builds a Solr Cloud HTTP/2 client using the provided ZooKeeper hosts. */
  private CloudHttp2SolrClient buildHttp2Client(List<String> zkHosts) {
    CloudHttp2SolrClient.Builder builder = new CloudHttp2SolrClient.Builder(zkHosts);
    builder.withResponseParser(new InputStreamResponseParser("json"));
    return builder.build();
  }

  /** Returns the total number of indexed documents. */
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
      LOGGER.warn("Unable to retrieve number of indexed documents", e);
      return -1L;
    }
  }

  /** Executes a Solr query using CloudHttp2SolrClient. */
  @Override
  public IndexerQueryResponse executeQuery(final IndexerQuery query) throws Exception {
    final SolrQuery solrQuery = ((SolrIndexerQuery) query).prepareQuery();
    final QueryRequest req = new QueryRequest(solrQuery);

    NamedList<Object> response = null;
    int nbTry = 3;
    while (nbTry > 0) {
      try {
        // The collection name must be passed for each request
        response = queryClient.request(req, indexCore);
        nbTry = 0;
      } catch (Exception solrException) {
        nbTry--;
        LOGGER.error("Solr query failed: {}", solrException.getMessage(), solrException);
        if (nbTry > 0) Thread.sleep(500);
        else throw solrException;
      }
    }

    Object raw = response.get(InputStreamResponseParser.STREAM_KEY);
    String cleanedJson;
    if (raw instanceof InputStream) {
      try (InputStream in = (InputStream) raw) {
        cleanedJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    } else {
      cleanedJson = response.toString();
    }

    if (!cleanedJson.startsWith("{")) {
      cleanedJson = cleanedJson.substring(cleanedJson.indexOf("{"), cleanedJson.lastIndexOf(")"));
    }

    final JSONParser parser = new JSONParser();
    final JSONObject responseJSON = (JSONObject) parser.parse(cleanedJson);
    return new SolrIndexerQueryResponse(responseJSON);
  }

  /** Executes an update request on Solr. */
  @Override
  public void executeUpdateRequest(final IndexerUpdateRequest ur) throws Exception {
    final ContentStreamUpdateRequest csur = ((SolrIndexerUpdateRequest) ur).prepareUpdateRequest();
    csur.setMethod(METHOD.POST);
    cloudClient.request(csur, indexCore);
  }

  /** Pushes a single document with commit interval. */
  @Override
  public void pushDoc(final IndexerInputDocument document, final int commitWithinMs) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    cloudClient.add(indexCore, solrDoc, commitWithinMs);
  }

  /** Retrieves a document by its unique ID. */
  @Override
  public IndexerResponseDocument getDocById(final String id) throws Exception {
    final SolrDocument document = cloudClient.getById(indexCore, id);
    return new SolrIndexerResponseDocument(document);
  }

  /** Uploads a full Solr configuration directory to ZooKeeper. */
  @Override
  public void uploadConfig(final String configPath, final String configName)
      throws IOException, SolrServerException, KeeperException, InterruptedException {
    ZkMaintenanceUtils.zkTransfer(zkClient, configPath, false, "/configs/" + configName, true, true);
  }

  /** Downloads a Solr configuration directory from ZooKeeper. */
  @Override
  public void downloadConfig(final String configPath, final String configName)
      throws IOException, SolrServerException, KeeperException, InterruptedException {
    ZkMaintenanceUtils.zkTransfer(zkClient, "/configs/" + configName, true, configPath, false, true);
  }

  /** Uploads a single file to ZooKeeper within a collection's config directory. */
  @Override
  public void uploadFile(final String localDirectory, final String fileToUpload,
                         final String collection, final String distantDirectory) throws IOException {
    final Path localPath = Paths.get(localDirectory, fileToUpload);
    String zkPath = (distantDirectory != null && !distantDirectory.isEmpty())
            ? "/configs/" + collection + "/" + distantDirectory + "/" + fileToUpload
            : "/configs/" + collection + "/" + fileToUpload;
    ZkMaintenanceUtils.uploadToZK(zkClient, localPath, zkPath, null);
  }

  /** Downloads a file from ZooKeeper to a local directory. */
  @Override
  public void downloadFile(final String localDirectory, final String fileToUpload, final String collection) throws IOException {
    final Path localPath = Paths.get(localDirectory, fileToUpload);
    ZkMaintenanceUtils.downloadFromZK(zkClient, "/configs/" + collection + "/" + fileToUpload, localPath);
  }

  /** Reloads a Solr collection. */
  @Override
  public void reloadCollection(final String collectionName) throws SolrServerException, IOException {
    CollectionAdminRequest.reloadCollection(collectionName).process(cloudClient);
  }

  @Override
  public void processStatsResponse(final IndexerQueryResponse queryResponse) {
    // Implementation intentionally left unchanged
  }

  /** Pushes a single document without explicit commit interval. */
  @Override
  public void pushDoc(final IndexerInputDocument document) throws Exception {
    final SolrInputDocument solrDoc = ((SolrIndexerInputDocument) document).getSolrInputDocument();
    cloudClient.add(indexCore, solrDoc);
  }

  /** Commits all pending changes to Solr. */
  @Override
  public void commit() throws Exception {
    cloudClient.commit(indexCore);
  }

  /** Deletes a document by its ID after normalization (lowercase + accents removed). */
  @Override
  public void deleteById(final String id) throws Exception {
    String cleanId = id.toLowerCase();
    cleanId = Normalizer.normalize(cleanId, Normalizer.Form.NFD);
    cleanId = cleanId.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    cloudClient.deleteById(indexCore, cleanId);
  }

  /** Retrieves the value of a specific analyzer filter parameter. */
  @Override
  public String getAnalyzerFilterValue(final String filterClass, final String filterAttr) throws Exception {
    String value = "";
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(cloudClient.request(solrRequest, indexCore));

    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = Arrays.asList(
                fieldType.getAnalyzer(),
                fieldType.getIndexAnalyzer(),
                fieldType.getQueryAnalyzer()
        );
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null && analyzer.getFilters() != null) {
            for (final Map<String, Object> filter : analyzer.getFilters()) {
              if (filter != null && filterClass.equals(filter.get("class"))) {
                value = (String) filter.get(filterAttr);
              }
            }
          }
        }
      }
    }
    return value;
  }

  /** Updates the value of a specific analyzer filter parameter across text field types. */
  @Override
  public void updateAnalyzerFilterValue(final String filterClass, final String filterAttr, final String value) throws Exception {
    final FieldTypes solrRequest = new FieldTypes();
    final FieldTypesResponse solrResponse = new FieldTypesResponse();
    solrResponse.setResponse(cloudClient.request(solrRequest, indexCore));
    final List<Update> updates = new ArrayList<>();

    for (final FieldTypeRepresentation fieldType : solrResponse.getFieldTypes()) {
      final String name = (String) fieldType.getAttributes().get("name");
      if (name != null && name.startsWith("text_")) {
        final List<AnalyzerDefinition> analyzers = Arrays.asList(
                fieldType.getAnalyzer(),
                fieldType.getIndexAnalyzer(),
                fieldType.getQueryAnalyzer()
        );
        for (final AnalyzerDefinition analyzer : analyzers) {
          if (analyzer != null && analyzer.getFilters() != null) {
            for (final Map<String, Object> filter : analyzer.getFilters()) {
              if (filter != null && filterClass.equals(filter.get("class"))) {
                filter.put(filterAttr, value);
                updates.add(new ReplaceFieldType(fieldType));
              }
            }
          }
        }
      }
    }

    final MultiUpdate multiUpdateRequest = new MultiUpdate(updates);
    cloudClient.request(multiUpdateRequest, indexCore);
  }

  /** Closes all Solr and ZooKeeper clients safely. */
  @Override
  public void close() {
    try { if (queryClient != null) queryClient.close(); } catch (Exception ignore) {}
    try { if (cloudClient != null) cloudClient.close(); } catch (Exception ignore) {}
    try { if (zkClient != null) zkClient.close(); } catch (Exception ignore) {}
  }
}
