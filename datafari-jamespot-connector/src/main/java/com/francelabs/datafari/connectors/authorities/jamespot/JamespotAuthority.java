/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.francelabs.datafari.connectors.authorities.jamespot;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.manifoldcf.core.util.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.manifoldcf.authorities.system.Logging;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.core.interfaces.CacheManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ICacheCreateHandle;
import org.apache.manifoldcf.core.interfaces.ICacheDescription;
import org.apache.manifoldcf.core.interfaces.ICacheHandle;
import org.apache.manifoldcf.core.interfaces.ICacheManager;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.StringSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.connectors.authorities.jamespot.Messages;
import com.francelabs.datafari.connectors.authorities.jamespot.api.JamespotUser;




public class JamespotAuthority extends org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector {



  /**
   * This is the active directory global deny token. This should be ingested
   * with all documents.
   */
  private static final String globalDenyToken = "DEAD_AUTHORITY";

  private static final AuthorizationResponse unreachableResponse = new AuthorizationResponse(new String[]{globalDenyToken},
      AuthorizationResponse.RESPONSE_UNREACHABLE);

  private static final AuthorizationResponse userNotFoundResponse = new AuthorizationResponse(new String[]{globalDenyToken},
      AuthorizationResponse.RESPONSE_USERNOTFOUND);

  private final static String ACTION_PARAM_NAME = "action";

  private final static String ACTION_AUTH = "auth";

  private final static String ACTION_CHECK = "check";

  /** Connection manager */
  private HttpClientConnectionManager connectionManager = null;

  /** HttpClientBuilder */
  private HttpClientBuilder builder = null;

  /** Httpclient instance */
  private CloseableHttpClient httpClient = null;

  /** Connection timeout */
  private int connectionTimeout = -1;

  /** Socket timeout */
  private int socketTimeout = -1;

  /** Session timeout */
  private long sessionTimeout = -1L;


  protected final static long sessionExpirationInterval = 300000L;

  private String newgenericLogin = null;

  private String newgenericPassword = null;

  private String newgenericEntryPoint = null;

  private String customHeaderName1 = null;

  private String customHeaderValue1 = null;

  private String customHeaderName2 = null;

  private String customHeaderValue2 = null;


  /** Connection timeout */
  private String connectionTimeoutString = null;

  /** Socket timeout */
  private String socketTimeoutString = null;

  private String responseLifeTimeString = null;

  private final int LRUsize = 1000;

  private final long responseLifetime = 60000L;


  private long sessionExpirationTime = -1L;

  private static final String EDIT_CONFIGURATION_AUTHORITY_JS = "editConfiguration.js";
  private static final String EDIT_CONFIGURATION_AUTHORITY_HTML = "editConfiguration_Server.html";
  private static final String VIEW_CONFIGURATION_AUTHORITY_HTML = "viewConfiguration.html";


  /**
   * Cache manager.
   */
  private ICacheManager cacheManager = null;

  /**
   * Constructor.
   */
  public JamespotAuthority() {
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
          RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build());
      poolingConnectionManager.setDefaultMaxPerRoute(1);
      poolingConnectionManager.setValidateAfterInactivity(2000);
      poolingConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).setSoTimeout(socketTimeout).build());

      this.connectionManager = poolingConnectionManager;

      final RequestConfig.Builder requestBuilder = RequestConfig.custom().setCircularRedirectsAllowed(true).setSocketTimeout(socketTimeout).setExpectContinueEnabled(false).setConnectTimeout(connectionTimeout)
          .setConnectionRequestTimeout(socketTimeout);

      final List<Header> headers = new ArrayList<>();

      // Optional Basic Auth
      if (newgenericLogin != null && !newgenericLogin.isEmpty() && newgenericPassword != null && !newgenericPassword.isEmpty()) {
        final String auth = newgenericLogin + ":" + newgenericPassword;
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        final String authHeader = "Basic " + encodedAuth;
        headers.add(new BasicHeader(HttpHeaders.AUTHORIZATION, authHeader));
      }

      // Optional custom header 1
      if (customHeaderName1 != null && !customHeaderName1.isEmpty() && customHeaderValue1 != null && !customHeaderValue1.isEmpty()) {
        headers.add(new BasicHeader(customHeaderName1, customHeaderValue1));
      }

      // Optional custom header 2
      if (customHeaderName2 != null && !customHeaderName2.isEmpty() && customHeaderValue2 != null && !customHeaderValue2.isEmpty()) {
        headers.add(new BasicHeader(customHeaderName2, customHeaderValue2));
      }

      this.builder = HttpClients.custom().setDefaultHeaders(headers).setConnectionManager(connectionManager).disableAutomaticRetries().setDefaultRequestConfig(requestBuilder.build());
      builder.setRequestExecutor(new HttpRequestExecutor(socketTimeout)).setRedirectStrategy(new DefaultRedirectStrategy());
      this.httpClient = builder.build();
    }
    sessionTimeout = System.currentTimeMillis() + sessionExpirationInterval;

  }

  /**
   * Set thread context.
   */
  @Override
  public void setThreadContext(IThreadContext tc)
      throws ManifoldCFException {
    super.setThreadContext(tc);
    cacheManager = CacheManagerFactory.make(tc);
  }

  /**
   * Connect. The configuration parameters are included.
   *
   * @param configParams are the configuration parameters for this connection.
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);
    newgenericEntryPoint = configParams.getParameter(JamespotConfig.PARAM_ADDRESS);
    connectionTimeoutString = configParams.getParameter(JamespotConfig.PARAM_CONNECTIONTIMEOUT);
    socketTimeoutString = configParams.getParameter(JamespotConfig.PARAM_SOCKETTIMEOUT);
    newgenericLogin = configParams.getParameter(JamespotConfig.PARAM_LOGIN);
    newgenericPassword = configParams.getParameter(JamespotConfig.PARAM_PASSWORD);
    responseLifeTimeString = configParams.getParameter(JamespotConfig.PARAM_RESPONSELIFETIME);
    customHeaderName1 = configParams.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_1);
    customHeaderValue1 = configParams.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_1);
    customHeaderName2 = configParams.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_2);
    customHeaderValue2 = configParams.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_2);

    if (newgenericLogin == null) {
      newgenericLogin = "";
    }

    if (newgenericPassword == null) {
      newgenericPassword = "";
    }

    if (connectionTimeoutString == null) {
      connectionTimeoutString = JamespotConfig.CONNECTIONTIMEOUT_DEFAULT;
    }

    if (socketTimeoutString == null) {
      socketTimeoutString = JamespotConfig.SOCKETTIMEOUT_DEFAULT;
    }

    if (responseLifeTimeString == null) {
      responseLifeTimeString = JamespotConfig.RESPONSELIFETIME_DEFAULT;
    }

    if (customHeaderName1 == null) {
      customHeaderName1 = "";
    }

    if (customHeaderValue1 == null) {
      customHeaderValue1 = "";
    }

    if (customHeaderName2 == null) {
      customHeaderName2 = "";
    }

    if (customHeaderValue2 == null) {
      customHeaderValue2 = "";
    }


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
   * Poll. The connection should be closed if it has been idle for too long.
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

  /** This method is called to assess whether to count this connector instance should
   * actually be counted as being connected.
   *@return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return sessionTimeout != -1L;
  }



  /**
   * Check connection for sanity.
   */
  @Override
  public String check() throws ManifoldCFException {
    getSession();
    CloseableHttpResponse response = null;
    try {
      try {
        //final String username = "admin@totem.com";
        Logging.authorityConnectors.debug("Testing API request: " + newgenericEntryPoint);
        final HttpGet httpGet = new HttpGet(newgenericEntryPoint);
        response = this.httpClient.execute(httpGet);
      } catch (final IOException e) {
        Logging.authorityConnectors.debug("Request failure: " + e.getMessage());
        return "Connection error: " + e.getMessage();
      }
      final int responseCode = response.getStatusLine().getStatusCode();
      if (responseCode != 200) {
        Logging.authorityConnectors.debug("Request KO: " + responseCode + " " + response.getStatusLine().getReasonPhrase());
        return "Bad response: " + response.getStatusLine();
      } else {
        Logging.authorityConnectors.debug("Request OK");
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

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect()
      throws ManifoldCFException {

    // Zero out all the stuff that we want to be sure we don't use again
    newgenericEntryPoint = null;
    newgenericLogin = null;
    newgenericPassword = null;
    customHeaderName1 = null;
    customHeaderValue1 = null;
    customHeaderName2 = null;
    customHeaderValue2 = null;

    super.disconnect();
  }

  protected String createCacheConnectionString() {
    StringBuilder sb = new StringBuilder();
    sb.append(newgenericEntryPoint).append("#").append(newgenericLogin);
    return sb.toString();
  }

  /**
   * Obtain the access tokens for a given user name.
   *
   * @param userName is the user name or identifier.
   * @return the response tokens (according to the current authority). (Should
   * throws an exception only when a condition cannot be properly described
   * within the authorization response object.)
   */
  @Override
  public AuthorizationResponse getAuthorizationResponse(String userName)
      throws ManifoldCFException {

    getSession();
    // Construct a cache description object
    ICacheDescription objectDescription = new GenericAuthorizationResponseDescription(userName,
        createCacheConnectionString(), this.responseLifetime, this.LRUsize);

    // Enter the cache
    ICacheHandle ch = cacheManager.enterCache(new ICacheDescription[]{objectDescription}, null, null);
    try {
      ICacheCreateHandle createHandle = cacheManager.enterCreateSection(ch);
      try {
        // Lookup the object
        AuthorizationResponse response = (AuthorizationResponse) cacheManager.lookupObject(createHandle, objectDescription);
        if (response != null) {
          if (Logging.authorityConnectors != null) {
            Logging.authorityConnectors.debug("New generic: response in the cache status: "+response.getResponseStatus());
          }
          return response;
        }
        // Create the object.
        response = getAuthorizationResponseUncached(userName);
        // Save it in the cache
        cacheManager.saveObject(createHandle, objectDescription, response);
        // And return it...
        return response;
      } finally {
        cacheManager.leaveCreateSection(createHandle);
      }
    } finally {
      cacheManager.leaveCache(ch);
    }
  }

  protected AuthorizationResponse getAuthorizationResponseUncached(String userName)
      throws ManifoldCFException {
    StringBuilder url = new StringBuilder(newgenericEntryPoint);

    url.append("?username="+userName);

    if (Logging.authorityConnectors != null) {
      Logging.authorityConnectors.debug("New generic: url: "+url);
    }

    List<String> permissions = new ArrayList<String>();
    permissions = getNewGenericUserSecurity(userName);

    if (permissions == null || permissions.isEmpty()) {
      Logging.authorityConnectors.debug("No security found for user '" + userName + "'");
      return RESPONSE_USERNOTFOUND_ADDITIVE;
    }

    Logging.authorityConnectors.debug("Found security groups for user '" + userName + "'");
    String[] tokens = new String[permissions.size()];
    int k = 0;
    while (k < tokens.length) {
      tokens[k] = (String) permissions.get(k);
      Logging.authorityConnectors.debug("security group "+k+" for user '" + userName + " " + (String) permissions.get(k) + "'");
      k++;
    }

    return new AuthorizationResponse(tokens, AuthorizationResponse.RESPONSE_OK);


  }

  /** Get security groups for new generic user */
protected List<String> getNewGenericUserSecurity(final String username)
    throws ManifoldCFException {

  getSession();

  final List<String> permissions = new ArrayList<>();

  final StringBuilder url = new StringBuilder(newgenericEntryPoint);
  url.append("?username=").append(URLEncoder.encode(username));

  final HttpGet httpGet = new HttpGet(url.toString());

  try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

    final int responseCode = response.getStatusLine().getStatusCode();
    if (responseCode != HttpStatus.SC_OK) {
      Logging.authorityConnectors.warn(
          "Jamespot: bad response for user '" + username + "': " + response.getStatusLine()
      );
      return permissions;
    }

    final String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

    final JSONParser parser = new JSONParser();
    final JSONObject jsonObject = (JSONObject) parser.parse(responseBody);

    final Object valObject = jsonObject.get("VAL");
    if (!(valObject instanceof JSONObject)) {
      Logging.authorityConnectors.warn(
          "Jamespot: missing or invalid VAL object for user '" + username + "'"
      );
      return permissions;
    }

    final Object valTokens = ((JSONObject) valObject).get("tokens");
    if (!(valTokens instanceof JSONArray)) {
      Logging.authorityConnectors.warn(
          "Jamespot: missing or invalid VAL.tokens array for user '" + username + "'"
      );
      return permissions;
    }

    final JSONArray security = (JSONArray) valTokens;

    for (final Object token : security) {
      if (token != null) {
        permissions.add(token.toString());
      }
    }

    return permissions;

  } catch (final IOException e) {
    throw new ManifoldCFException("Jamespot: error while calling authority endpoint", e);
  } catch (final ParseException e) {
    throw new ManifoldCFException("Jamespot: error while parsing authority JSON response", e);
  }
}

  /**
   * Obtain the default access tokens for a given user name.
   *
   * @param userName is the user name or identifier.
   * @return the default response tokens, presuming that the connect method
   * fails.
   */
  @Override
  public AuthorizationResponse getDefaultAuthorizationResponse(String userName) {
    // The default response if the getConnection method fails
    return unreachableResponse;
  }


  // UI support methods.
  //
  // These support methods are involved in setting up authority connection configuration information. The configuration methods cannot assume that the
  // current authority object is connected.  That is why they receive a thread context argument.
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
      Locale locale, ConfigParams parameters, List<String> tabsArray)
          throws ManifoldCFException, IOException {

    tabsArray.add(Messages.getString(locale, "NewGeneric.NewGenericTabName"));
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_AUTHORITY_JS, null);

  }

  @Override
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
      Locale locale, ConfigParams parameters, String tabName)
          throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("TabName", tabName);

    fillInServerTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIGURATION_AUTHORITY_HTML, velocityContext);


  }

  protected static void fillInServerTab(final Map<String, Object> velocityContext, final IHTTPOutput out, final ConfigParams parameters) throws ManifoldCFException {


    String newgenericAddress = parameters.getParameter(JamespotConfig.PARAM_ADDRESS);
    if (newgenericAddress == null) {
      newgenericAddress = JamespotConfig.ADDRESS_DEFAULT;
    }

    String connectionTimeout = parameters.getParameter(JamespotConfig.PARAM_CONNECTIONTIMEOUT);
    if (connectionTimeout == null)   {
      connectionTimeout = JamespotConfig.CONNECTIONTIMEOUT_DEFAULT;
    }

    String socketTimeout = parameters.getParameter(JamespotConfig.PARAM_SOCKETTIMEOUT);
    if (socketTimeout == null) {
      socketTimeout = JamespotConfig.SOCKETTIMEOUT_DEFAULT;
    }

    String login = parameters.getParameter(JamespotConfig.PARAM_LOGIN);
    if (login == null) {
      login = "";
    }
    String password = parameters.getParameter(JamespotConfig.PARAM_PASSWORD);
    if (password == null) {
      password = "";
    }

    String responselifetime = parameters.getParameter(JamespotConfig.PARAM_RESPONSELIFETIME);
    if (responselifetime == null) {
      responselifetime = JamespotConfig.RESPONSELIFETIME_DEFAULT;
    }

    String customHeaderName1 = parameters.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_1);
    if (customHeaderName1 == null) {
      customHeaderName1 = "";
    }

    String customHeaderValue1 = parameters.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_1);
    if (customHeaderValue1 == null) {
      customHeaderValue1 = "";
    }

    String customHeaderName2 = parameters.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_2);
    if (customHeaderName2 == null) {
      customHeaderName2 = "";
    }

    String customHeaderValue2 = parameters.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_2);
    if (customHeaderValue2 == null) {
      customHeaderValue2 = "";
    }
    // Fill in context

    velocityContext.put("ADDRESS", newgenericAddress);
    velocityContext.put("CONNECTIONTIMEOUT", connectionTimeout);
    velocityContext.put("SOCKETTIMEOUT", socketTimeout);
    velocityContext.put("LOGIN", login);
    velocityContext.put("PASSWORD", password);
    velocityContext.put("RESPONSELIFETIME", responselifetime);
    velocityContext.put("CUSTOMHEADERNAME1", customHeaderName1);
    velocityContext.put("CUSTOMHEADERVALUE1", customHeaderValue1);
    velocityContext.put("CUSTOMHEADERNAME2", customHeaderName2);
    velocityContext.put("CUSTOMHEADERVALUE2", customHeaderValue2);


  }


  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final Locale locale, final ConfigParams parameters) throws ManifoldCFException {
    final String newgenericAddress = variableContext.getParameter("newgenericAddress");

    if (newgenericAddress != null) {
      parameters.setParameter(JamespotConfig.PARAM_ADDRESS, newgenericAddress);
    }

    final String connectionTimeout = variableContext.getParameter(JamespotConfig.PARAM_CONNECTIONTIMEOUT);
    if (connectionTimeout != null) {
      parameters.setParameter(JamespotConfig.PARAM_CONNECTIONTIMEOUT, connectionTimeout);
    }

    final String socketTimeout = variableContext.getParameter(JamespotConfig.PARAM_SOCKETTIMEOUT);
    if (socketTimeout != null) {
      parameters.setParameter(JamespotConfig.PARAM_SOCKETTIMEOUT, socketTimeout);
    }

    final String login = variableContext.getParameter(JamespotConfig.PARAM_LOGIN);
    if (login != null) {
      parameters.setParameter(JamespotConfig.PARAM_LOGIN, login);
    }

    final String password = variableContext.getParameter(JamespotConfig.PARAM_PASSWORD);
    if (password != null) {
      parameters.setParameter(JamespotConfig.PARAM_PASSWORD, password);
    }

    final String responselifetime = variableContext.getParameter(JamespotConfig.PARAM_RESPONSELIFETIME);
    if (responselifetime != null) {
      parameters.setParameter(JamespotConfig.PARAM_RESPONSELIFETIME, responselifetime);
    }

    final String customHeaderName1 = variableContext.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_1);
    if (customHeaderName1 != null) {
      parameters.setParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_1, customHeaderName1);
    }

    final String customHeaderValue1 = variableContext.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_1);
    if (customHeaderValue1 != null) {
      parameters.setParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_1, customHeaderValue1);
    }

    final String customHeaderName2 = variableContext.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_2);
    if (customHeaderName2 != null) {
      parameters.setParameter(JamespotConfig.PARAM_CUSTOM_HEADER_NAME_2, customHeaderName2);
    }

    final String customHeaderValue2 = variableContext.getParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_2);
    if (customHeaderValue2 != null) {
      parameters.setParameter(JamespotConfig.PARAM_CUSTOM_HEADER_VALUE_2, customHeaderValue2);
    }

    return null;
  }


  /**
   * View configuration. This method is called in the body section of the connector's view configuration page. Its purpose is to present the
   * connection information to the user. The coder can presume that the HTML that is output from this configuration will be within appropriate
   * <html> and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    final Map<String, Object> velocityContext = new HashMap<>();
    fillInServerTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIGURATION_AUTHORITY_HTML, velocityContext);
  }

  // Protected methods
  protected static StringSet emptyStringSet = new StringSet();



  /**
   * This is the cache object descriptor for cached access tokens from this
   * connector.
   */
  protected class GenericAuthorizationResponseDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription {

    /**
     * The user name
     */
    protected String userName;

    /**
     * LDAP connection string with server name and base DN
     */
    protected String connectionString;

    /**
     * The response lifetime
     */
    protected long responseLifetime;

    /**
     * The expiration time
     */
    protected long expirationTime = -1;

    /**
     * Constructor.
     */
    public GenericAuthorizationResponseDescription(String userName, String connectionString, long responseLifetime, int LRUsize) {
      super("LDAPAuthority", LRUsize);
      this.userName = userName;
      this.connectionString = connectionString;
      this.responseLifetime = responseLifetime;
    }

    /**
     * Return the invalidation keys for this object.
     */
    @Override
    public StringSet getObjectKeys() {
      return emptyStringSet;
    }

    /**
     * Get the critical section name, used for synchronizing the creation of the
     * object
     */
    @Override
    public String getCriticalSectionName() {
      StringBuilder sb = new StringBuilder(getClass().getName());
      sb.append("-").append(userName).append("-").append(connectionString);
      return sb.toString();
    }

    /**
     * Return the object expiration interval
     */
    @Override
    public long getObjectExpirationTime(long currentTime) {
      if (expirationTime == -1) {
        expirationTime = currentTime + responseLifetime;
      }
      return expirationTime;
    }

    @Override
    public int hashCode() {
      return userName.hashCode() + connectionString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof GenericAuthorizationResponseDescription)) {
        return false;
      }
      GenericAuthorizationResponseDescription ard = (GenericAuthorizationResponseDescription) o;
      if (!ard.userName.equals(userName)) {
        return false;
      }
      if (!ard.connectionString.equals(connectionString)) {
        return false;
      }
      return true;
    }
  }

  static class PreemptiveAuth implements HttpRequestInterceptor {

    private Credentials credentials;

    public PreemptiveAuth(Credentials creds) {
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

    public CheckThread(HttpClient client, String url) {
      super();
      setDaemon(true);
      this.client = client;
      this.url = url;

    }

    @Override
    public void run() {
      HttpGet method = new HttpGet(url);
      try {
        HttpResponse response = client.execute(method);
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
      } catch (IOException ex) {
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


}
