/* $Id: JiraRepositoryConnector.java 1490585 2013-06-07 11:13:35Z kwright $ */

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

package org.apache.manifoldcf.crawler.connectors.jira;

import java.io.ByteArrayInputStream;
import org.apache.manifoldcf.core.common.*;
import org.apache.manifoldcf.connectorcommon.common.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;

import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPasswordMapperActivity;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;

import java.util.Map.Entry;

/**
 *
 * @author andrew
 */
public class JiraRepositoryConnector extends BaseRepositoryConnector {

  protected final static String ACTIVITY_READ = "read document";
  
  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = GLOBAL_DENY_TOKEN;

  // Nodes
  private static final String JOB_STARTPOINT_NODE_TYPE = "startpoint";
  private static final String JOB_QUERY_ATTRIBUTE = "query";
  private static final String JOB_SECURITY_NODE_TYPE = "security";
  private static final String JOB_VALUE_ATTRIBUTE = "value";
  private static final String JOB_ACCESS_NODE_TYPE = "access";
  private static final String JOB_TOKEN_ATTRIBUTE = "token";

  // Configuration tabs
  private static final String JIRA_SERVER_TAB_PROPERTY = "JiraRepositoryConnector.Server";
  private static final String JIRA_PROXY_TAB_PROPERTY = "JiraRepositoryConnector.Proxy";

  // Specification tabs
  private static final String JIRA_QUERY_TAB_PROPERTY = "JiraRepositoryConnector.JiraQuery";
  private static final String JIRA_SECURITY_TAB_PROPERTY = "JiraRepositoryConnector.Security";
  
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
   
  // Template names for specification
  /**
   * Forward to the javascript to check the specification parameters for the job
   */
  private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification_jira.js";
  /**
   * Forward to the template to edit the query for the job
   */
  private static final String EDIT_SPEC_FORWARD_JIRAQUERY = "editSpecification_jiraQuery.html";
  /**
   * Forward to the template to edit the security parameters for the job
   */
  private static final String EDIT_SPEC_FORWARD_SECURITY = "editSpecification_jiraSecurity.html";
  
  /**
   * Forward to the template to view the specification parameters for the job
   */
  private static final String VIEW_SPEC_FORWARD = "viewSpecification_jira.html";
  
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

  public JiraRepositoryConnector() {
    super();
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes
   * into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[]{ACTIVITY_READ};
  }

  /**
   * Get the bin name strings for a document identifier. The bin name
   * describes the queue to which the document will be assigned for throttling
   * purposes. Throttling controls the rate at which items in a given queue
   * are fetched; it does not say anything about the overall fetch rate, which
   * may operate on multiple queues or bins. For example, if you implement a
   * web crawler, a good choice of bin name would be the server name, since
   * that is likely to correspond to a real resource that will need real
   * throttle protection.
   *
   * @param documentIdentifier is the document identifier.
   * @return the set of bin names. If an empty array is returned, it is
   * equivalent to there being no request rate throttling available for this
   * identifier.
   */
  @Override
  public String[] getBinNames(String documentIdentifier) {
    return new String[]{jirahost};
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
    } catch (ServiceInterruption e) {
      return "Connection temporarily failed: " + e.getMessage();
    } catch (ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    }
  }


  /**
   * Set up a session
   */
  protected JiraSession getSession() throws ManifoldCFException, ServiceInterruption {
    if (session == null) {
      // Check for parameter validity

      if (StringUtils.isEmpty(jiraprotocol)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_PROTOCOL_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: jiraprotocol = '" + jiraprotocol + "'");
      }

      if (StringUtils.isEmpty(jirahost)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_HOST_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: jirahost = '" + jirahost + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: jiraport = '" + jiraport + "'");
      }

      if (StringUtils.isEmpty(jirapath)) {
        throw new ManifoldCFException("Parameter " + JiraConfig.JIRA_PATH_PARAM
            + " required but not set");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: jirapath = '" + jirapath + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: Clientid = '" + clientid + "'");
      }

      if (Logging.connectors.isDebugEnabled()) {
        Logging.connectors.debug("JIRA: Clientsecret = '" + clientsecret + "'");
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
   * Get the maximum number of documents to amalgamate together into one
   * batch, for this connector.
   *
   * @return the maximum number. 0 indicates "unlimited".
   */
  @Override
  public int getMaxDocumentRequest() {
    return 1;
  }

  /**
   * Return the list of relationship types that this connector recognizes.
   *
   * @return the list.
   */
  @Override
  public String[] getRelationshipTypes() {
    return new String[]{};
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

  /**
   * Fill in specification Velocity parameter map for JIRAQuery tab.
   */
  private static void fillInJIRAQuerySpecificationMap(Map<String, Object> newMap, Specification ds) {
    String JiraQuery = JiraConfig.JIRA_QUERY_DEFAULT;
    for (int i = 0; i < ds.getChildCount(); i++) {
      SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
        JiraQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
      }
    }
    newMap.put("JIRAQUERY", JiraQuery);
  }

  /**
   * Fill in specification Velocity parameter map for JIRASecurity tab.
   */
  private static void fillInJIRASecuritySpecificationMap(Map<String, Object> newMap, Specification ds) {
    List<Map<String,String>> accessTokenList = new ArrayList<Map<String,String>>();
    String securityValue = "on";
    for (int i = 0; i < ds.getChildCount(); i++) {
      SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
        String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
        Map<String,String> accessMap = new HashMap<String,String>();
        accessMap.put("TOKEN",token);
        accessTokenList.add(accessMap);
      } else if (sn.getType().equals(JOB_SECURITY_NODE_TYPE)) {
        securityValue = sn.getAttributeValue(JOB_VALUE_ATTRIBUTE);
      }
    }
    newMap.put("ACCESSTOKENS", accessTokenList);
    newMap.put("SECURITYON", securityValue);
  }

  /** View specification.
  * This method is called in the body section of a job's view page.  Its purpose is to present the document
  * specification information to the user.  The coder can presume that the HTML that is output from
  * this configuration will be within appropriate &lt;html&gt; and &lt;body&gt;tags.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the locale the output is preferred to be in.
  *@param ds is the current document specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  */
  @Override
  public void viewSpecification(IHTTPOutput out, Locale locale, Specification ds,
    int connectionSequenceNumber)
    throws ManifoldCFException, IOException {

    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));

    // Fill in the map with data from all tabs
    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);

    Messages.outputResourceWithVelocity(out,locale,VIEW_SPEC_FORWARD,paramMap);
  }

  /** Process a specification post.
  * This method is called at the start of job's edit or view page, whenever there is a possibility that form
  * data for a connection has been posted.  Its purpose is to gather form information and modify the
  * document specification accordingly.  The name of the posted form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param variableContext contains the post data, including binary file-upload information.
  *@param locale is the locale the output is preferred to be in.
  *@param ds is the current document specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of
  * the job (and cause a redirection to an error page).
  */
  @Override
  public String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification ds,
    int connectionSequenceNumber)
    throws ManifoldCFException {
    String seqPrefix = "s"+connectionSequenceNumber+"_";

    String jiraDriveQuery = variableContext.getParameter(seqPrefix+"jiraquery");
    if (jiraDriveQuery != null) {
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode oldNode = ds.getChild(i);
        if (oldNode.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
          ds.removeChild(i);
          break;
        }
        i++;
      }
      SpecificationNode node = new SpecificationNode(JOB_STARTPOINT_NODE_TYPE);
      node.setAttribute(JOB_QUERY_ATTRIBUTE, jiraDriveQuery);
      ds.addChild(ds.getChildCount(), node);
    }
    
    String securityOn = variableContext.getParameter(seqPrefix+"specsecurity");
    if (securityOn != null) {
      // Delete all security records first
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(JOB_SECURITY_NODE_TYPE))
          ds.removeChild(i);
        else
          i++;
      }
      SpecificationNode node = new SpecificationNode(JOB_SECURITY_NODE_TYPE);
      node.setAttribute(JOB_VALUE_ATTRIBUTE,securityOn);
      ds.addChild(ds.getChildCount(),node);
    }
    
    String xc = variableContext.getParameter(seqPrefix+"tokencount");
    if (xc != null) {
      // Delete all tokens first
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(JOB_ACCESS_NODE_TYPE))
          ds.removeChild(i);
        else
          i++;
      }

      int accessCount = Integer.parseInt(xc);
      i = 0;
      while (i < accessCount) {
        String accessDescription = "_"+Integer.toString(i);
        String accessOpName = seqPrefix+"accessop"+accessDescription;
        xc = variableContext.getParameter(accessOpName);
        if (xc != null && xc.equals("Delete")) {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        String accessSpec = variableContext.getParameter(seqPrefix+"spectoken"+accessDescription);
        SpecificationNode node = new SpecificationNode(JOB_ACCESS_NODE_TYPE);
        node.setAttribute(JOB_TOKEN_ATTRIBUTE,accessSpec);
        ds.addChild(ds.getChildCount(),node);
        i++;
      }

      String op = variableContext.getParameter(seqPrefix+"accessop");
      if (op != null && op.equals("Add"))
      {
        String accessspec = variableContext.getParameter(seqPrefix+"spectoken");
        SpecificationNode node = new SpecificationNode(JOB_ACCESS_NODE_TYPE);
        node.setAttribute(JOB_TOKEN_ATTRIBUTE,accessspec);
        ds.addChild(ds.getChildCount(),node);
      }
    }

    return null;
  }

  /** Output the specification body section.
  * This method is called in the body section of a job page which has selected a repository connection of the
  * current type.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate
  *  &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags.  The name of the form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the locale the output is preferred to be in.
  *@param ds is the current document specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@param actualSequenceNumber is the connection within the job that has currently been selected.
  *@param tabName is the current tab name.  (actualSequenceNumber, tabName) form a unique tuple within
  *  the job.
  */
  @Override
  public void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification ds,
    int connectionSequenceNumber, int actualSequenceNumber, String tabName)
    throws ManifoldCFException, IOException {

    // Output JIRAQuery tab
    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("TabName", tabName);
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));
    paramMap.put("SelectedNum", Integer.toString(actualSequenceNumber));

    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);
    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_FORWARD_JIRAQUERY,paramMap);
    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_FORWARD_SECURITY,paramMap);
  }

  /** Output the specification header section.
  * This method is called in the head section of a job page which has selected a repository connection of the
  * current type.  Its purpose is to add the required tabs to the list, and to output any javascript methods
  * that might be needed by the job editing HTML.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the locale the output is preferred to be in.
  *@param ds is the current document specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  @Override
  public void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification ds,
    int connectionSequenceNumber, List<String> tabsArray)
    throws ManifoldCFException, IOException {

    tabsArray.add(Messages.getString(locale, JIRA_QUERY_TAB_PROPERTY));
    tabsArray.add(Messages.getString(locale, JIRA_SECURITY_TAB_PROPERTY));

    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SeqNum", Integer.toString(connectionSequenceNumber));

    // Fill in the specification header map, using data from all tabs.
    fillInJIRAQuerySpecificationMap(paramMap, ds);
    fillInJIRASecuritySpecificationMap(paramMap, ds);

    Messages.outputResourceWithVelocity(out,locale,EDIT_SPEC_HEADER_FORWARD,paramMap);
  }

  /** Queue "seed" documents.  Seed documents are the starting places for crawling activity.  Documents
  * are seeded when this method calls appropriate methods in the passed in ISeedingActivity object.
  *
  * This method can choose to find repository changes that happen only during the specified time interval.
  * The seeds recorded by this method will be viewed by the framework based on what the
  * getConnectorModel() method returns.
  *
  * It is not a big problem if the connector chooses to create more seeds than are
  * strictly necessary; it is merely a question of overall work required.
  *
  * The end time and seeding version string passed to this method may be interpreted for greatest efficiency.
  * For continuous crawling jobs, this method will
  * be called once, when the job starts, and at various periodic intervals as the job executes.
  *
  * When a job's specification is changed, the framework automatically resets the seeding version string to null.  The
  * seeding version string may also be set to null on each job run, depending on the connector model returned by
  * getConnectorModel().
  *
  * Note that it is always ok to send MORE documents rather than less to this method.
  * The connector will be connected before this method can be called.
  *@param activities is the interface this method should use to perform whatever framework actions are desired.
  *@param spec is a document specification (that comes from the job).
  *@param seedTime is the end of the time range of documents to consider, exclusive.
  *@param lastSeedVersion is the last seeding version string for this job, or null if the job has no previous seeding version string.
  *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
  *@return an updated seeding version string, to be stored with the job.
  */
  @Override
  public String addSeedDocuments(ISeedingActivity activities, Specification spec,
    String lastSeedVersion, long seedTime, int jobMode)
    throws ManifoldCFException, ServiceInterruption {

    String jiraDriveQuery = JiraConfig.JIRA_QUERY_DEFAULT;
    int i = 0;
    while (i < spec.getChildCount()) {
      SpecificationNode sn = spec.getChild(i);
      if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
        jiraDriveQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
        break;
      }
      i++;
    }

    GetSeedsThread t = new GetSeedsThread(getSession(), jiraDriveQuery);
    try {
      t.start();
      boolean wasInterrupted = false;
      try {
        XThreadStringBuffer seedBuffer = t.getBuffer();
        // Pick up the paths, and add them to the activities, before we join with the child thread.
        while (true) {
          // The only kind of exceptions this can throw are going to shut the process down.
          String issueKey = seedBuffer.fetch();
          if (issueKey ==  null)
            break;
          // Add the pageID to the queue
          activities.addSeedDocument("I-"+issueKey);
        }
      } catch (InterruptedException e) {
        wasInterrupted = true;
        throw e;
      } catch (ManifoldCFException e) {
        if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
          wasInterrupted = true;
        throw e;
      } finally {
        if (!wasInterrupted)
          t.finishUp();
      }
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
    return "";
  }
  

  /** Process a set of documents.
  * This is the method that should cause each document to be fetched, processed, and the results either added
  * to the queue of documents for the current job, and/or entered into the incremental ingestion manager.
  * The document specification allows this class to filter what is done based on the job.
  * The connector will be connected before this method can be called.
  *@param documentIdentifiers is the set of document identifiers to process.
  *@param statuses are the currently-stored document versions for each document in the set of document identifiers
  * passed in above.
  *@param activities is the interface this method should use to queue up new document references
  * and ingest documents.
  *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
  *@param usesDefaultAuthority will be true only if the authority in use for these documents is the default one.
  */
  @Override
  public void processDocuments(String[] documentIdentifiers, IExistingVersions statuses, Specification spec,
    IProcessActivity activities, int jobMode, boolean usesDefaultAuthority)
    throws ManifoldCFException, ServiceInterruption {

    // Forced acls
    String[] acls = getAcls(spec);
    if (acls != null)
      java.util.Arrays.sort(acls);

    for (String documentIdentifier : documentIdentifiers) {
      
      if (documentIdentifier.startsWith("I-")) {
        // It is an issue
        String versionString;
        String[] aclsToUse;
        String issueID;
        JiraIssue jiraFile;
        
        issueID = documentIdentifier.substring(2);
        jiraFile = getIssue(issueID);
        if (jiraFile == null) {
          activities.deleteDocument(documentIdentifier);
          continue;
        }
        Date rev = jiraFile.getUpdatedDate();
        if (rev == null) {
          //a jira document that doesn't contain versioning information will NEVER be processed.
          // I don't know what this means, and whether it can ever occur.
          activities.deleteDocument(documentIdentifier);
          continue;
        }
        
        StringBuilder sb = new StringBuilder();

        if (acls == null) {
          // Get acls from issue
          List<String> users = getUsers(issueID);
          aclsToUse = (String[])users.toArray(new String[0]);
          java.util.Arrays.sort(aclsToUse);
        } else {
          aclsToUse = acls;
        }
          
        // Acls
        packList(sb,aclsToUse,'+');
        if (aclsToUse.length > 0) {
          sb.append('+');
          pack(sb,defaultAuthorityDenyToken,'+');
        } else
          sb.append('-');
        sb.append(rev.toString());
        
        versionString = sb.toString();

        if (!activities.checkDocumentNeedsReindexing(documentIdentifier,versionString))
          continue;

        if (Logging.connectors.isDebugEnabled()) {
          Logging.connectors.debug("JIRA: Processing document identifier '"
              + documentIdentifier + "'");
        }

        long startTime = System.currentTimeMillis();
        String errorCode = null;
        String errorDesc = null;
        Long fileSize = null;
          
        try {
          // Now do standard stuff
            
          String mimeType = "text/plain";
          Date createdDate = jiraFile.getCreatedDate();
          Date modifiedDate = jiraFile.getUpdatedDate();
          String documentURI = composeDocumentURI(getBaseUrl(session), jiraFile.getKey());

          if (!activities.checkURLIndexable(documentURI))
          {
            errorCode = activities.EXCLUDED_URL;
            errorDesc = "Excluded because of URL ('"+documentURI+"')";
            activities.noDocument(documentIdentifier, versionString);
            continue;
          }
            
          if (!activities.checkMimeTypeIndexable(mimeType))
          {
            errorCode = activities.EXCLUDED_MIMETYPE;
            errorDesc = "Excluded because of mime type ('"+mimeType+"')";
            activities.noDocument(documentIdentifier, versionString);
            continue;
          }
            
          if (!activities.checkDateIndexable(modifiedDate))
          {
            errorCode = activities.EXCLUDED_DATE;
            errorDesc = "Excluded because of date ("+modifiedDate+")";
            activities.noDocument(documentIdentifier, versionString);
            continue;
          }
            
          //otherwise process
          RepositoryDocument rd = new RepositoryDocument();
              
          // Turn into acls and add into description
          String[] denyAclsToUse;
          if (aclsToUse.length > 0)
            denyAclsToUse = new String[]{defaultAuthorityDenyToken};
          else
            denyAclsToUse = new String[0];
          rd.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT,aclsToUse,denyAclsToUse);

          rd.setMimeType(mimeType);
          if (createdDate != null)
            rd.setCreatedDate(createdDate);
          if (modifiedDate != null)
            rd.setModifiedDate(modifiedDate);
            
          rd.addField("webUrl", documentURI);
          rd.addField("key", jiraFile.getKey());
          rd.addField("self", jiraFile.getSelf());
          rd.addField("description", jiraFile.getDescription());

          // Get general document metadata
          Map<String,String[]> metadataMap = jiraFile.getMetadata();
              
          for (Entry<String, String[]> entry : metadataMap.entrySet()) {
            rd.addField(entry.getKey(), entry.getValue());
          }

          String document = getJiraBody(jiraFile);
          try {
            byte[] documentBytes = document.getBytes(StandardCharsets.UTF_8);
            long fileLength = documentBytes.length;
              
            if (!activities.checkLengthIndexable(fileLength))
            {
              errorCode = activities.EXCLUDED_LENGTH;
              errorDesc = "Excluded because of document length ("+fileLength+")";
              activities.noDocument(documentIdentifier, versionString);
              continue;
            }
                
            InputStream is = new ByteArrayInputStream(documentBytes);
            try {
              rd.setBinary(is, fileLength);
              activities.ingestDocumentWithException(documentIdentifier, versionString, documentURI, rd);
              // No errors.  Record the fact that we made it.
              errorCode = "OK";
              fileSize = new Long(fileLength);
            } finally {
              is.close();
            }
          } catch (java.io.IOException e) {
            errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            errorDesc = e.getMessage();
            handleIOException(e);
          }
        } catch (ManifoldCFException e) {
          if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            errorCode = null;
          throw e;
        } finally {
          if (errorCode != null)
            activities.recordActivity(new Long(startTime), ACTIVITY_READ,
              fileSize, documentIdentifier, errorCode, errorDesc, null);
        }

      } else {
        // Unrecognized identifier type
        activities.deleteDocument(documentIdentifier);
        continue;
      }
    }
  }
  
  protected static String getJiraBody(JiraIssue jiraFile) {
    String summary = jiraFile.getSummary();
    String description = jiraFile.getDescription();
    StringBuilder body = new StringBuilder();
    if (summary != null)
      body.append(summary);
    if (description != null) {
      if (body.length() > 0)
        body.append(" : ");
      body.append(description);
    }
    return body.toString();
  }
  
  
  /**
   * Compose the "real" url of the jira issue (BASEURL+/browse/+ISSUEKEY)
   * @param baseUrl
   * @param key
   * @return
   */
  private String composeDocumentURI(String baseUrl, String key) {
	  if (!baseUrl.endsWith("/"))
	  	baseUrl = baseUrl + "/";
      return baseUrl + "browse/" + key;
  }

  /** Grab forced acl out of document specification.
  *@param spec is the document specification.
  *@return the acls, or null if security is on (and the acls need to be fetched)
  */
  protected static String[] getAcls(Specification spec) {
    Set<String> map = new HashSet<String>();
    for (int i = 0; i < spec.getChildCount(); i++) {
      SpecificationNode sn = spec.getChild(i);
      if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
        String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
        map.add(token);
      }
      else if (sn.getType().equals(JOB_SECURITY_NODE_TYPE)) {
        String onOff = sn.getAttributeValue(JOB_VALUE_ATTRIBUTE);
        if (onOff != null && onOff.equals("on"))
          return null;
      }
    }

    String[] rval = new String[map.size()];
    Iterator<String> iter = map.iterator();
    int i = 0;
    while (iter.hasNext()) {
      rval[i++] = (String)iter.next();
    }
    return rval;
  }

  private static void handleIOException(IOException e)
    throws ManifoldCFException, ServiceInterruption {
    if (!(e instanceof java.net.SocketTimeoutException) && (e instanceof InterruptedIOException)) {
      throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
        ManifoldCFException.INTERRUPTED);
    }
    Logging.connectors.warn("JIRA: IO exception: "+e.getMessage(), e);
    long currentTime = System.currentTimeMillis();
    throw new ServiceInterruption("IO exception: "+e.getMessage(), e, currentTime + 300000L,
      currentTime + 3 * 60 * 60000L,-1,false);
  }
  
  private static void handleResponseException(ResponseException e)
    throws ManifoldCFException, ServiceInterruption {
    throw new ManifoldCFException("Unexpected response: "+e.getMessage(),e);
  }

  // Background threads

  protected static class GetUsersThread extends Thread {

    protected final JiraSession session;
    protected final String issueKey;
    protected Throwable exception = null;
    protected List<String> result = null;

    public GetUsersThread(JiraSession session, String issueKey) {
      super();
      this.session = session;
      this.issueKey = issueKey;
      setDaemon(true);
    }

    public void run() {
      try {
        result = session.getUsers(issueKey);
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
    
    public List<String> getResult() {
      return result;
    }

  }

  protected List<String> getUsers(String issueKey) throws ManifoldCFException, ServiceInterruption {
    GetUsersThread t = new GetUsersThread(getSession(), issueKey);
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
    return null;
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

  protected void checkConnection() throws ManifoldCFException, ServiceInterruption {
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

  protected static class GetSeedsThread extends Thread {

    protected Throwable exception = null;
    protected final JiraSession session;
    protected final String jiraDriveQuery;
    protected final XThreadStringBuffer seedBuffer;
    
    public GetSeedsThread(JiraSession session, String jiraDriveQuery) {
      super();
      this.session = session;
      this.jiraDriveQuery = jiraDriveQuery;
      this.seedBuffer = new XThreadStringBuffer();
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        session.getSeeds(seedBuffer, jiraDriveQuery);
      } catch (Throwable e) {
        this.exception = e;
      } finally {
        seedBuffer.signalDone();
      }
    }

    public XThreadStringBuffer getBuffer() {
      return seedBuffer;
    }
    
    public void finishUp()
      throws InterruptedException, IOException, ResponseException {
      seedBuffer.abandon();
      join();
      Throwable thr = exception;
      if (thr != null) {
        if (thr instanceof IOException)
          throw (IOException) thr;
        else if (thr instanceof ResponseException)
          throw (ResponseException) thr;
        else if (thr instanceof RuntimeException)
          throw (RuntimeException) thr;
        else if (thr instanceof Error)
          throw (Error) thr;
        else
          throw new RuntimeException("Unhandled exception of type: "+thr.getClass().getName(),thr);
      }
    }
  }

  protected JiraIssue getIssue(String issueID)
    throws ManifoldCFException, ServiceInterruption {
    GetIssueThread t = new GetIssueThread(getSession(), issueID);
    try {
      t.start();
      t.finishUp();
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
    return t.getResponse();
  }
  
  
  protected String getBaseUrl(JiraSession jiraSession) throws ManifoldCFException, ServiceInterruption {
	  String url = "";
	  try {
		  url = jiraSession.getBaseUrl();
		  return url;
	    } catch (java.net.SocketTimeoutException e) {
	        handleIOException(e);
	    } catch (InterruptedIOException e) {
	        handleIOException(e);
	    } catch (IOException e) {
	        handleIOException(e);
	    } catch (ResponseException e) {
	        handleResponseException(e);
	    }
	return url;
  }
  
  protected static class GetIssueThread extends Thread {

    protected final JiraSession session;
    protected final String nodeId;
    protected Throwable exception = null;
    protected JiraIssue response = null;

    public GetIssueThread(JiraSession session, String nodeId) {
      super();
      setDaemon(true);
      this.session = session;
      this.nodeId = nodeId;
    }

    public void run() {
      try {
        response = session.getIssue(nodeId);
      } catch (Throwable e) {
        this.exception = e;
      }
    }

    public JiraIssue getResponse() {
      return response;
    }
    
    public void finishUp() throws InterruptedException, IOException, ResponseException {
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

}

