/* $Id$ */

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.francelabs.datafari.transformation.spacy.fastapi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This connector works as a transformation connector,
 * It sends the document text data of the configured metadata fields to 
 * a spacy web service setup with fastapi to perform named entity recognition
 */
public class SpacyNER extends org.apache.manifoldcf.agents.transformation.BaseTransformationConnector {
  public static final String _rcsid = "@(#)$Id$";

  private static final String EDIT_CONFIGURATION_JS = "editConfiguration.js";
  private static final String EDIT_CONFIGURATION_SERVER_HTML = "editConfiguration_API.html";
  private static final String VIEW_CONFIGURATION_HTML = "viewConfiguration.html";
  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_TIKASERVER_HTML = "editSpecification.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

  protected static final String ACTIVITY_NER_EXTRACTION = "extractNER";

  protected static final String[] activitiesList = new String[] { ACTIVITY_NER_EXTRACTION };
  protected final static long sessionExpirationInterval = 300000L;

  /** We handle up to 64K in memory; after that we go to disk. */
  protected static final long inMemoryMaximumFile = 65536;

  // Raw parameters

  /** Spacy fastapi server address */
  private String serverAddress = null;

  /** Connection timeout */
  private String connectionTimeoutString = null;

  /** Socket timeout */
  private String socketTimeoutString = null;

  // Computed parameters

  /** Session timeout */
  private long sessionTimeout = -1L;

  /** Connection timeout */
  private int connectionTimeout = -1;

  /** Socket timeout */
  private int socketTimeout = -1;

  /** Connection manager */
  private HttpClientConnectionManager connectionManager = null;

  /** HttpClientBuilder */
  private HttpClientBuilder builder = null;

  /** Httpclient instance */
  private CloseableHttpClient httpClient = null;

  private static final String getModelsPath = "/models";
  private static final String processPath = "/process/";
  private static final String defaultPrefix = "entity_";

  private static final Logger LOGGER = LogManager.getLogger(SpacyNER.class.getName());

  /**
   * Connect.
   *
   * @param configParameters is the set of configuration parameters, which in this case describe the server and timeouts.
   */
  @Override
  public void connect(final ConfigParams configParameters) {
    super.connect(configParameters);
    serverAddress = configParameters.getParameter(SpacyNERConfig.PARAM_SERVERADDRESS);
    socketTimeoutString = configParameters.getParameter(SpacyNERConfig.SOCKET_TIMEOUT);
    connectionTimeoutString = configParameters.getParameter(SpacyNERConfig.CONNECTION_TIMEOUT);
  }

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    expireSession();

    super.disconnect();
  }

  /**
   * This method is periodically called for all connectors that are connected but not in active use.
   */
  @Override
  public void poll() throws ManifoldCFException {
    if (System.currentTimeMillis() >= sessionTimeout) {
      expireSession();
    }
    if (connectionManager != null) {
      connectionManager.closeIdleConnections(60000L, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * This method is called to assess whether to count this connector instance should actually be counted as being connected.
   *
   * @return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return sessionTimeout != -1L;
  }

  /** Set up a session */
  protected void getSession() throws ManifoldCFException {
    if (sessionTimeout == -1L) {
      if (serverAddress == null || serverAddress.length() == 0) {
        throw new ManifoldCFException("Missing server address");
      }
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

      this.builder = HttpClients.custom().setConnectionManager(connectionManager).disableAutomaticRetries().setDefaultRequestConfig(requestBuilder.build());
      builder.setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new DefaultRedirectStrategy());
      this.httpClient = builder.build();

    }
    sessionTimeout = System.currentTimeMillis() + sessionExpirationInterval;
  }

  /** Expire the current session */
  protected void expireSession() throws ManifoldCFException {
    httpClient = null;
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
    connectionManager = null;
    sessionTimeout = -1L;
  }

  /**
   * Test the connection. Returns a string describing the connection integrity.
   *
   * @return the connection's status as a displayable string.
   */
  @Override
  public String check() throws ManifoldCFException {
    getSession();
    final String getModelsURL = serverAddress + getModelsPath;

    // Check if we can access the list of available models, this ensures the server exists and respond
    try (CloseableHttpResponse response = performGetRequest(getModelsURL);) {
      if (response.getStatusLine().getStatusCode() == 200) {
        final String stResponse = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());
        // Close response to free resources
        response.close();
        final JSONParser parser = new JSONParser();
        final JSONArray jsonAr = (JSONArray) parser.parse(stResponse);
        if (jsonAr != null && jsonAr.size() > 0) {
          return super.check();
        } else {
          return "Error: Could not retrieve the list of available models or it is empty.";
        }
      } else {
        final int responseCode = response.getStatusLine().getStatusCode();
        final String message = response.getStatusLine().getReasonPhrase();
        return responseCode + " " + message;
      }
    } catch (final Exception e) {
      return e.getMessage();
    }
  }

  /**
   * Return a list of activities that this connector generates. The connector does NOT need to be connected before this method is called.
   *
   * @return the set of activities.
   */
  @Override
  public String[] getActivitiesList() {
    return activitiesList;
  }

  /**
   * Output the configuration header section. This method is called in the head section of the connector's configuration page. Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabsArray     is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "SpacyNER.SpacyNERTabName"));
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_JS, null);
  }

  /**
   * Output the configuration body section. This method is called in the body section of the connector's configuration page. Its purpose is to present the required form elements for editing. The coder
   * can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName       is the current tab name.
   */
  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName)
      throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TabName", tabName);
    fillInAPITab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_SERVER_HTML, velocityContext);
  }

  /**
   * Process a configuration post. This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been posted. Its
   * purpose is to gather form information and modify the configuration parameters accordingly. The name of the posted form is "editconnection".
   *
   * @param threadContext   is the local thread context.
   * @param variableContext is the set of variables available from the post, including binary file post information.
   * @param parameters      are the configuration parameters, as they currently exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {
    final String serverAddress = variableContext.getParameter("serverAddress");

    if (serverAddress != null) {
      parameters.setParameter(SpacyNERConfig.PARAM_SERVERADDRESS, serverAddress);
    }

    final String connectionTimeout = variableContext.getParameter("connectionTimeout");
    if (connectionTimeout != null) {
      parameters.setParameter(SpacyNERConfig.CONNECTION_TIMEOUT, connectionTimeout);
    }

    final String socketTimeout = variableContext.getParameter("socketTimeout");
    if (socketTimeout != null) {
      parameters.setParameter(SpacyNERConfig.SOCKET_TIMEOUT, socketTimeout);
    }

    return null;
  }

  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the connection information to the user. The coder can presume
   * that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    fillInAPITab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIGURATION_HTML, velocityContext);
  }

  protected static void fillInAPITab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {
    String serverAddress = parameters.getParameter(SpacyNERConfig.PARAM_SERVERADDRESS);
    if (serverAddress == null) {
      serverAddress = "";
    }

    String connectionTimeout = parameters.getParameter(SpacyNERConfig.CONNECTION_TIMEOUT);
    if (connectionTimeout == null) {
      connectionTimeout = SpacyNERConfig.CONNECTION_TIMEOUT_DEFAULT_VALUE;
    }

    String socketTimeout = parameters.getParameter(SpacyNERConfig.SOCKET_TIMEOUT);
    if (socketTimeout == null) {
      socketTimeout = SpacyNERConfig.SOCKET_TIMEOUT_DEFAULT_VALUE;
    }

    // Fill in context
    velocityContext.put("SERVERADDRESS", serverAddress);
    velocityContext.put("CONNECTIONTIMEOUT", connectionTimeout);
    velocityContext.put("SOCKETTIMEOUT", socketTimeout);
  }

  /**
   * Get an output version string, given an output specification. The output version string is used to uniquely describe the pertinent details of the output specification and the configuration, to
   * allow the Connector Framework to determine whether a document will need to be output again. Note that the contents of the document cannot be considered by this method, and that a different
   * version string (defined in IRepositoryConnector) is used to describe the version of the actual document.
   *
   * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be necessary.
   *
   * @param os is the current output specification for the job that is doing the crawling.
   * @return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal, the document will not need to be sent
   *         again to the output data store.
   */
  @Override
  public VersionContext getPipelineDescription(final Specification os) throws ManifoldCFException, ServiceInterruption {
    final SpecPacker sp = new SpecPacker(os);
    return new VersionContext(sp.toPackedString(), params, os);
  }

  // We intercept checks pertaining to the document format and send modified
  // checks further down

  /**
   * Detect if a mime type is acceptable or not. This method is used to determine whether it makes sense to fetch a document in the first place.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param mimeType            is the mime type of the document.
   * @param checkActivity       is an object including the activities that can be performed by this method.
   * @return true if the mime type can be accepted by this connector.
   */
  @Override
  public boolean checkMimeTypeIndexable(final VersionContext pipelineDescription, final String mimeType, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    // We should see what Tika will transform
    // MHL
    // Do a downstream check
    return checkActivity.checkMimeTypeIndexable("text/plain;charset=utf-8");
  }

  /**
   * Pre-determine whether a document (passed here as a File object) is acceptable or not. This method is used to determine whether a document needs to be actually transferred. This hook is provided
   * mainly to support search engines that only handle a small set of accepted file types.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param localFile           is the local file to check.
   * @param checkActivity       is an object including the activities that can be done by this method.
   * @return true if the file is acceptable, false if not.
   */
  @Override
  public boolean checkDocumentIndexable(final VersionContext pipelineDescription, final File localFile, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    return true;
  }

  /**
   * Pre-determine whether a document's length is acceptable. This method is used to determine whether to fetch a document in the first place.
   *
   * @param pipelineDescription is the document's pipeline version string, for this connection.
   * @param length              is the length of the document.
   * @param checkActivity       is an object including the activities that can be done by this method.
   * @return true if the file is acceptable, false if not.
   */
  @Override
  public boolean checkLengthIndexable(final VersionContext pipelineDescription, final long length, final IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption {
    // Always true
    return true;
  }

  /**
   * Add (or replace) a document in the output data store using the connector. This method presumes that the connector object has been configured, and it is thus able to communicate with the output
   * data store should that be necessary. The OutputSpecification is *not* provided to this method, because the goal is consistency, and if output is done it must be consistent with the output
   * description, since that was what was partly used to determine if output should be taking place. So it may be necessary for this method to decode an output description string in order to determine
   * what should be done.
   *
   * @param documentURI         is the URI of the document. The URI is presumed to be the unique identifier which the output data store will use to process and serve the document. This URI is
   *                            constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
   * @param outputDescription   is the description string that was constructed for this document by the getOutputDescription() method.
   * @param document            is the document data to be processed (handed to the output data store).
   * @param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in with the repository document. May be null.
   * @param activities          is the handle to an object that the implementer of a pipeline connector may use to perform operations, such as logging processing activity, or sending a modified
   *                            document to the next stage in the pipeline.
   * @return the document status (accepted or permanently rejected).
   * @throws IOException only if there's a stream error reading the document data.
   */
  @Override
  public int addOrReplaceDocumentWithException(final String documentURI, final VersionContext pipelineDescription, final RepositoryDocument document, final String authorityNameString,
      final IOutputAddActivity activities) throws ManifoldCFException, ServiceInterruption, IOException {

    long startTime = System.currentTimeMillis();
    // First of all init session
    getSession();

    final SpecPacker spec = new SpecPacker(pipelineDescription.getSpecification());

    String status = "OK";
    String description = "";

    // Load the document content into memory so that we can read it several times
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    document.getBinaryStream().transferTo(baos);
    // The input stream of the document has been totally read so set a new one
    final byte[] bA = baos.toByteArray();
    document.setBinary(new ByteArrayInputStream(bA), bA.length);

    try (final InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())); final BufferedReader br = new BufferedReader(isr);) {

      // Get the model to use
      final String modelToUse = spec.getModelToUse();

      // Get the endpoint to use
      final String endpointToUse = spec.getEndpointToUse().isEmpty() ? processPath : spec.getEndpointToUse();

      final String outputFieldPrefix = spec.getOutputFieldPrefix().isEmpty() ? defaultPrefix : spec.getOutputFieldPrefix();

      // StringBuilder that will handle the doc content to process
      StringBuilder contentToProcess = null;
      if (isr != null) {
        contentToProcess = new StringBuilder();
        String str = br.readLine();
        if (str != null) {
          contentToProcess.append(str);
          while ((str = br.readLine()) != null) {
            contentToProcess.append('\n');
            contentToProcess.append(str);
            LOGGER.debug("text line by line :"+str);
          }
        }
      }

      LOGGER.debug("final contentToProcess : "+contentToProcess.toString());
      String contentToProcessEncoded = URLEncoder.encode(contentToProcess.toString(),java.nio.charset.StandardCharsets.UTF_8.toString());
      contentToProcessEncoded = contentToProcessEncoded.replace("+", "%20");
      final JSONArray detectedEntities = sendNerExtractionRequest(contentToProcessEncoded, modelToUse, endpointToUse);
      if (detectedEntities == null) {
        // Either there was no text to process or the JSON response was weird (not the right keys, no entities)
        status = "NOTPROCESSED";
        description = "The document has no text or the JSON response was malformed or contained no entities";
      } else {
        // We want to add one field per entity type with all the identified entities text.
        HashMap<String, HashSet<String>> entitiesByTypes = new HashMap<>();
        for (int i = 0; i<detectedEntities.size(); i++) {
          JSONObject currentEntity = (JSONObject) (detectedEntities.get(i));
          String type = (String) (currentEntity.get("label"));
          if (entitiesByTypes.get(type) == null) {
            entitiesByTypes.put(type, new HashSet<String>());
          }
          entitiesByTypes.get(type).add((String) (currentEntity.get("text")));
        }

        for (Map.Entry<String, HashSet<String>> entry : entitiesByTypes.entrySet()) {
          document.addField(outputFieldPrefix + entry.getKey(), entry.getValue().toArray(new String[1]));

        }
      }

    } catch (final IOException e) {
      status = "ERROR";
      description = "An error happened during the entity extraction process: " + e.getMessage();
      LOGGER.error(description, e);
    }

    activities.recordActivity(Long.valueOf(startTime), ACTIVITY_NER_EXTRACTION, document.getBinaryLength(), documentURI, status, description);
    return activities.sendDocument(documentURI, document);
  }

  private CloseableHttpResponse performPostRequest(final String request, final String body) throws IOException {
    try {
      final HttpPost postRequest = new HttpPost(request);
      final HttpEntity entity = EntityBuilder.create().setText(body).setContentType(ContentType.APPLICATION_JSON).build();
      postRequest.setEntity(entity);
      final CloseableHttpResponse response = httpClient.execute(postRequest);
      return response;
    } catch (final IOException e) {
      LOGGER.error("Unable to perform POST request: " + request, e);
      throw e;
    }
  }

  private CloseableHttpResponse performGetRequest(final String request) throws IOException {
    try {
      final HttpGet getRequest = new HttpGet(request);
      final CloseableHttpResponse response = httpClient.execute(getRequest);
      return response;
    } catch (final IOException e) {
      LOGGER.error("Unable to perform GET request: " + request, e);
      throw e;
    }

  }

  private JSONArray sendNerExtractionRequest(final String textToProcess, final String modelToUse, final String endpointToUse)
      throws IOException {

    JSONArray extractedEntities = null;

    if (textToProcess.length() > 0) {
      final String processURL = serverAddress + endpointToUse;

      // Initialize HTTP client and request body
      String body = "{\"document\" : {\"text\": \"" + textToProcess + "\"}";
      if (modelToUse == null || modelToUse.isEmpty()) {
        body += "}";
      } else {
        body += ", \"model\":\"" + modelToUse + "\"}";
      }

      // Send the request to extract NER
      try (CloseableHttpResponse response = performPostRequest(processURL, body);) {
        if (response.getStatusLine().getStatusCode() == 200) {
          String stResponse = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());
          // Close response to free resources
          response.close();
          final JSONParser parser = new JSONParser();
          JSONObject jsonOb = (JSONObject) parser.parse(stResponse);
          if (jsonOb.get("result") != null) {
            final JSONObject resultObj = (JSONObject)(jsonOb.get("result"));
            if (resultObj.get("ents") != null){
              extractedEntities = (JSONArray) (resultObj.get("ents"));
            }
          }
        } else {
          LOGGER.debug("raw response :"+IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name()));
          final int responseCode = response.getStatusLine().getStatusCode();
          final String message = response.getStatusLine().getReasonPhrase();
          throw new IOException("Bad response for Spacy fastapi request: " + responseCode + " - " + message);
        }
      } catch (final ParseException e) {
        throw new IOException("Unable to parse Spacy fastapi request", e);
      }
    }
    return extractedEntities;
  }

  /**
   * Obtain the name of the form check javascript method to call.
   *
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return the name of the form check javascript method.
   */
  @Override
  public String getFormCheckJavascriptMethodName(final int connectionSequenceNumber) {
    return "s" + connectionSequenceNumber + "_checkSpecification";
  }

  /**
   * Obtain the name of the form presave check javascript method to call.
   *
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return the name of the form presave check javascript method.
   */
  @Override
  public String getFormPresaveCheckJavascriptMethodName(final int connectionSequenceNumber) {
    return "s" + connectionSequenceNumber + "_checkSpecificationForSave";
  }

  /**
   * Output the specification header section. This method is called in the head section of a job page which has selected a pipeline connection of the current type. Its purpose is to add the required
   * tabs to the list, and to output any javascript methods that might be needed by the job editing HTML.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param os                       is the current pipeline specification for this connection.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param tabsArray                is an array of tab names. Add to this array any tab names that are specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification os, final int connectionSequenceNumber, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    tabsArray.add(Messages.getString(locale, "SpacyNER.SpacyNERTabName"));

    // Fill in the specification header map, using data from all tabs.
    fillInSpacyNERSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JS, paramMap);
  }

  /**
   * Output the specification body section. This method is called in the body section of a job page which has selected a pipeline connection of the current type. Its purpose is to present the required
   * form elements for editing. The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the form is
   * "editjob".
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param os                       is the current pipeline specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param actualSequenceNumber     is the connection within the job that has currently been selected.
   * @param tabName                  is the current tab name.
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
    fillInSpacyNERSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_TIKASERVER_HTML, paramMap);
  }

  /**
   * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been posted. Its purpose is to
   * gather form information and modify the transformation specification accordingly. The name of the posted form is "editjob".
   *
   * @param variableContext          contains the post data, including binary file-upload information.
   * @param locale                   is the preferred local of the output.
   * @param os                       is the current pipeline specification for this job.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification os, final int connectionSequenceNumber) throws ManifoldCFException {
    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    // Gather model to use name
    final SpecificationNode node = new SpecificationNode(SpacyNERConfig.NODE_MODELTOUSE);
    final String modelToUse = variableContext.getParameter(seqPrefix + "modelToUse");
    if (modelToUse != null) {
      node.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, modelToUse);
    } else {
      node.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, "");
    }
    os.addChild(os.getChildCount(), node);

    // Gather endpoint to use name
    final SpecificationNode endpointNode = new SpecificationNode(SpacyNERConfig.NODE_ENDPOINTTOUSE);
    final String endpointToUse = variableContext.getParameter(seqPrefix + "endpointToUse");
    if (endpointToUse != null) {
      endpointNode.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, endpointToUse);
    } else {
      endpointNode.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, "");
    }
    os.addChild(os.getChildCount(), endpointNode);

    // Gather output field name
    final SpecificationNode outputFieldPrefixNode = new SpecificationNode(SpacyNERConfig.NODE_OUTPUTFIELDPREFIX);
    final String outputFieldPrefix = variableContext.getParameter(seqPrefix + "outputFieldPrefix");
    if (outputFieldPrefix != null) {
      outputFieldPrefixNode.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, outputFieldPrefix);
    } else {
      outputFieldPrefixNode.setAttribute(SpacyNERConfig.ATTRIBUTE_VALUE, "");
    }
    os.addChild(os.getChildCount(), outputFieldPrefixNode);
    return null;
  }

  /**
   * View specification. This method is called in the body section of a job's view page. Its purpose is to present the pipeline specification information to the user. The coder can presume that the
   * HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *
   * @param out                      is the output to which any HTML should be sent.
   * @param locale                   is the preferred local of the output.
   * @param connectionSequenceNumber is the unique number of this connection within the job.
   * @param os                       is the current pipeline specification for this job.
   */
  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification os, final int connectionSequenceNumber) throws ManifoldCFException, IOException {
    final Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

    // Fill in the map with data from all tabs
    fillInSpacyNERSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

  }

  protected static void fillInSpacyNERSpecificationMap(final Map<String, Object> paramMap, final Specification os) {
    // Prep for field mappings
    String modelToUse = "";
    String endpointToUse = "";
    String outputFieldPrefix = "";
    for (int i = 0; i < os.getChildCount(); i++) {
      final SpecificationNode sn = os.getChild(i);
      if (sn.getType().equals(SpacyNERConfig.NODE_MODELTOUSE)) {
        modelToUse = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(SpacyNERConfig.NODE_ENDPOINTTOUSE)) {
        endpointToUse = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
      } else if (sn.getType().equals(SpacyNERConfig.NODE_OUTPUTFIELDPREFIX)) {
        outputFieldPrefix = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
      }
    }
    paramMap.put("MODELTOUSE", modelToUse);
    paramMap.put("ENDPOINTTOUSE", endpointToUse);
    paramMap.put("OUTPUTFIELDPREFIX", outputFieldPrefix);
  }

  protected static int handleIOException(final IOException e) throws ManifoldCFException {
    // IOException reading from our local storage...
    if (e instanceof InterruptedIOException) {
      throw new ManifoldCFException(e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    }
    throw new ManifoldCFException(e.getMessage(), e);
  }

  protected static interface DestinationStorage {
    /**
     * Get the output stream to write to. Caller should explicitly close this stream when done writing.
     */
    public OutputStream getOutputStream() throws ManifoldCFException;

    /**
     * Get new binary length.
     */
    public long getBinaryLength() throws ManifoldCFException;

    /**
     * Get the input stream to read from. Caller should explicitly close this stream when done reading.
     */
    public InputStream getInputStream() throws ManifoldCFException;

    /**
     * Close the object and clean up everything. This should be called when the data is no longer needed.
     */
    public void close() throws ManifoldCFException;
  }

  protected static class FileDestinationStorage implements DestinationStorage {
    protected final File outputFile;
    protected final OutputStream outputStream;

    public FileDestinationStorage() throws ManifoldCFException {
      File outputFile;
      OutputStream outputStream;
      try {
        outputFile = File.createTempFile("mcfspacy", "tmp");
        outputStream = new FileOutputStream(outputFile);
      } catch (final IOException e) {
        handleIOException(e);
        outputFile = null;
        outputStream = null;
      }
      this.outputFile = outputFile;
      this.outputStream = outputStream;
    }

    @Override
    public OutputStream getOutputStream() throws ManifoldCFException {
      return outputStream;
    }

    /**
     * Get new binary length.
     */
    @Override
    public long getBinaryLength() throws ManifoldCFException {
      return outputFile.length();
    }

    /**
     * Get the input stream to read from. Caller should explicitly close this stream when done reading.
     */
    @Override
    public InputStream getInputStream() throws ManifoldCFException {
      try {
        return new FileInputStream(outputFile);
      } catch (final IOException e) {
        handleIOException(e);
        return null;
      }
    }

    /**
     * Close the object and clean up everything. This should be called when the data is no longer needed.
     */
    @Override
    public void close() throws ManifoldCFException {
      outputFile.delete();
    }

  }

  protected static class MemoryDestinationStorage implements DestinationStorage {
    protected final ByteArrayOutputStream outputStream;

    public MemoryDestinationStorage(final int sizeHint) {
      outputStream = new ByteArrayOutputStream(sizeHint);
    }

    @Override
    public OutputStream getOutputStream() throws ManifoldCFException {
      return outputStream;
    }

    /**
     * Get new binary length.
     */
    @Override
    public long getBinaryLength() throws ManifoldCFException {
      return outputStream.size();
    }

    /**
     * Get the input stream to read from. Caller should explicitly close this stream when done reading.
     */
    @Override
    public InputStream getInputStream() throws ManifoldCFException {
      return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Close the object and clean up everything. This should be called when the data is no longer needed.
     */
    @Override
    public void close() throws ManifoldCFException {
    }

  }

  protected static class SpecPacker {

    private final String modelToUse;
    private final String endpointToUse;
    private final String outputFieldPrefix;

    public SpecPacker(final Specification os) {
      String modelToUse = "";
      String outputFieldPrefix = "";
      String endpointToUse = "";
      for (int i = 0; i < os.getChildCount(); i++) {
        final SpecificationNode sn = os.getChild(i);

        if (sn.getType().equals(SpacyNERConfig.NODE_MODELTOUSE)) {
          final String value = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
          modelToUse = value;
        } else if (sn.getType().equals(SpacyNERConfig.NODE_ENDPOINTTOUSE)) {
          final String value = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
          endpointToUse = value;
        } else if (sn.getType().equals(SpacyNERConfig.NODE_OUTPUTFIELDPREFIX)) {
          final String value = sn.getAttributeValue(SpacyNERConfig.ATTRIBUTE_VALUE);
          outputFieldPrefix = value;
        }
      }
      this.modelToUse = modelToUse;
      this.outputFieldPrefix = outputFieldPrefix;
      this.endpointToUse = endpointToUse;
    }

    public String toPackedString() {
      final StringBuilder sb = new StringBuilder();

      // Keep all metadata
      if (!modelToUse.isEmpty()) {
        sb.append(modelToUse);
      }

      if (!endpointToUse.isEmpty()) {
        sb.append(endpointToUse);
      }

      if (!outputFieldPrefix.isEmpty()) {
        if (sb.length() > 0) {
          sb.append("+");
        }
        sb.append(outputFieldPrefix);
      }

      return sb.toString();
    }

    public String getModelToUse() {
      return modelToUse;
    }

    public String getEndpointToUse() {
      return endpointToUse;
    }

    public String getOutputFieldPrefix() {
      return outputFieldPrefix;
    }

  }

}
