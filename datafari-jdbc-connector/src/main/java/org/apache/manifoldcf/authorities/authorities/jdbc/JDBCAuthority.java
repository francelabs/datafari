/*
 * Copyright 2012 The Apache Software Foundation.
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
package org.apache.manifoldcf.authorities.authorities.jdbc;

import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.authorities.system.Logging;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.cachemanager.BaseDescription;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.jdbc.IDynamicResultRow;
import org.apache.manifoldcf.jdbc.IDynamicResultSet;
import org.apache.manifoldcf.jdbc.JDBCConnection;
import org.apache.manifoldcf.jdbc.JDBCConstants;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.*;

/**
 *
 * @author krycek
 */
public class JDBCAuthority extends BaseAuthorityConnector {

  public static final String _rcsid = "@(#)$Id: JDBCAuthority.java $";

  protected JDBCConnection connection = null;
  protected String jdbcProvider = null;
  protected String accessMethod = null;
  protected String host = null;
  protected String databaseName = null;
  protected String rawDriverString = null;
  protected boolean sslTrustAll = false;
  protected String userName = null;
  protected String password = null;

  protected String idQuery = null;
  protected String tokenQuery = null;

  private long responseLifetime = 60000L; //60sec
  private int LRUsize = 1000;
  /**
   * Cache manager.
   */
  private ICacheManager cacheManager = null;

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

    jdbcProvider = configParams.getParameter(JDBCConstants.providerParameter);
    accessMethod = configParams.getParameter(JDBCConstants.methodParameter);
    host = configParams.getParameter(JDBCConstants.hostParameter);
    databaseName = configParams.getParameter(JDBCConstants.databaseNameParameter);
    rawDriverString = configParams.getParameter(JDBCConstants.driverStringParameter);
    userName = configParams.getParameter(JDBCConstants.databaseUserName);
    password = configParams.getObfuscatedParameter(JDBCConstants.databasePassword);
    sslTrustAll = Boolean.valueOf(configParams.getParameter(JDBCConstants.sslTrustAll));

    idQuery = configParams.getParameter(JDBCConstants.databaseUserIdQuery);
    tokenQuery = configParams.getParameter(JDBCConstants.databaseTokensQuery);
  }

  /**
   * Check status of connection.
   */
  @Override
  public String check()
    throws ManifoldCFException {
    try {
      getSession();
      // Attempt to fetch a connection; if this succeeds we pass
      connection.testConnection();
      return super.check();
    } catch (ServiceInterruption e) {
      if (Logging.authorityConnectors.isDebugEnabled()) {
        Logging.authorityConnectors.debug("Service interruption in check(): " + e.getMessage(), e);
      }
      return "Transient error: " + e.getMessage();
    }
  }

  /**
   * Close the connection. Call this before discarding the repository connector.
   */
  @Override
  public void disconnect()
    throws ManifoldCFException {
    connection = null;
    host = null;
    jdbcProvider = null;
    accessMethod = null;
    databaseName = null;
    rawDriverString = null;
    userName = null;
    password = null;

    super.disconnect();
  }

  /**
   * Set up a session
   */
  protected void getSession()
    throws ManifoldCFException, ServiceInterruption {
    if (connection == null) {
      if (jdbcProvider == null || jdbcProvider.length() == 0) {
        throw new ManifoldCFException("Missing parameter '" + JDBCConstants.providerParameter + "'");
      }
      if ((host == null || host.length() == 0) && (rawDriverString == null || rawDriverString.length() == 0))
        throw new ManifoldCFException("Missing parameter '"+JDBCConstants.hostParameter+"' or '"+JDBCConstants.driverStringParameter+"'");

      connection = new JDBCConnection(jdbcProvider,(accessMethod==null || accessMethod.equals("name")),host,databaseName,rawDriverString,userName,password, sslTrustAll);
    }
  }

  private String createCacheConnectionString() {
    StringBuilder sb = new StringBuilder();
    sb.append(jdbcProvider).append("|")
      .append((host==null)?"":host).append("|")
      .append((databaseName==null)?"":databaseName).append("|")
      .append((rawDriverString==null)?"":rawDriverString).append("|")
      .append(userName);
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
    // Construct a cache description object
    ICacheDescription objectDescription = new JdbcAuthorizationResponseDescription(userName, createCacheConnectionString(), idQuery, tokenQuery, this.responseLifetime, this.LRUsize);

    // Enter the cache
    ICacheHandle ch = cacheManager.enterCache(new ICacheDescription[]{objectDescription}, null, null);
    try {
      ICacheCreateHandle createHandle = cacheManager.enterCreateSection(ch);
      try {
        // Lookup the object
        AuthorizationResponse response = (AuthorizationResponse) cacheManager.lookupObject(createHandle, objectDescription);
        if (response != null) {
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

  public AuthorizationResponse getAuthorizationResponseUncached(String userName)
    throws ManifoldCFException {
    try
    {
      getSession();

      VariableMap vm = new VariableMap();
      addConstant(vm, JDBCConstants.idReturnVariable, JDBCConstants.idReturnColumnName);
      addVariable(vm, JDBCConstants.userNameVariable, userName);

      // Find user id
      ArrayList paramList = new ArrayList();
      StringBuilder sb = new StringBuilder();
      substituteQuery(idQuery, vm, sb, paramList);

      IDynamicResultSet idSet;
      try {
        idSet = connection.executeUncachedQuery(sb.toString(),paramList,-1);
      }
      catch (ServiceInterruption e)
      {
        return RESPONSE_UNREACHABLE;
      }
      catch (ManifoldCFException e)
      {
        throw e;
      }

      String uid;
      try {
        IDynamicResultRow row = idSet.getNextRow();
        if (row == null)
          return RESPONSE_USERNOTFOUND;
        try
        {
          Object oUid = row.getValue(JDBCConstants.idReturnColumnName);
          if (oUid == null)
            throw new ManifoldCFException("Bad id query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\".");
          uid = JDBCConnection.readAsString(oUid);
        }
        finally
        {
          row.close();
        }
      } finally {
        idSet.close();
      }

      if (uid.isEmpty()) {
        return RESPONSE_USERNOTFOUND;
      }

      // now check tokens
      vm = new VariableMap();
      addConstant(vm, JDBCConstants.tokenReturnVariable, JDBCConstants.tokenReturnColumnName);
      addVariable(vm, JDBCConstants.userNameVariable, userName);
      addVariable(vm, JDBCConstants.userIDVariable, uid);
      sb = new StringBuilder();
      paramList = new ArrayList();
      substituteQuery(tokenQuery, vm, sb, paramList);
      
      try {
        idSet = connection.executeUncachedQuery(sb.toString(),paramList,-1);
      }
      catch (ServiceInterruption e)
      {
        return RESPONSE_UNREACHABLE;
      }
      catch (ManifoldCFException e)
      {
        throw e;
      }

      ArrayList<String> tokenArray = new ArrayList<String>();
      try {
        while (true)
        {
          IDynamicResultRow row = idSet.getNextRow();
          if (row == null)
            break;
          try
          {
            Object oToken = row.getValue(JDBCConstants.tokenReturnColumnName);
            if (oToken == null)
              throw new ManifoldCFException("Bad token query; doesn't return $(TOKENCOLUMN) column.  Try using quotes around $(TOKENCOLUMN) variable, e.g. \"$(TOKENCOLUMN)\".");
            String token = JDBCConnection.readAsString(oToken);

            if (!token.isEmpty()) {
              tokenArray.add(token);
            }
          }
          finally
          {
            row.close();
          }
        }
      } finally {
        idSet.close();
      }
      return new AuthorizationResponse(tokenArray.toArray(new String[0]), AuthorizationResponse.RESPONSE_OK);
    }
    catch (ServiceInterruption e)
    {
      Logging.authorityConnectors.warn("JDBCAuthority: Service interruption: "+e.getMessage(),e);
      return RESPONSE_UNREACHABLE;
    }
  }

  // UI support methods.
  //
  // These support methods come in two varieties.  The first bunch is involved in setting up connection configuration information.  The second bunch
  // is involved in presenting and editing document specification information for a job.  The two kinds of methods are accordingly treated differently,
  // in that the first bunch cannot assume that the current connector object is connected, while the second bunch can.  That is why the first bunch
  // receives a thread context argument for all UI methods, while the second bunch does not need one (since it has already been applied via the connect()
  // method, above).
  /**
   * Output the configuration header section. This method is called in the head
   * section of the connector's configuration page. Its purpose is to add the
   * required tabs to the list, and to output any javascript methods that might
   * be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @param tabsArray is an array of tab names. Add to this array any tab names
   * that are specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, List<String> tabsArray)
    throws ManifoldCFException, IOException {
    tabsArray.add(Messages.getString(locale, "JDBCAuthority.DatabaseType"));
    tabsArray.add(Messages.getString(locale, "JDBCAuthority.Server"));
    tabsArray.add(Messages.getString(locale, "JDBCAuthority.Credentials"));
    tabsArray.add(Messages.getString(locale, "JDBCAuthority.Queries"));

    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"+
"function checkConfigForSave()\n"+
"{\n"+
"  if (editconnection.databasehost.value == \"\" && editconnection.rawjdbcstring.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.PleaseFillInADatabaseServerName") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.Server") + "\");\n"+
"    editconnection.databasehost.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editconnection.databasename.value == \"\" && editconnection.rawjdbcstring.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.PleaseFillInTheNameOfTheDatabase") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.Server") + "\");\n"+
"    editconnection.databasename.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editconnection.username.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.PleaseSupplyTheDatabaseUsernameForThisConnection") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale, "JDBCAuthority.Credentials") + "\");\n"+
"    editconnection.username.focus();\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"\n"+
"//-->\n"+
"</script>\n"
    );
  }

  /**
   * Output the configuration body section. This method is called in the body
   * section of the connector's configuration page. Its purpose is to present
   * the required form elements for editing. The coder can presume that the HTML
   * that is output from this configuration will be within appropriate &lt;html&gt;,
   * &lt;body&gt;, and &lt;form&gt; tags. The name of the form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param out is the output to which any HTML should be sent.
   * @param parameters are the configuration parameters, as they currently
   * exist, for this connection being configured.
   * @param tabName is the current tab name.
   */
  @Override
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, String tabName)
    throws ManifoldCFException, IOException {
    String lJdbcProvider = parameters.getParameter(JDBCConstants.providerParameter);
    if (lJdbcProvider == null) {
      lJdbcProvider = JDBCConstants.PROVIDER_ORACLE;
    }
    String lAccessMethod = parameters.getParameter(JDBCConstants.methodParameter);
    if (lAccessMethod == null)
      lAccessMethod = "name";
    String lHost = parameters.getParameter(JDBCConstants.hostParameter);
    if (lHost == null) {
      lHost = "localhost";
    }
    String lDatabaseName = parameters.getParameter(JDBCConstants.databaseNameParameter);
    if (lDatabaseName == null) {
      lDatabaseName = "database";
    }
    String rawJDBCString = parameters.getParameter(JDBCConstants.driverStringParameter);
    if (rawJDBCString == null)
      rawJDBCString = "";
    boolean sslTrustAll = Boolean.valueOf(parameters.getParameter(JDBCConstants.sslTrustAll));
    String databaseUser = parameters.getParameter(JDBCConstants.databaseUserName);
    if (databaseUser == null) {
      databaseUser = "";
    }
    String databasePassword = parameters.getObfuscatedParameter(JDBCConstants.databasePassword);
    if (databasePassword == null) {
      databasePassword = "";
    } else {
      databasePassword = out.mapPasswordToKey(databasePassword);
    }
    String lIdQuery = parameters.getParameter(JDBCConstants.databaseUserIdQuery);
    if (lIdQuery == null) {
      lIdQuery = "SELECT idfield AS $(IDCOLUMN) FROM usertable WHERE login = $(USERNAME)";
    }
    String lTokenQuery = parameters.getParameter(JDBCConstants.databaseTokensQuery);
    if (lTokenQuery == null) {
      lTokenQuery = "SELECT groupnamefield AS $(TOKENCOLUMN) FROM grouptable WHERE user_id = $(UID) OR login = $(USERNAME)";
    }

    // "Database Type" tab
    if (tabName.equals(Messages.getString(locale, "JDBCAuthority.DatabaseType"))) {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.DatabaseType2") + "</nobr></td><td class=\"value\">\n"+
"      <select multiple=\"false\" name=\"databasetype\" style=\"height: 75px;\">\n"+
"        <option value=\""+JDBCConstants.PROVIDER_ORACLE+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_ORACLE) ? "selected=\"selected\"" : "") + ">Oracle</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_POSTGRESQL+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_POSTGRESQL) ? "selected=\"selected\"" : "") + ">Postgres SQL</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_JTDS_SQLSERVER+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_JTDS_SQLSERVER) ? "selected=\"selected\"" : "") + ">MS SQL Server (&gt; V6.5)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MS_SQLSERVER+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_MS_SQLSERVER) ? "selected=\"selected\"" : "") + ">MS SQL Server (&gt;= 2012)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_SYBASE+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_SYBASE) ? "selected=\"selected\"" : "") + ">Sybase (&gt;= V10)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MYSQL+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_MYSQL) ? "selected=\"selected\"" : "") + ">MySQL (&gt;= V5)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MARIADB+"\" " + (lJdbcProvider.equals(JDBCConstants.PROVIDER_MARIADB) ? "selected=\"selected\"" : "") + ">MariaDB</option>\n"+
"      </select>\n"+
"    </td>\n"+
"  </tr>\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCAuthority.AccessMethod") + "</nobr></td><td class=\"value\">\n"+
"      <select multiple=\"false\" name=\"accessmethod\" style=\"height: 50px;\">\n"+
"        <option value=\"name\" "+(lAccessMethod.equals("name")?"selected=\"selected\"":"")+">"+Messages.getBodyString(locale,"JDBCAuthority.ByName")+"</option>\n"+
"        <option value=\"label\" "+(lAccessMethod.equals("label")?"selected=\"selected\"":"")+">"+Messages.getBodyString(locale,"JDBCAuthority.ByLabel")+"</option>\n"+
"      </select>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n");
    } else {
      out.print(
"<input type=\"hidden\" name=\"databasetype\" value=\"" + lJdbcProvider + "\"/>\n"+
"<input type=\"hidden\" name=\"accessmethod\" value=\""+lAccessMethod+"\"/>\n"
      );
    }

    // "Server" tab
    if (tabName.equals(Messages.getString(locale, "JDBCAuthority.Server"))) {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.DatabaseHostAndPort") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"64\" name=\"databasehost\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lHost) + "\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.DatabaseServiceNameOrInstanceDatabase") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"32\" name=\"databasename\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lDatabaseName) + "\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCAuthority.RawDatabaseConnectString") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"80\" name=\"rawjdbcstring\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(rawJDBCString)+"\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCAuthority.SSLTrustAll") + "</nobr></td><td class=\"value\"><input type=\"checkbox\" name=\""+ JDBCConstants.sslTrustAll +"\" value=\"true\""+ (sslTrustAll ? "checked":"") + " /></td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    } else {
      out.print(
"<input type=\"hidden\" name=\"databasehost\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lHost) + "\"/>\n"+
"<input type=\"hidden\" name=\"databasename\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lDatabaseName) + "\"/>\n"+
"<input type=\"hidden\" name=\"rawjdbcstring\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(rawJDBCString)+"\"/>\n"+
"<input type=\"hidden\" name=\""+ JDBCConstants.sslTrustAll +"\" value=\""+sslTrustAll+"\"/>\n"
      );
    }

    // "Credentials" tab
    if (tabName.equals(Messages.getString(locale, "JDBCAuthority.Credentials"))) {
      out.print(
        "<table class=\"displaytable\">\n"
        + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
        + "  <tr>\n"
        + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.UserName") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"32\" name=\"username\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseUser) + "\"/></td>\n"
        + "  </tr>\n"
        + "  <tr>\n"
        + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.Password") + "</nobr></td><td class=\"value\"><input type=\"password\" size=\"32\" name=\"password\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databasePassword) + "\"/></td>\n"
        + "  </tr>\n"
        + "</table>\n");
    } else {
      out.print(
        "<input type=\"hidden\" name=\"username\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseUser) + "\"/>\n"
        + "<input type=\"hidden\" name=\"password\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databasePassword) + "\"/>\n");
    }

    if (tabName.equals(Messages.getString(locale, "JDBCAuthority.Queries"))) {
      out.print(
        "<table class=\"displaytable\">\n"
        + "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
        + "  <tr>\n"
        + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.UserIdQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.returnUserIdOrEmptyResultset") + "</nobr></td>\n"
        + "    <td class=\"value\"><textarea name=\"idquery\" cols=\"64\" rows=\"6\">" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(lIdQuery) + "</textarea></td>\n"
        + "  </tr>\n"
        + "  <tr>\n"
        + "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.TokenQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.returnTokensForUser") + "</nobr></td>\n"
        + "    <td class=\"value\"><textarea name=\"tokenquery\" cols=\"64\" rows=\"6\">" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(lTokenQuery) + "</textarea></td>\n"
        + "  </tr>\n"
        + "</table>\n");
    } else {
      out.print(
        "<input type=\"hidden\" name=\"idquery\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lIdQuery) + "\"/>\n"
        + "<input type=\"hidden\" name=\"tokenquery\" value=\"" + org.apache.manifoldcf.ui.util.Encoder.attributeEscape(lTokenQuery) + "\"/>\n");
    }

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
   * @return null if all is well, or a string error message if there is an error
   * that should prevent saving of the connection (and cause a redirection to an
   * error page).
   */
  @Override
  public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
    Locale locale, ConfigParams parameters)
    throws ManifoldCFException {
    String type = variableContext.getParameter("databasetype");
    if (type != null) {
      parameters.setParameter(JDBCConstants.providerParameter, type);
    }

    String accessMethod = variableContext.getParameter("accessmethod");
    if (accessMethod != null)
      parameters.setParameter(JDBCConstants.methodParameter,accessMethod);

    String lHost = variableContext.getParameter("databasehost");
    if (lHost != null) {
      parameters.setParameter(JDBCConstants.hostParameter, lHost);
    }

    String lDatabaseName = variableContext.getParameter("databasename");
    if (lDatabaseName != null) {
      parameters.setParameter(JDBCConstants.databaseNameParameter, lDatabaseName);
    }

    String rawJDBCString = variableContext.getParameter("rawjdbcstring");
    if (rawJDBCString != null)
      parameters.setParameter(JDBCConstants.driverStringParameter,rawJDBCString);

    String sslTrustAll = variableContext.getParameter(JDBCConstants.sslTrustAll);
    if (sslTrustAll != null)
      parameters.setParameter(JDBCConstants.sslTrustAll,sslTrustAll);

    String lUserName = variableContext.getParameter("username");
    if (lUserName != null) {
      parameters.setParameter(JDBCConstants.databaseUserName, lUserName);
    }

    String lPassword = variableContext.getParameter("password");
    if (lPassword != null) {
      parameters.setObfuscatedParameter(JDBCConstants.databasePassword, variableContext.mapKeyToPassword(lPassword));
    }

    String lIdQuery = variableContext.getParameter("idquery");
    if (lIdQuery != null) {
      parameters.setParameter(JDBCConstants.databaseUserIdQuery, lIdQuery);
    }

    String lTokenQuery = variableContext.getParameter("tokenquery");
    if (lTokenQuery != null) {
      parameters.setParameter(JDBCConstants.databaseTokensQuery, lTokenQuery);
    }

    return null;
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
    Locale locale, ConfigParams parameters)
    throws ManifoldCFException, IOException {
    out.print(
      "<table class=\"displaytable\">\n"
      + "  <tr>\n"
      + "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale, "JDBCAuthority.Parameters") + "</nobr></td>\n"
      + "    <td class=\"value\" colspan=\"3\">\n");
    Iterator iter = parameters.listParameters();
    while (iter.hasNext()) {
      String param = (String) iter.next();
      String value = parameters.getParameter(param);
      if (param.length() >= "password".length() && param.substring(param.length() - "password".length()).equalsIgnoreCase("password")) {
        out.print(
          "      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=********</nobr><br/>\n");
      } else if (param.length() >= "keystore".length() && param.substring(param.length() - "keystore".length()).equalsIgnoreCase("keystore")) {
        IKeystoreManager kmanager = KeystoreManagerFactory.make("", value);
        out.print(
          "      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=&lt;" + Integer.toString(kmanager.getContents().length) + " certificate(s)&gt;</nobr><br/>\n");
      } else {
        out.print(
          "      <nobr>" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param) + "=" + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value) + "</nobr><br/>\n");
      }
    }
    out.print(
      "    </td>\n"
      + "  </tr>\n"
      + "</table>\n");
  }

  /**
   * Given a query, and a parameter map, substitute it. Each variable
   * substitutes the string, and it also substitutes zero or more query
   * parameters.
   */
  protected static void substituteQuery(String inputString, VariableMap inputMap, StringBuilder outputQuery, ArrayList outputParams)
    throws ManifoldCFException {
    // We are looking for strings that look like this: $(something)
    // Right at the moment we don't care even about quotes, so we just want to look for $(.
    int startIndex = 0;
    while (true) {
      int nextIndex = inputString.indexOf("$(", startIndex);
      if (nextIndex == -1) {
        outputQuery.append(inputString.substring(startIndex));
        break;
      }
      int endIndex = inputString.indexOf(")", nextIndex);
      if (endIndex == -1) {
        outputQuery.append(inputString.substring(startIndex));
        break;
      }
      String variableName = inputString.substring(nextIndex + 2, endIndex);
      VariableMapItem item = inputMap.getVariable(variableName);
      if (item == null) {
        throw new ManifoldCFException("No such substitution variable: $(" + variableName + ")");
      }
      outputQuery.append(inputString.substring(startIndex, nextIndex));
      outputQuery.append(item.getValue());
      ArrayList inputParams = item.getParameters();
      if (inputParams != null) {
        int i = 0;
        while (i < inputParams.size()) {
          Object x = inputParams.get(i++);
          outputParams.add(x);
        }
      }
      startIndex = endIndex + 1;
    }
  }

  /**
   * Add string query variables
   */
  protected static void addVariable(VariableMap map, String varName, String variable) {
    ArrayList params = new ArrayList();
    params.add(variable);
    map.addVariable(varName, "?", params);
  }

  /**
   * Add string query constants
   */
  protected static void addConstant(VariableMap map, String varName, String value) {
    map.addVariable(varName, value, null);
  }

  // pass params to preparedStatement
  protected static void loadPS(PreparedStatement ps, ArrayList data)
    throws java.sql.SQLException, ManifoldCFException {
    if (data != null) {
      for (int i = 0; i < data.size(); i++) {
        // If the input type is a string, then set it as such.
        // Otherwise, if it's an input stream, we make a blob out of it.
        Object x = data.get(i);
        if (x instanceof String) {
          String value = (String) x;
          // letting database do lame conversion!
          ps.setString(i + 1, value);
        }
        if (x instanceof BinaryInput) {
          BinaryInput value = (BinaryInput) x;
          // System.out.println("Blob length on write = "+Long.toString(value.getLength()));
          // The oracle driver does a binary conversion to base 64 when writing data
          // into a clob column using a binary stream operator.  Since at this
          // point there is no way to distinguish the two, and since our tests use CLOB,
          // this code doesn't work for them.
          // So, for now, use the ascii stream method.
          //ps.setBinaryStream(i+1,value.getStream(),(int)value.getLength());
          ps.setAsciiStream(i + 1, value.getStream(), (int) value.getLength());
        }
        if (x instanceof java.util.Date) {
          ps.setDate(i + 1, new java.sql.Date(((java.util.Date) x).getTime()));
        }
        if (x instanceof Long) {
          ps.setLong(i + 1, ((Long) x).longValue());
        }
        if (x instanceof TimeMarker) {
          ps.setTimestamp(i + 1, new java.sql.Timestamp(((Long) x).longValue()));
        }
        if (x instanceof Double) {
          ps.setDouble(i + 1, ((Double) x).doubleValue());
        }
        if (x instanceof Integer) {
          ps.setInt(i + 1, ((Integer) x).intValue());
        }
        if (x instanceof Float) {
          ps.setFloat(i + 1, ((Float) x).floatValue());
        }
      }
    }
  }

  /**
   * Variable map entry.
   */
  protected static class VariableMapItem {

    protected String value;
    protected ArrayList params;

    /**
     * Constructor.
     */
    public VariableMapItem(String value, ArrayList params) {
      this.value = value;
      this.params = params;
    }

    /**
     * Get value.
     */
    public String getValue() {
      return value;
    }

    /**
     * Get parameters.
     */
    public ArrayList getParameters() {
      return params;
    }
  }

  /**
   * Variable map.
   */
  protected static class VariableMap {

    protected Map variableMap = new HashMap();

    /**
     * Constructor
     */
    public VariableMap() {
    }

    /**
     * Add a variable map entry
     */
    public void addVariable(String variableName, String value, ArrayList parameters) {
      VariableMapItem e = new VariableMapItem(value, parameters);
      variableMap.put(variableName, e);
    }

    /**
     * Get a variable map entry
     */
    public VariableMapItem getVariable(String variableName) {
      return (VariableMapItem) variableMap.get(variableName);
    }
  }
  protected static StringSet emptyStringSet = new StringSet();

  /**
   * This is the cache object descriptor for cached access tokens from this
   * connector.
   */
  protected class JdbcAuthorizationResponseDescription extends BaseDescription {

    /**
     * The user name
     */
    protected final String userName;
    /**
     * LDAP connection string with server name and base DN
     */
    protected final String connectionString;
    /** The user query. */
    protected final String userQuery;
    /** The token query. */
    protected final String tokenQuery;
    /**
     * The response lifetime
     */
    protected final long responseLifetime;
    /**
     * The expiration time
     */
    protected long expirationTime = -1;

    /**
     * Constructor.
     */
    public JdbcAuthorizationResponseDescription(String userName, String connectionString, String userQuery, String tokenQuery, long responseLifetime, int LRUsize) {
      super("JDBCAuthority", LRUsize);
      this.userName = userName;
      this.connectionString = connectionString;
      this.userQuery = userQuery;
      this.tokenQuery = tokenQuery;
      this.responseLifetime = responseLifetime;
    }

    /**
     * Return the invalidation keys for this object.
     */
    public StringSet getObjectKeys() {
      return emptyStringSet;
    }

    /**
     * Get the critical section name, used for synchronizing the creation of the
     * object
     */
    public String getCriticalSectionName() {
      StringBuilder sb = new StringBuilder(getClass().getName());
      sb.append("-").append(userName).append("-").append(connectionString).append("-")
        .append(userQuery).append("-").append(tokenQuery);
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
      return userName.hashCode() + connectionString.hashCode() +
        userQuery.hashCode() + tokenQuery.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof JdbcAuthorizationResponseDescription)) {
        return false;
      }
      JdbcAuthorizationResponseDescription ard = (JdbcAuthorizationResponseDescription) o;
      if (!ard.userName.equals(userName)) {
        return false;
      }
      if (!ard.connectionString.equals(connectionString)) {
        return false;
      }
      if (!ard.userQuery.equals(userQuery)) {
        return false;
      }
      if (!ard.tokenQuery.equals(tokenQuery)) {
        return false;
      }
      return true;
    }
  }
}
