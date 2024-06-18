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

package org.apache.manifoldcf.authorities.authorities.confluence.v6;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.core.interfaces.CacheManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ICacheCreateHandle;
import org.apache.manifoldcf.core.interfaces.ICacheDescription;
import org.apache.manifoldcf.core.interfaces.ICacheHandle;
import org.apache.manifoldcf.core.interfaces.ICacheManager;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPasswordMapperActivity;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.StringSet;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.ConfluenceConfiguration;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.client.ConfluenceClient;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceUser;
import org.apache.manifoldcf.authorities.system.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Confluence Authority Connector class
 * </p>
 * <p>
 * ManifoldCF Authority connector to deal with Confluence documents
 * </p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 */
public class ConfluenceAuthorityConnector extends BaseAuthorityConnector {

  /*
   * Prefix for Confluence configuration and specification parameters
   */
  private static final String PARAMETER_PREFIX = "confluence_";

  /* Configuration tabs */
  private static final String CONF_SERVER_TAB_PROPERTY = "ConfluenceAuthorityConnector.Server";
  
  private static final String CONF_CACHE_TAB_PROPERTY = "ConfluenceAuthorityConnector.Cache";

  // pages & js
  // Template names for Confluence configuration
  /**
   * Forward to the javascript to check the configuration parameters
   */
  private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration_conf.js";
  /**
   * Cache tab template
   */
  private static final String EDIT_CONFIG_FORWARD_CACHE = "editConfiguration_conf_cache.html";
  /**
   * Server tab template
   */
  private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_conf_server.html";

  /**
   * Forward to the HTML template to view the configuration parameters
   */
  private static final String VIEW_CONFIG_FORWARD = "viewConfiguration_conf.html";


  private Logger logger = LoggerFactory
      .getLogger(ConfluenceAuthorityConnector.class);

  /* Confluence instance parameters */
  protected String protocol = null;
  protected String host = null;
  protected String port = null;
  protected String path = null;
  protected String username = null;
  protected String password = null;
  protected String socketTimeout = null;
  protected String connectionTimeout = null;
  private String cacheLifetime = null;
  private String cacheLRUsize = null;
  private final long responseLifetime = 60000L;
  private final int LRUsize = 1000;

  protected String proxyUsername = null;
  protected String proxyPassword = null;
  protected String proxyProtocol = null;
  protected String proxyHost = null;
  protected String proxyPort = null;

  /** Cache manager. */
  private ICacheManager cacheManager = null;

  protected ConfluenceClient confluenceClient = null;

  /**
   * <p>
   * Default constructor
   * </p>
   */
  public ConfluenceAuthorityConnector() {
    super();
  }
  
  /**
   * Used Mainly for testing
   * 
   * @param cm
   */
  public void setCacheManager(ICacheManager cm){
    this.cacheManager = cm;
  }
  
  /**
   * Used Mainly for testing
   * 
   * @param client Injected Confluence Client
   */
  public void setConfluenceClient(ConfluenceClient client){
    this.confluenceClient = client;
  }
  
  /**
   * Set thread context.
   */
  @Override
  public void setThreadContext(final IThreadContext tc) throws ManifoldCFException {
    super.setThreadContext(tc);
    cacheManager = CacheManagerFactory.make(tc);
  }

  /**
   * Clear thread context.
   */
  @Override
  public void clearThreadContext() {
    super.clearThreadContext();
    cacheManager = null;
  }

  

  /**
   * Close the connection. Call this before discarding the connection.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    if (confluenceClient != null) {
      confluenceClient = null;
    }

    protocol = null;
    host = null;
    port = null;
    path = null;
    username = null;
    password = null;
    socketTimeout = null;
    connectionTimeout = null;
    cacheLifetime = null;
    cacheLRUsize = null;
    proxyUsername = null;
    proxyPassword = null;
    proxyProtocol = null;
    proxyHost = null;
    proxyPort = null;

  }

  /**
   * Makes connection to server
   * 
   * 
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);

    protocol = params.getParameter(ConfluenceConfiguration.Server.PROTOCOL);
    host = params.getParameter(ConfluenceConfiguration.Server.HOST);
    port = params.getParameter(ConfluenceConfiguration.Server.PORT);
    path = params.getParameter(ConfluenceConfiguration.Server.PATH);
    username = params.getParameter(ConfluenceConfiguration.Server.USERNAME);
    password = params
        .getObfuscatedParameter(ConfluenceConfiguration.Server.PASSWORD);
    socketTimeout = params.getParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    connectionTimeout = params.getParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    
    cacheLifetime = params.getParameter(ConfluenceConfiguration.Authority.CACHE_LIFETIME);
    if (cacheLifetime == null) {
      cacheLifetime = "1";
    }
    cacheLRUsize = params.getParameter(ConfluenceConfiguration.Authority.CACHE_LRU_SIZE);
    if (cacheLRUsize == null) {
      cacheLRUsize = "1000";
    }
    proxyUsername = params.getParameter(ConfluenceConfiguration.Server.PROXY_USERNAME);
    proxyPassword = params.getObfuscatedParameter(ConfluenceConfiguration.Server.PROXY_PASSWORD);
    proxyProtocol = params.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);
    proxyHost = params.getParameter(ConfluenceConfiguration.Server.PROXY_HOST);
    proxyPort = params.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);

    try {
      initConfluenceClient();
    } catch (ManifoldCFException e) {
      logger.debug(
          "Not possible to initialize Confluence client. Reason: {}",
          e.getMessage());
    }
  }

  /**
   * Checks if connection is available
   */
  @Override
  public String check() throws ManifoldCFException {
    try {
      if (!isConnected()) {
        initConfluenceClient();
      }
      Boolean result = confluenceClient.checkAuth();
      if (result)
        return super.check();
      else
        throw new ManifoldCFException(
            "Confluence instance could not be reached");
    } catch (ServiceInterruption e) {
      return "Connection temporarily failed: " + e.getMessage();
    } catch (ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    } catch (Exception e) {
      return "Connection failed: " + e.getMessage();
    }
  }

  /**
   * <p>
   * Initialize Confluence client using the configured parameters
   * 
   * @throws ManifoldCFException
   */
  protected void initConfluenceClient() throws ManifoldCFException {
    if (confluenceClient == null) {

      if (StringUtils.isEmpty(protocol)) {
        throw new ManifoldCFException("Parameter "
            + ConfluenceConfiguration.Server.PROTOCOL
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence protocol = '" + protocol
            + "'");
      }

      if (StringUtils.isEmpty(host)) {
        throw new ManifoldCFException("Parameter "
            + ConfluenceConfiguration.Server.HOST
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence host = '" + host + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence port = '" + port + "'");
      }

      if (StringUtils.isEmpty(path)) {
        throw new ManifoldCFException("Parameter "
            + ConfluenceConfiguration.Server.PATH
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence path = '" + path + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence username = '" + username
            + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors
            .debug("Confluence password '" + password != null ? "set"
                : "not set" + "'");
      }
      
      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence socket timeout = '" + socketTimeout
            + "'");
      }
      
      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Confluence connection timeout = '" + connectionTimeout
            + "'");
      }

      int portInt;
      if (port != null && port.length() > 0) {
        try {
          portInt = Integer.parseInt(port);
        } catch (NumberFormatException e) {
          throw new ManifoldCFException("Bad number: "
              + e.getMessage(), e);
        }
      } else {
        if (protocol.toLowerCase(Locale.ROOT).equals("http"))
          portInt = 80;
        else
          portInt = 443;
      }

      int socketTimeoutInt;
      if (socketTimeout != null && socketTimeout.length() > 0) {
        try {
          socketTimeoutInt = Integer.parseInt(socketTimeout);
        } catch (NumberFormatException e) {
          throw new ManifoldCFException("Bad number: "
              + e.getMessage(), e);
        }
      } else {
        socketTimeoutInt = 900000;
      }
      
      int connectionTimeoutInt;
      if (connectionTimeout != null && connectionTimeout.length() > 0) {
        try {
          connectionTimeoutInt = Integer.parseInt(connectionTimeout);
        } catch (NumberFormatException e) {
          throw new ManifoldCFException("Bad number: "
              + e.getMessage(), e);
        }
      } else {
        connectionTimeoutInt = 60000;
      }

      int proxyPortInt;
      if (proxyPort != null && proxyPort.length() > 0) {
          try {
              proxyPortInt = Integer.parseInt(proxyPort);
          } catch (NumberFormatException e) {
            throw new ManifoldCFException("Bad number: "
                + e.getMessage(), e);
          }
      } else {
          proxyPortInt = -1;
      }

      /* Generating a client to perform Confluence requests */
      confluenceClient = new ConfluenceClient(protocol, host, portInt,
          path, username, password, socketTimeoutInt, connectionTimeoutInt,
          proxyUsername, proxyPassword, proxyProtocol, proxyHost, proxyPortInt);
    }

  }

  /**
   * This method is called to assess whether to count this connector instance
   * should actually be counted as being connected.
   *
   * @return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return confluenceClient != null;
  }
  
  private void fillInCacheTab(final Map<String, String> velocityContext, final IPasswordMapperActivity mapper, final ConfigParams parameters) {
    String cacheLifetime = parameters.getParameter(ConfluenceConfiguration.Authority.CACHE_LIFETIME);
    if (cacheLifetime == null) {
      cacheLifetime = "1";
    }
    velocityContext.put("CACHE_LIFETIME", cacheLifetime);
    String cacheLRUsize = parameters.getParameter(ConfluenceConfiguration.Authority.CACHE_LRU_SIZE);
    if (cacheLRUsize == null) {
      cacheLRUsize = "1000";
    }
    velocityContext.put("CACHE_LRU_SIZE", cacheLRUsize);
  }


  private void fillInServerConfigurationMap(Map<String, String> serverMap,
      IPasswordMapperActivity mapper, ConfigParams parameters) {
    String confluenceProtocol = parameters
        .getParameter(ConfluenceConfiguration.Server.PROTOCOL);
    String confluenceHost = parameters
        .getParameter(ConfluenceConfiguration.Server.HOST);
    String confluencePort = parameters
        .getParameter(ConfluenceConfiguration.Server.PORT);
    String confluencePath = parameters
        .getParameter(ConfluenceConfiguration.Server.PATH);
    String confluenceUsername = parameters
        .getParameter(ConfluenceConfiguration.Server.USERNAME);
    String confluencePassword = parameters
        .getObfuscatedParameter(ConfluenceConfiguration.Server.PASSWORD);
    String confluenceSocketTimeout = parameters
        .getParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    String confluenceConnectionTimeout = parameters
        .getParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    String confluenceProxyUsername = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_USERNAME);
    String confluenceProxyPassword = parameters.getObfuscatedParameter(ConfluenceConfiguration.Server.PROXY_PASSWORD);
    String confluenceProxyProtocol = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_PROTOCOL);
    String confluenceProxyHost = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_HOST);
    String confluenceProxyPort = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);

    if (confluenceProtocol == null)
      confluenceProtocol = ConfluenceConfiguration.Server.PROTOCOL_DEFAULT_VALUE;
    if (confluenceHost == null)
      confluenceHost = ConfluenceConfiguration.Server.HOST_DEFAULT_VALUE;
    if (confluencePort == null)
      confluencePort = ConfluenceConfiguration.Server.PORT_DEFAULT_VALUE;
    if (confluencePath == null)
      confluencePath = ConfluenceConfiguration.Server.PATH_DEFAULT_VALUE;

    if (confluenceUsername == null)
      confluenceUsername = ConfluenceConfiguration.Server.USERNAME_DEFAULT_VALUE;
    if (confluencePassword == null)
      confluencePassword = ConfluenceConfiguration.Server.PASSWORD_DEFAULT_VALUE;
    else
      confluencePassword = mapper.mapPasswordToKey(confluencePassword);
    
    if(confluenceSocketTimeout == null) {
      confluenceSocketTimeout = ConfluenceConfiguration.Server.SOCKET_TIMEOUT_DEFAULT_VALUE;
    }
    if(confluenceConnectionTimeout == null) {
      confluenceConnectionTimeout = ConfluenceConfiguration.Server.CONNECTION_TIMEOUT_DEFAULT_VALUE;
    }
    if (confluenceProxyUsername == null)
        confluenceProxyUsername = ConfluenceConfiguration.Server.PROXY_USERNAME_DEFAULT_VALUE;
    if (confluenceProxyPassword == null)
        confluenceProxyPassword = ConfluenceConfiguration.Server.PROXY_PASSWORD_DEFAULT_VALUE;
    else
        confluenceProxyPassword = mapper.mapPasswordToKey(confluenceProxyPassword);
    if (confluenceProxyProtocol == null)
      confluenceProxyProtocol = ConfluenceConfiguration.Server.PROXY_PROTOCOL_DEFAULT_VALUE;
    if (confluenceProxyHost == null)
        confluenceProxyHost = ConfluenceConfiguration.Server.PROXY_HOST_DEFAULT_VALUE;
    if (confluenceProxyPort == null)
        confluenceProxyPort = ConfluenceConfiguration.Server.PROXY_PORT_DEFAULT_VALUE;

    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PROTOCOL, confluenceProtocol);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.HOST,
        confluenceHost);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PORT,
        confluencePort);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PATH,
        confluencePath);
    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.USERNAME, confluenceUsername);
    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PASSWORD, confluencePassword);
    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.SOCKET_TIMEOUT, confluenceSocketTimeout);
    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.CONNECTION_TIMEOUT, confluenceConnectionTimeout);
    serverMap.put(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PROXY_USERNAME, confluenceProxyUsername);
    serverMap.put(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PROXY_PASSWORD, confluenceProxyPassword);
    serverMap.put(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PROXY_PROTOCOL, confluenceProxyProtocol);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PROXY_HOST,
            confluenceProxyHost);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PROXY_PORT,
            confluenceProxyPort);
  }

  @Override
  public void viewConfiguration(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters)
      throws ManifoldCFException, IOException {
    Map<String, String> paramMap = new HashMap<String, String>();

    /* Fill server configuration parameters */
    fillInServerConfigurationMap(paramMap, out, parameters);
    fillInCacheTab(paramMap, out, parameters);

    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIG_FORWARD,
        paramMap, true);
  }

  @Override
  public void outputConfigurationHeader(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters,
      List<String> tabsArray) throws ManifoldCFException, IOException {
    // Add the Server tab
    tabsArray.add(Messages.getString(locale, CONF_SERVER_TAB_PROPERTY));
    tabsArray.add(Messages.getString(locale, CONF_CACHE_TAB_PROPERTY));
    // Map the parameters
    Map<String, String> paramMap = new HashMap<String, String>();

    /* Fill server configuration parameters */
    fillInServerConfigurationMap(paramMap, out, parameters);
    /* Fill cache configuration parameters */
    fillInCacheTab(paramMap, out, parameters);

    // Output the Javascript - only one Velocity template for all tabs
    Messages.outputResourceWithVelocity(out, locale,
        EDIT_CONFIG_HEADER_FORWARD, paramMap, true);
  }

  @Override
  public void outputConfigurationBody(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters,
      String tabName) throws ManifoldCFException, IOException {

    // Call the Velocity templates for each tab
    Map<String, String> paramMap = new HashMap<String, String>();
    // Set the tab name
    paramMap.put("TabName", tabName);

    // Fill in the parameters
    fillInServerConfigurationMap(paramMap, out, parameters);
    fillInCacheTab(paramMap, out, parameters);

    // Server tab
    Messages.outputResourceWithVelocity(out, locale,
        EDIT_CONFIG_FORWARD_SERVER, paramMap, true);
    
    // Cache tab
    Messages.outputResourceWithVelocity(out, locale,
        EDIT_CONFIG_FORWARD_CACHE, paramMap, true);

  }

  /*
   * Repository specification post handle, (server and proxy & client secret
   * etc)
   * 
   * @see
   * org.apache.manifoldcf.core.connector.BaseConnector#processConfigurationPost
   * (org.apache.manifoldcf.core.interfaces.IThreadContext,
   * org.apache.manifoldcf.core.interfaces.IPostParameters,
   * org.apache.manifoldcf.core.interfaces.ConfigParams)
   */
  @Override
  public String processConfigurationPost(IThreadContext threadContext,
      IPostParameters variableContext, ConfigParams parameters)
      throws ManifoldCFException {
    
    // Cache parameters
    final String cacheLifetime = variableContext.getParameter(ConfluenceConfiguration.Authority.CACHE_LIFETIME);
    if (cacheLifetime != null) {
      parameters.setParameter(ConfluenceConfiguration.Authority.CACHE_LIFETIME, cacheLifetime);
    }
    final String cacheLRUsize = variableContext.getParameter(ConfluenceConfiguration.Authority.CACHE_LRU_SIZE);
    if (cacheLRUsize != null) {
      parameters.setParameter(ConfluenceConfiguration.Authority.CACHE_LRU_SIZE, cacheLRUsize);
    }

    String confluenceProtocol = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PROTOCOL);
    if (confluenceProtocol != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PROTOCOL,
          confluenceProtocol);

    String confluenceHost = variableContext.getParameter(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.HOST);
    if (confluenceHost != null)
      parameters.setParameter(ConfluenceConfiguration.Server.HOST,
          confluenceHost);

    String confluencePort = variableContext.getParameter(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PORT);
    if (confluencePort != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PORT,
          confluencePort);

    String confluencePath = variableContext.getParameter(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PATH);
    if (confluencePath != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PATH,
          confluencePath);

    String confluenceUsername = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.USERNAME);
    if (confluenceUsername != null)
      parameters.setParameter(ConfluenceConfiguration.Server.USERNAME,
          confluenceUsername);

    String confluencePassword = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PASSWORD);
    if (confluencePassword != null)
      parameters.setObfuscatedParameter(
          ConfluenceConfiguration.Server.PASSWORD,
          variableContext.mapKeyToPassword(confluencePassword));
    
    String confluenceSocketTimeout = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    if (confluenceSocketTimeout != null)
      parameters.setParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT,
          confluenceSocketTimeout);
    
    String confluenceConnectionTimeout = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    if (confluenceConnectionTimeout != null)
      parameters.setParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT,
          confluenceConnectionTimeout);
    String confluenceProxyProtocol = variableContext
            .getParameter(PARAMETER_PREFIX
                + ConfluenceConfiguration.Server.PROXY_PROTOCOL);
    if (confluenceProxyProtocol != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PROXY_PROTOCOL,
          confluenceProxyProtocol);

    String confluenceProxyHost = variableContext.getParameter(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PROXY_HOST);
    if (confluenceProxyHost != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PROXY_HOST,
          confluenceProxyHost);

    String confluenceProxyPort = variableContext.getParameter(PARAMETER_PREFIX
        + ConfluenceConfiguration.Server.PROXY_PORT);
    if (confluenceProxyPort != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PROXY_PORT,
          confluenceProxyPort);

    String confluenceProxyUsername = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PROXY_USERNAME);
    if (confluenceProxyUsername != null)
      parameters.setParameter(ConfluenceConfiguration.Server.PROXY_USERNAME,
          confluenceProxyUsername);

    String confluenceProxyPassword = variableContext
        .getParameter(PARAMETER_PREFIX
            + ConfluenceConfiguration.Server.PROXY_PASSWORD);
    if (confluenceProxyPassword != null)
      parameters.setObfuscatedParameter(
          ConfluenceConfiguration.Server.PROXY_PASSWORD,
          variableContext.mapKeyToPassword(confluenceProxyPassword));

    /* null means process configuration has been successful */
    return null;
  }
  
    /*
     * (non-Javadoc)
     * @see org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector#getDefaultAuthorizationResponse(java.lang.String)
     */
    @Override
    public AuthorizationResponse getDefaultAuthorizationResponse(String userName) {
      return RESPONSE_UNREACHABLE;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector#getAuthorizationResponse(java.lang.String)
     */
    @Override
    public AuthorizationResponse getAuthorizationResponse(String userName)
        throws ManifoldCFException {
      
      String finalUsername = userName;
      // If the username contains the domain, must clean it
      if(finalUsername.indexOf("@") != -1) {
        finalUsername = finalUsername.substring(0, finalUsername.indexOf("@"));
      }
      
      if (Logging.authorityConnectors != null && Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Get Confluence autorizations for user '" + finalUsername + "'");
      }
      
      if(cacheManager != null) {
      
        // Construct a cache description object
        final ICacheDescription objectDescription = new AuthorizationResponseDescription(finalUsername, this.responseLifetime, this.LRUsize);
  
        // Enter the cache
        final ICacheHandle ch = cacheManager.enterCache(new ICacheDescription[] { objectDescription }, null, null);
        try {
          final ICacheCreateHandle createHandle = cacheManager.enterCreateSection(ch);
          try {
            // Lookup the object
            AuthorizationResponse response = (AuthorizationResponse) cacheManager.lookupObject(createHandle, objectDescription);
            if (response != null) {
              if (Logging.authorityConnectors != null && Logging.authorityConnectors.isDebugEnabled()) {
                Logging.authorityConnectors.debug("Found cached Confluence autorizations for user '" + finalUsername + "'");
              }
              return response;
            }
            // Create the object.
            response = getAuthorizationResponseUncached(finalUsername);
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
      } else {
        return getAuthorizationResponseUncached(finalUsername);
      }
    }
    
    private AuthorizationResponse getAuthorizationResponseUncached(final String userName) throws ManifoldCFException {
      if (Logging.authorityConnectors != null && Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Get uncached Confluence autorizations for user '" + userName + "'");
      }
      try {
        ConfluenceUser confluenceUser = confluenceClient.getUserAuthorities(userName);
        if (confluenceUser.getUsername() == null
            || confluenceUser.getUsername().isEmpty()
            || confluenceUser.getAuthorities().isEmpty()) {
          if (Logging.authorityConnectors != null && Logging.authorityConnectors.isDebugEnabled()) {
            Logging.authorityConnectors.debug("No Confluence user found for user '" + userName + "'");
          }
          return RESPONSE_USERNOTFOUND;
        } else {
          if (Logging.authorityConnectors != null && Logging.authorityConnectors.isDebugEnabled()) {
            Logging.authorityConnectors.debug("Found Confluence corresponding user for user '" + userName + "'");
          }
          return new AuthorizationResponse(
              confluenceUser.getAuthorities().toArray(new String[confluenceUser.getAuthorities().size()]),
              AuthorizationResponse.RESPONSE_OK);
        }
      } catch (Exception e) {
        return RESPONSE_UNREACHABLE;
      }

    }
    
    protected static StringSet emptyStringSet = new StringSet();
    
    /**
     * This is the cache object descriptor for cached access tokens from this connector.
     */
    protected static class AuthorizationResponseDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription {
      /** The user name */
      protected String userName;
      /** The response lifetime */
      protected long responseLifetime;
      /** The expiration time */
      protected long expirationTime = -1;

      /** Constructor. */
      public AuthorizationResponseDescription(final String userName, final long responseLifetime, final int LRUsize) {
        super("SharePointADAuthority", LRUsize);
        this.userName = userName;
        this.responseLifetime = responseLifetime;
      }

      /** Return the invalidation keys for this object. */
      @Override
      public StringSet getObjectKeys() {
        return emptyStringSet;
      }

      /** Get the critical section name, used for synchronizing the creation of the object */
      @Override
      public String getCriticalSectionName() {
        final StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("-").append(userName);
        return sb.toString();
      }

      /** Return the object expiration interval */
      @Override
      public long getObjectExpirationTime(final long currentTime) {
        if (expirationTime == -1) {
          expirationTime = currentTime + responseLifetime;
        }
        return expirationTime;
      }

      @Override
      public int hashCode() {
        final int rval = userName.hashCode();
        return rval;
      }

      @Override
      public boolean equals(final Object o) {
        if (!(o instanceof AuthorizationResponseDescription)) {
          return false;
        }
        final AuthorizationResponseDescription ard = (AuthorizationResponseDescription) o;
        if (!ard.userName.equals(userName)) {
          return false;
        }
        return true;
      }

    }

}
