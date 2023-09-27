/* $Id: ActiveDirectoryAuthority.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package org.apache.manifoldcf.authorities.authorities.activedirectory;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.authorities.interfaces.*;
import org.apache.manifoldcf.authorities.system.Logging;
import org.apache.manifoldcf.authorities.system.ManifoldCF;

import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.naming.ldap.*;
import javax.naming.directory.*;

import static org.apache.manifoldcf.connectorcommon.common.LdapEscaper.escapeDN;
import static org.apache.manifoldcf.connectorcommon.common.LdapEscaper.escapeFilter;


/** This is the Active Directory implementation of the IAuthorityConnector interface.
* Access tokens for this connector are simple SIDs, except for the "global deny" token, which
* is designed to allow the authority to shut off access to all authorized documents when the
* user is unrecognized or the domain controller does not respond.
*/
public class ActiveDirectoryAuthority extends org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector
{
  public static final String _rcsid = "@(#)$Id: ActiveDirectoryAuthority.java 988245 2010-08-23 18:39:35Z kwright $";

  // Data from the parameters

  /** The list of suffixes and the associated domain controllers */
  private List<DCRule> dCRules = null;
  /** How to create a connection for a DC, keyed by DC name */
  private Map<String,DCConnectionParameters> dCConnectionParameters = null;

  private boolean hasSessionParameters = false;
  private String cacheLifetime = null;
  private String cacheLRUsize = null;
  private long responseLifetime = 60000L;
  private int LRUsize = 1000;
  private String ldapConnectionTimeout = null;
  private boolean returnGroupsName = false;

  /** Session information for all DC's we talk with. */
  private Map<String,DCSessionInfo> sessionInfo = null;

  /** Cache manager. */
  private ICacheManager cacheManager = null;


  /** The length of time in milliseconds that the connection remains idle before expiring.  Currently 5 minutes. */
  private static final long expirationInterval = 300000L;

  /** Constructor.
  */
  public ActiveDirectoryAuthority()
  {
  }

  /** Set thread context.
  */
  @Override
  public void setThreadContext(IThreadContext tc)
    throws ManifoldCFException
  {
    super.setThreadContext(tc);
    cacheManager = CacheManagerFactory.make(tc);
  }
  
  /** Clear thread context.
  */
  @Override
  public void clearThreadContext()
  {
    super.clearThreadContext();
    cacheManager = null;
  }
  
  /** Connect.  The configuration parameters are included.
  *@param configParams are the configuration parameters for this connection.
  */
  @Override
  public void connect(ConfigParams configParams)
  {
    super.connect(configParams);
    
    // Allocate the session data, currently empty
    sessionInfo = new HashMap<String,DCSessionInfo>();
    
    // Set up the DC param set, and the rules
    dCRules = new ArrayList<DCRule>();
    dCConnectionParameters = new HashMap<String,DCConnectionParameters>();
    // For backwards compatibility, look at old-style parameters
    String domainControllerName = params.getParameter(ActiveDirectoryConfig.PARAM_DOMAINCONTROLLER);
    String userName = params.getParameter(ActiveDirectoryConfig.PARAM_USERNAME);
    String password = params.getObfuscatedParameter(ActiveDirectoryConfig.PARAM_PASSWORD);
    String authentication = params.getParameter(ActiveDirectoryConfig.PARAM_AUTHENTICATION);
    String userACLsUsername = params.getParameter(ActiveDirectoryConfig.PARAM_USERACLsUSERNAME);
    if (domainControllerName != null)
    {
      // Map the old-style parameters into the new-style structures.
      dCConnectionParameters.put(domainControllerName,new DCConnectionParameters(userName,password,authentication,userACLsUsername));
      // Create a single rule, too
      dCRules.add(new DCRule("",domainControllerName));
    }
    else
    {
      // New-style parameters.  Read from the config info.
      int i = 0;
      while (i < params.getChildCount())
      {
        ConfigNode cn = params.getChild(i++);
        if (cn.getType().equals(ActiveDirectoryConfig.NODE_DOMAINCONTROLLER))
        {
          // Domain controller name is the actual key...
          String dcName = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_DOMAINCONTROLLER);
          // Set up the parameters for the domain controller
          dCConnectionParameters.put(dcName,new DCConnectionParameters(cn.getAttributeValue(ActiveDirectoryConfig.ATTR_USERNAME),
            deobfuscate(cn.getAttributeValue(ActiveDirectoryConfig.ATTR_PASSWORD)),
            cn.getAttributeValue(ActiveDirectoryConfig.ATTR_AUTHENTICATION),
            cn.getAttributeValue(ActiveDirectoryConfig.ATTR_USERACLsUSERNAME)));
          // Order-based rule, as well
          dCRules.add(new DCRule(cn.getAttributeValue(ActiveDirectoryConfig.ATTR_SUFFIX),dcName));
        }
      }
    }

    ldapConnectionTimeout = params.getParameter(ActiveDirectoryConfig.PARAM_LDAPCONNECTIONTIMEOUT);
    if (ldapConnectionTimeout == null)
      ldapConnectionTimeout = "60000";
    cacheLifetime = params.getParameter(ActiveDirectoryConfig.PARAM_CACHELIFETIME);
    if (cacheLifetime == null)
      cacheLifetime = "1";
    cacheLRUsize = params.getParameter(ActiveDirectoryConfig.PARAM_CACHELRUSIZE);
    if (cacheLRUsize == null)
      cacheLRUsize = "1000";

    if (params.getParameter(ActiveDirectoryConfig.PARAM_ACLSGROUPSNAME) != null) {
      returnGroupsName = params.getParameter(ActiveDirectoryConfig.PARAM_ACLSGROUPSNAME).equals("true");
    }
  }

  protected static String deobfuscate(String input)
  {
    if (input == null)
      return null;
    try
    {
      return ManifoldCF.deobfuscate(input);
    }
    catch (ManifoldCFException e)
    {
      return "";
    }
  }
  
  // All methods below this line will ONLY be called if a connect() call succeeded
  // on this instance!

  /** Check connection for sanity.
  */
  @Override
  public String check()
    throws ManifoldCFException
  {
    // Set up the basic session...
    getSessionParameters();
    // Clear the DC session info, so we're forced to redo it
    for (Map.Entry<String,DCSessionInfo> sessionEntry : sessionInfo.entrySet())
    {
      sessionEntry.getValue().closeConnection();
    }
    // Loop through all domain controllers and attempt to establish a session with each one.
    for (String domainController : dCConnectionParameters.keySet())
    {
      createDCSession(domainController);
    }
    return super.check();
  }

  /** Create or lookup a session for a domain controller.
  */
  protected LdapContext createDCSession(String domainController)
    throws ManifoldCFException
  {
    getSessionParameters();
    DCConnectionParameters parms = dCConnectionParameters.get(domainController);
    // Find the session in the hash, if it exists
    DCSessionInfo session = sessionInfo.get(domainController);
    if (session == null)
    {
      session = new DCSessionInfo();
      sessionInfo.put(domainController,session);
    }
    return session.getSession(domainController,parms,ldapConnectionTimeout);
  }
  
  /** Poll.  The connection should be closed if it has been idle for too long.
  */
  @Override
  public void poll()
    throws ManifoldCFException
  {
    long currentTime = System.currentTimeMillis();
    for (Map.Entry<String,DCSessionInfo> sessionEntry : sessionInfo.entrySet())
    {
      sessionEntry.getValue().closeIfExpired(currentTime);
    }
    super.poll();
  }
  
  /** This method is called to assess whether to count this connector instance should
  * actually be counted as being connected.
  *@return true if the connector instance is actually connected.
  */
  @Override
  public boolean isConnected()
  {
    for (Map.Entry<String,DCSessionInfo> sessionEntry : sessionInfo.entrySet())
    {
      if (sessionEntry.getValue().isOpen())
        return true;
    }
    return false;
  }

  /** Close the connection.  Call this before discarding the repository connector.
  */
  @Override
  public void disconnect()
    throws ManifoldCFException
  {
    hasSessionParameters = false;

    // Close all connections
    for (Map.Entry<String,DCSessionInfo> sessionEntry : sessionInfo.entrySet())
    {
      sessionEntry.getValue().closeConnection();
    }
    sessionInfo = null;
    
    cacheLifetime = null;
    cacheLRUsize = null;
    ldapConnectionTimeout = null;
    super.disconnect();
  }

  /** Obtain the access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the response tokens (according to the current authority).
  * (Should throws an exception only when a condition cannot be properly described within the authorization response object.)
  */
  @Override
  public AuthorizationResponse getAuthorizationResponse(String userName)
    throws ManifoldCFException
  {
    // This sets up parameters we need to construct the response description
    getSessionParameters();

    // Construct a cache description object
    ICacheDescription objectDescription = new AuthorizationResponseDescription(userName,
      dCConnectionParameters,dCRules,this.responseLifetime,this.LRUsize);
    
    // Enter the cache
    ICacheHandle ch = cacheManager.enterCache(new ICacheDescription[]{objectDescription},null,null);
    try
    {
      ICacheCreateHandle createHandle = cacheManager.enterCreateSection(ch);
      try
      {
        // Lookup the object
        AuthorizationResponse response = (AuthorizationResponse)cacheManager.lookupObject(createHandle,objectDescription);
        if (response != null)
          return response;
        // Create the object.
        response = getAuthorizationResponseUncached(userName);
        // Save it in the cache
        cacheManager.saveObject(createHandle,objectDescription,response);
        // And return it...
        return response;
      }
      finally
      {
        cacheManager.leaveCreateSection(createHandle);
      }
    }
    finally
    {
      cacheManager.leaveCache(ch);
    }
  }
  
  /** Obtain the access tokens for a given user name, uncached.
  *@param userName is the user name or identifier.
  *@return the response tokens (according to the current authority).
  * (Should throws an exception only when a condition cannot be properly described within the authorization response object.)
  */
  protected AuthorizationResponse getAuthorizationResponseUncached(String userName)
    throws ManifoldCFException
  {
    //String searchBase = "CN=Administrator,CN=Users,DC=qa-ad-76,DC=metacarta,DC=com";
    int index = userName.indexOf("@");
    if (index == -1)
      throw new ManifoldCFException("Username is in unexpected form (no @): '"+userName+"'");
    String userPart = userName.substring(0,index);
    String domainPart = userName.substring(index+1);
    
    // Now, look through the rules for the matching domain controller
    String domainController = null;
    for (DCRule rule : dCRules)
    {
      String suffix = rule.getSuffix();
      if (suffix.length() == 0 || domainPart.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT)) &&
        (suffix.length() == domainPart.length() || domainPart.charAt((domainPart.length()-suffix.length())-1) == '.'))
      {
        domainController = rule.getDomainControllerName();
        break;
      }
    }
    if (domainController == null)
    {
      // No domain controller found for the user, so return "user not found".
      Logging.authorityConnectors.info("User not found: " + userName);
      return RESPONSE_USERNOTFOUND;
    }
    
    // Look up connection parameters
    DCConnectionParameters dcParams = dCConnectionParameters.get(domainController);
    if (dcParams == null)
    {
      // No domain controller, even though it's mentioned in a rule
      Logging.authorityConnectors.info("User not found: " + userName);
      return RESPONSE_USERNOTFOUND;
    }
    
    // Use the complete fqn if the field is the "userPrincipalName"
    String userACLsUsername = dcParams.getUserACLsUsername();
    if (userACLsUsername != null && userACLsUsername.equals("userPrincipalName")){
    	userPart = userName;
    }
    
    //Build the DN searchBase from domain part
    StringBuilder domainsb = new StringBuilder();
    int j = 0;
    while (true)
    {
      if (j > 0)
        domainsb.append(",");

      int k = domainPart.indexOf(".",j);
      if (k == -1)
      {
        domainsb.append("DC=").append(escapeDN(domainPart.substring(j)));
        break;
      }
      domainsb.append("DC=").append(escapeDN(domainPart.substring(j,k)));
      j = k+1;
    }

    try
    {
      // Establish a session with the selected domain controller
      LdapContext ctx = createDCSession(domainController);
    
      //Get DistinguishedName (for this method we are using DomainPart as a searchBase ie: DC=qa-ad-76,DC=metacarta,DC=com")
      String searchBase = getDistinguishedName(ctx, userPart, domainsb.toString(), userACLsUsername);
      if (searchBase == null) {
        Logging.authorityConnectors.info("User not found: " + userName);
        return RESPONSE_USERNOTFOUND;
      }

      //specify the LDAP search filter
      String searchFilter = "(objectClass=user)";

      //Create the search controls for finding the access tokens	
      SearchControls searchCtls = new SearchControls();

      //Specify the search scope, must be base level search for tokenGroups
      searchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);
   
      //Specify the attributes to return
      String returnedAtts[]={"tokenGroups","objectSid"};
      searchCtls.setReturningAttributes(returnedAtts);

      // placeholder for an LDAP filter that will store SIDs of the groups the user belongs to
      final StringBuffer groupsSearchFilter = new StringBuffer();
      groupsSearchFilter.append("(|");

      //Search for tokens.  Since every user *must* have a SID, the "no user" detection should be safe.
      NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);

      ArrayList theGroups = new ArrayList();

      //Loop through the search results
      while (answer.hasMoreElements())
      {
        SearchResult sr = (SearchResult)answer.next();
 
        //the sr.GetName should be null, as it is relative to the base object
        
        final Attributes attrs = sr.getAttributes();
        if (attrs != null)
        {
          try
          {
            if (attrs.get("tokenGroups") != null) {
              for (final NamingEnumeration ae = attrs.get("tokenGroups").getAll(); ae.hasMore();) {
                final byte[] sid = (byte[]) ae.next();
                theGroups.add(sid2String(sid));
                if (returnGroupsName) {
                  groupsSearchFilter.append("(objectSid=" + sid2String(sid) + ")");
                }
              }
              groupsSearchFilter.append(")");
            }

            if (attrs.get("objectSid") != null) {
              for (final NamingEnumeration ae = attrs.get("objectSid").getAll(); ae.hasMore();) {
                theGroups.add(sid2String((byte[]) ae.next()));
              }
            }
 
          }	 
          catch (NamingException e)
          {
            Logging.authorityConnectors.error("Naming exception: " + e.getMessage(), e);
            throw new ManifoldCFException(e.getMessage(),e);
          }
				
        }
      }

      if (theGroups.size() == 0) {
        Logging.authorityConnectors.info("User not found: " + userName);
        return RESPONSE_USERNOTFOUND;
      }

      if (returnGroupsName) {
        // Search for groups the user belongs to in order to get their names
        // Create the search controls
        final SearchControls groupsSearchCtls = new SearchControls();

        // Specify the search scope
        groupsSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        // In principle, the attribute used to find the group name is the same as that used to find the user name
        final String[] groupsReturnedAttrs = { "sAMAccountName" };
        groupsSearchCtls.setReturningAttributes(groupsReturnedAttrs);

        // Search for objects using the filter
        final NamingEnumeration<SearchResult> groupsAnswer = ctx.search(domainsb.toString(), groupsSearchFilter.toString(), groupsSearchCtls);

        // Loop through the search results
        while (groupsAnswer.hasMoreElements()) {

          final SearchResult sr = (SearchResult) groupsAnswer.next();
          final Attributes attrs = sr.getAttributes();
          if (attrs != null) {
            theGroups.add(attrs.get("sAMAccountName").get());
          }
        }
      }

      // All users get certain well-known groups
      theGroups.add("S-1-1-0");

      String[] tokens = new String[theGroups.size()];
      int k = 0;
      while (k < tokens.length)
      {
        tokens[k] = (String)theGroups.get(k);
        k++;
      }
      
      return new AuthorizationResponse(tokens,AuthorizationResponse.RESPONSE_OK);

    }
    catch (NameNotFoundException e)
    {
      // This means that the user doesn't exist
      Logging.authorityConnectors.error("User not found: " + userName + " Exception: " + e.getMessage(), e);
      return RESPONSE_USERNOTFOUND;
    }
    catch (NamingException e)
    {
      // Unreachable
      Logging.authorityConnectors.error("Response Unreachable: " + e.getMessage(), e);
      return RESPONSE_UNREACHABLE;
    }
  }

  /** Obtain the default access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the default response tokens, presuming that the connect method fails.
  */
  @Override
  public AuthorizationResponse getDefaultAuthorizationResponse(String userName)
  {
    // The default response if the getConnection method fails
    return RESPONSE_UNREACHABLE;
  }

  // UI support methods.
  //
  // These support methods are involved in setting up authority connection configuration information. The configuration methods cannot assume that the
  // current authority object is connected.  That is why they receive a thread context argument.
    
  /** Output the configuration header section.
  * This method is called in the head section of the connector's configuration page.  Its purpose is to add the required tabs to the list, and to output any
  * javascript methods that might be needed by the configuration editing HTML.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out, Locale locale, ConfigParams parameters, List<String> tabsArray)
    throws ManifoldCFException, IOException
  {
    tabsArray.add(Messages.getString(locale,"ActiveDirectoryAuthority.DomainController"));
    tabsArray.add(Messages.getString(locale,"ActiveDirectoryAuthority.Cache"));
    tabsArray.add(Messages.getString(locale, "ActiveDirectoryAuthority.ACLs"));
    Messages.outputResourceWithVelocity(out,locale,"editConfiguration.js",null);
  }
  
  /** Output the configuration body section.
  * This method is called in the body section of the authority connector's configuration page.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags.  The name of the
  * form is "editconnection".
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabName is the current tab name.
  */
  @Override
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
    throws ManifoldCFException, IOException
  {
    Map<String,Object> velocityContext = new HashMap<String,Object>();
    velocityContext.put("TabName",tabName);
    fillInDomainControllerTab(velocityContext,out,parameters);
    fillInCacheTab(velocityContext,out,parameters);
    fillInACLsTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out,locale,"editConfiguration_DomainController.html",velocityContext);
    Messages.outputResourceWithVelocity(out,locale,"editConfiguration_Cache.html",velocityContext);
    Messages.outputResourceWithVelocity(out, locale, "editConfiguration_ACLs.html", velocityContext);
  }
  
  protected static void fillInDomainControllerTab(Map<String,Object> velocityContext, IPasswordMapperActivity mapper, ConfigParams parameters)
  {
    String domainControllerName = parameters.getParameter(ActiveDirectoryConfig.PARAM_DOMAINCONTROLLER);
    String userName = parameters.getParameter(ActiveDirectoryConfig.PARAM_USERNAME);
    String password = parameters.getObfuscatedParameter(ActiveDirectoryConfig.PARAM_PASSWORD);
    String authentication = parameters.getParameter(ActiveDirectoryConfig.PARAM_AUTHENTICATION);
    String userACLsUsername = parameters.getParameter(ActiveDirectoryConfig.PARAM_USERACLsUSERNAME);
    List<Map<String,String>> domainControllers = new ArrayList<Map<String,String>>();
    
    // Backwards compatibility: if domain controller parameter is set, create an entry in the map.
    if (domainControllerName != null)
    {
      domainControllers.add(createDomainControllerMap(mapper,"",domainControllerName,userName,password,authentication,userACLsUsername));
    }
    else
    {
      // Go through nodes looking for DC nodes
      int i = 0;
      while (i < parameters.getChildCount())
      {
        ConfigNode cn = parameters.getChild(i++);
        if (cn.getType().equals(ActiveDirectoryConfig.NODE_DOMAINCONTROLLER))
        {
          // Grab the info
          String dcSuffix = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_SUFFIX);
          String dcDomainController = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_DOMAINCONTROLLER);
          String dcUserName = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_USERNAME);
          String dcPassword = deobfuscate(cn.getAttributeValue(ActiveDirectoryConfig.ATTR_PASSWORD));
          String dcAuthentication = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_AUTHENTICATION);
          String dcUserACLsUsername = cn.getAttributeValue(ActiveDirectoryConfig.ATTR_USERACLsUSERNAME);
          domainControllers.add(createDomainControllerMap(mapper,dcSuffix,dcDomainController,dcUserName,dcPassword,dcAuthentication,dcUserACLsUsername));
        }
      }
    }
    velocityContext.put("DOMAINCONTROLLERS",domainControllers);
    String ldapConnectionTimeout = parameters.getParameter(ActiveDirectoryConfig.PARAM_LDAPCONNECTIONTIMEOUT);
    if (ldapConnectionTimeout == null)
      ldapConnectionTimeout = "60000";
    velocityContext.put("LDAPCONNECTIONTIMEOUT", ldapConnectionTimeout);
  }

  protected static Map<String,String> createDomainControllerMap(IPasswordMapperActivity mapper, String suffix, String domainControllerName,
    String userName, String password, String authentication, String userACLsUsername)
  {
    Map<String,String> defaultMap = new HashMap<String,String>();
    if (suffix != null)
      defaultMap.put("SUFFIX",suffix);
    if (domainControllerName != null)
      defaultMap.put("DOMAINCONTROLLER",domainControllerName);
    if (userName != null)
      defaultMap.put("USERNAME",userName);
    if (password != null)
      defaultMap.put("PASSWORD",mapper.mapPasswordToKey(password));
    if (authentication != null)
      defaultMap.put("AUTHENTICATION",authentication);
    if (userACLsUsername != null)
      defaultMap.put("USERACLsUSERNAME",userACLsUsername);
    return defaultMap;
  }
  
  protected static void fillInCacheTab(Map<String,Object> velocityContext, IPasswordMapperActivity mapper, ConfigParams parameters)
  {
    String cacheLifetime = parameters.getParameter(ActiveDirectoryConfig.PARAM_CACHELIFETIME);
    if (cacheLifetime == null)
      cacheLifetime = "1";
    velocityContext.put("CACHELIFETIME",cacheLifetime);
    String cacheLRUsize = parameters.getParameter(ActiveDirectoryConfig.PARAM_CACHELRUSIZE);
    if (cacheLRUsize == null)
      cacheLRUsize = "1000";
    velocityContext.put("CACHELRUSIZE",cacheLRUsize);
  }

  protected static void fillInACLsTab(final Map<String, Object> velocityContext, final IPasswordMapperActivity mapper, final ConfigParams parameters) {
    String aclsGroupsName = parameters.getParameter(ActiveDirectoryConfig.PARAM_ACLSGROUPSNAME);
    if (aclsGroupsName == null)
      aclsGroupsName = "false";
    velocityContext.put("ACLSGROUPSNAME", aclsGroupsName);
  }

  /** Process a configuration post.
  * This method is called at the start of the authority connector's configuration page, whenever there is a possibility that form data for a connection has been
  * posted.  Its purpose is to gather form information and modify the configuration parameters accordingly.
  * The name of the posted form is "editconnection".
  *@param threadContext is the local thread context.
  *@param variableContext is the set of variables available from the post, including binary file post information.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
  */
  @Override
  public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext, Locale locale, ConfigParams parameters)
    throws ManifoldCFException
  {
    String x = variableContext.getParameter("dcrecord_count");
    if (x != null)
    {
      // Delete old stuff
      parameters.setParameter(ActiveDirectoryConfig.PARAM_DOMAINCONTROLLER,null);
      parameters.setParameter(ActiveDirectoryConfig.PARAM_USERNAME,null);
      parameters.setParameter(ActiveDirectoryConfig.PARAM_PASSWORD,null);
      parameters.setParameter(ActiveDirectoryConfig.PARAM_AUTHENTICATION,null);
      parameters.setParameter(ActiveDirectoryConfig.PARAM_USERACLsUSERNAME,null);
      // Delete old nodes
      int i = 0;
      while (i < parameters.getChildCount())
      {
        ConfigNode cn = parameters.getChild(i);
        if (cn.getType().equals(ActiveDirectoryConfig.NODE_DOMAINCONTROLLER))
          parameters.removeChild(i);
        else
          i++;
      }
      // Scan form fields and apply operations
      int count = Integer.parseInt(x);
      i = 0;
      String op;
      
      Set<String> seenDomains = new HashSet<String>();
      
      while (i < count)
      {
        op = variableContext.getParameter("dcrecord_op_"+i);
        if (op != null && op.equals("Insert"))
        {
          // Insert a new record right here
          addDomainController(seenDomains,parameters,
            variableContext.getParameter("dcrecord_suffix"),
            variableContext.getParameter("dcrecord_domaincontrollername"),
            variableContext.getParameter("dcrecord_username"),
            variableContext.mapKeyToPassword(variableContext.getParameter("dcrecord_password")),
            variableContext.getParameter("dcrecord_authentication"),
            variableContext.getParameter("dcrecord_userACLsUsername"));
        }
        if (op == null || !op.equals("Delete"))
        {
          // Add this record back in
          addDomainController(seenDomains,parameters,
            variableContext.getParameter("dcrecord_suffix_"+i),
            variableContext.getParameter("dcrecord_domaincontrollername_"+i),
            variableContext.getParameter("dcrecord_username_"+i),
            variableContext.mapKeyToPassword(variableContext.getParameter("dcrecord_password_"+i)),
            variableContext.getParameter("dcrecord_authentication_"+i),
            variableContext.getParameter("dcrecord_userACLsUsername_"+i));
        }
        i++;
      }
      op = variableContext.getParameter("dcrecord_op");
      if (op != null && op.equals("Add"))
      {
        // Insert a new record right here
        addDomainController(seenDomains,parameters,
          variableContext.getParameter("dcrecord_suffix"),
          variableContext.getParameter("dcrecord_domaincontrollername"),
          variableContext.getParameter("dcrecord_username"),
          variableContext.getParameter("dcrecord_password"),
          variableContext.getParameter("dcrecord_authentication"),
          variableContext.getParameter("dcrecord_userACLsUsername"));
      }
    }
    String ldapConnectionTimeout = variableContext.getParameter("ldapconnectiontimeout");
    if (ldapConnectionTimeout != null)
      parameters.setParameter(ActiveDirectoryConfig.PARAM_LDAPCONNECTIONTIMEOUT,ldapConnectionTimeout);
    String cacheLifetime = variableContext.getParameter("cachelifetime");
    if (cacheLifetime != null)
      parameters.setParameter(ActiveDirectoryConfig.PARAM_CACHELIFETIME,cacheLifetime);
    String cacheLRUsize = variableContext.getParameter("cachelrusize");
    if (cacheLRUsize != null)
      parameters.setParameter(ActiveDirectoryConfig.PARAM_CACHELRUSIZE,cacheLRUsize);

    parameters.setParameter(ActiveDirectoryConfig.PARAM_ACLSGROUPSNAME, "false");
    final String aclsGroupsName = variableContext.getParameter("aclsgroupsname");
    if (aclsGroupsName != null) {
      parameters.setParameter(ActiveDirectoryConfig.PARAM_ACLSGROUPSNAME, aclsGroupsName);
    }

    return null;
  }
  
  protected static void addDomainController(Set<String> seenDomains, ConfigParams parameters,
    String suffix, String domainControllerName, String userName, String password, String authentication,
    String userACLsUsername)
    throws ManifoldCFException
  {
    if (!seenDomains.contains(domainControllerName))
    {
      ConfigNode cn = new ConfigNode(ActiveDirectoryConfig.NODE_DOMAINCONTROLLER);
      cn.setAttribute(ActiveDirectoryConfig.ATTR_SUFFIX,suffix);
      cn.setAttribute(ActiveDirectoryConfig.ATTR_DOMAINCONTROLLER,domainControllerName);
      cn.setAttribute(ActiveDirectoryConfig.ATTR_USERNAME,userName);
      cn.setAttribute(ActiveDirectoryConfig.ATTR_PASSWORD,ManifoldCF.obfuscate(password));
      cn.setAttribute(ActiveDirectoryConfig.ATTR_AUTHENTICATION,authentication);
      cn.setAttribute(ActiveDirectoryConfig.ATTR_USERACLsUSERNAME,userACLsUsername);
      parameters.addChild(parameters.getChildCount(),cn);
      seenDomains.add(domainControllerName);
    }
  }
  
  /** View configuration.
  * This method is called in the body section of the authority connector's view configuration page.  Its purpose is to present the connection information to the user.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt; and &lt;body&gt;tags.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out, Locale locale, ConfigParams parameters)
    throws ManifoldCFException, IOException
  {
    Map<String,Object> velocityContext = new HashMap<String,Object>();
    fillInDomainControllerTab(velocityContext,out,parameters);
    fillInCacheTab(velocityContext,out,parameters);
    fillInACLsTab(velocityContext, out, parameters);
    Messages.outputResourceWithVelocity(out,locale,"viewConfiguration.html",velocityContext);
  }

  // Protected methods

  /** Get parameters needed for caching.
  */
  protected void getSessionParameters()
    throws ManifoldCFException
  {
    if (!hasSessionParameters)
    {
      try
      {
        responseLifetime = Long.parseLong(this.cacheLifetime) * 60L * 1000L;
        LRUsize = Integer.parseInt(this.cacheLRUsize);
      }
      catch (NumberFormatException e)
      {
        Logging.authorityConnectors.error("Cache lifetime or Cache LRU size must be an integer: " + e.getMessage(), e);
        throw new ManifoldCFException("Cache lifetime or Cache LRU size must be an integer: "+e.getMessage(),e);
      }
      hasSessionParameters = true;
    }
  }
  
  
  /** Obtain the DistinguishedName for a given user logon name.
  *@param ctx is the ldap context to use.
  *@param userName (Domain Logon Name) is the user name or identifier.
  *@param searchBase (Full Domain Name for the search ie: DC=qa-ad-76,DC=metacarta,DC=com)
  *@return DistinguishedName for given domain user logon name. 
  * (Should throws an exception if user is not found.)
  */
  protected String getDistinguishedName(LdapContext ctx, String userName, String searchBase, String userACLsUsername)
    throws ManifoldCFException
  {
    String returnedAtts[] = {"distinguishedName"};
    String searchFilter = "(&(objectClass=user)(" + userACLsUsername + "=" + escapeFilter(userName) + "))";
    SearchControls searchCtls = new SearchControls();
    searchCtls.setReturningAttributes(returnedAtts);
    //Specify the search scope  
    searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchCtls.setReturningAttributes(returnedAtts);

    try
    {
      NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);
      while (answer.hasMoreElements())
      {
        SearchResult sr = (SearchResult)answer.next();
        Attributes attrs = sr.getAttributes();
        if (attrs != null)
        {
          String dn = attrs.get("distinguishedName").get().toString();
          return dn;
        }
      }
      return null;
    }
    catch (NamingException e)
    {
      Logging.authorityConnectors.error("Naming exception: " + e.getMessage(), e);
      throw new ManifoldCFException(e.getMessage(),e);
    }
  }

  /** Convert a binary SID to a string */
  protected static String sid2String(byte[] SID)
  {
    StringBuilder strSID = new StringBuilder("S");
    long version = SID[0];
    strSID.append("-").append(Long.toString(version));
    long authority = SID[4];
    for (int i = 0;i<4;i++)
    {
      authority <<= 8;
      authority += SID[4+i] & 0xFF;
    }
    strSID.append("-").append(Long.toString(authority));
    long count = SID[2];
    count <<= 8;
    count += SID[1] & 0xFF;
    for (int j=0;j<count;j++)
    {
      long rid = SID[11 + (j*4)] & 0xFF;
      for (int k=1;k<4;k++)
      {
        rid <<= 8;
        rid += SID[11-k + (j*4)] & 0xFF;
      }
      strSID.append("-").append(Long.toString(rid));
    }
    return strSID.toString();
  }

  /** Class representing the session information for a specific domain controller
  * connection.
  */
  protected static class DCSessionInfo
  {
    /** The initialized LDAP context (which functions as a session) */
    private LdapContext ctx = null;
    /** The time of last access to this ctx object */
    private long expiration = -1L;
    
    public DCSessionInfo()
    {
    }

    /** Initialize the session. */
    public LdapContext getSession(String domainControllerName, DCConnectionParameters params, String ldapConnectionTimeout)
      throws ManifoldCFException
    {
      String authentication = params.getAuthentication();
      String userName = params.getUserName();
      String password = params.getPassword();
      
      while (true)
      {
        if (ctx == null)
        {
          // Calculate the ldap url first
          String ldapURL = "ldap://" + domainControllerName + ":389";
          
          Hashtable env = new Hashtable();
          env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
          env.put(Context.SECURITY_AUTHENTICATION,authentication);      
          env.put(Context.SECURITY_PRINCIPAL,userName);
          env.put(Context.SECURITY_CREDENTIALS,password);
                                    
          //connect to my domain controller
          env.put(Context.PROVIDER_URL,ldapURL);
                    
          //specify attributes to be returned in binary format
          env.put("java.naming.ldap.attributes.binary","tokenGroups objectSid");

          env.put("com.sun.jndi.ldap.connect.timeout", ldapConnectionTimeout);
          
          // Now, try the connection...
          try
          {
            Logging.authorityConnectors.info("LDAP Context environment properties: " + printLdapContextEnvironment(env));
            ctx = new InitialLdapContext(env,null);
            // If successful, break
            break;
          }
          catch (AuthenticationException e)
          {
            // This means we couldn't authenticate!
            Logging.authorityConnectors.error("Authentication problem authenticating admin user '"+userName+"': "+e.getMessage(),e);
            throw new ManifoldCFException("Authentication problem authenticating admin user '"+userName+"': "+e.getMessage(),e);
          }
          catch (CommunicationException e)
          {
            // This means we couldn't connect, most likely
            Logging.authorityConnectors.error("Couldn't communicate with domain controller '"+domainControllerName+"': "+e.getMessage(),e);
            throw new ManifoldCFException("Couldn't communicate with domain controller '"+domainControllerName+"': "+e.getMessage(),e);
          }
          catch (NamingException e)
          {
            Logging.authorityConnectors.error("Naming exception: " + e.getMessage(), e);
            throw new ManifoldCFException(e.getMessage(),e);
          }
        }
        else
        {
          // Attempt to reconnect.  I *hope* this is efficient and doesn't do unnecessary work.
          try
          {
            ctx.reconnect(null);
            // Break on apparent success
            break;
          }
          catch (AuthenticationException e)
          {
            // This means we couldn't authenticate!  Log it and retry creating a whole new context.
            Logging.authorityConnectors.error("Authentication exception: " + e.getMessage() + ", explanation: " + e.getExplanation(), e);
          }
          catch (CommunicationException e)
          {
            // This means we couldn't connect, most likely.  Log it and retry creating a whole new context.
            Logging.authorityConnectors.error("Communication exception: " + e.getMessage() + ", explanation: " + e.getExplanation(), e);
          }
          catch (NamingException e)
          {
            Logging.authorityConnectors.error("Naming exception: " + e.getMessage() + ", explanation: " + e.getExplanation(), e);
          }
          
          // So we have no chance of leaking resources, attempt to close the context.
          closeConnection();
          // Loop back around to try our luck with a fresh connection.

        }
      }
      
      // Set the expiration time anew
      expiration = System.currentTimeMillis() + expirationInterval;
      return ctx;
    }
    
    /** Close the connection handle. */
    protected void closeConnection()
    {
      if (ctx != null)
      {
        try
        {
          ctx.close();
        }
        catch (NamingException e)
        {
          // Eat this error
        }
        ctx = null;
        expiration = -1L;
      }
    }

    /** Close connection if it has expired. */
    protected void closeIfExpired(long currentTime)
    {
      if (expiration != -1L && currentTime > expiration)
        closeConnection();
    }

    /** Check if open */
    protected boolean isOpen()
    {
      return ctx != null;
    }

    /**
     * Stringifies LDAP Context environment variable
     * @param env LDAP Context environment variable
     * @return Stringified LDAP Context environment. Password is masked if set.
     */
    private String printLdapContextEnvironment(Hashtable env) {
      Hashtable copyEnv = new Hashtable<>(env);
      if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)){
        copyEnv.put(Context.SECURITY_CREDENTIALS, "********");
      }
      return Arrays.toString(copyEnv.entrySet().toArray());
    }
  }

  /** Class describing a domain suffix and corresponding domain controller name rule.
  */
  protected static class DCRule
  {
    private String suffix;
    private String domainControllerName;
    
    public DCRule(String suffix, String domainControllerName)
    {
      this.suffix = suffix;
      this.domainControllerName = domainControllerName;
    }
    
    public String getSuffix()
    {
      return suffix;
    }
    
    public String getDomainControllerName()
    {
      return domainControllerName;
    }
  }
  
  /** Class describing the connection parameters to a domain controller.
  */
  protected static class DCConnectionParameters
  {
    private String userName;
    private String password;
    private String authentication;
    private String userACLsUsername;

    public DCConnectionParameters(String userName, String password, String authentication, String userACLsUsername)
    {
      this.userName = userName;
      this.password = password;
      this.authentication = authentication;
      this.userACLsUsername = userACLsUsername;
    }
    
    public String getUserName()
    {
      return userName;
    }
    
    public String getPassword()
    {
      return password;
    }
    
    public String getAuthentication()
    {
      return authentication;
    }
    
    public String getUserACLsUsername()
    {
      return userACLsUsername;
    }
  }
  
  protected static StringSet emptyStringSet = new StringSet();
  
  /** This is the cache object descriptor for cached access tokens from
  * this connector.
  */
  protected static class AuthorizationResponseDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription
  {
    /** The user name */
    protected String userName;
    /** Connection parameters */
    protected Map<String,DCConnectionParameters> dcConnectionParams;
    /** Rules */
    protected List<DCRule> dcRules;
    /** The response lifetime */
    protected long responseLifetime;
    /** The expiration time */
    protected long expirationTime = -1;
    
    /** Constructor. */
    public AuthorizationResponseDescription(String userName, Map<String,DCConnectionParameters> dcConnectionParams,
      List<DCRule> dcRules, long responseLifetime, int LRUsize)
    {
      super("ActiveDirectoryAuthority",LRUsize);
      this.userName = userName;
      this.dcConnectionParams = dcConnectionParams;
      this.dcRules = dcRules;
      this.responseLifetime = responseLifetime;
    }

    /** Return the invalidation keys for this object. */
    public StringSet getObjectKeys()
    {
      return emptyStringSet;
    }

    /** Get the critical section name, used for synchronizing the creation of the object */
    public String getCriticalSectionName()
    {
      StringBuilder sb = new StringBuilder(getClass().getName());
      sb.append("-").append(userName);
      for (DCRule rule : dcRules)
      {
        sb.append("-").append(rule.getSuffix());
        String domainController = rule.getDomainControllerName();
        DCConnectionParameters params = dcConnectionParams.get(domainController);
        sb.append("-").append(domainController).append("-").append(params.getUserName()).append("-").append(params.getPassword());
      }
      return sb.toString();
    }

    /** Return the object expiration interval */
    public long getObjectExpirationTime(long currentTime)
    {
      if (expirationTime == -1)
        expirationTime = currentTime + responseLifetime;
      return expirationTime;
    }

    public int hashCode()
    {
      int rval = userName.hashCode();
      for (DCRule rule : dcRules)
      {
        String domainController = rule.getDomainControllerName();
        DCConnectionParameters params = dcConnectionParams.get(domainController);
        rval += rule.getSuffix().hashCode() + domainController.hashCode() + params.getUserName().hashCode() + params.getPassword().hashCode();
      }
      return rval;
    }
    
    public boolean equals(Object o)
    {
      if (!(o instanceof AuthorizationResponseDescription))
        return false;
      AuthorizationResponseDescription ard = (AuthorizationResponseDescription)o;
      if (!ard.userName.equals(userName))
        return false;
      if (ard.dcRules.size() != dcRules.size())
        return false;
      for (int i = 0 ; i < dcRules.size() ; i++)
      {
        DCRule rule = dcRules.get(i);
        DCRule ardRule = ard.dcRules.get(i);
        if (!rule.getSuffix().equals(ardRule.getSuffix()) || !rule.getDomainControllerName().equals(ardRule.getDomainControllerName()))
          return false;
        String domainController = rule.getDomainControllerName();
        DCConnectionParameters params = dcConnectionParams.get(domainController);
        DCConnectionParameters ardParams = ard.dcConnectionParams.get(domainController);
        if (!params.getUserName().equals(ardParams.getUserName()) || !params.getPassword().equals(ardParams.getPassword()))
          return false;
      }
      return true;
    }
    
  }
  
}


