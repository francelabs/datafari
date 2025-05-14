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

package org.apache.manifoldcf.authorities.authorities.jira;

import org.apache.manifoldcf.core.common.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import org.apache.manifoldcf.authorities.system.Logging;
import org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPasswordMapperActivity;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import java.util.Map.Entry;

/** Jira Authority Connector.  This connector verifies user existence against Jira.
 */
public class JiraAuthorityConnector extends BaseAuthorityConnector {

  // Configuration tabs
  private static final String JIRA_SERVER_TAB_PROPERTY = "JiraAuthorityConnector.Server";
  private static final String JIRA_PROXY_TAB_PROPERTY = "JiraAuthorityConnector.Proxy";
  
  // Template names for configuration
  /**
   * Forward to the javascript to check the configuration parameters
   */
  private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration_jira.js";
  /**
   * Server tab template
   */
  private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_jira_server.html";
  /**
   * Proxy tab template
   */
  private static final String EDIT_CONFIG_FORWARD_PROXY = "editConfiguration_jira_proxy.html";
  
  /**
   * Forward to the HTML template to view the configuration parameters
   */
  private static final String VIEW_CONFIG_FORWARD = "viewConfiguration_jira.html";
   
  // Session data
  protected JiraSession session = null;
  protected long lastSessionFetch = -1L;
  protected static final long timeToRelease = 300000L;
  
  // Parameter data
  protected String jiraprotocol = null;
  protected String jirahost = null;
  protected String jiraport = null;
  protected String jirapath = null;
  protected String clientid = null;
  protected String clientsecret = null;

  protected String jiraproxyhost = null;
  protected String jiraproxyport = null;
  protected String jiraproxydomain = null;
  protected String jiraproxyusername = null;
  protected String jiraproxypassword = null;
  
  public JiraAuthorityConnector() {
    super();
  }

  /**
   * Close the connection. Call this before discarding the connection.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    if (session != null) {
      session.close();
      session = null;
      lastSessionFetch = -1L;
    }

    jiraprotocol = null;
    jirahost = null;
    jiraport = null;
    jirapath = null;
    clientid = null;
    clientsecret = null;
    
    jiraproxyhost = null;
    jiraproxyport = null;
    jiraproxydomain = null;
    jiraproxyusername = null;
    jiraproxypassword = null;
  }

  /**
   * This method create a new JIRA session for a JIRA
   * repository, if the repositoryId is not provided in the configuration, the
   * connector will retrieve all the repositories exposed for this endpoint
   * the it will start to use the first one.
   *
   * @param configParams is the set of configuration parameters, which in
   * this case describe the target appliance, basic auth configuration, etc.
   * (This formerly came out of the ini file.)
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);

    jiraprotocol = params.getParameter(JiraConfig.JIRA_PROTOCOL_PARAM);
    jirahost = params.getParameter(JiraConfig.JIRA_HOST_PARAM);
    jiraport = params.getParameter(JiraConfig.JIRA_PORT_PARAM);
    jirapath = params.getParameter(JiraConfig.JIRA_PATH_PARAM);
    clientid = params.getParameter(JiraConfig.CLIENT_ID_PARAM);
    clientsecret = params.getObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM);
    
    jiraproxyhost = params.getParameter(JiraConfig.JIRA_PROXYHOST_PARAM);
    jiraproxyport = params.getParameter(JiraConfig.JIRA_PROXYPORT_PARAM);
    jiraproxydomain = params.getParameter(JiraConfig.JIRA_PROXYDOMAIN_PARAM);
    jiraproxyusername = params.getParameter(JiraConfig.JIRA_PROXYUSERNAME_PARAM);
    jiraproxypassword = params.getObfuscatedParameter(JiraConfig.JIRA_PROXYPASSWORD_PARAM);
    
  }

  /**
   * Test the connection. Returns a string describing the connection
   * integrity.
   *
   * @return the connection's status as a displayable string.
   */
  @Override
  public String check() throws ManifoldCFException {
    try {
      checkConnection();
      return super.check();
    } catch (ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    }
  }


  /**
   * Set up a session
   */
  protected JiraSession getSession() throws ManifoldCFException {
    if (session == null) {
      // Check for parameter validity

      if (StringUtils.isEmpty(jiraprotocol)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_PROTOCOL_PARAM
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: jiraprotocol = '" + jiraprotocol + "'");
      }

      if (StringUtils.isEmpty(jirahost)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_HOST_PARAM
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: jirahost = '" + jirahost + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: jiraport = '" + jiraport + "'");
      }

      if (StringUtils.isEmpty(jirapath)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_PATH_PARAM
            + " required but not set");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: jirapath = '" + jirapath + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: Clientid = '" + clientid + "'");
      }

      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("JIRA: Clientsecret = '" + clientsecret + "'");
      }

      int portInt;
      if (jiraport != null && jiraport.length() > 0)
      {
        try
        {
          portInt = Integer.parseInt(jiraport);
        }
        catch (NumberFormatException e)
        {
          throw new ManifoldCFException("Bad number: "+e.getMessage(),e);
        }
      }
      else
      {
        if (jiraprotocol.toLowerCase(Locale.ROOT).equals("http"))
          portInt = 80;
        else
          portInt = 443;
      }

      int proxyPortInt;
      if (jiraproxyport != null && jiraproxyport.length() > 0)
      {
        try
        {
          proxyPortInt = Integer.parseInt(jiraproxyport);
        }
        catch (NumberFormatException e)
        {
          throw new ManifoldCFException("Bad number: "+e.getMessage(),e);
        }
      }
      else
        proxyPortInt = 8080;

      session = new JiraSession(clientid, clientsecret,
        jiraprotocol, jirahost, portInt, jirapath,
        jiraproxyhost, proxyPortInt, jiraproxydomain, jiraproxyusername, jiraproxypassword);

    }
    lastSessionFetch = System.currentTimeMillis();
    return session;
  }

  /** This method is called to assess whether to count this connector instance should
  * actually be counted as being connected.
  *@return true if the connector instance is actually connected.
  */
  @Override
  public boolean isConnected()
  {
    return session != null;
  }

  @Override
  public void poll() throws ManifoldCFException {
    if (lastSessionFetch == -1L) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime >= lastSessionFetch + timeToRelease) {
      session.close();
      session = null;
      lastSessionFetch = -1L;
    }
  }

  /**
   * Fill in a Server tab configuration parameter map for calling a Velocity
   * template.
   *
   * @param newMap is the map to fill in
   * @param parameters is the current set of configuration parameters
   */
  private static void fillInServerConfigurationMap(Map<String, Object> newMap, IPasswordMapperActivity mapper, ConfigParams parameters) {
    String jiraprotocol = parameters.getParameter(JiraConfig.JIRA_PROTOCOL_PARAM);
    String jirahost = parameters.getParameter(JiraConfig.JIRA_HOST_PARAM);
    String jiraport = parameters.getParameter(JiraConfig.JIRA_PORT_PARAM);
    String jirapath = parameters.getParameter(JiraConfig.JIRA_PATH_PARAM);
    String clientid = parameters.getParameter(JiraConfig.CLIENT_ID_PARAM);
    String clientsecret = parameters.getObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM);

    if (jiraprotocol == null)
      jiraprotocol = JiraConfig.JIRA_PROTOCOL_DEFAULT;
    if (jirahost == null)
      jirahost = JiraConfig.JIRA_HOST_DEFAULT;
    if (jiraport == null)
      jiraport = JiraConfig.JIRA_PORT_DEFAULT;
    if (jirapath == null)
      jirapath = JiraConfig.JIRA_PATH_DEFAULT;
    
    if (clientid == null)
      clientid = JiraConfig.CLIENT_ID_DEFAULT;
    if (clientsecret == null)
      clientsecret = JiraConfig.CLIENT_SECRET_DEFAULT;
    else
      clientsecret = mapper.mapPasswordToKey(clientsecret);

    newMap.put("JIRAPROTOCOL", jiraprotocol);
    newMap.put("JIRAHOST", jirahost);
    newMap.put("JIRAPORT", jiraport);
    newMap.put("JIRAPATH", jirapath);
    newMap.put("CLIENTID", clientid);
    newMap.put("CLIENTSECRET", clientsecret);
  }

  /**
   * Fill in a Proxy tab configuration parameter map for calling a Velocity
   * template.
   *
   * @param newMap is the map to fill in
   * @param parameters is the current set of configuration parameters
   */
  private static void fillInProxyConfigurationMap(Map<String, Object> newMap, IPasswordMapperActivity mapper, ConfigParams parameters) {
    String jiraproxyhost = parameters.getParameter(JiraConfig.JIRA_PROXYHOST_PARAM);
    String jiraproxyport = parameters.getParameter(JiraConfig.JIRA_PROXYPORT_PARAM);
    String jiraproxydomain = parameters.getParameter(JiraConfig.JIRA_PROXYDOMAIN_PARAM);
    String jiraproxyusername = parameters.getParameter(JiraConfig.JIRA_PROXYUSERNAME_PARAM);
    String jiraproxypassword = parameters.getObfuscatedParameter(JiraConfig.JIRA_PROXYPASSWORD_PARAM);

    if (jiraproxyhost == null)
      jiraproxyhost = JiraConfig.JIRA_PROXYHOST_DEFAULT;
    if (jiraproxyport == null)
      jiraproxyport = JiraConfig.JIRA_PROXYPORT_DEFAULT;

    if (jiraproxydomain == null)
      jiraproxydomain = JiraConfig.JIRA_PROXYDOMAIN_DEFAULT;
    if (jiraproxyusername == null)
      jiraproxyusername = JiraConfig.JIRA_PROXYUSERNAME_DEFAULT;
    if (jiraproxypassword == null)
      jiraproxypassword = JiraConfig.JIRA_PROXYPASSWORD_DEFAULT;
    else
      jiraproxypassword = mapper.mapPasswordToKey(jiraproxypassword);

    newMap.put("JIRAPROXYHOST", jiraproxyhost);
    newMap.put("JIRAPROXYPORT", jiraproxyport);
    newMap.put("JIRAPROXYDOMAIN", jiraproxydomain);
    newMap.put("JIRAPROXYUSERNAME", jiraproxyusername);
    newMap.put("JIRAPROXYPASSWORD", jiraproxypassword);
  }

  /**
   * View configuration. This method is called in the body section of the
   * connector's view configuration page. Its purpose is to present the
   * connection information to the user. The coder can presume that the HTML
   * that is output from this configuration will be within appropriate &lt;html&gt;
   * and &lt;body&gt; tags.
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
      Locale locale, ConfigParams parameters) throws ManifoldCFException, IOException {
    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in map from each tab
    fillInServerConfigurationMap(paramMap, out, parameters);
    fillInProxyConfigurationMap(paramMap, out, parameters);

    Messages.outputResourceWithVelocity(out,locale,VIEW_CONFIG_FORWARD,paramMap);
  }

  /**
   *
   * Output the configuration header section. This method is called in the
   * head section of the connector's configuration page. Its purpose is to add
   * the required tabs to the list, and to output any javascript methods that
   * might be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @param tabsArray is an array of tab names. Add to this array any tab
   * names that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters, List<String> tabsArray)
      throws ManifoldCFException, IOException {
    // Add the Server tab
    tabsArray.add(Messages.getString(locale, JIRA_SERVER_TAB_PROPERTY));
    // Add the Proxy tab
    tabsArray.add(Messages.getString(locale, JIRA_PROXY_TAB_PROPERTY));
    // Map the parameters
    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Fill in the parameters from each tab
    fillInServerConfigurationMap(paramMap, out, parameters);
    fillInProxyConfigurationMap(paramMap, out, parameters);
        
    // Output the Javascript - only one Velocity template for all tabs
    Messages.outputResourceWithVelocity(out,locale,EDIT_CONFIG_HEADER_FORWARD,paramMap);
  }

  @Override
  public void outputConfigurationBody(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
      throws ManifoldCFException, IOException {


    // Call the Velocity templates for each tab
    Map<String, Object> paramMap = new HashMap<String, Object>();
    // Set the tab name
    paramMap.put("TabName", tabName);

    // Fill in the parameters
    fillInServerConfigurationMap(paramMap, out, parameters);
    fillInProxyConfigurationMap(paramMap, out, parameters);
        
    // Server tab
    Messages.outputResourceWithVelocity(out,locale,EDIT_CONFIG_FORWARD_SERVER,paramMap);
    // Proxy tab
    Messages.outputResourceWithVelocity(out,locale,EDIT_CONFIG_FORWARD_PROXY,paramMap);

  }

  /**
   * Process a configuration post. This method is called at the start of the
   * connector's configuration page, whenever there is a possibility that form
   * data for a connection has been posted. Its purpose is to gather form
   * information and modify the configuration parameters accordingly. The name
   * of the posted form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param variableContext is the set of variables available from the post,
   * including binary file post information.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @return null if all is well, or a string error message if there is an
   * error that should prevent saving of the connection (and cause a
   * redirection to an error page).
   *
   */
  @Override
  public String processConfigurationPost(IThreadContext threadContext,
    IPostParameters variableContext, ConfigParams parameters)
    throws ManifoldCFException {

    // Server tab parameters

    String jiraprotocol = variableContext.getParameter("jiraprotocol");
    if (jiraprotocol != null)
      parameters.setParameter(JiraConfig.JIRA_PROTOCOL_PARAM, jiraprotocol);

    String jirahost = variableContext.getParameter("jirahost");
    if (jirahost != null)
      parameters.setParameter(JiraConfig.JIRA_HOST_PARAM, jirahost);

    String jiraport = variableContext.getParameter("jiraport");
    if (jiraport != null)
      parameters.setParameter(JiraConfig.JIRA_PORT_PARAM, jiraport);

    String jirapath = variableContext.getParameter("jirapath");
    if (jirapath != null)
      parameters.setParameter(JiraConfig.JIRA_PATH_PARAM, jirapath);

    String clientid = variableContext.getParameter("clientid");
    if (clientid != null)
      parameters.setParameter(JiraConfig.CLIENT_ID_PARAM, clientid);

    String clientsecret = variableContext.getParameter("clientsecret");
    if (clientsecret != null)
      parameters.setObfuscatedParameter(JiraConfig.CLIENT_SECRET_PARAM, variableContext.mapKeyToPassword(clientsecret));

    // Proxy tab parameters
    
    String jiraproxyhost = variableContext.getParameter("jiraproxyhost");
    if (jiraproxyhost != null)
      parameters.setParameter(JiraConfig.JIRA_PROXYHOST_PARAM, jiraproxyhost);

    String jiraproxyport = variableContext.getParameter("jiraproxyport");
    if (jiraproxyport != null)
      parameters.setParameter(JiraConfig.JIRA_PROXYPORT_PARAM, jiraproxyport);
    
    String jiraproxydomain = variableContext.getParameter("jiraproxydomain");
    if (jiraproxydomain != null)
      parameters.setParameter(JiraConfig.JIRA_PROXYDOMAIN_PARAM, jiraproxydomain);

    String jiraproxyusername = variableContext.getParameter("jiraproxyusername");
    if (jiraproxyusername != null)
      parameters.setParameter(JiraConfig.JIRA_PROXYUSERNAME_PARAM, jiraproxyusername);

    String jiraproxypassword = variableContext.getParameter("jiraproxypassword");
    if (jiraproxypassword != null)
      parameters.setObfuscatedParameter(JiraConfig.JIRA_PROXYPASSWORD_PARAM, variableContext.mapKeyToPassword(jiraproxypassword));

    return null;
  }

  /** Obtain the access tokens for a given Active Directory user name.
  *@param userName is the user name or identifier.
  *@return the response tokens (according to the current authority).
  * (Should throws an exception only when a condition cannot be properly described within the authorization response object.)
  */
  @Override
  public AuthorizationResponse getAuthorizationResponse(String userName)
    throws ManifoldCFException {
    if (checkUserExists(userName))
      return new AuthorizationResponse(new String[]{userName},AuthorizationResponse.RESPONSE_OK);
    return RESPONSE_USERNOTFOUND;
  }

  /** Obtain the default access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the default response tokens, presuming that the connect method fails.
  */
  @Override
  public AuthorizationResponse getDefaultAuthorizationResponse(String userName) {
    return RESPONSE_UNREACHABLE;
  }

  private static void handleIOException(IOException e)
    throws ManifoldCFException {
    if (!(e instanceof java.net.SocketTimeoutException) && (e instanceof InterruptedIOException)) {
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    }
    Logging.authorityConnectors.warn("JIRA: IO exception: "+e.getMessage(), e);
    throw new ManifoldCFException("IO exception: "+e.getMessage(), e);
  }

  private static void handleResponseException(ResponseException e)
    throws ManifoldCFException {
    throw new ManifoldCFException("Response exception: "+e.getMessage(),e);
  }
  
  // Background threads

  protected static class CheckUserExistsThread extends Thread {
    protected final JiraSession session;
    protected final String userName;
    protected Throwable exception = null;
    protected boolean result = false;

    public CheckUserExistsThread(JiraSession session, String userName) {
      super();
      this.session = session;
      this.userName = userName;
      setDaemon(true);
    }

    public void run() {
      try {
        result = session.checkUserExists(userName);
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public void finishUp()
      throws InterruptedException, IOException, ResponseException {
      join();
      Throwable thr = exception;
      if (thr != null) {
        if (thr instanceof IOException) {
          throw (IOException) thr;
        } else if (thr instanceof ResponseException) {
          throw (ResponseException) thr;
        } else if (thr instanceof RuntimeException) {
          throw (RuntimeException) thr;
        } else {
          throw (Error) thr;
        }
      }
    }
    
    public boolean getResult() {
      return result;
    }
    
  }
  
  protected boolean checkUserExists(String userName) throws ManifoldCFException {
    CheckUserExistsThread t = new CheckUserExistsThread(getSession(), userName);
    try {
      t.start();
      t.finishUp();
      return t.getResult();
    } catch (InterruptedException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (java.net.SocketTimeoutException e) {
      handleIOException(e);
    } catch (InterruptedIOException e) {
      t.interrupt();
      handleIOException(e);
    } catch (IOException e) {
      handleIOException(e);
    } catch (ResponseException e) {
      handleResponseException(e);
    }
    return false;
  }

  protected static class CheckConnectionThread extends Thread {

    protected final JiraSession session;
    protected Throwable exception = null;

    public CheckConnectionThread(JiraSession session) {
      super();
      this.session = session;
      setDaemon(true);
    }

    public void run() {
      try {
        session.getRepositoryInfo();
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public void finishUp()
      throws InterruptedException, IOException, ResponseException {
      join();
      Throwable thr = exception;
      if (thr != null) {
        if (thr instanceof IOException) {
          throw (IOException) thr;
        } else if (thr instanceof ResponseException) {
          throw (ResponseException) thr;
        } else if (thr instanceof RuntimeException) {
          throw (RuntimeException) thr;
        } else {
          throw (Error) thr;
        }
      }
    }
  }

  protected void checkConnection() throws ManifoldCFException {
    CheckConnectionThread t = new CheckConnectionThread(getSession());
    try {
      t.start();
      t.finishUp();
      return;
    } catch (InterruptedException e) {
      t.interrupt();
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    } catch (java.net.SocketTimeoutException e) {
      handleIOException(e);
    } catch (InterruptedIOException e) {
      t.interrupt();
      handleIOException(e);
    } catch (IOException e) {
      handleIOException(e);
    } catch (ResponseException e) {
      handleResponseException(e);
    }
  }

}

