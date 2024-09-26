/* $Id: SvnConnector.java 994959 2010-09-08 10:04:42Z krycek $ */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.manifoldcf.crawler.connectors.solr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.connectorcommon.common.XThreadStringBuffer;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.util.URLEncoder;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;

public class SolrIngesterConnector extends BaseRepositoryConnector {

  public static final String _rcsid = "@(#)$Id: solringesterConnector.java 994959 2010-09-08 10:04:42Z redguy $";

  private final static String ACTION_PARAM_NAME = "action";

  private final static String ACTION_CHECK = "check";

  private final static String ACTION_SEED = "seed";

  private final static String ACTION_ITEMS = "items";

  private final static String ACTION_ITEM = "item";

  /** Connection timeout */
  private int connectionTimeout = -1;

  /** Socket timeout */
  private int socketTimeout = -1;

  /** Session timeout */
  private long sessionTimeout = -1L;

  protected final static long sessionExpirationInterval = 300000L;

  private String solringesterLogin = null;

  private String solringesterPassword = null;

  /** Connection manager */
  private HttpClientConnectionManager connectionManager = null;

  /** HttpClientBuilder */
  private HttpClientBuilder builder = null;

  /** Httpclient instance */
  private CloseableHttpClient httpClient = null;

  private HttpSolrClient httpSolrClient = null;

  /** Connection timeout */
  private String connectionTimeoutString = null;

  /** Socket timeout */
  private String socketTimeoutString = null;

  private String solringesterEntryPoint = null;

  private static final String date_target_field = "last_modified";

  private final static String versionField = "_version_";

  protected static final String RELATIONSHIP_RELATED = "related";

  private static final String EDIT_CONFIGURATION_CONNECTOR_JS = "editConfiguration_connector.js";
  private static final String EDIT_CONFIGURATION_CONNECTOR_HTML = "editConfiguration_connector.html";
  private static final String VIEW_CONFIGURATION_CONNECTOR_HTML = "viewConfiguration_connector.html";

  private static final String EDIT_SPECIFICATION_JOB_PARAMETERS_JS = "editSpecification_job_parameters.js";
  private static final String EDIT_SPECIFICATION_JOB_PARAMETERS_HTML = "editSpecification_job_parameters.html";
  private static final String VIEW_SPECIFICATION_JOB_PARAMETERS_HTML = "viewSpecification_job_parameters.html";

  private static final String EDIT_SPECIFICATION_JOB_SECURITY_JS = "editSpecification_job_security.js";
  private static final String EDIT_SPECIFICATION_JOB_SECURITY_HTML = "editSpecification_job_security.html";
  private static final String VIEW_SPECIFICATION_JOB_SECURITY_HTML = "viewSpecification_job_security.html";

  protected final static String ACTIVITY_GET = "get document";

  private static final Level DOCPROCESSLEVEL = Level.forName("DOCPROCESS", 450);
  private static final Logger LOGGER = LogManager.getLogger(SolrIngesterConnector.class.getName());

  /**
   * Constructor.
   */
  public SolrIngesterConnector() {
  }

  @Override
  public int getMaxDocumentRequest() {
    return 1;
  }

  @Override
  public String[] getRelationshipTypes() {
    return new String[] { RELATIONSHIP_RELATED };
  }

  @Override
  public int getConnectorModel() {
    return SolrIngesterConnector.MODEL_ADD_CHANGE;
  }

  @Override
  public String[] getActivitiesList() {
    return new String[] { ACTIVITY_GET };
  }

  /**
   * For any given document, list the bins that it is a member of.
   */
  @Override
  public String[] getBinNames(final String documentIdentifier) {
    // Return the host name
    return new String[] { solringesterEntryPoint };
  }

  // All methods below this line will ONLY be called if a connect() call succeeded
  // on this instance!
  /**
   * Connect. The configuration parameters are included.
   *
   * @param configParams
   *          are the configuration parameters for this connection. Note well: There are no exceptions allowed from this call, since it is
   *          expected to mainly establish connection parameters.
   */
  @Override
  public void connect(final ConfigParams configParams) {
    super.connect(configParams);
    solringesterEntryPoint = configParams.getParameter(SolrIngesterConfig.PARAM_SOLRADDRESS);
    solringesterLogin = configParams.getParameter(SolrIngesterConfig.PARAM_SOLRUSERNAME);
    solringesterPassword = "";
    try {
      solringesterPassword = ManifoldCF.deobfuscate(configParams.getParameter(SolrIngesterConfig.PARAM_SOLRPASSWORD));
    } catch (final ManifoldCFException ignore) {
    }
    connectionTimeoutString = configParams.getParameter(SolrIngesterConfig.PARAM_CONNECTIONTIMEOUT);
    socketTimeoutString = configParams.getParameter(SolrIngesterConfig.PARAM_SOCKETTIMEOUT);

    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("SolrIngester: Connection to '" + solringesterEntryPoint + "'");
    }

  }

  @Override
  public void disconnect() throws ManifoldCFException {
    socketTimeoutString = null;
    connectionTimeoutString = null;
    sessionTimeout = -1L;
    httpClient = null;
    httpSolrClient = null;
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
    connectionManager = null;
    solringesterLogin = null;
    solringesterPassword = null;
    solringesterEntryPoint = null;
    super.disconnect();
  }

  protected void expireSession() throws ManifoldCFException {
    httpClient = null;
    httpSolrClient = null;
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
    connectionManager = null;
    sessionTimeout = -1L;
  }

  @Override
  public void poll() throws ManifoldCFException {
    if (System.currentTimeMillis() >= sessionTimeout) {
      expireSession();
    }
    if (connectionManager != null) {
      connectionManager.closeIdleConnections(60000L, TimeUnit.MILLISECONDS);
    }
  }

  protected void getSession() throws ManifoldCFException {
    if (sessionTimeout == -1L) {

      try {
        this.connectionTimeout = Integer.parseInt(connectionTimeoutString);
      } catch (final NumberFormatException e) {
        throw new ManifoldCFException("Bad connection timeout number: " + connectionTimeoutString);
      }
      try {
        this.socketTimeout = Integer.parseInt(socketTimeoutString);
      } catch (final NumberFormatException e) {
        throw new ManifoldCFException("Bad socket timeout number: " + socketTimeoutString);
      }

      final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
      SSLConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
      try {
        final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
      } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
        throw new ManifoldCFException("SSL context initialization failure", e);
      }

      final PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(
          RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build());
      poolingConnectionManager.setDefaultMaxPerRoute(1);
      poolingConnectionManager.setValidateAfterInactivity(2000);
      poolingConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).setSoTimeout(socketTimeout).build());

      this.connectionManager = poolingConnectionManager;

      final RequestConfig.Builder requestBuilder = RequestConfig.custom().setCircularRedirectsAllowed(true).setSocketTimeout(socketTimeout).setExpectContinueEnabled(false)
          .setConnectTimeout(connectionTimeout).setConnectionRequestTimeout(socketTimeout);

      final List<Header> headers = new ArrayList<>();
      if (solringesterLogin != null && !solringesterLogin.isEmpty() && solringesterPassword != null) {
        final String auth = solringesterLogin + ":" + solringesterPassword;
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + new String(encodedAuth);
        final Header basicAuthheader = new BasicHeader(HttpHeaders.AUTHORIZATION, authHeader);
        headers.add(basicAuthheader);
      }

      this.builder = HttpClients.custom().setDefaultHeaders(headers).setConnectionManager(connectionManager).disableAutomaticRetries().setDefaultRequestConfig(requestBuilder.build());
      builder.setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new DefaultRedirectStrategy());
      this.httpClient = builder.build();
      this.httpSolrClient = new HttpSolrClient.Builder(solringesterEntryPoint).withHttpClient(httpClient).withConnectionTimeout(connectionTimeout).withSocketTimeout(socketTimeout).build();
    }
    sessionTimeout = System.currentTimeMillis() + sessionExpirationInterval;

  }

  @Override
  public String check() throws ManifoldCFException {
    getSession();

    final HttpGet httpGet = new HttpGet(solringesterEntryPoint);
    CloseableHttpResponse response = null;
    try {
      try {
        response = this.httpClient.execute(httpGet);
      } catch (final IOException e) {
        return "Connection error: " + e.getMessage();
      }
      final int responseCode = response.getStatusLine().getStatusCode();
      if (responseCode != 200) {
        return "Bad response: " + response.getStatusLine();
      }
      return super.check();
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (final IOException e) {
          return "Connection error: " + e.getMessage();
        }
      }
    }
  }

  @Override
  public String addSeedDocuments(final ISeedingActivity activities, final Specification spec, final String lastSeedVersion, final long seedTime, final int jobMode)
      throws ManifoldCFException, ServiceInterruption {

    long startTime;

    String idFieldName = null;
    String collection = null;
    String dateField = null;
    final String contentField = null;
    String rowsNumberString = null;
    String filter = null;

    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("SolrIngester: Connection to '" + solringesterEntryPoint + "'");
    }

    // Retrieve configuration parameters
    for (int l = 0; l < spec.getChildCount(); l++) {
      final SpecificationNode sn = spec.getChild(l);
      if (sn.getType() == SolrIngesterConfig.COLLECTION_NAME) {
        collection = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.ID_FIELD) {
        idFieldName = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }

      if (sn.getType() == SolrIngesterConfig.ROWS_NUMBER) {
        rowsNumberString = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.DATE_FIELD) {
        dateField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }

      if (sn.getType() == SolrIngesterConfig.FILTER_CONDITION) {
        filter = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
    }

    if (lastSeedVersion == null) {
      startTime = 0L;
    } else {
      // Unpack seed time from seed version string
      startTime = new Long(lastSeedVersion).longValue();
    }
    getSession();
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
    sdf.setTimeZone(TimeZone.getDefault());

    final StringBuilder url = new StringBuilder(solringesterEntryPoint);
    url.append("?").append(ACTION_PARAM_NAME).append("=").append(ACTION_SEED);
    if (startTime > 0) {
      url.append("&startTime=").append(sdf.format(new Date(startTime)));
    }
    url.append("&endTime=").append(sdf.format(new Date(seedTime)));

    // ExecuteSeedingThread t = new ExecuteSeedingThread(client, url.toString());

    long dateSolr;
    String filterDate = null;

    
    if (lastSeedVersion != null && !lastSeedVersion.isEmpty() && !lastSeedVersion.contentEquals("0")) {
      dateSolr = new Long(lastSeedVersion).longValue();
      String dateSolrString = sdf.format(dateSolr);
      filterDate = dateField+":["+dateSolrString+ " TO NOW]";
    }
    else {
      dateSolr = 0L;
    }

  

    final int rowsNumber = Integer.valueOf(rowsNumberString);
    try {
      SolrQuery query;
      if (filter == null || filter == "*:*") {
        query = new SolrQuery("*:*").setRows(rowsNumber).setSort(idFieldName, SolrQuery.ORDER.asc);
      } else {
        query = new SolrQuery("*:*").addFilterQuery(filter).setRows(rowsNumber).setSort(idFieldName, SolrQuery.ORDER.asc);
      }
      if (filterDate != null && !filterDate.isEmpty() && !filterDate.contentEquals("0")) {
        query.addFilterQuery(filterDate);
      }
      query.setFields(idFieldName);
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      boolean done = false;
      while (!done) {
        query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse response;
        response = httpSolrClient.query(collection, query);
        String nextCursorMark = cursorMark;
        try {
          nextCursorMark = response.getNextCursorMark();
        } catch (Exception e) {
          // Something wrong with the response, break
          Logging.connectors.warn("SolrIngester: the type of the reponse getnextcursormark is not recognized as a String");
          break;
        }
        final SolrDocumentList documents = response.getResults();
        
        for (final SolrDocument document : documents) {
          activities.addSeedDocument((String) document.getFieldValue(idFieldName));
        }
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
      }
    } catch (final SolrServerException | IOException e) {
      Logging.connectors.error("Unable to perform Solr requests", e);
      throw new ManifoldCFException("Unable to perform Solr requests", e);
    }

    return new Long(seedTime).toString();

  }

  @Override
  public void processDocuments(final String[] documentIdentifiers, final IExistingVersions statuses, final Specification spec, final IProcessActivity activities, final int jobMode,
      final boolean usesDefaultAuthority) throws ManifoldCFException, ServiceInterruption {
    
        getSession();

    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("SolrIngester: ProcessDocuments method");
    }
    // Get parameters configuration
    String collection = null;
    String idFieldName = null;
    boolean securityActivated = false;
    String dateField = null;
    String contentField = null;
    String securityField = null;
    String securityField2 = null;
    String rowsNumberString = null;

    String errorCode = null;
    String description = "";

    final long startFetchTime = System.currentTimeMillis();

    
    // Hashmap
    final Map<String, String> mapFields = new HashMap<String, String>();

    for (int l = 0; l < spec.getChildCount(); l++) {
      final SpecificationNode sn = spec.getChild(l);
      if (sn.getType() == SolrIngesterConfig.COLLECTION_NAME) {
        collection = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.SECURITY_ACTIVATED) {
        securityActivated = Boolean.parseBoolean(sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE));
      }
      if (sn.getType() == SolrIngesterConfig.ID_FIELD) {
        idFieldName = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.DATE_FIELD) {
        dateField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.CONTENT_FIELD) {
        contentField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.SECURITY_FIELD) {
        securityField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType() == SolrIngesterConfig.SECURITY_FIELD2) {
        securityField2 = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
      if (sn.getType().equals(SolrIngesterConfig.NODE_FIELDMAP)) {
        final String source = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_SOURCE);
        final String target = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_TARGET);
        mapFields.put(source, target);
      }
      if (sn.getType() == SolrIngesterConfig.ROWS_NUMBER) {
        rowsNumberString = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);
      }
    }

    final String versionString = null;

    final StringBuilder url = new StringBuilder(solringesterEntryPoint);

    url.append("?").append(ACTION_PARAM_NAME).append("=").append(ACTION_ITEMS);
    for (int i = 0; i < documentIdentifiers.length; i++) {
      url.append("&id[]=").append(URLEncoder.encode(documentIdentifiers[i]));
    }

    /*
     * Step 1 query that gets idFieldName and versionFieldName
     *
     */
    final String documentIdentifiersString = "\"" + String.join("\" OR \"", documentIdentifiers) + "\"";
    final int rowsNumber = Integer.valueOf(rowsNumberString);
    final HashMap<String, String> existingIds = new HashMap<String, String>();
    if (Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("SolrIngester: docidentifiers size '" + documentIdentifiers.length);

    }
    if (documentIdentifiers.length > 0) {
      try {
        SolrQuery query;
        query = new SolrQuery("*:*").setRows(rowsNumber).setSort(idFieldName, SolrQuery.ORDER.asc);
        query.setFields(idFieldName, versionField);
        query.addFilterQuery(idFieldName + ":(" + documentIdentifiersString + ")");

        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
          query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
          QueryResponse response;
          response = httpSolrClient.query(collection, query);

          String nextCursorMark = cursorMark;
          try {
            nextCursorMark = response.getNextCursorMark();
          } catch (Exception e) {
            // Something wrong with the response, break
            Logging.connectors.warn("SolrIngester: the type of the reponse getnextcursormark is not recognized as a String");
            break;
          }
          final SolrDocumentList documents = response.getResults();
          for (final SolrDocument document : documents) {
            existingIds.put((String) document.getFieldValue(idFieldName), String.valueOf(document.getFieldValue(versionField)));
          }
          if (cursorMark.equals(nextCursorMark)) {
            done = true;
          }
          cursorMark = nextCursorMark;
        }

        /*
         * End step 1
         */

        /*
         * Step 2 Compare the version stored in MCF and store id without version or with different version
         *
         */
        final Set<String> toProcess = new HashSet<String>();
        final Iterator it = existingIds.entrySet().iterator();

        while (it.hasNext()) {

          final Map.Entry pair = (Map.Entry) it.next();

          final String idStringBis = (String) pair.getKey();
          final String versionStringBis = (String) pair.getValue();

          if (versionStringBis.length() == 0 || activities.checkDocumentNeedsReindexing(idStringBis, versionStringBis)) {
            toProcess.add("\"" + (String) pair.getKey() + "\"");

          }
          // it.remove(); // avoids a ConcurrentModificationException
        }

        /*
         * Step 3 : Build query from toProcess list and index all the content
         */

        if (toProcess.size() > 0) {
          final List<String> listToProcess = new ArrayList<String>(toProcess);
          final String listToProcessString = String.join(" OR ", listToProcess);

          // Process the document
          final RepositoryDocument doc = new RepositoryDocument();

          query = new SolrQuery("*:*").setRows(rowsNumber).setSort(idFieldName, SolrQuery.ORDER.asc);
          query.setFields("*");
          query.addFilterQuery(idFieldName + ":(" + listToProcessString + ")");

          cursorMark = CursorMarkParams.CURSOR_MARK_START;
          done = false;
          while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse response;

            SolrDocumentList documents = null;
            response = httpSolrClient.query(collection, query);
            String nextCursorMark = cursorMark;
            try {
              nextCursorMark = response.getNextCursorMark();
            } catch (Exception e) {
              // Something wrong with the response, break
              Logging.connectors.warn("SolrIngester: the type of the reponse getnextcursormark is not recognized as a String");
              break;
            }
            documents = response.getResults();
            InputStream is = null;
            for (final SolrDocument document : documents) {
              LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_START|SOLR|{}", document.getFieldValue(idFieldName));

              for (final Map.Entry<String, String> entry : mapFields.entrySet()) {
                ArrayList<Object> listFieldValues = null;
                if (document.getFieldValues(entry.getKey()) != null) {
                  listFieldValues = (ArrayList<Object>) document.getFieldValues(entry.getKey());
                  if (listFieldValues != null) {

                    // TODO
                    // For now supports String, int, long, date fields 
                    if (listFieldValues.get(0) instanceof String) {
                      final String[] tablistFieldValues = listFieldValues.toArray(new String[0]);
                      doc.addField(entry.getValue(), tablistFieldValues);
                    }
                    else if (listFieldValues.get(0) instanceof Long) {
                      final Long[] tablistFieldValues = listFieldValues.toArray(new Long[0]);
                      String[] string_list = new String[tablistFieldValues.length];

                      for(int i = 0; i < tablistFieldValues.length; i++){
                        string_list[i] = String.valueOf(tablistFieldValues[i]);
                      }
                      doc.addField(entry.getValue(), string_list);
                    }
                    else if (listFieldValues.get(0) instanceof Integer) {
                      final Integer[] tablistFieldValues = listFieldValues.toArray(new Integer[0]);
                      String[] string_list = new String[tablistFieldValues.length];

                      for(int i = 0; i < tablistFieldValues.length; i++){
                        string_list[i] = String.valueOf(tablistFieldValues[i]);
                      }
                      doc.addField(entry.getValue(), string_list);
                    }
                    else if (listFieldValues.get(0) instanceof Date) {
                      final Date[] tablistFieldValues = listFieldValues.toArray(new Date[0]);
                      doc.addField(entry.getValue(), tablistFieldValues);
                    }
                    else {
                        Logging.connectors.warn("SolrIngester: the type of the field "+entry.getKey() + " is not recognized");
                    }

                  }
                }
              }

              // TODO
              // For now you can indicate the date field of the source. But the date field of the target is hardcoded and its value is last_modified
             if (document.get(dateField)!= null) {
               doc.addField(date_target_field, (Date) document.getFirstValue(dateField));
              }
              doc.setFileName((String) document.getFieldValue(idFieldName));

              if (Logging.connectors.isDebugEnabled()) {
                Logging.connectors.debug("doc '" + (String) document.getFieldValue(idFieldName));
              }

              // Content part
              ArrayList<Object> contentFieldValues = null;
              String contentFieldValuesString = "";

              if (document.getFieldValues(contentField) != null) {
                contentFieldValues = (ArrayList<Object>) document.getFieldValues(contentField);
                if (contentFieldValues != null) {
                  final String[] tabContentFieldValues = contentFieldValues.toArray(new String[0]);
                  for (final String s : tabContentFieldValues) {
                    contentFieldValuesString = contentFieldValuesString + " " + s;
                  }
                }
              } else if (document.getFieldValue(contentField) != null) {

                contentFieldValuesString = (String) document.getFieldValue(contentField);
              } else {
                contentFieldValuesString = "";
              }

              is = new ByteArrayInputStream(contentFieldValuesString.getBytes(StandardCharsets.UTF_8));

              // security part
              if (securityActivated == true) {
                
                if (Logging.connectors.isDebugEnabled()) {
                  Logging.connectors.debug("Security part");
                }

                if (document.getFieldValues(securityField) != null && document.getFieldValues(securityField2) != null) {
                  ArrayList<Object> securityFieldValues = null;
                  ArrayList<Object> securityFieldValues2 = null;

                  securityFieldValues = (ArrayList<Object>) document.getFieldValues(securityField);
                  securityFieldValues2 = (ArrayList<Object>) document.getFieldValues(securityField2);
                  if (Logging.connectors.isDebugEnabled()) {
                    Logging.connectors.debug("Security field 1 : " + securityFieldValues.toString());
                    Logging.connectors.debug("Security field 2 : " + securityFieldValues2.toString());
                  }

                  String[] securityValues = null;
                  final ArrayList<Object> all = new ArrayList<Object>();
                  all.addAll(securityFieldValues);
                  all.addAll(securityFieldValues2);

                  final HashSet hs = new HashSet();
                  hs.addAll(all);
                  all.clear();
                  all.addAll(hs);
                  securityValues = all.toArray(new String[0]);
                  doc.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, securityValues, new String[] { GLOBAL_DENY_TOKEN });
                  securityFieldValues = null;
                  securityFieldValues2 = null;
                } else if (document.getFieldValues(securityField) != null) {
                  ArrayList<Object> securityFieldValues = null;
                  securityFieldValues = (ArrayList<Object>) document.getFieldValues(securityField);
                  String[] tabsecurityFieldValues = securityFieldValues.toArray(new String[0]);
                  doc.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, tabsecurityFieldValues, new String[] { GLOBAL_DENY_TOKEN });
                  securityFieldValues = null;
                  tabsecurityFieldValues = null;
                } else if (document.getFieldValues(securityField2) != null) {
                  ArrayList<Object> securityFieldValues2 = null;
                  securityFieldValues2 = (ArrayList<Object>) document.getFieldValues(securityField2);
                  String[] tabsecurityFieldValues2 = securityFieldValues2.toArray(new String[0]);
                  doc.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, tabsecurityFieldValues2, new String[] { GLOBAL_DENY_TOKEN });
                  securityFieldValues2 = null;
                  tabsecurityFieldValues2 = null;
                }

              }

              // Can only index while background thread is running!
              try {
                doc.setBinary(is, is.available());
                // doc.setFileName(documentIdentifier);
                activities.ingestDocumentWithException(String.valueOf(document.getFieldValue(idFieldName)), String.valueOf(document.getFieldValue(versionField)),
                    String.valueOf(document.getFieldValue(idFieldName)), doc);
                activities.recordActivity(new Long(startFetchTime), ACTIVITY_GET, doc.getBinaryLength(), document.getFieldValue(idFieldName).toString(), "OK", "", null);
                is.close();
              } catch (final IOException e) {
                errorCode = "ERROR";
                description = "Unable to perform Solr requests";
                Logging.connectors.error(description, e);
              }

            }
            if (cursorMark.equals(nextCursorMark)) {
              done = true;
            }
            cursorMark = nextCursorMark;
          }

        }
      } catch (final SolrServerException | IOException e) {
        errorCode = "ERROR";
        description = "Unable to perform Solr requests";
        Logging.connectors.error(description, e);
      } catch (final SolrException solrException) {
        errorCode = String.valueOf(solrException.code());
        description = solrException.getMessage();
        Logging.connectors.error(description, solrException);
      }

      /*
       * Etape 4 Iterate into documentIdentifiers and check if each id is present into existingIds. If not, delete the doc      */

      for (int i = 0; i < documentIdentifiers.length; i++) {
        if (!existingIds.containsKey(documentIdentifiers[i])) {
          activities.deleteDocument(documentIdentifiers[i]);
          activities.recordActivity(new Long(startFetchTime), ACTIVITY_GET, 0L, documentIdentifiers[i], "NOTFOUND", "Document not found in Solr", null);
        } else if (errorCode != null) {
          Logging.connectors.warn("Prepare to skip document with id=" + documentIdentifiers[i] + ", due to error: " + errorCode + " - " + description);
          activities.deleteDocument(documentIdentifiers[i]);
          Logging.connectors.warn("Document with id=" + documentIdentifiers[i] + " skipped OK !");
          activities.recordActivity(new Long(startFetchTime), ACTIVITY_GET, 0L, documentIdentifiers[i], errorCode, description, null);
        }

      }
    }

   

  }

  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification os, final int connectionSequenceNumber) throws ManifoldCFException {
    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    String x;

    x = variableContext.getParameter(seqPrefix + "fieldmapping_count");
    if (x != null && x.length() > 0) {
      // About to gather the fieldmapping nodes, so get rid of the old ones.
      int i = 0;
      while (i < os.getChildCount()) {
        final SpecificationNode node = os.getChild(i);
        if (node.getType().equals(SolrIngesterConfig.NODE_FIELDMAP)) {
          os.removeChild(i);
        } else {
          i++;
        }
      }
      final int count = Integer.parseInt(x);
      i = 0;
      while (i < count) {
        final String prefix = seqPrefix + "fieldmapping_";
        final String suffix = "_" + Integer.toString(i);
        final String op = variableContext.getParameter(prefix + "op" + suffix);
        if (op == null || !op.equals("Delete")) {
          // Gather the fieldmap etc.
          final String source = variableContext.getParameter(prefix + "source" + suffix);
          String target = variableContext.getParameter(prefix + "target" + suffix);
          if (target == null) {
            target = "";
          }
          final SpecificationNode node = new SpecificationNode(SolrIngesterConfig.NODE_FIELDMAP);
          node.setAttribute(SolrIngesterConfig.ATTRIBUTE_SOURCE, source);
          node.setAttribute(SolrIngesterConfig.ATTRIBUTE_TARGET, target);
          os.addChild(os.getChildCount(), node);
        }
        i++;
      }

      final String addop = variableContext.getParameter(seqPrefix + "fieldmapping_op");
      if (addop != null && addop.equals("Add")) {
        final String source = variableContext.getParameter(seqPrefix + "fieldmapping_source");
        String target = variableContext.getParameter(seqPrefix + "fieldmapping_target");
        if (target == null) {
          target = "";
        }
        final SpecificationNode node = new SpecificationNode(SolrIngesterConfig.NODE_FIELDMAP);
        node.setAttribute(SolrIngesterConfig.ATTRIBUTE_SOURCE, source);
        node.setAttribute(SolrIngesterConfig.ATTRIBUTE_TARGET, target);
        os.addChild(os.getChildCount(), node);
      }

      final SpecificationNode node2 = new SpecificationNode(SolrIngesterConfig.SECURITY_ACTIVATED);
      final String securityActivated = variableContext.getParameter(seqPrefix + "securityactivated");

      if (securityActivated != null) {
        node2.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, securityActivated);
      } else {
        node2.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, "false");
      }
      os.addChild(os.getChildCount(), node2);

      final SpecificationNode node3 = new SpecificationNode(SolrIngesterConfig.SECURITY_FIELD);
      final String securityField = variableContext.getParameter(seqPrefix + "securityfield");

      node3.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, securityField);

      os.addChild(os.getChildCount(), node3);

      final SpecificationNode node4 = new SpecificationNode(SolrIngesterConfig.ID_FIELD);
      final String fieldId = variableContext.getParameter(seqPrefix + "fieldid");

      node4.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, fieldId);

      os.addChild(os.getChildCount(), node4);

      final SpecificationNode node5 = new SpecificationNode(SolrIngesterConfig.DATE_FIELD);
      final String fieldDate = variableContext.getParameter(seqPrefix + "fielddate");

      node5.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, fieldDate);

      os.addChild(os.getChildCount(), node5);

      final SpecificationNode node6 = new SpecificationNode(SolrIngesterConfig.CONTENT_FIELD);
      final String fieldContent = variableContext.getParameter(seqPrefix + "fieldcontent");

      node6.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, fieldContent);

      os.addChild(os.getChildCount(), node6);

      final SpecificationNode node7 = new SpecificationNode(SolrIngesterConfig.COLLECTION_NAME);
      final String collectionName = variableContext.getParameter(seqPrefix + "collection");

      node7.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, collectionName);

      os.addChild(os.getChildCount(), node7);

      final SpecificationNode node8 = new SpecificationNode(SolrIngesterConfig.FILTER_CONDITION);
      final String filter = variableContext.getParameter(seqPrefix + "filter");

      node8.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, filter);

      os.addChild(os.getChildCount(), node8);

      final SpecificationNode node9 = new SpecificationNode(SolrIngesterConfig.ROWS_NUMBER);
      final String rowsNumber = variableContext.getParameter(seqPrefix + "rowsnumber");

      node9.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, rowsNumber);

      os.addChild(os.getChildCount(), node9);

      final SpecificationNode node10 = new SpecificationNode(SolrIngesterConfig.SECURITY_FIELD2);
      final String securityField2 = variableContext.getParameter(seqPrefix + "securityfield2");

      node10.setAttribute(SolrIngesterConfig.ATTRIBUTE_VALUE, securityField2);

      os.addChild(os.getChildCount(), node10);
    }

    return null;
  }

  /**
   * Output the configuration header section. This method is called in the head section of the connector's configuration page. Its purpose is
   * to add the required tabs to the list, and to output any javascript methods that might be needed by the configuration editing HTML.
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabsArray
   *          is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "SolrIngester.SolrIngesterTabName"));
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_CONNECTOR_JS, null);
  }

  /**
   * Output the configuration body section. This method is called in the body section of the connector's configuration page. Its purpose is to
   * present the required form elements for editing. The coder can presume that the HTML that is output from this configuration will be within
   * appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags. The name of the form is "editconnection".
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName
   *          is the current tab name.
   */
  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName)
      throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TabName", tabName);
    fillInServerTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_CONNECTOR_HTML, velocityContext);
  }

  /**
   * Process a configuration post. This method is called at the start of the connector's configuration page, whenever there is a possibility
   * that form data for a connection has been posted. Its purpose is to gather form information and modify the configuration parameters
   * accordingly. The name of the posted form is "editconnection".
   *
   * @param threadContext
   *          is the local thread context.
   * @param variableContext
   *          is the set of variables available from the post, including binary file post information.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a
   *         redirection to an error page).
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {
    final String solrAddress = variableContext.getParameter("solrAddress");
    if (solrAddress != null) {
      parameters.setParameter(SolrIngesterConfig.PARAM_SOLRADDRESS, solrAddress);
    }

    final String solrUsername = variableContext.getParameter("solrUsername");
    if (solrUsername != null) {
      parameters.setParameter(SolrIngesterConfig.PARAM_SOLRUSERNAME, solrUsername);
    }

    final String solrPassword = variableContext.getParameter("solrPassword");
    if (solrPassword != null) {
      parameters.setParameter(SolrIngesterConfig.PARAM_SOLRPASSWORD, ManifoldCF.obfuscate(variableContext.mapKeyToPassword(solrPassword)));
    }

    final String connectionTimeout = variableContext.getParameter(SolrIngesterConfig.PARAM_CONNECTIONTIMEOUT);
    if (connectionTimeout != null) {
      parameters.setParameter(SolrIngesterConfig.PARAM_CONNECTIONTIMEOUT, connectionTimeout);
    }

    final String socketTimeout = variableContext.getParameter(SolrIngesterConfig.PARAM_SOCKETTIMEOUT);
    if (socketTimeout != null) {
      parameters.setParameter(SolrIngesterConfig.PARAM_SOCKETTIMEOUT, socketTimeout);
    }

    return null;
  }

  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the
   * connection information to the user. The coder can presume that the HTML that is output from this configuration will be within appropriate
   * &lt;html&gt; and &lt;body&gt; tags.
   *
   * @param threadContext
   *          is the local thread context.
   * @param out
   *          is the output to which any HTML should be sent.
   * @param parameters
   *          are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    fillInServerTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIGURATION_CONNECTOR_HTML, velocityContext);
  }

  protected static void fillInServerTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {

    String solrAddress = parameters.getParameter(SolrIngesterConfig.PARAM_SOLRADDRESS);

    if (solrAddress == null) {
      solrAddress = SolrIngesterConfig.SOLRADDRESS_DEFAULT;
    }

    String solrUsername = parameters.getParameter(SolrIngesterConfig.PARAM_SOLRUSERNAME);

    if (solrUsername == null) {
      solrUsername = SolrIngesterConfig.SOLRUSERNAME_DEFAULT;
    }

    String solrPassword = parameters.getParameter(SolrIngesterConfig.PARAM_SOLRPASSWORD);

    if (solrPassword == null) {
      solrPassword = SolrIngesterConfig.SOLRPASSWORD_DEFAULT;
    }

    String connectionTimeout = parameters.getParameter(SolrIngesterConfig.PARAM_CONNECTIONTIMEOUT);
    if (connectionTimeout == null) {
      connectionTimeout = SolrIngesterConfig.CONNECTIONTIMEOUT_DEFAULT;
    }

    String socketTimeout = parameters.getParameter(SolrIngesterConfig.PARAM_SOCKETTIMEOUT);
    if (socketTimeout == null) {
      socketTimeout = SolrIngesterConfig.SOCKETTIMEOUT_DEFAULT;
    }
    // Fill in context

    velocityContext.put("SOLRADDRESS", solrAddress);
    velocityContext.put("SOLRUSERNAME", solrUsername);
    velocityContext.put("SOLRPASSWORD", solrPassword);
    velocityContext.put("CONNECTIONTIMEOUT", connectionTimeout);
    velocityContext.put("SOCKETTIMEOUT", socketTimeout);

  }

  /**
   * Output the specification header section. This method is called in the head section of a job page which has selected a pipeline connection
   * of the current type. Its purpose is to add the required tabs to the list, and to output any javascript methods that might be needed by
   * the job editing HTML.
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the preferred local of the output.
   * @param os
   *          is the current pipeline specification for this connection.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @param tabsArray
   *          is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification os, final int connectionSequenceNumber, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    tabsArray.add(Messages.getString(locale, "SolrIngester.SecurityTabName"));
    tabsArray.add(Messages.getString(locale, "SolrIngester.ParametersTabName"));

    // Fill in the specification header map, using data from all tabs.

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JOB_PARAMETERS_JS, paramMap);
    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JOB_SECURITY_JS, paramMap);
  }

  /**
   * Output the specification body section. This method is called in the body section of a job page which has selected a pipeline connection
   * of the current type. Its purpose is to present the required form elements for editing. The coder can presume that the HTML that is output
   * from this configuration will be within appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags. The name of the form is "editjob".
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the preferred local of the output.
   * @param os
   *          is the current pipeline specification for this job.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @param actualSequenceNumber
   *          is the connection within the job that has currently been selected.
   * @param tabName
   *          is the current tab name.
   */
  @Override
  public void outputSpecificationBody(final IHTTPOutput out, final Locale locale, final Specification os, final int connectionSequenceNumber, final int actualSequenceNumber, final String tabName)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();

    // Set the tab name
    paramMap.put("TABNAME", tabName);
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));
    paramMap.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

    // Fill in the field mapping tab data
    fillInFieldMappingSpecificationMap(paramMap, os);
    // fillInSecuritySpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JOB_PARAMETERS_HTML, paramMap);
    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JOB_SECURITY_HTML, paramMap);
  }

  /**
   * View specification. This method is called in the body section of a job's view page. Its purpose is to present the pipeline specification
   * information to the user. The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt; and
   * &lt;body&gt; tags.
   *
   * @param out
   *          is the output to which any HTML should be sent.
   * @param locale
   *          is the preferred local of the output.
   * @param connectionSequenceNumber
   *          is the unique number of this connection within the job.
   * @param os
   *          is the current pipeline specification for this job.
   */

  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification os, final int connectionSequenceNumber) throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    // Fill in the map with data from all tabs
    fillInFieldMappingSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_JOB_PARAMETERS_HTML, paramMap);
    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_JOB_SECURITY_HTML, paramMap);

  }

  protected static void fillInFieldMappingSpecificationMap(final Map<String, Object> paramMap, final Specification os) {

    String securityActivated = "false";
    String securityField = "";
    String securityField2 = "";
    String idField = "id";
    String dateField = "last_modified";
    String contentField = "content";
    String filterCondition = "*:*";
    String collection = "techproducts";
    String rowsNumber = "50";

    final List<Map<String, String>> fieldMappings = new ArrayList<>();

    for (int i = 0; i < os.getChildCount(); i++) {
      final SpecificationNode sn = os.getChild(i);

      if (sn.getType().equals(SolrIngesterConfig.NODE_FIELDMAP)) {
        final String source = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_SOURCE);
        String target = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_TARGET);
        String targetDisplay;
        if (target == null) {
          target = "";
          targetDisplay = "(remove)";
        } else {
          targetDisplay = target;
        }
        final Map<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("SOURCE", source);
        fieldMapping.put("TARGET", target);
        fieldMapping.put("TARGETDISPLAY", targetDisplay);

        fieldMappings.add(fieldMapping);
      } else if (sn.getType().equals(SolrIngesterConfig.SECURITY_ACTIVATED)) {
        securityActivated = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.SECURITY_FIELD)) {
        securityField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.SECURITY_FIELD2)) {
        securityField2 = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.ID_FIELD)) {
        idField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.DATE_FIELD)) {
        dateField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.CONTENT_FIELD)) {
        contentField = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.FILTER_CONDITION)) {
        filterCondition = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.COLLECTION_NAME)) {
        collection = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      } else if (sn.getType().equals(SolrIngesterConfig.ROWS_NUMBER)) {
        rowsNumber = sn.getAttributeValue(SolrIngesterConfig.ATTRIBUTE_VALUE);

      }
    }
    // Prep for field mappings

    paramMap.put("FIELDMAPPINGS", fieldMappings);
    paramMap.put("SECURITYACTIVATION", securityActivated);
    paramMap.put("SECURITYFIELD", securityField);
    paramMap.put("SECURITYFIELD2", securityField2);
    paramMap.put("FIELDID", idField);
    paramMap.put("FIELDDATE", dateField);
    paramMap.put("FIELDCONTENT", contentField);
    paramMap.put("FILTERCONDITION", filterCondition);
    paramMap.put("COLLECTION", collection);
    paramMap.put("ROWSNUMBER", rowsNumber);

  }

  static class PreemptiveAuth implements HttpRequestInterceptor {

    private final Credentials credentials;

    public PreemptiveAuth(final Credentials creds) {
      this.credentials = creds;
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      request.addHeader(new BasicScheme(StandardCharsets.US_ASCII).authenticate(credentials, request, context));
    }
  }

  protected static class CheckThread extends Thread {

    protected HttpClient client;

    protected String url;

    protected Throwable exception = null;

    protected String result = "Unknown";

    public CheckThread(final HttpClient client, final String url) {
      super();
      setDaemon(true);
      this.client = client;
      this.url = url;
    }

    @Override
    public void run() {
      final HttpGet method = new HttpGet(url);
      try {
        final HttpResponse response = client.execute(method);
        try {
          if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            result = "Connection failed: " + response.getStatusLine().getReasonPhrase();
            return;
          }
          EntityUtils.consume(response.getEntity());
          result = "Connection OK";
        } finally {
          EntityUtils.consume(response.getEntity());
          method.releaseConnection();
        }
      } catch (final IOException ex) {
        exception = ex;
      }
    }

    public Throwable getException() {
      return exception;
    }

    public String getResult() {
      return result;
    }
  }

  protected static class ExecuteSeedingThread extends Thread {

    protected final HttpClient client;

    protected final String url;

    protected final XThreadStringBuffer seedBuffer;

    protected Throwable exception = null;

    public ExecuteSeedingThread(final HttpClient client, final String url) {
      super();
      setDaemon(true);
      this.client = client;
      this.url = url;
      seedBuffer = new XThreadStringBuffer();
    }

    public XThreadStringBuffer getBuffer() {
      return seedBuffer;
    }
  }
}
