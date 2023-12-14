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

package org.apache.manifoldcf.crawler.connectors.confluence.v6;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPasswordMapperActivity;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.client.ConfluenceClient;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Attachment;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceResponse;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceRestrictionsResponse;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Page;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.PageType;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Restrictions;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Restrictions.ReadRestrictions;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.Space;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.util.ConfluenceUtil;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * <p>
 * Confluence Repository Connector class
 * </p>
 * <p>
 * ManifoldCF Repository connector to deal with Confluence documents
 * </p>
 *
 * @author Julien Massiera &amp; Antonio David Perez Morales
 *
 */
public class ConfluenceRepositoryConnector extends BaseRepositoryConnector {

  protected final static String ACTIVITY_READ = "read document";

  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = GLOBAL_DENY_TOKEN;

  private static final String CHILD_PREFIX = "child+";

  /*
   * Prefix for Confluence configuration and specification parameters
   */
  private static final String PARAMETER_PREFIX = "confluence_";

  /* Configuration tabs */
  private static final String CONF_SERVER_TAB_PROPERTY = "ConfluenceRepositoryConnector.Server";

  /* Specification tabs */
  private static final String CONF_SECURITY_TAB_PROPERTY = "ConfluenceRepositoryConnector.Security";
  private static final String CONF_SPACES_TAB_PROPERTY = "ConfluenceRepositoryConnector.Spaces";
  private static final String CONF_PAGES_TAB_PROPERTY = "ConfluenceRepositoryConnector.Pages";
  private static Level DOCPROCESSLEVEL = Level.forName("DOCPROCESS", 450);
  private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(ConfluenceRepositoryConnector.class.getName());

  // pages & js
  // Template names for Confluence configuration
  /**
   * Forward to the javascript to check the configuration parameters
   */
  private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration_conf.js";
  /**
   * Server tab template
   */
  private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_conf_server.html";

  /**
   * Forward to the HTML template to view the configuration parameters
   */
  private static final String VIEW_CONFIG_FORWARD = "viewConfiguration_conf.html";

  // Template names for Confluence job specification
  /**
   * Forward to the javascript to check the specification parameters for the job
   */
  private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification_conf.js";
  /**
   * Forward to the template to edit the security for the job
   */
  private static final String EDIT_SPEC_FORWARD_SECURITY = "editSpecification_confSecurity.html";
  /**
   * Forward to the template to edit the spaces for the job
   */
  private static final String EDIT_SPEC_FORWARD_SPACES = "editSpecification_confSpaces.html";

  /**
   * Forward to the template to edit the pages configuration for the job
   */
  private static final String EDIT_SPEC_FORWARD_CONF_PAGES = "editSpecification_confPages.html";

  /**
   * Forward to the template to view the specification parameters for the job
   */
  private static final String VIEW_SPEC_FORWARD = "viewSpecification_conf.html";

  protected long lastSessionFetch = -1L;
  protected static final long timeToRelease = 300000L;

  protected final static long interruptionRetryTime = 5L * 60L * 1000L;

  private final Logger logger = LoggerFactory.getLogger(ConfluenceRepositoryConnector.class);

  /* Confluence instance parameters */
  protected String protocol = null;
  protected String host = null;
  protected String port = null;
  protected String path = null;
  protected String username = null;
  protected String password = null;
  protected String socketTimeout = null;
  protected String connectionTimeout = null;
  protected String retryIntervalString = null;
  protected String retryNumberString = null;

  protected String proxyUsername = null;
  protected String proxyPassword = null;
  protected String proxyProtocol = null;
  protected String proxyHost = null;
  protected String proxyPort = null;

  /** Retry interval */
  protected long retryInterval = -1L;

  /** Retry number */
  protected int retryNumber = -1;

  protected ConfluenceClient confluenceClient = null;

  /**
   * <p>
   * Default constructor
   * </p>
   */
  public ConfluenceRepositoryConnector() {
    super();
  }

  /**
   * Set Confluence Client (Mainly for Testing)
   *
   * @param confluenceClient
   */
  public void setConfluenceClient(final ConfluenceClient confluenceClient) {
    this.confluenceClient = confluenceClient;
  }

  @Override
  public String[] getActivitiesList() {
    return new String[] { ACTIVITY_READ };
  }

  @Override
  public String[] getBinNames(final String documentIdentifier) {
    return new String[] { host };
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
    retryIntervalString = null;
    retryNumberString = null;
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
  public void connect(final ConfigParams configParams) {
    super.connect(configParams);

    protocol = params.getParameter(ConfluenceConfiguration.Server.PROTOCOL);
    host = params.getParameter(ConfluenceConfiguration.Server.HOST);
    port = params.getParameter(ConfluenceConfiguration.Server.PORT);
    path = params.getParameter(ConfluenceConfiguration.Server.PATH);
    username = params.getParameter(ConfluenceConfiguration.Server.USERNAME);
    password = params.getObfuscatedParameter(ConfluenceConfiguration.Server.PASSWORD);
    socketTimeout = params.getParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    connectionTimeout = params.getParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    retryIntervalString = configParams.getParameter(ConfluenceConfiguration.Server.RETRY_INTERVAL);
    retryNumberString = configParams.getParameter(ConfluenceConfiguration.Server.RETRY_NUMBER);

    proxyUsername = params.getParameter(ConfluenceConfiguration.Server.PROXY_USERNAME);
    proxyPassword = params.getObfuscatedParameter(ConfluenceConfiguration.Server.PROXY_PASSWORD);
    proxyProtocol = params.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);
    proxyHost = params.getParameter(ConfluenceConfiguration.Server.PROXY_HOST);
    proxyPort = params.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);

    try {
      initConfluenceClient();
    } catch (final ManifoldCFException e) {
      logger.debug("Not possible to initialize Confluence client. Reason: {}", e.getMessage());
      e.printStackTrace();
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
      final Boolean result = confluenceClient.check();
      if (result) {
        return super.check();
      } else {
        throw new ManifoldCFException("Confluence instance could not be reached");
      }
    } catch (final ServiceInterruption e) {
      return "Connection temporarily failed: " + e.getMessage();
    } catch (final ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    } catch (final Exception e) {
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
        throw new ManifoldCFException("Parameter " + ConfluenceConfiguration.Server.PROTOCOL + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence protocol = '" + protocol + "'");
      }

      if (StringUtils.isEmpty(host)) {
        throw new ManifoldCFException("Parameter " + ConfluenceConfiguration.Server.HOST + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence host = '" + host + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence port = '" + port + "'");
      }

//      if (StringUtils.isEmpty(path)) {
//        throw new ManifoldCFException("Parameter "
//            + ConfluenceConfiguration.Server.PATH
//            + " required but not set");
//      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence path = '" + path + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence username = '" + username + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence password '" + password != null ? "set" : "not set" + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence socket timeout = '" + socketTimeout + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("Confluence connection timeout = '" + connectionTimeout + "'");
      }

      int portInt;
      if (port != null && port.length() > 0) {
        try {
          portInt = Integer.parseInt(port);
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException("Bad number: " + e.getMessage(), e);
        }
      } else {
        if (protocol.toLowerCase(Locale.ROOT).equals("http")) {
          portInt = 80;
        } else {
          portInt = 443;
        }
      }

      int socketTimeoutInt;
      if (socketTimeout != null && socketTimeout.length() > 0) {
        try {
          socketTimeoutInt = Integer.parseInt(socketTimeout);
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException("Bad number: " + e.getMessage(), e);
        }
      } else {
        socketTimeoutInt = 900000;
      }

      int connectionTimeoutInt;
      if (connectionTimeout != null && connectionTimeout.length() > 0) {
        try {
          connectionTimeoutInt = Integer.parseInt(connectionTimeout);
        } catch (final NumberFormatException e) {
          throw new ManifoldCFException("Bad number: " + e.getMessage(), e);
        }
      } else {
        connectionTimeoutInt = 60000;
      }

      try {
        this.retryInterval = Long.parseLong(retryIntervalString);
      } catch (final NumberFormatException e) {
        throw new ManifoldCFException("Bad retry interval number: " + retryIntervalString);
      }

      try {
        this.retryNumber = Integer.parseInt(retryNumberString);
      } catch (final NumberFormatException e) {
        throw new ManifoldCFException("Bad retry number: " + retryNumberString);
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
      confluenceClient = new ConfluenceClient(protocol, host, portInt, path, username, password, socketTimeoutInt, connectionTimeoutInt,
              proxyUsername, proxyPassword, proxyProtocol, proxyHost, proxyPortInt);
      lastSessionFetch = System.currentTimeMillis();
    }

  }

  /**
   * This method is called to assess whether to count this connector instance should actually be counted as being connected.
   *
   * @return true if the connector instance is actually connected.
   */
  @Override
  public boolean isConnected() {
    return confluenceClient != null;
  }

  @Override
  public void poll() throws ManifoldCFException {
    if (lastSessionFetch == -1L) {
      return;
    }

    final long currentTime = System.currentTimeMillis();
    if (currentTime >= lastSessionFetch + timeToRelease) {
      confluenceClient.close();
      confluenceClient = null;
      lastSessionFetch = -1L;
    }
  }

  @Override
  public int getMaxDocumentRequest() {
    return super.getMaxDocumentRequest();
  }

  /**
   * Return the list of relationship types that this connector recognizes.
   *
   * @return the list.
   */
  @Override
  public String[] getRelationshipTypes() {
    return new String[] {};
  }

  private void fillInServerConfigurationMap(final Map<String, String> serverMap, final IPasswordMapperActivity mapper, final ConfigParams parameters) {
    String confluenceProtocol = parameters.getParameter(ConfluenceConfiguration.Server.PROTOCOL);
    String confluenceHost = parameters.getParameter(ConfluenceConfiguration.Server.HOST);
    String confluencePort = parameters.getParameter(ConfluenceConfiguration.Server.PORT);
    String confluencePath = parameters.getParameter(ConfluenceConfiguration.Server.PATH);
    String confluenceUsername = parameters.getParameter(ConfluenceConfiguration.Server.USERNAME);
    String confluencePassword = parameters.getObfuscatedParameter(ConfluenceConfiguration.Server.PASSWORD);
    String confluenceSocketTimeout = parameters.getParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    String confluenceConnectionTimeout = parameters.getParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    String confluenceRetryNumber = parameters.getParameter(ConfluenceConfiguration.Server.RETRY_NUMBER);
    String confluenceRetryInterval = parameters.getParameter(ConfluenceConfiguration.Server.RETRY_INTERVAL);

    String confluenceProxyUsername = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_USERNAME);
    String confluenceProxyPassword = parameters.getObfuscatedParameter(ConfluenceConfiguration.Server.PROXY_PASSWORD);
    String confluenceProxyProtocol = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_PROTOCOL);
    String confluenceProxyHost = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_HOST);
    String confluenceProxyPort = parameters.getParameter(ConfluenceConfiguration.Server.PROXY_PORT);

    if (confluenceProtocol == null) {
      confluenceProtocol = ConfluenceConfiguration.Server.PROTOCOL_DEFAULT_VALUE;
    }
    if (confluenceHost == null) {
      confluenceHost = ConfluenceConfiguration.Server.HOST_DEFAULT_VALUE;
    }
    if (confluencePort == null) {
      confluencePort = ConfluenceConfiguration.Server.PORT_DEFAULT_VALUE;
    }
    if (confluencePath == null) {
      confluencePath = ConfluenceConfiguration.Server.PATH_DEFAULT_VALUE;
    }

    if (confluenceUsername == null) {
      confluenceUsername = ConfluenceConfiguration.Server.USERNAME_DEFAULT_VALUE;
    }
    if (confluencePassword == null) {
      confluencePassword = ConfluenceConfiguration.Server.PASSWORD_DEFAULT_VALUE;
    } else {
      confluencePassword = mapper.mapPasswordToKey(confluencePassword);
    }

    if (confluenceSocketTimeout == null) {
      confluenceSocketTimeout = ConfluenceConfiguration.Server.SOCKET_TIMEOUT_DEFAULT_VALUE;
    }
    if (confluenceConnectionTimeout == null) {
      confluenceConnectionTimeout = ConfluenceConfiguration.Server.CONNECTION_TIMEOUT_DEFAULT_VALUE;
    }

    if (confluenceRetryNumber == null) {
      confluenceRetryNumber = ConfluenceConfiguration.Server.RETRY_NUMBER_DEFAULT_VALUE;
    }
    if (confluenceRetryInterval == null) {
      confluenceRetryInterval = ConfluenceConfiguration.Server.RETRY_INTERVAL_DEFAULT_VALUE;
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

    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PROTOCOL, confluenceProtocol);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.HOST, confluenceHost);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PORT, confluencePort);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PATH, confluencePath);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.USERNAME, confluenceUsername);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PASSWORD, confluencePassword);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.SOCKET_TIMEOUT, confluenceSocketTimeout);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.CONNECTION_TIMEOUT, confluenceConnectionTimeout);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.RETRY_NUMBER, confluenceRetryNumber);
    serverMap.put(PARAMETER_PREFIX + ConfluenceConfiguration.Server.RETRY_INTERVAL, confluenceRetryInterval);
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
  public void viewConfiguration(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters) throws ManifoldCFException, IOException {
    final Map<String, String> paramMap = new HashMap<String, String>();

    /* Fill server configuration parameters */
    fillInServerConfigurationMap(paramMap, out, parameters);

    Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIG_FORWARD, paramMap, true);
  }

  @Override
  public void outputConfigurationHeader(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final List<String> tabsArray)
      throws ManifoldCFException, IOException {
    // Add the Server tab
    tabsArray.add(Messages.getString(locale, CONF_SERVER_TAB_PROPERTY));
    // Map the parameters
    final Map<String, String> paramMap = new HashMap<String, String>();

    /* Fill server configuration parameters */
    fillInServerConfigurationMap(paramMap, out, parameters);

    // Output the Javascript - only one Velocity template for all tabs
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_HEADER_FORWARD, paramMap, true);
  }

  @Override
  public void outputConfigurationBody(final IThreadContext threadContext, final IHTTPOutput out, final Locale locale, final ConfigParams parameters, final String tabName)
      throws ManifoldCFException, IOException {

    // Call the Velocity templates for each tab
    final Map<String, String> paramMap = new HashMap<String, String>();
    // Set the tab name
    paramMap.put("TabName", tabName);

    // Fill in the parameters
    fillInServerConfigurationMap(paramMap, out, parameters);

    // Server tab
    Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_FORWARD_SERVER, paramMap, true);

  }

  /*
   * Repository specification post handle, (server and proxy & client secret etc)
   *
   * @see org.apache.manifoldcf.core.connector.BaseConnector#processConfigurationPost (org.apache.manifoldcf.core.interfaces.IThreadContext, org.apache.manifoldcf.core.interfaces.IPostParameters,
   * org.apache.manifoldcf.core.interfaces.ConfigParams)
   */
  @Override
  public String processConfigurationPost(final IThreadContext threadContext, final IPostParameters variableContext, final ConfigParams parameters) throws ManifoldCFException {

    final String confluenceProtocol = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PROTOCOL);
    if (confluenceProtocol != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.PROTOCOL, confluenceProtocol);
    }

    final String confluenceHost = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.HOST);
    if (confluenceHost != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.HOST, confluenceHost);
    }

    final String confluencePort = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PORT);
    if (confluencePort != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.PORT, confluencePort);
    }

    final String confluencePath = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PATH);
    if (confluencePath != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.PATH, confluencePath);
    }

    final String confluenceUsername = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.USERNAME);
    if (confluenceUsername != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.USERNAME, confluenceUsername);
    }

    final String confluencePassword = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.PASSWORD);
    if (confluencePassword != null) {
      parameters.setObfuscatedParameter(ConfluenceConfiguration.Server.PASSWORD, variableContext.mapKeyToPassword(confluencePassword));
    }

    final String confluenceSocketTimeout = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.SOCKET_TIMEOUT);
    if (confluenceSocketTimeout != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.SOCKET_TIMEOUT, confluenceSocketTimeout);
    }

    final String confluenceConnectionTimeout = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.CONNECTION_TIMEOUT);
    if (confluenceConnectionTimeout != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.CONNECTION_TIMEOUT, confluenceConnectionTimeout);
    }

    final String confluenceRetryNumber = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.RETRY_NUMBER);
    if (confluenceRetryNumber != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.RETRY_NUMBER, confluenceRetryNumber);
    }

    final String confluenceRetryInterval = variableContext.getParameter(PARAMETER_PREFIX + ConfluenceConfiguration.Server.RETRY_INTERVAL);
    if (confluenceRetryInterval != null) {
      parameters.setParameter(ConfluenceConfiguration.Server.RETRY_INTERVAL, confluenceRetryInterval);
    }

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

  /**
   * <p>
   * Fill the configured spaces into the map
   * </p>
   *
   * @param newMap
   * @param cs
   */
  private void fillInConfSpacesSpecificationMap(final Map<String, Object> newMap, final ConfluenceSpecification cs) {

    newMap.put(ConfluenceConfiguration.Specification.SPACES.toUpperCase(Locale.ROOT), cs.getSpaces());
  }

  private void fillInConfSecuritySpecificationMap(final Map<String, Object> newMap, final ConfluenceSpecification cs) {

    newMap.put(ConfluenceConfiguration.Specification.ACTIVATE_SECURITY_ATTRIBUTE_KEY.toUpperCase(Locale.ROOT), cs.isSecurityActive().toString());
    return;

  }

  /**
   * <p>
   * Fill the pages configuration into the map
   * </p>
   *
   * @param newMap
   * @param cs
   */
  private void fillInConfPagesSpecificationMap(final Map<String, Object> newMap, final ConfluenceSpecification cs) {

    newMap.put(ConfluenceConfiguration.Specification.PROCESS_ATTACHMENTS_ATTRIBUTE_KEY.toUpperCase(Locale.ROOT), cs.isProcessAttachments().toString());
    newMap.put(ConfluenceConfiguration.Specification.PAGETYPE.toUpperCase(Locale.ROOT), cs.getPageType());
    return;

  }

  @Override
  public void viewSpecification(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException, IOException {

    final Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));

    final ConfluenceSpecification cs = ConfluenceSpecification.from(ds);

    fillInConfSecuritySpecificationMap(paramMap, cs);
    fillInConfSpacesSpecificationMap(paramMap, cs);
    fillInConfPagesSpecificationMap(paramMap, cs);

    Messages.outputResourceWithVelocity(out, locale, VIEW_SPEC_FORWARD, paramMap);
  }

  /*
   * Handle job specification post
   *
   * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector# processSpecificationPost (org.apache.manifoldcf.core.interfaces.IPostParameters,
   * org.apache.manifoldcf.crawler.interfaces.DocumentSpecification)
   */

  @Override
  public String processSpecificationPost(final IPostParameters variableContext, final Locale locale, final Specification ds, final int connectionSequenceNumber) throws ManifoldCFException {

    final String seqPrefix = "s" + connectionSequenceNumber + "_";

    String xc = variableContext.getParameter(seqPrefix + "spacescount");
    if (xc != null) {
      // Delete all preconfigured spaces
      int i = 0;
      while (i < ds.getChildCount()) {
        final SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(ConfluenceConfiguration.Specification.SPACES)) {
          ds.removeChild(i);
        } else {
          i++;
        }
      }

      final SpecificationNode spaces = new SpecificationNode(ConfluenceConfiguration.Specification.SPACES);
      ds.addChild(ds.getChildCount(), spaces);
      final int spacesCount = Integer.parseInt(xc);
      i = 0;
      while (i < spacesCount) {
        final String spaceDescription = "_" + Integer.toString(i);
        final String spaceOpName = seqPrefix + "spaceop" + spaceDescription;
        xc = variableContext.getParameter(spaceOpName);
        if (xc != null && xc.equals("Delete")) {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        final String spaceKey = variableContext.getParameter(seqPrefix + "space" + spaceDescription);
        final SpecificationNode node = new SpecificationNode(ConfluenceConfiguration.Specification.SPACE);
        node.setAttribute(ConfluenceConfiguration.Specification.SPACE_KEY_ATTRIBUTE, spaceKey);
        spaces.addChild(spaces.getChildCount(), node);
        i++;
      }

      final String op = variableContext.getParameter(seqPrefix + "spaceop");
      if (op != null && op.equals("Add")) {
        final String spaceSpec = variableContext.getParameter(seqPrefix + "space");
        final SpecificationNode node = new SpecificationNode(ConfluenceConfiguration.Specification.SPACE);
        node.setAttribute(ConfluenceConfiguration.Specification.SPACE_KEY_ATTRIBUTE, spaceSpec);
        spaces.addChild(spaces.getChildCount(), node);
      }
    }

    /* Delete security configuration */
    int i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(ConfluenceConfiguration.Specification.SECURITY)) {
        ds.removeChild(i);
      } else {
        i++;
      }
    }

    final SpecificationNode security = new SpecificationNode(ConfluenceConfiguration.Specification.SECURITY);
    ds.addChild(ds.getChildCount(), security);

    final String activateSecurity = variableContext.getParameter(seqPrefix + ConfluenceConfiguration.Specification.ACTIVATE_SECURITY_ATTRIBUTE_KEY);
    if (activateSecurity != null && !activateSecurity.isEmpty()) {
      security.setAttribute(ConfluenceConfiguration.Specification.ACTIVATE_SECURITY_ATTRIBUTE_KEY, String.valueOf(activateSecurity));
    }

    /* Delete pages configuration */
    i = 0;
    while (i < ds.getChildCount()) {
      final SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(ConfluenceConfiguration.Specification.PAGES)) {
        ds.removeChild(i);
      } else {
        i++;
      }
    }

    final SpecificationNode pages = new SpecificationNode(ConfluenceConfiguration.Specification.PAGES);
    ds.addChild(ds.getChildCount(), pages);

    final String procAttachments = variableContext.getParameter(seqPrefix + ConfluenceConfiguration.Specification.PROCESS_ATTACHMENTS_ATTRIBUTE_KEY);
    if (procAttachments != null && !procAttachments.isEmpty()) {
      pages.setAttribute(ConfluenceConfiguration.Specification.PROCESS_ATTACHMENTS_ATTRIBUTE_KEY, String.valueOf(procAttachments));
    }

    final String pageType = variableContext.getParameter(seqPrefix + ConfluenceConfiguration.Specification.PAGETYPE);
    if (pageType != null && !pageType.isEmpty()) {
      pages.setAttribute(ConfluenceConfiguration.Specification.PAGETYPE, pageType);
    }

    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector# outputSpecificationBody (org.apache.manifoldcf.core.interfaces.IHTTPOutput, java.util.Locale,
   * org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, java.lang.String)
   */
  @Override
  public void outputSpecificationBody(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final int actualSequenceNumber, final String tabName)
      throws ManifoldCFException, IOException {

    // Output JIRAQuery tab
    final Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("TabName", tabName);
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));
    paramMap.put("SelectedNum", Integer.toString(actualSequenceNumber));

    final ConfluenceSpecification cs = ConfluenceSpecification.from(ds);

    fillInConfSecuritySpecificationMap(paramMap, cs);
    fillInConfSpacesSpecificationMap(paramMap, cs);
    fillInConfPagesSpecificationMap(paramMap, cs);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_FORWARD_SECURITY, paramMap);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_FORWARD_SPACES, paramMap);

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_FORWARD_CONF_PAGES, paramMap);
  }

  /*
   * Header for the specification
   *
   * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector# outputSpecificationHeader (org.apache.manifoldcf.core.interfaces.IHTTPOutput, java.util.Locale,
   * org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, java.util.List)
   */
  @Override
  public void outputSpecificationHeader(final IHTTPOutput out, final Locale locale, final Specification ds, final int connectionSequenceNumber, final List<String> tabsArray)
      throws ManifoldCFException, IOException {

    tabsArray.add(Messages.getString(locale, CONF_SECURITY_TAB_PROPERTY));
    tabsArray.add(Messages.getString(locale, CONF_SPACES_TAB_PROPERTY));
    tabsArray.add(Messages.getString(locale, CONF_PAGES_TAB_PROPERTY));

    final Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));

    Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_HEADER_FORWARD, paramMap);
  }

  /*
   * Adding seed documents
   *
   * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector# addSeedDocuments (org.apache.manifoldcf.crawler.interfaces.ISeedingActivity,
   * org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, long, long, int)
   */
  @Override
  public String addSeedDocuments(final ISeedingActivity activities, final Specification spec, final String lastSeedVersion, final long seedTime, final int jobMode)
      throws ManifoldCFException, ServiceInterruption {

    if (!isConnected()) {
      initConfluenceClient();
    }

    try {

      /*
       * Not uses delta seeding because Confluence can't be queried using dates or in a ordered way, only start and limit which can cause problems if an already indexed document is deleted, because we
       * will miss some to-be indexed docs due to the last start parameter stored in the last execution
       */
      // if(lastSeedVersion != null && !lastSeedVersion.isEmpty()) {
      // StringTokenizer tokenizer = new
      // StringTokenizer(lastSeedVersion,"|");
      //
      // lastStart = new Long(lastSeedVersion);
      // }

      final ConfluenceSpecification confluenceSpecification = ConfluenceSpecification.from(spec);
      List<String> spaceKeys = confluenceSpecification.getSpaces();
      final String pageType = confluenceSpecification.getPageType();

      if (spaceKeys.isEmpty()) {
        logger.info("No spaces configured. Processing all spaces");
        spaceKeys = getAllSpaceKeys();
      }

      for (final String space : spaceKeys) {
        logger.info("Processing configured space {}", space);
        addSeedDocumentsForSpace(space, Optional.<String>of(pageType), activities, confluenceSpecification, lastSeedVersion, seedTime, jobMode);
      }

      return "";
    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
      return null;
    }
  }

  private List<Page> getPageChilds(final String pageId) throws ManifoldCFException, ServiceInterruption {
    long lastStart = 0;
    final long defaultSize = 25;
    final List<Page> pageChilds = new ArrayList<>();

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug(new MessageFormat("Starting from {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getPageChilds" }));
    }

    try {
      Boolean isLast = true;
      do {
        final ConfluenceResponse<Page> response = confluenceClient.getPageChilds((int) lastStart, (int) defaultSize, pageId);

        if (response != null) {
          int count = 0;
          for (final Page page : response.getResults()) {
            pageChilds.add(page);
            count++;
          }

          lastStart += count;
          isLast = response.isLast();
          if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug(new MessageFormat("New start {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getPageChilds" }));
          }
        } else {
          break;
        }
      } while (!isLast);

    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
    }
    return pageChilds;

  }

  private List<Restrictions> getPageReadRestrictions(final String pageId) throws ManifoldCFException, ServiceInterruption {
    long lastStart = 0;
    final long defaultSize = 200;
    final List<Restrictions> restrictionsList = new ArrayList<Restrictions>();

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug(new MessageFormat("Starting from {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getAllSpaceKeys" }));
    }

    try {
      Boolean isLast = true;
      do {
        final ConfluenceRestrictionsResponse<Restrictions> response = confluenceClient.getPageReadRestrictions((int) lastStart, (int) defaultSize, pageId);

        if (response != null) {
          if (response.getResult() != null) {
            restrictionsList.add(response.getResult());
          }

          isLast = response.isLast();
          if (!isLast) {
            lastStart += defaultSize;
          }
          if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug(new MessageFormat("New start {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getAllSpaceKeys" }));
          }
        } else {
          break;
        }
      } while (!isLast);

    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
    }
    return restrictionsList;
  }

  private List<String> getAllSpaceKeys() throws ManifoldCFException, ServiceInterruption {
    final List<String> spaceKeys = new ArrayList<String>();
    long lastStart = 0;
    final long defaultSize = 25;

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug(new MessageFormat("Starting from {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getAllSpaceKeys" }));
    }

    try {
      Boolean isLast = true;
      do {
        final ConfluenceResponse<Space> response = confluenceClient.getSpaces((int) lastStart, (int) defaultSize, Optional.<String>absent(), Optional.<String>absent());

        if (response != null) {
          int count = 0;
          for (final Space space : response.getResults()) {
            spaceKeys.add(space.getKey());
            count++;
          }

          lastStart += count;
          isLast = response.isLast();
          if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug(new MessageFormat("New start {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, "getAllSpaceKeys" }));
          }
        } else {
          break;
        }
      } while (!isLast);
    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
    }
    return spaceKeys;
  }

  /**
   * <p>
   * Add seed documents for a given optional space
   * </p>
   *
   * @throws ServiceInterruption
   * @throws ManifoldCFException
   */
  private void addSeedDocumentsForSpace(final String space, final Optional<String> pageType, final ISeedingActivity activities, final ConfluenceSpecification confluenceSpec,
      final String lastSeedVersion, final long seedTime, final int jobMode) throws ManifoldCFException, ServiceInterruption {

    long lastStart = 0;
    final long defaultSize = 50;

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      final String spaceDesc = "space with key " + space;
      Logging.connectors.debug(new MessageFormat("Starting from {0} and size {1} for {2}", Locale.ROOT).format(new Object[] { lastStart, defaultSize, spaceDesc }));
    }

    try {
      Boolean isLast = true;
      do {
        final ConfluenceResponse<Page> response = confluenceClient.getSpaceRootPages((int) lastStart, (int) defaultSize, space, pageType);
//        final ConfluenceResponse<Page> response = confluenceClient.getPages(
//            (int) lastStart, (int) defaultSize, space, pageType);

        if (response != null) {
          int count = 0;
          for (final Page page : response.getResults()) {

            activities.addSeedDocument(page.getId());
            if (confluenceSpec.isProcessAttachments()) {
              processSeedAttachments(page, activities);
            }
            count++;
          }
          if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug(new MessageFormat("Fetched and added {0} seed documents", Locale.ROOT).format(new Object[] { new Integer(count) }));
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

    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
    }

  }

  /**
   * <p>
   * Process seed attachments for the given page
   * </p>
   *
   * @param page
   * @param activities
   */
  private void processSeedAttachments(final Page page, final ISeedingActivity activities) throws ManifoldCFException, ServiceInterruption {
    long lastStart = 0;
    final long defaultSize = 50;

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug(new MessageFormat("Processing page {} attachments starting from {} and size {}", Locale.ROOT).format(new Object[] { page.getId(), lastStart, defaultSize }));
    }

    try {
      Boolean isLast = true;
      do {
        final ConfluenceResponse<Attachment> response = confluenceClient.getPageAttachments(page.getId(), (int) lastStart, (int) defaultSize);

        if (response != null) {
          int count = 0;
          for (final Page resultPage : response.getResults()) {
            activities.addSeedDocument(ConfluenceUtil.generateRepositoryDocumentIdentifier(resultPage.getId(), page.getId()));
            count++;
          }

          if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
            Logging.connectors.debug(new MessageFormat("Fetched and added {} seed document attachments for page {}", Locale.ROOT).format(new Object[] { new Integer(count), page.getId() }));
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

    } catch (final Exception e) {
      handleConfluenceDownException(e, "seeding");
    }
  }

  protected static void handleConfluenceDownException(final Exception e, final String context) throws ManifoldCFException, ServiceInterruption {
    final long currentTime = System.currentTimeMillis();

    // Server doesn't appear to by up. Try for a brief time then give up.
    final String message = "Server appears down during " + context + ": " + e.getMessage();
    Logging.connectors.warn(message, e);
    throw new ServiceInterruption(message, e, currentTime + interruptionRetryTime, -1L, 3, true);
  }

  /**
   * Handle page exception : retry 3rd times with a 5 minutes interval without aborting job in case of failure
   *
   * @param e
   * @param context The error context (ex: 'page processing')
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
  protected void handlePageException(final Exception e, final String context) throws ManifoldCFException, ServiceInterruption {
    final long currentTime = System.currentTimeMillis();

    // Server doesn't appear to by up. Try for a brief time then give up.
    final String message = "Server appears down during " + context + ": " + e.getMessage();
    Logging.connectors.warn(message, e);
    throw new ServiceInterruption(message, e, currentTime + retryInterval, -1L, retryNumber, false);
  }

  /*
   * Process documents
   *
   * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector# processDocuments(java.lang.String[], java.lang.String[], org.apache.manifoldcf.crawler.interfaces.IProcessActivity,
   * org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, boolean[])
   */
  @Override
  public void processDocuments(final String[] documentIdentifiers, final IExistingVersions statuses, final Specification spec, final IProcessActivity activities, final int jobMode,
      final boolean usesDefaultAuthority) throws ManifoldCFException, ServiceInterruption {

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("Process Confluence documents: Inside processDocuments");
    }

    final ConfluenceSpecification confluenceSpecification = ConfluenceSpecification.from(spec);

    final boolean activeSecurity = confluenceSpecification.isSecurityActive();

    for (int i = 0; i < documentIdentifiers.length; i++) {
      final String documentIdentifier = documentIdentifiers[i];
      LOGGER.log(DOCPROCESSLEVEL, "DOC_PROCESS_START|CONFLUENCE|{}", documentIdentifier);
      String pageId = documentIdentifier;
      final String version = statuses.getIndexedVersionString(documentIdentifier);
      final List<String> parentRestrictions = new ArrayList<>();
      if (pageId.startsWith(CHILD_PREFIX)) {
        final JSONParser parser = new JSONParser();
        try {
          final JSONObject child = (JSONObject) parser.parse(new StringReader(pageId.substring(CHILD_PREFIX.length())));
          pageId = child.get("id").toString();
          final JSONArray arrParentRestrictions = (JSONArray) child.get("parentRestricions");
          arrParentRestrictions.forEach(pr -> parentRestrictions.add(pr.toString()));
          parentRestrictions.sort(String::compareToIgnoreCase);
        } catch (IOException | ParseException e) {
          handleException(e);
        }
      }

      final long startTime = System.currentTimeMillis();
      long fileSize = 0L;
      final String errorCode = "OK";
      final String errorDesc = StringUtils.EMPTY;
      ProcessResult pResult = null;
      final boolean doLog = true;

      try {
        if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug("Confluence: Processing document identifier '" + pageId + "'");
        }

        /* Ensure Confluence client is connected */
        if (!isConnected()) {
          initConfluenceClient();
        }

        if (ConfluenceUtil.isAttachment(pageId)) {
          pResult = processPageAsAttachment(activeSecurity, documentIdentifier, parentRestrictions, pageId, version, activities, doLog);
        } else {
          pResult = processPage(activeSecurity, documentIdentifier, parentRestrictions, pageId, version, activities, doLog, Maps.<String, String>newHashMap());
        }
      } catch (final IOException ioe) {
        handleIOException(ioe);
      } catch (final Exception e) {
        handleException(e);
      }

      finally {
        if (doLog) {
          if (pResult != null && pResult.errorCode != null && !pResult.errorCode.isEmpty()) {
            activities.recordActivity(new Long(startTime), ACTIVITY_READ, pResult.fileSize, pageId, pResult.errorCode, pResult.errorDescription, null);
          } else {
            if (pResult != null) {
              fileSize = pResult.fileSize;
            }
            activities.recordActivity(new Long(startTime), ACTIVITY_READ, fileSize, pageId, errorCode, errorDesc, null);
          }
        }
      }

    }
  }

  /**
   * <p>
   * Process the specific page
   * </p>
   *
   * @param activeSecurity     Security enabled/disabled
   * @param documentIdentifier The original documentIdentifier
   * @param parentRestrictions The list of parent restrictions
   * @param pageId             The pageId being an attachment
   * @param version            The version of the page
   * @param activities
   * @param doLog
   *
   * @throws ManifoldCFException
   * @throws IOException
   * @throws ServiceInterruption
   */
  private ProcessResult processPage(final boolean activeSecurity, final String documentIdentifier, final List<String> parentRestrictions, final String pageId, final String version,
      final IProcessActivity activities, final boolean doLog, final Map<String, String> extraProperties) throws ManifoldCFException, ServiceInterruption, IOException {
    Page page = new Page();
    try {
      page = confluenceClient.getPage(pageId);
    } catch (final Exception e) {
      handlePageException(e, "page processing");
    }
    if (page != null) {
      return processPageInternal(activeSecurity, parentRestrictions, page, documentIdentifier, version, activities, doLog, extraProperties);
    } else {
      return null;
    }

  }

  /**
   * <p>
   * Process the specific attachment
   * </p>
   *
   * @param activeSecurity     Security enabled/disabled
   * @param documentIdentifier The original documentIdentifier
   * @param parentRestrictions The list of parent restrictions
   * @param pageId             The pageId being an attachment
   * @param version            The version of the page
   * @param activities
   * @param doLog
   * @throws IOException
   * @throws ServiceInterruption
   */
  private ProcessResult processPageAsAttachment(final boolean activeSecurity, final String documentIdentifier, final List<String> parentRestrictions, final String pageId, final String version,
      final IProcessActivity activities, final boolean doLog) throws ManifoldCFException, ServiceInterruption, IOException {

    final String[] ids = ConfluenceUtil.getAttachmentAndPageId(pageId);
    Attachment attachment = new Attachment();
    try {
      attachment = confluenceClient.getAttachment(ids[0]);
    } catch (final Exception e) {
      handlePageException(e, "attachment processing");
    }
    final Map<String, String> extraProperties = Maps.newHashMap();
    extraProperties.put("attachedBy", ids[1]);
    return processPageInternal(activeSecurity, parentRestrictions, attachment, documentIdentifier, version, activities, doLog, extraProperties);

  }

  /**
   * <p>
   * Process the specific page
   * </p>
   *
   * @param activeSecurity             Security enabled/disabled
   * @param parentRestrictions         The list of parent restrictions
   * @param page                       The page to process
   * @param manifoldDocumentIdentifier
   * @param version                    The version of the page
   * @param activities
   * @param doLog
   *
   * @throws ManifoldCFException
   * @throws IOException
   * @throws ServiceInterruption
   */
  private ProcessResult processPageInternal(final boolean activeSecurity, final List<String> parentRestrictions, final Page page, final String manifoldDocumentIdentifier, final String version,
      final IProcessActivity activities, final boolean doLog, final Map<String, String> extraProperties) throws ManifoldCFException, ServiceInterruption, IOException {

    if (Logging.connectors != null && Logging.connectors.isDebugEnabled()) {
      Logging.connectors.debug("Confluence: This content exists: " + page.getId());
    }

    final RepositoryDocument rd = new RepositoryDocument();
    final Date createdDate = page.getCreatedDate();
    final Date lastModified = page.getLastModifiedDate();
    final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.ROOT);

    /*
     * Retain page in Manifold because it has not changed from last time This is needed to keep the identifier in Manifold data, because by default if a document is not retained nor ingested, it will
     * be deleted by the framework
     */
    final StringBuilder versionBuilder = new StringBuilder();
    versionBuilder.append(df.format(lastModified));
    final List<String> pageRestrictions = new ArrayList<String>();
    if (activeSecurity) {
      final List<Restrictions> restrictions = getPageReadRestrictions(page.getId());
      for (final Restrictions res : restrictions) {
        final ReadRestrictions rr = res.getReadRestrictions();
        rr.getUsers().forEach(user -> {
          pageRestrictions.add("user-" + user.getUserKey());
        });
        rr.getGroups().forEach(group -> {
          pageRestrictions.add("group-" + group.getName());
        });
      }
    }
    // Order the page restrictions alphabetically so the version will be always the same in case the same restrictions between two crawls are
    // not retrieved in the same order
    pageRestrictions.sort(String::compareToIgnoreCase);
    versionBuilder.append("+");
    packList(versionBuilder, pageRestrictions, '+');
    versionBuilder.append("+");
    packList(versionBuilder, parentRestrictions, '+');
    final String lastVersion = versionBuilder.toString();

    // Get and reference page direct childs if any
    if (page.getType() == PageType.PAGE) {
      final List<Page> pageChilds = getPageChilds(page.getId());
      for (final Page childPage : pageChilds) {
        final JSONObject child = new JSONObject();
        child.put("id", childPage.getId());
        final List<String> childParentRestrictions = new ArrayList<>();
        // MCF only manage one level of parent ACLs, so if the current page has restrictions they must replace the current parent restrictions for
        // its child pages
        if (activeSecurity) {
          if (pageRestrictions.isEmpty()) {
            childParentRestrictions.addAll(parentRestrictions);
          } else {
            childParentRestrictions.addAll(pageRestrictions);
          }
        }
        childParentRestrictions.sort(String::compareToIgnoreCase);
        child.put("parentRestricions", childParentRestrictions);
        activities.addDocumentReference(CHILD_PREFIX + child.toJSONString());
      }
    }

    if (!activities.checkDocumentNeedsReindexing(manifoldDocumentIdentifier, lastVersion)) {
      return new ProcessResult(page.getLength(), "RETAINED", "");
    }

    if (!activities.checkLengthIndexable(page.getLength())) {
      activities.noDocument(manifoldDocumentIdentifier, lastVersion);
      final String errorCode = IProcessActivity.EXCLUDED_LENGTH;
      final String errorDesc = "Excluding document because of length (" + page.getLength() + ")";
      return new ProcessResult(page.getLength(), errorCode, errorDesc);
    }

    if (!activities.checkMimeTypeIndexable(page.getMediaType())) {
      activities.noDocument(manifoldDocumentIdentifier, lastVersion);
      final String errorCode = IProcessActivity.EXCLUDED_MIMETYPE;
      final String errorDesc = "Excluding document because of mime type (" + page.getMediaType() + ")";
      return new ProcessResult(page.getLength(), errorCode, errorDesc);
    }

    if (!activities.checkDateIndexable(lastModified)) {
      activities.noDocument(manifoldDocumentIdentifier, lastVersion);
      final String errorCode = IProcessActivity.EXCLUDED_DATE;
      final String errorDesc = "Excluding document because of date (" + lastModified + ")";
      return new ProcessResult(page.getLength(), errorCode, errorDesc);
    }

    if (!activities.checkURLIndexable(page.getWebUrl())) {
      activities.noDocument(manifoldDocumentIdentifier, lastVersion);
      final String errorCode = IProcessActivity.EXCLUDED_URL;
      final String errorDesc = "Excluding document because of URL ('" + page.getWebUrl() + "')";
      return new ProcessResult(page.getLength(), errorCode, errorDesc);
    }

    /* Add repository document information */
    rd.setMimeType(page.getMediaType());
    if (createdDate != null) {
      rd.setCreatedDate(createdDate);
    }
    if (lastModified != null) {
      rd.setModifiedDate(lastModified);
    }
    rd.setIndexingDate(new Date());

    /* Adding Page Metadata */
    final Map<String, Object> pageMetadata = page.getMetadataAsMap();
    for (final Entry<String, Object> entry : pageMetadata.entrySet()) {
      if (entry.getValue() instanceof List) {
        final List<?> list = (List<?>) entry.getValue();
        rd.addField(entry.getKey(), list.toArray(new String[list.size()]));
      } else if (entry.getValue() != null) {
        final String key = entry.getKey();
        final String value = entry.getValue().toString();
        rd.addField(key, value);
        if (key.toLowerCase(Locale.ROOT).contentEquals("title")) {
          rd.addField("stream_name", value);
        }
      }
    }
    rd.addField("source", "confluence");

    /* Adding extra properties */
    for (final Entry<String, String> entry : extraProperties.entrySet()) {
      rd.addField(entry.getKey(), entry.getValue());
    }

    final String documentURI = page.getWebUrl();

    /* Set repository document ACLs */
    if (activeSecurity) {

      rd.setSecurity(RepositoryDocument.SECURITY_TYPE_SHARE, new String[] { "space-" + page.getSpace() }, new String[] { defaultAuthorityDenyToken });

      if (parentRestrictions.size() > 0) {
        rd.setSecurity(RepositoryDocument.SECURITY_TYPE_PARENT, parentRestrictions.toArray(new String[0]), new String[] { defaultAuthorityDenyToken });
      }

      if (pageRestrictions.size() > 0) {
        rd.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT, pageRestrictions.toArray(new String[0]), new String[] { defaultAuthorityDenyToken });
      }

    }

    rd.setBinary(page.getContentStream(), page.getLength());
    rd.addField("size", String.valueOf(page.getLength()));
    rd.addField("url", documentURI);

    /* Ingest document */
    activities.ingestDocumentWithException(manifoldDocumentIdentifier, lastVersion, documentURI, rd);

    return new ProcessResult(page.getLength(), null, null);
  }

  /**
   * <p>
   * Handles IO Exception to manage whether the exception is an interruption so that the process needs to be executed again later on
   * </p>
   *
   * @param e The Exception
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
  private void handleIOException(final IOException e) throws ManifoldCFException, ServiceInterruption {
    if (!(e instanceof java.net.SocketTimeoutException) && e instanceof InterruptedIOException) {
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e, ManifoldCFException.INTERRUPTED);
    }
    Logging.connectors.warn("IO exception: " + e.getMessage(), e);
    final long currentTime = System.currentTimeMillis();
    throw new ServiceInterruption("IO exception: " + e.getMessage(), e, currentTime + retryInterval, currentTime + 3 * 60 * 60000L, -1, false);
  }

  /**
   * <p>
   * Handles general exceptions
   * </p>
   *
   * @param e The Exception
   * @throws ServiceInterruption
   * @throws ManifoldCFException
   */
  private static void handleException(final Exception e) throws ServiceInterruption, ManifoldCFException {
    if (!(e instanceof ServiceInterruption)) {
      Logging.connectors.warn("Exception: " + e.getMessage(), e);
      throw new ManifoldCFException("Exception: " + e.getMessage(), e, ManifoldCFException.REPOSITORY_CONNECTION_ERROR);
    } else {
      throw (ServiceInterruption) e;
    }
  }

  private class ProcessResult {
    private final long fileSize;
    private final String errorCode;
    private final String errorDescription;

    private ProcessResult(final long fileSize, final String errorCode, final String errorDescription) {
      this.fileSize = fileSize;
      this.errorCode = errorCode;
      this.errorDescription = errorDescription;
    }
  }

  /**
   * <p>
   * Internal private class used to parse and keep the specification configuration in object format
   * </p>
   *
   * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
   *
   */
  private static class ConfluenceSpecification {
    private List<String> spaces;
    private Boolean activateSecurity = true;
    private Boolean processAttachments = false;
    private String pageType = null;

    public Boolean isSecurityActive() {
      return this.activateSecurity;
    }

    /**
     * <p>
     * Returns if attachments should be processed
     * </p>
     *
     * @return a {@code Boolean} indicating if the attachments should be processed or not
     */
    public Boolean isProcessAttachments() {
      return this.processAttachments;
    }

    /**
     * <p>
     * Returns the list of configured spaces or an empty list meaning that all spaces should be processed
     * </p>
     *
     * @return a {@code List<String>} of configured spaces
     */
    public List<String> getSpaces() {
      return this.spaces;
    }

    /**
     * <p>
     * Returns configured page type
     * </p>
     *
     * @return a {@code String} of configured page type
     */
    public String getPageType() {
      if (this.pageType == null || this.pageType.isEmpty()) {
        return "page";
      }

      return this.pageType;
    }

    public static ConfluenceSpecification from(final Specification spec) {
      final ConfluenceSpecification cs = new ConfluenceSpecification();
      cs.spaces = Lists.newArrayList();
      for (int i = 0, len = spec.getChildCount(); i < len; i++) {
        final SpecificationNode sn = spec.getChild(i);
        if (sn.getType().equals(ConfluenceConfiguration.Specification.SPACES)) {
          for (int j = 0, sLen = sn.getChildCount(); j < sLen; j++) {
            final SpecificationNode specNode = sn.getChild(j);
            if (specNode.getType().equals(ConfluenceConfiguration.Specification.SPACE)) {
              cs.spaces.add(specNode.getAttributeValue(ConfluenceConfiguration.Specification.SPACE_KEY_ATTRIBUTE));

            }
          }

        } else if (sn.getType().equals(ConfluenceConfiguration.Specification.PAGES)) {
          final String s = sn.getAttributeValue(ConfluenceConfiguration.Specification.PROCESS_ATTACHMENTS_ATTRIBUTE_KEY);
          cs.processAttachments = Boolean.valueOf(s);
          cs.pageType = sn.getAttributeValue(ConfluenceConfiguration.Specification.PAGETYPE);
        } else if (sn.getType().equals(ConfluenceConfiguration.Specification.SECURITY)) {
          final String s = sn.getAttributeValue(ConfluenceConfiguration.Specification.ACTIVATE_SECURITY_ATTRIBUTE_KEY);
          cs.activateSecurity = Boolean.valueOf(s);
        }
      }

      return cs;

    }
  }

}
