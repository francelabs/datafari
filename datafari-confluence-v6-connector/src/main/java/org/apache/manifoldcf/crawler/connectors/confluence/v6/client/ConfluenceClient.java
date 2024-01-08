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

package org.apache.manifoldcf.crawler.connectors.confluence.v6.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.connectorcommon.common.InterruptibleSocketFactory;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.exception.ConfluenceException;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Attachment;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceResource;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceResponse;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceRestrictionsResponse;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceUser;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Group;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Label;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.MutableAttachment;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.MutablePage;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Page;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Restrictions;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Space;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.User;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.builder.ConfluenceResourceBuilder;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * <p>
 * ConfluenceClient class
 * </p>
 * <p>
 * This class is intended to be used to interact with Confluence REST API
 * </p>
 * <p>
 * There are some methods that make use of the Confluence JSON-RPC 2.0 API, but until all the methods are ported to the new REST API, we will have to use them to leverage all the features provided by
 * Confluence
 * </p>
 *
 * @author Julien Massiera &amp; Antonio David Perez Morales;
 *
 */
public class ConfluenceClient {

  private static final String VIEW_PERMISSION = "view";

  private static final String CONTENT_PATH = "/rest/api/content";
  private static final String AUTHORITY_PATH = "/rpc/json-rpc/confluenceservice-v2/";
  private static final String SPACES_PATH = "/rest/api/space";
  private static final String CHILD_PAGE_PATH = "/child/page";
  private static final String USER_PATH = "/rest/api/user";
  private static final String USER_GROUPS_PATH = "/rest/api/user/memberof";
  private static final String READ_RESTRICTIONS_PATH = "/restriction/byOperation/read";
  private static final String EXPANDABLE_PARAMETERS = "expand=body.view,metadata.labels,space,history,version";
  private static final String CHILD_ATTACHMENTS_PATH = "/child/attachment/";
  private static final String LABEL_PATH = "/label";

  private final Logger logger = LoggerFactory.getLogger(ConfluenceClient.class);

  private final String protocol;
  private final Integer port;
  private final String host;
  private final String path;
  private final String username;
  private final String password;

  protected String proxyUsername = null;
  protected String proxyPassword = null;
  protected String proxyProtocol = null;
  protected String proxyHost = null;
  protected int proxyPort = -1;

  private int socketTimeout = 900000;
  private int connectionTimeout = 60000;

  private CloseableHttpClient httpClient;
  private HttpClientContext httpContext;

  /**
   * <p>
   * Creates a new client instance using the given parameters
   * </p>
   *
   * @param protocol the protocol
   * @param host     the host
   * @param port     the port
   * @param path     the path to Confluence instance
   * @param username the username used to make the requests. Null or empty to use anonymous user
   * @param password the password
   * @throws ManifoldCFException
   */
  public ConfluenceClient(final String protocol, final String host, final Integer port, final String path, final String username, final String password, final int socketTimeout,
      final int connectionTimeout, final String proxyUsername, final String proxyPassword, final String proxyProtocol, final String proxyHost, final int proxyPort) throws ManifoldCFException {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.path = path;
    this.username = username;
    this.password = password;
    this.socketTimeout = socketTimeout;
    this.connectionTimeout = connectionTimeout;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
    this.proxyProtocol = proxyProtocol;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;

    connect();
  }

  /**
   * <p>
   * Connect methods used to initialize the underlying client
   * </p>
   *
   * @throws ManifoldCFException
   */
  private void connect() throws ManifoldCFException {
    HttpHost proxy = null;
    CredentialsProvider credentialsProvider = null;
    if (this.proxyHost != null && this.proxyHost.length() > 0 && this.proxyPort != -1) {
      proxy = new HttpHost(this.proxyHost, this.proxyPort);
      if (this.proxyUsername != null && this.proxyUsername.length() > 0 && this.proxyPassword != null && this.proxyPassword.length() > 0) {
          credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(
                  new AuthScope(this.proxyHost, this.proxyPort),
                  new UsernamePasswordCredentials(this.proxyUsername, this.proxyPassword)
          );
      }
    }
    final javax.net.ssl.SSLSocketFactory httpsSocketFactory = KeystoreManagerFactory.getTrustingSecureSocketFactory();
    final SSLConnectionSocketFactory myFactory = new SSLConnectionSocketFactory(new InterruptibleSocketFactory(httpsSocketFactory, connectionTimeout), NoopHostnameVerifier.INSTANCE);

    final PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(
        RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", myFactory).build());
    poolingConnectionManager.setDefaultMaxPerRoute(1);
    poolingConnectionManager.setValidateAfterInactivity(2000);
    poolingConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).setSoTimeout(socketTimeout).build());

    final RequestConfig.Builder requestBuilder = RequestConfig.custom().setCircularRedirectsAllowed(true).setSocketTimeout(socketTimeout).setExpectContinueEnabled(true)
        .setConnectTimeout(connectionTimeout).setConnectionRequestTimeout(socketTimeout);

    if (proxy != null) {
        requestBuilder.setProxy(proxy);
    }

    HttpClientBuilder clientBuilder = HttpClients.custom();

    if (credentialsProvider != null) {
        clientBuilder = clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    }

    httpClient = clientBuilder.setConnectionManager(poolingConnectionManager).disableAutomaticRetries().setDefaultRequestConfig(requestBuilder.build())
        .setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new LaxRedirectStrategy()).build();

  }

  /**
   * <p>
   * Close the client. No further requests can be done
   * </p>
   */
  public void close() {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (final IOException e) {
        logger.debug("Error closing http connection. Reason: {}", e.getMessage());
        e.printStackTrace();
      }
    }
  }

  /**
   * <p>
   * Check method used to test if Confluence instance is up and running
   * </p>
   *
   * @return a {@code Boolean} indicating whether the Confluence instance is alive or not
   *
   * @throws Exception
   */
  public boolean check() throws Exception {
    HttpResponse response;
    try {
      if (httpClient == null) {
        connect();
      }

      final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s?limit=1", protocol, host, port, path, CONTENT_PATH);
      logger.debug("[Processing] Hitting url: {} for confluence status check fetching : ", "Confluence URL", sanitizeUrl(url));
      final HttpGet httpGet = createGetRequest(url);
      response = httpClient.execute(httpGet);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new Exception("[Checking connection] Confluence server appears to be down");
      } else {
        return true;
      }
    } catch (final IOException e) {
      logger.warn("[Checking connection] Confluence server appears to be down", e);
      throw new Exception("Confluence appears to be down", e);
    }
  }

  /**
   * <p>
   * Check method used to test if Confluence instance is up and running when using Authority connector (JSON-RPC API)
   * </p>
   * <p>
   * This method will be deleted when all JSON-RPC methods are available through the REST API
   *
   * @return a {@code Boolean} indicating whether the Confluence instance is alive or not
   *
   * @throws Exception
   */
  public boolean checkAuth() throws Exception {
    try {
      if (httpClient == null) {
        connect();
      }
      getSpaces();
      return true;
    } catch (final Exception e) {
      logger.warn("[Checking connection] Confluence server appears to be down", e);
      throw e;
    }
  }

  /**
   * <p>
   * Create a get request for the given url
   * </p>
   *
   * @param url the url
   * @return the created {@code HttpGet} instance
   */
  private HttpGet createGetRequest(final String url) {
    final String finalUrl = useBasicAuthentication() ? url + "&os_authType=basic" : url;
    final String sanitizedUrl = sanitizeUrl(finalUrl);
    final HttpGet httpGet = new HttpGet(sanitizedUrl);
    httpGet.addHeader("Accept", "application/json");
    if (useBasicAuthentication()) {
      httpGet.addHeader("Authorization", "Basic " + Base64.encodeBase64String(String.format(Locale.ROOT, "%s:%s", this.username, this.password).getBytes(Charset.forName("UTF-8"))));
    }
    return httpGet;
  }

  /**
   *
   * @param start
   * @param limit
   * @param space
   * @param pageType
   * @return
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public ConfluenceResponse<Page> getSpaceRootPages(final int start, final int limit, final String space, final Optional<String> pageType) throws Exception {
    String contentType = "page";
    if (pageType.isPresent()) {
      contentType = pageType.get();
    }
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s/content/%s?limit=%s&start=%s&depth=root", protocol, host, port, path, SPACES_PATH, space, contentType, limit, start);
    return (ConfluenceResponse<Page>) getConfluenceResources(url, Page.builder());
  }

  @SuppressWarnings("unchecked")
  public ConfluenceResponse<Page> getPageChilds(final int start, final int limit, final String pageId) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s%s?limit=%s&start=%s", protocol, host, port, path, CONTENT_PATH, pageId, CHILD_PAGE_PATH, limit, start);
    return (ConfluenceResponse<Page>) getConfluenceResources(url, Page.builder());
  }

  /**
   * <p>
   * Get a list of Confluence pages using pagination
   * </p>
   *
   * @param start The start value to get pages from
   * @param limit The number of pages to get from start
   * @return a {@code ConfluenceResponse} containing the result pages and some pagination values
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public ConfluenceResponse<Page> getPages(final int start, final int limit, final Optional<String> space, final Optional<String> pageType) throws Exception {
    String url = String.format(Locale.ROOT, "%s://%s:%s%s%s?limit=%s&start=%s", protocol, host, port, path, CONTENT_PATH, limit, start);
    if (space.isPresent()) {
      url = String.format(Locale.ROOT, "%s&spaceKey=%s", url, space.get());
    }
    if (pageType.isPresent()) {
      url = String.format(Locale.ROOT, "%s&type=%s", url, pageType.get());
    }
    return (ConfluenceResponse<Page>) getConfluenceResources(url, Page.builder());
  }

  /**
   * <p>
   * Get the {@code ConfluenceResources} from the given url
   * </p>
   *
   * @param url     The url identifying the REST resource to get the documents
   * @param builder The builder used to build the resources contained in the response
   * @return a {@code ConfluenceResponse} containing the page results
   * @throws Exception
   */
  private ConfluenceResponse<? extends ConfluenceResource> getConfluenceResources(final String url, final ConfluenceResourceBuilder<? extends ConfluenceResource> builder) throws Exception {
    logger.debug("[Processing] Hitting url for get confluence resources: {}", sanitizeUrl(url));

    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        if (response.getStatusLine().getStatusCode() != 404) {
          throw new Exception("Confluence error. " + errorDesc);
        } else {
          logger.error("[Processing] Failed to get page {}. Error: {}", url, errorDesc);
          return null;
        }
      }
      final ConfluenceResponse<? extends ConfluenceResource> confluenceResponse = responseFromHttpEntity(response.getEntity(), builder);
      EntityUtils.consume(response.getEntity());
      return confluenceResponse;
    } catch (final IOException e) {
      logger.error("[Processing] Failed to get page(s)", e);
      throw new Exception("Confluence appears to be down", e);
    }
  }

  /**
   * <p>
   * Get the {@code ConfluenceResources} from the given url
   * </p>
   *
   * @param url     The url identifying the REST resource to get the documents
   * @param builder The builder used to build the resources contained in the response
   * @return a {@code ConfluenceRestrictionsResponse} containing the page results
   * @throws Exception
   */
  private ConfluenceRestrictionsResponse<? extends ConfluenceResource> getConfluenceRestrictionsResources(final String url, final ConfluenceResourceBuilder<? extends ConfluenceResource> builder)
      throws Exception {
    logger.debug("[Processing] Hitting url for get confluence resources: {}", sanitizeUrl(url));

    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        if (response.getStatusLine().getStatusCode() != 404) {
          throw new Exception("Confluence error. " + errorDesc);
        } else {
          logger.error("[Processing] Failed to get page {}. Error: {}", url, errorDesc);
          return null;
        }
      }
      final ConfluenceRestrictionsResponse<? extends ConfluenceResource> confluenceResponse = restrictionsResponseFromHttpEntity(response.getEntity(), builder);
      EntityUtils.consume(response.getEntity());
      return confluenceResponse;
    } catch (final IOException e) {
      logger.error("[Processing] Failed to get page(s)", e);
      throw new Exception("Confluence appears to be down", e);
    }
  }

  /**
   * <p>
   * Creates a ConfluenceResponse from the entity returned in the HttpResponse
   * </p>
   *
   * @param entity the {@code HttpEntity} to extract the response from
   * @return a {@code ConfluenceResponse} with the requested information
   * @throws Exception
   */
  private <T extends ConfluenceResource> ConfluenceResponse<T> responseFromHttpEntity(final HttpEntity entity, final ConfluenceResourceBuilder<T> builder) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");

    final JSONParser parser = new JSONParser();
    final JSONObject responseObject = (JSONObject) parser.parse(new StringReader(stringEntity));
    final ConfluenceResponse<T> response = ConfluenceResponse.fromJson(responseObject, builder);
    if (response.getResults().size() == 0) {
      logger.debug("[Processing] No {} found in the Confluence response", builder.getType().getSimpleName());
    }

    return response;
  }

  /**
   * <p>
   * Creates a ConfluenceResponse from the entity returned in the HttpResponse
   * </p>
   *
   * @param entity the {@code HttpEntity} to extract the response from
   * @return a {@code ConfluenceResponse} with the requested information
   * @throws Exception
   */
  private <T extends ConfluenceResource> ConfluenceRestrictionsResponse<T> restrictionsResponseFromHttpEntity(final HttpEntity entity, final ConfluenceResourceBuilder<T> builder) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");

    final JSONParser parser = new JSONParser();
    final JSONObject responseObject = (JSONObject) parser.parse(new StringReader(stringEntity));
    final ConfluenceRestrictionsResponse<T> response = ConfluenceRestrictionsResponse.fromJson(responseObject, builder);
    if (response.getResult() == null) {
      logger.debug("[Processing] No {} found in the Confluence Restrictions response", builder.getType().getSimpleName());
    }

    return response;
  }

  /**
   * <p>
   * Get the attachments of the given page
   * </p>
   *
   * @param pageId the page id
   * @return a {@code ConfluenceResponse} instance containing the attachment results and some pagination values
   * @throws Exception
   */
  public ConfluenceResponse<Attachment> getPageAttachments(final String pageId) throws Exception {
    return getPageAttachments(pageId, 0, 50);
  }

  /**
   * <p>
   * Get the attachments of the given page using pagination
   * </p>
   *
   * @param pageId the page id
   * @param start  The start value to get attachments from
   * @param limit  The number of attachments to get from start
   * @return a {@code ConfluenceResponse} instance containing the attachment results and some pagination values
   * @throws Exception
   */
  public ConfluenceResponse<Attachment> getPageAttachments(final String pageId, final int start, final int limit) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s%s?limit=%s&start=%s", protocol, host, port, path, CONTENT_PATH, pageId, CHILD_ATTACHMENTS_PATH, limit, start);
    @SuppressWarnings("unchecked")
    final ConfluenceResponse<Attachment> confluenceResources = (ConfluenceResponse<Attachment>) getConfluenceResources(url, Attachment.builder());
    return confluenceResources;
  }

  /**
   * <p>
   * Gets a specific attachment contained in the specific page
   * </p>
   *
   * @param attachmentId
   * @return the {@code Attachment} instance
   * @throws Exception
   */
  public Attachment getAttachment(final String attachmentId) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s?%s", protocol, host, port, path, CONTENT_PATH, attachmentId, EXPANDABLE_PARAMETERS);
    logger.debug("[Processing] Hitting url for getting document content : {}", sanitizeUrl(url));
    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        response.close();
        throw new Exception("Confluence error. " + errorDesc);
      }
      final HttpEntity entity = response.getEntity();
      final MutableAttachment attachment = attachmentFromHttpEntity(entity);
      EntityUtils.consume(entity);
      retrieveAndSetAttachmentContent(attachment);
      return attachment;
    } catch (final Exception e) {
      logger.error("[Processing] Failed to get attachment {}. Error: {}", url, e.getMessage());
      throw e;
    }
  }

  /**
   * <p>
   * Downloads and retrieves the attachment content, setting it in the given {@code Attachment} instance
   * </p>
   *
   * @param attachment the {@code Attachment} instance to download and set the content
   * @throws Exception
   */
  private void retrieveAndSetAttachmentContent(final MutableAttachment attachment) throws Exception {
    final StringBuilder sb = new StringBuilder();
    sb.append(attachment.getBaseUrl()).append(attachment.getUrlContext()).append(attachment.getDownloadUrl());
    final String url = sanitizeUrl(sb.toString());
    logger.debug("[Processing] Hitting url for getting attachment content : {}", url);
    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        if (response.getStatusLine().getStatusCode() != 404) {
          throw new Exception("Confluence error. " + errorDesc);
        } else {
          logger.error("[Processing] Failed to get attachment content {}. Error: {}", url, errorDesc);
        }
      } else {
        attachment.setLength(response.getEntity().getContentLength());
        final byte[] byteContent = IOUtils.toByteArray(response.getEntity().getContent());
        EntityUtils.consumeQuietly(response.getEntity());
        attachment.setContentStream(new ByteArrayInputStream(byteContent));
      }
    } catch (final Exception e) {

      logger.error("[Processing] Failed to get attachment content from {}. Error: {}", url, e.getMessage());
      throw e;
    }

  }

  /**
   * <p>
   * Get a Confluence page identified by its id
   * </p>
   *
   * @param pageId the page id
   * @return the Confluence page
   * @throws Exception
   */
  public Page getPage(final String pageId) throws Exception {
    String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s?%s", protocol, host, port, path, CONTENT_PATH, pageId, EXPANDABLE_PARAMETERS);
    url = sanitizeUrl(url);
    logger.debug("[Processing] Hitting url for getting document content : {}", url);
    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        if (response.getStatusLine().getStatusCode() != 404) {
          throw new Exception("Confluence error. " + errorDesc);
        } else {
          logger.error("[Processing] Failed to get page {}. Error: {}", url, errorDesc);
          return null;
        }
      }
      final HttpEntity entity = response.getEntity();
      final MutablePage page = pageFromHttpEntity(entity);
      EntityUtils.consume(entity);
      final List<Label> labels = getLabels(pageId);
      page.setLabels(labels);
      return page;
    } catch (final Exception e) {
      logger.error("[Processing] Failed to get page {}. Error: {}", url, e.getMessage());
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  public ConfluenceRestrictionsResponse<Restrictions> getPageReadRestrictions(final int start, final int limit, final String pageId) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s%s?limit=%s&start=%s", protocol, host, port, path, CONTENT_PATH, pageId, READ_RESTRICTIONS_PATH, limit, start);
    return (ConfluenceRestrictionsResponse<Restrictions>) getConfluenceRestrictionsResources(url, Restrictions.builder());
  }

  /**
   * <p>
   * Get the labels of a specific page
   * </p>
   *
   * @param pageId The pageId to get the labels
   * @return a {@code List<Label>} of labels
   */
  public List<Label> getLabels(final String pageId) {

    final List<Label> labels = Lists.newArrayList();
    int lastStart = 0;
    final int limit = 50;
    boolean isLast = false;
    do {
      String url = String.format(Locale.ROOT, "%s://%s:%s%s%s/%s/%s?start=%s&limit=%s", protocol, host, port, path, CONTENT_PATH, pageId, LABEL_PATH, lastStart, limit);
      url = sanitizeUrl(url);
      logger.debug("[Processing] Hitting url for getting page labels : {}", url);
      try {
        @SuppressWarnings("unchecked")
        final ConfluenceResponse<Label> response = (ConfluenceResponse<Label>) getConfluenceResources(url, Label.builder());
        if (response != null) {
          labels.addAll(response.getResults());
          lastStart += response.getResults().size();
          isLast = response.isLast();
        } else {
          break;
        }
      } catch (final Exception e) {
        logger.debug("Error getting labels for page {}. Reason: {}", pageId, e.getMessage());
      }
    } while (!isLast);

    return labels;
  }

  /**
   *
   * @param username
   * @return
   * @throws Exception
   */
  public ConfluenceUser getUserAuthorities(final String username) throws Exception {
    final List<String> authorities = Lists.<String>newArrayList();
    final List<Group> groups = getUserGroups(username);
    groups.forEach(group -> {
      authorities.add("group-" + group.getName());
    });
    final User user = getConfluenceUser(username);
    if (user != null) {
      authorities.add("user-" + user.getUserKey());
    }
    final List<Space> spaces = getSpaces();
    for (final Space space : spaces) {
      final List<String> permissions = getSpacePermissionsForUser(space, username);
      if (permissions.contains(VIEW_PERMISSION)) {
        authorities.add("space-" + space.getKey());
      }
    }
    return new ConfluenceUser(username, authorities);

  }

  private User getConfluenceUser(final String username) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s?username=%s", protocol, host, port, path, USER_PATH, username);
    final HttpGet httpGet = createGetRequest(url);
    try (CloseableHttpResponse response = executeRequest(httpGet);) {
      if (response.getStatusLine().getStatusCode() != 200) {
        final String errorDesc = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        if (response.getStatusLine().getStatusCode() != 404) {
          throw new Exception("Confluence error. " + errorDesc);
        } else {
          logger.error("[Processing] Failed to get page {}. Error: {}", url, errorDesc);
          return null;
        }
      }
      final HttpEntity entity = response.getEntity();
      final User user = userFromHttpEntity(entity);
      EntityUtils.consume(entity);
      return user;
    }
  }

  private List<Group> getUserGroups(final String username) throws Exception {
    long lastStart = 0;
    final long defaultSize = 50;
    final List<Group> groups = new ArrayList<Group>();

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      final String groupsDesc = "groups of user " + username;
      Logging.connectors.debug(new MessageFormat("Starting from {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, groupsDesc }));
    }

    Boolean isLast = true;
    do {
      final ConfluenceResponse<Group> response = getUserGroups((int) lastStart, (int) defaultSize, username);

      if (response != null) {
        int count = 0;
        for (final Group group : response.getResults()) {
          groups.add(group);
          count++;
        }

        lastStart += count;
        isLast = response.isLast();
        if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug(new MessageFormat("New start {0} and size {1}", Locale.ROOT).format(new Object[] { lastStart, defaultSize }));
        }
      } else {
        break;
      }
    } while (!isLast);

    return groups;
  }

  @SuppressWarnings("unchecked")
  public ConfluenceResponse<Group> getUserGroups(final int start, final int limit, final String username) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%s?username=%s&limit=%s&start=%s", protocol, host, port, path, USER_GROUPS_PATH, username, limit, start);
    return (ConfluenceResponse<Group>) getConfluenceResources(url, Group.builder());
  }

  private HttpPost createPostRequest(final String url) {
    final HttpPost httpPost = new HttpPost(url);
    httpPost.addHeader("Accept", "application/json");
    httpPost.addHeader("Content-Type", "application/json");
    if (useBasicAuthentication()) {
      httpPost.addHeader("Authorization", "Basic " + Base64.encodeBase64String(String.format(Locale.ROOT, "%s:%s", this.username, this.password).getBytes(Charset.forName("UTF-8"))));
    }
    return httpPost;
  }

  /**
   * <p>
   * Execute the given {@code HttpUriRequest} using the configured client
   * </p>
   *
   * @param request the {@code HttpUriRequest} to be executed
   * @return the {@code HttpResponse} object returned from the server
   * @throws Exception
   */
  private CloseableHttpResponse executeRequest(final HttpUriRequest request) throws Exception {
    final String url = request.getURI().toString();
    logger.debug("[Processing] Hitting url for getting document content : {}", url);

    CloseableHttpResponse response = null;
    try {
      response = httpClient.execute(request, httpContext);
      return response;
    } catch (final Exception e) {
      if (response != null) {
        response.close();
      }
      logger.error("[Processing] Failed to get page {}. Error: {}", url, e.getMessage());
      throw e;
    }
  }

  /**
   * <p>
   * Creates a Confluence user object from the given entity returned by the server
   * </p>
   *
   * @param entity the {@code HttpEntity} to create the {@code User}
   * @return the Confluence user instance
   * @throws Exception
   */
  private User userFromHttpEntity(final HttpEntity entity) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");

    final JSONParser parser = new JSONParser();
    final JSONObject responseObject = (JSONObject) parser.parse(new StringReader(stringEntity));
    final User user = User.builder().fromJson(responseObject);
    return user;
  }

  /**
   * <p>
   * Creates a Confluence page object from the given entity returned by the server
   * </p>
   *
   * @param entity the {@code HttpEntity} to create the {@code MutablePage} from
   * @return the Confluence page instance
   * @throws Exception
   */
  private MutablePage pageFromHttpEntity(final HttpEntity entity) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");

    final JSONParser parser = new JSONParser();
    final JSONObject responseObject = (JSONObject) parser.parse(new StringReader(stringEntity));
    @SuppressWarnings("unchecked")
    final MutablePage response = ((ConfluenceResourceBuilder<MutablePage>) MutablePage.builder()).fromJson(responseObject, new MutablePage());
    return response;
  }

  /**
   * <p>
   * Creates a {@code MutableAttachment} object from the given entity returned by the server
   * </p>
   *
   * @param entity the {@code HttpEntity} to create the {@code MutableAttachment} from
   * @return the Confluence MutableAttachment instance
   * @throws Exception
   */
  private MutableAttachment attachmentFromHttpEntity(final HttpEntity entity) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");
    final JSONParser parser = new JSONParser();
    final JSONObject responseObject = (JSONObject) parser.parse(new StringReader(stringEntity));
    ;
    final MutableAttachment response = (MutableAttachment) Attachment.builder().fromJson(responseObject, new MutableAttachment());
    return response;
  }

  /**
   * <p>
   * Method to check if basic authentication must be used
   * </p>
   *
   * @return {@code Boolean} indicating whether basic authentication must be used or not
   */
  private boolean useBasicAuthentication() {
    return this.username != null && !"".equals(username) && this.password != null;
  }

  /**
   * <p>
   * Sanitize the given url replacing the appearance of more than one slash by only one slash
   * </p>
   *
   * @param url The url to sanitize
   * @return the sanitized url
   */
  private String sanitizeUrl(final String url) {
    final int colonIndex = url.indexOf(":");
    final String urlWithoutProtocol = url.startsWith("http") ? url.substring(colonIndex + 3) : url;
    final String sanitizedUrl = urlWithoutProtocol.replaceAll("\\/+", "/");
    return url.substring(0, colonIndex) + "://" + sanitizedUrl;
  }

  @SuppressWarnings("unchecked")
  public ConfluenceResponse<Space> getSpaces(final int start, final int limit, final Optional<String> spaceType, final Optional<String> spaceStatus) throws Exception {
    String url = String.format(Locale.ROOT, "%s://%s:%s%s%s?limit=%s&start=%s", protocol, host, port, path, SPACES_PATH, limit, start);
    if (spaceType.isPresent()) {
      url = String.format(Locale.ROOT, "%s&type=%s", url, spaceType.get());
    }
    if (spaceStatus.isPresent()) {
      url = String.format(Locale.ROOT, "%s&status=%s", url, spaceStatus.get());
    }
    return (ConfluenceResponse<Space>) getConfluenceResources(url, Space.builder());
  }

  /**
   * Get all spaces from Confluence
   *
   * @return all found spaces
   * @throws Exception
   */
  private List<Space> getSpaces() throws Exception {
    final List<Space> spaces = new ArrayList<>();
    long lastStart = 0;
    final long defaultSize = 25;
    Boolean isLast = true;
    do {
      final ConfluenceResponse<Space> response = getSpaces((int) lastStart, (int) defaultSize, Optional.<String>absent(), Optional.<String>absent());

      if (response != null) {
        spaces.addAll(response.getResults());

        lastStart += response.getResults().size();
        isLast = response.isLast();
        if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug(new MessageFormat("New start {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getSpaces" }));
        }
      } else {
        break;
      }
    } while (!isLast);
    return spaces;
  }

  private List<String> getSpacePermissionsForUser(final Space space, final String username) throws Exception {
    final String url = String.format(Locale.ROOT, "%s://%s:%s%s%sgetPermissionsForUser", protocol, host, port, path, AUTHORITY_PATH);

    logger.debug("[Processing] Hitting url {} for getting Confluence permissions for user {} in space {}", url, username, space.getKey());

    final HttpPost httpPost = createPostRequest(url);
    final JSONArray jsonArray = new JSONArray();
    jsonArray.add(space.getKey());
    jsonArray.add(username);
    final StringEntity stringEntity = new StringEntity(jsonArray.toJSONString());
    httpPost.setEntity(stringEntity);
    final HttpResponse response = httpClient.execute(httpPost);
    if (response.getStatusLine().getStatusCode() != 200) {
      throw new ConfluenceException("Confluence error. " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
    }
    final HttpEntity entity = response.getEntity();
    final List<String> permissions = permissionsFromHttpEntity(entity, space.getName(), username);
    EntityUtils.consume(entity);
    return permissions;
  }

  private List<String> permissionsFromHttpEntity(final HttpEntity entity, final String space, final String username) throws Exception {
    final String stringEntity = EntityUtils.toString(entity, "UTF-8");
    final JSONParser parser = new JSONParser();
    final Object parsedReponse = parser.parse(new StringReader(stringEntity));
    final List<String> permissions = Lists.newArrayList();
    if (parsedReponse instanceof JSONArray) {
      final JSONArray responseObject = (JSONArray) parsedReponse;
      for (int i = 0, len = responseObject.size(); i < len; i++) {
        permissions.add(responseObject.get(i).toString());
      }
    } else {
      final JSONObject responseObject = (JSONObject) parsedReponse;
      if (responseObject.containsKey("error")) {
        final JSONObject error = (JSONObject) responseObject.get("error");
        final String message = error.get("message").toString();
        // Probably has no permissions to get this space's permissions
        logger.warn("Confluence authority: Can't get permissions of user '" + username + "' for space '" + space + "'; " + message);
        return new ArrayList<>(0);
      } else {
        throw new Exception("Unexpected JSON format: " + responseObject);
      }
    }

    return permissions;
  }
}
