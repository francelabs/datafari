/* $Id: JDBCConnector.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package org.apache.manifoldcf.crawler.connectors.jdbc;

import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.connectorcommon.interfaces.IKeystoreManager;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.IExistingVersions;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.jdbc.IDynamicResultRow;
import org.apache.manifoldcf.jdbc.IDynamicResultSet;
import org.apache.manifoldcf.jdbc.JDBCConnection;
import org.apache.manifoldcf.jdbc.JDBCConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** JDBC repository connector.
*/
public class JDBCConnector extends org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector
{
  public static final String _rcsid = "@(#)$Id: JDBCConnector.java 988245 2010-08-23 18:39:35Z kwright $";

  // Activities that we know about
  protected final static String ACTIVITY_EXTERNAL_QUERY = "external query";
  protected final static String ACTIVITY_FETCH = "fetch";
  
  // Activities list
  protected static final String[] activitiesList = new String[]{ACTIVITY_EXTERNAL_QUERY, ACTIVITY_FETCH};

  /** Deny access token for default authority */
  private final static String defaultAuthorityDenyToken = "DEAD_AUTHORITY";

  protected JDBCConnection connection = null;
  protected String jdbcProvider = null;
  protected String accessMethod = null;
  protected String host = null;
  protected String databaseName = null;
  protected String rawDriverString = null;
  protected boolean sslTrustAll = false;
  protected String userName = null;
  protected String password = null;

  /** Constructor.
  */
  public JDBCConnector()
  {
  }

  /** Set up a session */
  protected void getSession()
    throws ManifoldCFException
  {
    if (connection == null)
    {
      if (jdbcProvider == null || jdbcProvider.length() == 0)
        throw new ManifoldCFException("Missing parameter '"+JDBCConstants.providerParameter+"'");
      if ((host == null || host.length() == 0) && (rawDriverString == null || rawDriverString.length() == 0))
        throw new ManifoldCFException("Missing parameter '"+JDBCConstants.hostParameter+"' or '"+JDBCConstants.driverStringParameter+"'");

      connection = new JDBCConnection(jdbcProvider,(accessMethod==null || accessMethod.equals("name")),host,databaseName,rawDriverString,userName,password, sslTrustAll);
    }
  }

  /** Return the list of activities that this connector supports (i.e. writes into the log).
  *@return the list.
  */
  @Override
  public String[] getActivitiesList()
  {
    return activitiesList;
  }

  /** Model.  Depending on what people enter for the seeding query, this could be either ALL or
  * could be less than that.  So, I've decided it will be at least the adds and changes, and
  * won't include the deletes.
  */
  @Override
  public int getConnectorModel()
  {
    return MODEL_ADD_CHANGE;
  }

  /** Connect.  The configuration parameters are included.
  *@param configParams are the configuration parameters for this connection.
  */
  @Override
  public void connect(ConfigParams configParams)
  {
    super.connect(configParams);

    jdbcProvider = configParams.getParameter(JDBCConstants.providerParameter);
    accessMethod = configParams.getParameter(JDBCConstants.methodParameter);
    host = configParams.getParameter(JDBCConstants.hostParameter);
    databaseName = configParams.getParameter(JDBCConstants.databaseNameParameter);
    rawDriverString = configParams.getParameter(JDBCConstants.driverStringParameter);
    sslTrustAll = Boolean.valueOf(configParams.getParameter(JDBCConstants.sslTrustAll));
    userName= configParams.getParameter(JDBCConstants.databaseUserName);
    password = configParams.getObfuscatedParameter(JDBCConstants.databasePassword);
  }

  /** Check status of connection.
  */
  @Override
  public String check()
    throws ManifoldCFException
  {
    try
    {
      getSession();
      // Attempt to fetch a connection; if this succeeds we pass
      connection.testConnection();
      return super.check();
    }
    catch (ServiceInterruption e)
    {
      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("Service interruption in check(): "+e.getMessage(),e);
      return "Transient error: "+e.getMessage();
    }
  }

  /** Close the connection.  Call this before discarding the repository connector.
  */
  @Override
  public void disconnect()
    throws ManifoldCFException
  {
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

  /** Get the bin name string for a document identifier.  The bin name describes the queue to which the
  * document will be assigned for throttling purposes.  Throttling controls the rate at which items in a
  * given queue are fetched; it does not say anything about the overall fetch rate, which may operate on
  * multiple queues or bins.
  * For example, if you implement a web crawler, a good choice of bin name would be the server name, since
  * that is likely to correspond to a real resource that will need real throttle protection.
  *@param documentIdentifier is the document identifier.
  *@return the bin name.
  */
  @Override
  public String[] getBinNames(String documentIdentifier)
  {
    return new String[]{(rawDriverString==null||rawDriverString.length()==0)?host:rawDriverString};
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
    throws ManifoldCFException, ServiceInterruption
  {
    long startTime;
    if (lastSeedVersion == null)
      startTime = 0L;
    else
    {
      // Unpack seed time from seed version string
      startTime = new Long(lastSeedVersion).longValue();
    }

    getSession();

    // Set up the query
    TableSpec ts = new TableSpec(spec);

    VariableMap vm = new VariableMap();
    addConstant(vm,JDBCConstants.idReturnVariable,JDBCConstants.idReturnColumnName);
    addVariable(vm,JDBCConstants.startTimeVariable,startTime);
    addVariable(vm,JDBCConstants.endTimeVariable,seedTime);

    // Do the substitution
    ArrayList paramList = new ArrayList();
    StringBuilder sb = new StringBuilder();
    substituteQuery(ts.idQuery,vm,sb,paramList);

    IDynamicResultSet idSet;

    String queryText = sb.toString();
    long startQueryTime = System.currentTimeMillis();
    // Contract for IDynamicResultset indicates that if successfully obtained, it MUST
    // be closed.
    try
    {
      idSet = connection.executeUncachedQuery(queryText,paramList,-1);
    }
    catch (ServiceInterruption e)
    {
      // If failure, record the failure.
      activities.recordActivity(new Long(startQueryTime), ACTIVITY_EXTERNAL_QUERY, null,
        createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
      throw e;
    }
    catch (ManifoldCFException e)
    {
      // If failure, record the failure.
      activities.recordActivity(new Long(startQueryTime), ACTIVITY_EXTERNAL_QUERY, null,
        createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
      throw e;
    }

    try
    {
      // If success, record that too.
      activities.recordActivity(new Long(startQueryTime), ACTIVITY_EXTERNAL_QUERY, null,
        createQueryString(queryText,paramList), "OK", null, null);

      while (true)
      {
        IDynamicResultRow row = idSet.getNextRow();
        if (row == null)
          break;
        try
        {
          Object o = row.getValue(JDBCConstants.idReturnColumnName);
          if (o == null)
            throw new ManifoldCFException("Bad seed query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\", or, for MySQL, select \"by label\" in your repository connection.");
          String idValue = JDBCConnection.readAsString(o);
          activities.addSeedDocument(idValue);
        }
        finally
        {
          row.close();
        }
      }
    }
    finally
    {
      idSet.close();
    }
    return new Long(seedTime).toString();
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
    throws ManifoldCFException, ServiceInterruption
  {
    TableSpec ts = new TableSpec(spec);
    
    Set<String> acls = ts.getAcls();

    String[] versionsReturned = new String[documentIdentifiers.length];

    // If there is no version query, then always return empty string for all documents.
    // This will mean that processDocuments will be called
    // for all.  ProcessDocuments will then be responsible for doing document deletes itself,
    // based on the query results.

    Map<String,String> documentVersions = new HashMap<String,String>();
    if (ts.versionQuery != null && ts.versionQuery.length() > 0)
    {
      // If there IS a versions query, do it.  First set up the variables, then do the substitution.
      VariableMap vm = new VariableMap();
      addConstant(vm,JDBCConstants.idReturnVariable,JDBCConstants.idReturnColumnName);
      addConstant(vm,JDBCConstants.versionReturnVariable,JDBCConstants.versionReturnColumnName);
      if (addIDList(vm,JDBCConstants.idListVariable,documentIdentifiers,null))
      {
        // Do the substitution
        ArrayList paramList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        substituteQuery(ts.versionQuery,vm,sb,paramList);

        // Now, build a result return, and a hash table so we can correlate the returned values with the place to put them.
        // We presume that if the row is missing, the document is gone.
        // Fire off the query!
        getSession();
        IDynamicResultSet result;
        String queryText = sb.toString();
        long startTime = System.currentTimeMillis();
        // Get a dynamic resultset.  Contract for dynamic resultset is that if
        // one is returned, it MUST be closed, or a connection will leak.
        try
        {
          result = connection.executeUncachedQuery(queryText,paramList,-1);
        }
        catch (ManifoldCFException e)
        {
          // If failure, record the failure.
          if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
            activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
              createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
          throw e;
        }
        try
        {
          // If success, record that too.
          activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
            createQueryString(queryText,paramList), "OK", null, null);
          // Now, go through resultset
          while (true)
          {
            IDynamicResultRow row = result.getNextRow();
            if (row == null)
              break;
            try
            {
              Object o = row.getValue(JDBCConstants.idReturnColumnName);
              if (o == null)
                throw new ManifoldCFException("Bad version query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\", or, for MySQL, select \"by label\" in your repository connection.");
              String idValue = JDBCConnection.readAsString(o);
              o = row.getValue(JDBCConstants.versionReturnColumnName);
              String versionValue;
              // Null version is OK; make it a ""
              if (o == null)
                versionValue = "";
              else
                versionValue = JDBCConnection.readAsString(o);
              documentVersions.put(idValue,versionValue);
            }
            finally
            {
              row.close();
            }
          }
        }
        finally
        {
          result.close();
        }
      }
    }
    else
    {
      for (String documentIdentifier : documentIdentifiers)
      {
        documentVersions.put(documentIdentifier,"");
      }
    }

    // Delete the documents that had no version, and work only on ones that did
    final Set<String> fetchDocuments = documentVersions.keySet();
    for (String documentIdentifier : documentIdentifiers)
    {
      String documentVersion = documentVersions.get(documentIdentifier);
      if (documentVersion == null)
      {
        activities.deleteDocument(documentIdentifier);
        continue;
      }
    }

    // Pick up document acls
    Map<String,Set<String>> documentAcls = new HashMap<String,Set<String>>();
    if (ts.securityOn)
    {
      if (acls.size() == 0 && ts.aclQuery != null && ts.aclQuery.length() > 0)
      {
        // If there IS an acls query, do it.  First set up the variables, then do the substitution.
        VariableMap vm = new VariableMap();
        addConstant(vm,JDBCConstants.idReturnVariable,JDBCConstants.idReturnColumnName);
        addConstant(vm,JDBCConstants.tokenReturnVariable,JDBCConstants.tokenReturnColumnName);
        if (addIDList(vm,JDBCConstants.idListVariable,documentIdentifiers,fetchDocuments))
        {
          // Do the substitution
          ArrayList paramList = new ArrayList();
          StringBuilder sb = new StringBuilder();
          substituteQuery(ts.aclQuery,vm,sb,paramList);

          // Fire off the query!
          getSession();
          IDynamicResultSet result;
          String queryText = sb.toString();
          long startTime = System.currentTimeMillis();
          // Get a dynamic resultset.  Contract for dynamic resultset is that if
          // one is returned, it MUST be closed, or a connection will leak.
          try
          {
            result = connection.executeUncachedQuery(queryText,paramList,-1);
          }
          catch (ManifoldCFException e)
          {
            // If failure, record the failure.
            if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
              activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
                createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
            throw e;
          }
          try
          {
            // If success, record that too.
            activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
              createQueryString(queryText,paramList), "OK", null, null);
            // Now, go through resultset
            while (true)
            {
              IDynamicResultRow row = result.getNextRow();
              if (row == null)
                break;
              try
              {
                Object o = row.getValue(JDBCConstants.idReturnColumnName);
                if (o == null)
                  throw new ManifoldCFException("Bad acl query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\", or, for MySQL, select \"by label\" in your repository connection.");
                String idValue = JDBCConnection.readAsString(o);
                o = row.getValue(JDBCConstants.tokenReturnColumnName);
                String tokenValue;
                if (o == null)
                  tokenValue = "";
                else
                  tokenValue = JDBCConnection.readAsString(o);
                // Versions that are "", when processed, will have their acls fetched at that time...
                Set<String> dcs = documentAcls.get(idValue);
                if (dcs == null)
                {
                  dcs = new HashSet<String>();
                  documentAcls.put(idValue,dcs);
                }
                dcs.add(tokenValue);
              }
              finally
              {
                row.close();
              }
            }
          }
          finally
          {
            result.close();
          }
        }
      }
      else
      {
        for (String documentIdentifier : fetchDocuments)
        {
          documentAcls.put(documentIdentifier,acls);
        }
      }
    }
    
    // Map from identifier to version string
    final Map<String,String> map = new HashMap<>();
    // This is the set of documents actually seen
    final Set<String> seenDocuments = new HashSet<>();
    
    for (String documentIdentifier : fetchDocuments)
    {
      String documentVersion = documentVersions.get(documentIdentifier);
      if (documentVersion.length() == 0)
      {
        map.put(documentIdentifier,documentVersion);
      }
      else
      {
        // Compute a full version string
        StringBuilder sb = new StringBuilder();
        Set<String> dAcls = documentAcls.get(documentIdentifier);
        if (dAcls == null)
          sb.append('-');
        else
        {
          sb.append('+');
          String[] aclValues = new String[dAcls.size()];
          int k = 0;
          for (String acl : dAcls)
          {
            aclValues[k++] = acl;
          }
          Arrays.sort(aclValues);
          packList(sb,aclValues,'+');
        }

        sb.append(documentVersion).append("=").append(ts.dataQuery);
        String versionValue = sb.toString();

        if (activities.checkDocumentNeedsReindexing(documentIdentifier,versionValue))
        {
          map.put(documentIdentifier,versionValue);
        }
      }
    }
    
    // We have a primary query and a number of attribute queries to execute.
    // We execute the attribute queries first because those do not include binary data.
    final Map<String, Map<String, Set<String>>> attributeValues = new HashMap<String, Map<String, Set<String>>>();
    int index = 0;
    while (index < spec.getChildCount())
    {
      SpecificationNode sn = spec.getChild(index++);
      if (sn.getType().equals(JDBCConstants.attributeQueryNode))
      {
        final String attributeName = sn.getAttributeValue(JDBCConstants.attributeName);
        final String attributeQuery = sn.getValue();
        // Fire off attribute query
        VariableMap attrVm = new VariableMap();
        addConstant(attrVm,JDBCConstants.idReturnVariable,JDBCConstants.idReturnColumnName);
        addConstant(attrVm,JDBCConstants.dataReturnVariable,JDBCConstants.dataReturnColumnName);
        if (!addIDList(attrVm,JDBCConstants.idListVariable,documentIdentifiers,map.keySet()))
          continue;
        
        // Do the substitution
        ArrayList paramList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        substituteQuery(attributeQuery,attrVm,sb,paramList);

        // Fire off the query!
        getSession();
        IDynamicResultSet result;
        String queryText = sb.toString();
        long startTime = System.currentTimeMillis();
        // Get a dynamic resultset.  Contract for dynamic resultset is that if
        // one is returned, it MUST be closed, or a connection will leak.
        try
        {
          result = connection.executeUncachedQuery(queryText,paramList,-1);
        }
        catch (ManifoldCFException e)
        {
          // If failure, record the failure.
          if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
            activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
              createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
          throw e;
        }
        try
        {
          // If success, record that too.
          activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
            createQueryString(queryText,paramList), "OK", null, null);
          // Now, go through resultset
          while (true)
          {
            IDynamicResultRow row = result.getNextRow();
            if (row == null)
              break;
            try
            {
              Object o = row.getValue(JDBCConstants.idReturnColumnName);
              if (o == null)
                throw new ManifoldCFException("Bad attribute query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\", or, for MySQL, select \"by label\" in your repository connection.");
              String idValue = JDBCConnection.readAsString(o);
              o = row.getValue(JDBCConstants.dataReturnColumnName);
              String dataValue;
              if (o == null)
                dataValue = "";
              else
                dataValue = JDBCConnection.readAsString(o);
              Map<String, Set<String>> avs = attributeValues.get(idValue);
              if (avs == null)
              {
                avs = new HashMap<String, Set<String>>();
                attributeValues.put(idValue,avs);
              }
              Set<String> dataValues = avs.get(attributeName);
              if (dataValues == null)
              {
                dataValues = new HashSet<String>();
                avs.put(attributeName, dataValues);
              }
              dataValues.add(dataValue);
            }
            finally
            {
              row.close();
            }
          }
        }
        finally
        {
          result.close();
        }
      }
    }
    
    // For all the documents not marked "scan only", form a query and pick up the contents.
    // If the contents is not found, then explicitly call the delete action method.
    VariableMap vm = new VariableMap();
    addConstant(vm,JDBCConstants.idReturnVariable,JDBCConstants.idReturnColumnName);
    addConstant(vm,JDBCConstants.urlReturnVariable,JDBCConstants.urlReturnColumnName);
    addConstant(vm,JDBCConstants.dataReturnVariable,JDBCConstants.dataReturnColumnName);
    addConstant(vm,JDBCConstants.contentTypeReturnVariable,JDBCConstants.contentTypeReturnColumnName);
    if (!addIDList(vm,JDBCConstants.idListVariable,documentIdentifiers,map.keySet()))
      return;

    // Do the substitution
    ArrayList paramList = new ArrayList();
    StringBuilder sb = new StringBuilder();
    substituteQuery(ts.dataQuery,vm,sb,paramList);

    // Execute the query
    getSession();
    IDynamicResultSet result;
    String queryText = sb.toString();
    long startTime = System.currentTimeMillis();
    // Get a dynamic resultset.  Contract for dynamic resultset is that if
    // one is returned, it MUST be closed, or a connection will leak.
    try
    {
      result = connection.executeUncachedQuery(queryText,paramList,-1);
    }
    catch (ManifoldCFException e)
    {
      // If failure, record the failure.
      activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
        createQueryString(queryText,paramList), "ERROR", e.getMessage(), null);
      throw e;
    }
    try
    {
      // If success, record that too.
      activities.recordActivity(new Long(startTime), ACTIVITY_EXTERNAL_QUERY, null,
        createQueryString(queryText,paramList), "OK", null, null);

      while (true)
      {
        IDynamicResultRow row = result.getNextRow();
        if (row == null)
          break;
        try
        {
          Object o = row.getValue(JDBCConstants.idReturnColumnName);
          if (o == null)
            throw new ManifoldCFException("Bad document query; doesn't return $(IDCOLUMN) column.  Try using quotes around $(IDCOLUMN) variable, e.g. \"$(IDCOLUMN)\", or, for MySQL, select \"by label\" in your repository connection.");
          final String id = JDBCConnection.readAsString(o);
          seenDocuments.add(id);
          
          String errorCode = null;
          String errorDesc = null;
          Long fileLengthLong = null;
          long fetchStartTime = System.currentTimeMillis();
          
          try
          {
            String version = map.get(id);
            if (version == null)
              // Does not need refetching
              continue;

            // This document was marked as "not scan only", so we expect to find it.
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("JDBC: Document data result found for '"+id+"'");
            
            // We will determine the disposition of the document here, so remove this id from the map in order that we know what we still
            // need to delete when all done.
            map.remove(id);

            o = row.getValue(JDBCConstants.urlReturnColumnName);
            if (o == null)
            {
              Logging.connectors.debug("JDBC: Document '"+id+"' has a null url - skipping");
              errorCode = activities.NULL_URL;
              errorDesc = "Excluded because document had a null URL";
              activities.noDocument(id,version);
              continue;
            }
            
            // This is not right - url can apparently be a BinaryInput
            String url = JDBCConnection.readAsString(o);
            boolean validURL;
            try
            {
              // Check to be sure url is valid
              new java.net.URI(url);
              validURL = true;
            }
            catch (java.net.URISyntaxException e)
            {
              validURL = false;
            }

            if (!validURL)
            {
              Logging.connectors.debug("JDBC: Document '"+id+"' has an illegal url: '"+url+"' - skipping");
              errorCode = activities.BAD_URL;
              errorDesc = "Excluded because document had illegal URL ('"+url+"')";
              activities.noDocument(id,version);
              continue;
            }
            
            // Process the document itself
            Object contents = row.getValue(JDBCConstants.dataReturnColumnName);
            // Null data is allowed; we just ignore these
            if (contents == null)
            {
              Logging.connectors.debug("JDBC: Document '"+id+"' seems to have null data - skipping");
              errorCode = "NULLDATA";
              errorDesc = "Excluded because document had null data";
              activities.noDocument(id,version);
              continue;
            }
            
            String contentType;
            o = row.getValue(JDBCConstants.contentTypeReturnColumnName);
            if (o != null)
              contentType = JDBCConnection.readAsString(o);
            else
            {
              if (contents instanceof BinaryInput)
                contentType = "application/octet-stream";
              else if (contents instanceof CharacterInput)
                contentType = "text/plain; charset=utf-8";
              else
                contentType = "text/plain";
            }
                    
            if (!activities.checkMimeTypeIndexable(contentType))
            {
              Logging.connectors.debug("JDBC: Document '"+id+"' excluded because of mime type - skipping");
              errorCode = activities.EXCLUDED_MIMETYPE;
              errorDesc = "Excluded because of mime type ("+contentType+")";
              activities.noDocument(id,version);
              continue;
            }
                    
            if (!activities.checkURLIndexable(url))
            {
              Logging.connectors.debug("JDBC: Document '"+id+"' excluded because of url - skipping");
              errorCode = activities.EXCLUDED_URL;
              errorDesc = "Excluded because of URL ('"+url+"')";
              activities.noDocument(id,version);
              continue;
            }

            // An ingestion will take place for this document.
            RepositoryDocument rd = new RepositoryDocument();
            rd.setMimeType(contentType);

            applyMultiAttributeValues(rd,attributeValues.get(id));
            applyAccessTokens(rd,documentAcls.get(id));
            applyMetadata(rd,row);

            if (contents instanceof BinaryInput)
            {

              BinaryInput bi = (BinaryInput)contents;
              long fileLength = bi.getLength();
                      
              if (!activities.checkLengthIndexable(fileLength))
              {
                Logging.connectors.debug("JDBC: Document '"+id+"' excluded because of length - skipping");
                errorCode = activities.EXCLUDED_LENGTH;
                errorDesc = "Excluded because of length ("+fileLength+")";
                activities.noDocument(id, version);
                continue;
              }

              try
              {
                // Read the stream
                InputStream is = bi.getStream();
                try
                {
                  rd.setBinary(is,fileLength);
                  activities.ingestDocumentWithException(id, version, url, rd);
                  errorCode = "OK";
                  fileLengthLong = new Long(fileLength);
                }
                finally
                {
                  is.close();
                }
              }
              catch (IOException e)
              {
                errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = e.getMessage();
                handleIOException(id,e);
              }
            }
            else if (contents instanceof CharacterInput)
            {
              CharacterInput ci = (CharacterInput)contents;
              long fileLength = ci.getUtf8StreamLength();
              
              if (!activities.checkLengthIndexable(fileLength))
              {
                Logging.connectors.debug("JDBC: Document '"+id+"' excluded because of length - skipping");
                errorCode = activities.EXCLUDED_LENGTH;
                errorDesc = "Excluded because of length ("+fileLength+")";
                activities.noDocument(id, version);
                continue;
              }
                      
              try
              {
                // Read the stream
                InputStream is = ci.getUtf8Stream();
                try
                {
                  rd.setBinary(is,fileLength);
                  activities.ingestDocumentWithException(id, version, url, rd);
                  errorCode = "OK";
                  fileLengthLong = new Long(fileLength);
                }
                finally
                {
                  is.close();
                }
              }
              catch (IOException e)
              {
                errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = e.getMessage();
                handleIOException(id,e);
              }
            }
            else
            {
              // Turn it into a string, and then into a stream
              String value = contents.toString();
              byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
              long fileLength = bytes.length;
              
              if (!activities.checkLengthIndexable(fileLength))
              {
                Logging.connectors.debug("JDBC: Document '"+id+"' excluded because of length - skipping");
                errorCode = activities.EXCLUDED_LENGTH;
                errorDesc = "Excluded because of length ("+fileLength+")";
                activities.noDocument(id, version);
                continue;
              }

              try
              {
                InputStream is = new ByteArrayInputStream(bytes);
                try
                {
                  rd.setBinary(is,fileLength);
                  activities.ingestDocumentWithException(id, version, url, rd);
                  errorCode = "OK";
                  fileLengthLong = new Long(fileLength);
                }
                finally
                {
                  is.close();
                }
              }
              catch (IOException e)
              {
                errorCode = e.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                errorDesc = e.getMessage();
                handleIOException(id,e);
              }
            }
          }
          catch (ManifoldCFException e)
          {
            if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
              errorCode = null;
            throw e;
          }
          finally
          {
            if (errorCode != null)
              activities.recordActivity(new Long(fetchStartTime), ACTIVITY_FETCH,
                fileLengthLong, id, errorCode, errorDesc, null);
          }
        }
        finally
        {
          row.close();
        }
      }

    }
    finally
    {
      result.close();
    }
    
    // Now, go through the original id's, and see which ones are still in the map.  These
    // did not appear in the result and are presumed to be gone from the database, and thus must be deleted.
    for (final String documentIdentifier : fetchDocuments)
    {
      if (!seenDocuments.contains(documentIdentifier))
      {
        // Never saw it in the fetch attempt
        activities.deleteDocument(documentIdentifier);
      }
      else
      {
        // Saw it in the fetch attempt, and we might have fetched it
        final String documentVersion = map.get(documentIdentifier);
        if (documentVersion != null)
        {
          // This means we did not see it (or data for it) in the result set.  Delete it!
          activities.noDocument(documentIdentifier,documentVersion);
          activities.recordActivity(null, ACTIVITY_FETCH,
            null, documentIdentifier, "NOTFETCHED", "Document was not seen by processing query", null);
        }
      }
    }

  }
  
  protected static void handleIOException(String id, IOException e)
    throws ManifoldCFException, ServiceInterruption
  {
    if (e instanceof java.net.SocketTimeoutException)
    {
      throw new ManifoldCFException("Socket timeout reading database data: "+e.getMessage(),e);
    }
    if (e instanceof InterruptedIOException)
    {
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    throw new ManifoldCFException("Error reading database data: "+e.getMessage(),e);
  }
  
  // UI support methods.
  //
  // These support methods come in two varieties.  The first bunch is involved in setting up connection configuration information.  The second bunch
  // is involved in presenting and editing document specification information for a job.  The two kinds of methods are accordingly treated differently,
  // in that the first bunch cannot assume that the current connector object is connected, while the second bunch can.  That is why the first bunch
  // receives a thread context argument for all UI methods, while the second bunch does not need one (since it has already been applied via the connect()
  // method, above).
    
  /** Output the configuration header section.
  * This method is called in the head section of the connector's configuration page.  Its purpose is to add the required tabs to the list, and to output any
  * javascript methods that might be needed by the configuration editing HTML.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, List<String> tabsArray)
    throws ManifoldCFException, IOException
  {
    tabsArray.add(Messages.getString(locale,"JDBCConnector.DatabaseType"));
    tabsArray.add(Messages.getString(locale,"JDBCConnector.Server"));
    tabsArray.add(Messages.getString(locale,"JDBCConnector.Credentials"));

    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"+
"function checkConfigForSave()\n"+
"{\n"+
"  if (editconnection.databasehost.value == \"\" && editconnection.rawjdbcstring.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.PleaseFillInADatabaseServerName") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.Server") + "\");\n"+
"    editconnection.databasehost.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editconnection.databasename.value == \"\" && editconnection.rawjdbcstring.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.PleaseFillInTheNameOfTheDatabase") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.Server") + "\");\n"+
"    editconnection.databasename.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editconnection.username.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.PleaseSupplyTheDatabaseUsernameForThisConnection") + "\");\n"+
"    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.Credentials") + "\");\n"+
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
  
  /** Output the configuration body section.
  * This method is called in the body section of the connector's configuration page.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt;, &lt;body&gt;, and &lt;form&gt; tags.  The name of the
  * form is "editconnection".
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabName is the current tab name.
  */
  @Override
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, String tabName)
    throws ManifoldCFException, IOException
  {
    String jdbcProvider = parameters.getParameter(JDBCConstants.providerParameter);
    if (jdbcProvider == null)
      jdbcProvider = JDBCConstants.PROVIDER_ORACLE;
    String accessMethod = parameters.getParameter(JDBCConstants.methodParameter);
    if (accessMethod == null)
      accessMethod = "name";
    String host = parameters.getParameter(JDBCConstants.hostParameter);
    if (host == null)
      host = "localhost";
    String databaseName = parameters.getParameter(JDBCConstants.databaseNameParameter);
    if (databaseName == null)
      databaseName = "database";
    String rawJDBCString = parameters.getParameter(JDBCConstants.driverStringParameter);
    if (rawJDBCString == null)
      rawJDBCString = "";
    boolean sslTrustAll = Boolean.valueOf(parameters.getParameter(JDBCConstants.sslTrustAll));
    String databaseUser = parameters.getParameter(JDBCConstants.databaseUserName);
    if (databaseUser == null)
      databaseUser = "";
    String databasePassword = parameters.getObfuscatedParameter(JDBCConstants.databasePassword);
    if (databasePassword == null)
      databasePassword = "";
    else
      databasePassword = out.mapPasswordToKey(databasePassword);

    // "Database Type" tab
    if (tabName.equals(Messages.getString(locale,"JDBCConnector.DatabaseType")))
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.DatabaseType2") + "</nobr></td><td class=\"value\">\n"+
"      <select multiple=\"false\" name=\"databasetype\" style=\"height: 75px;\">\n"+
"        <option value=\""+JDBCConstants.PROVIDER_ORACLE+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_ORACLE)?"selected=\"selected\"":"")+">Oracle</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_POSTGRESQL+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_POSTGRESQL)?"selected=\"selected\"":"")+">Postgres SQL</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_JTDS_SQLSERVER+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_JTDS_SQLSERVER)?"selected=\"selected\"":"")+">MS SQL Server (&gt; V6.5)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MS_SQLSERVER+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_MS_SQLSERVER)?"selected=\"selected\"":"")+">MS SQL Server (&gt;= 2012)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_SYBASE+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_SYBASE)?"selected=\"selected\"":"")+">Sybase (&gt;= V10)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MYSQL+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_MYSQL)?"selected=\"selected\"":"")+">MySQL (&gt;= V5)</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_MARIADB+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_MARIADB)?"selected=\"selected\"":"")+">MariaDB</option>\n"+
"        <option value=\""+JDBCConstants.PROVIDER_CSV+"\" "+(jdbcProvider.equals(JDBCConstants.PROVIDER_CSV)?"selected=\"selected\"":"")+">CSV</option>\n"+
"      </select>\n"+
"    </td>\n"+
"  </tr>\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AccessMethod") + "</nobr></td><td class=\"value\">\n"+
"      <select multiple=\"false\" name=\"accessmethod\" style=\"height: 50px;\">\n"+
"        <option value=\"name\" "+(accessMethod.equals("name")?"selected=\"selected\"":"")+">"+Messages.getBodyString(locale,"JDBCConnector.ByName")+"</option>\n"+
"        <option value=\"label\" "+(accessMethod.equals("label")?"selected=\"selected\"":"")+">"+Messages.getBodyString(locale,"JDBCConnector.ByLabel")+"</option>\n"+
"      </select>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\"databasetype\" value=\""+jdbcProvider+"\"/>\n"+
"<input type=\"hidden\" name=\"accessmethod\" value=\""+accessMethod+"\"/>\n"
      );
    }

    // "Server" tab
    if (tabName.equals(Messages.getString(locale,"JDBCConnector.Server")))
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.DatabaseHostAndPort") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"64\" name=\"databasehost\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(host)+"\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.DatabaseServiceNameOrInstanceDatabase") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"32\" name=\"databasename\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseName)+"\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.RawDatabaseConnectString") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"80\" name=\"rawjdbcstring\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(rawJDBCString)+"\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.SSLTrustAll") + "</nobr></td><td class=\"value\"><input type=\"checkbox\" name=\""+ JDBCConstants.sslTrustAll +"\" value=\"true\""+ (sslTrustAll ? "checked":"") + " /></td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\"databasehost\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(host)+"\"/>\n"+
"<input type=\"hidden\" name=\"databasename\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseName)+"\"/>\n"+
"<input type=\"hidden\" name=\"rawjdbcstring\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(rawJDBCString)+"\"/>\n"+
"<input type=\"hidden\" name=\""+ JDBCConstants.sslTrustAll +"\" value=\""+sslTrustAll+"\"/>\n"
      );
    }

    // "Credentials" tab
    if (tabName.equals(Messages.getString(locale,"JDBCConnector.Credentials")))
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.UserName") + "</nobr></td><td class=\"value\"><input type=\"text\" size=\"32\" name=\"username\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseUser)+"\"/></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.Password") + "</nobr></td><td class=\"value\"><input type=\"password\" size=\"32\" name=\"password\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databasePassword)+"\"/></td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\"username\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databaseUser)+"\"/>\n"+
"<input type=\"hidden\" name=\"password\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(databasePassword)+"\"/>\n"
      );
    }
  }
  
  /** Process a configuration post.
  * This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been
  * posted.  Its purpose is to gather form information and modify the configuration parameters accordingly.
  * The name of the posted form is "editconnection".
  *@param threadContext is the local thread context.
  *@param variableContext is the set of variables available from the post, including binary file post information.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
  */
  @Override
  public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
    Locale locale, ConfigParams parameters)
    throws ManifoldCFException
  {
    String type = variableContext.getParameter("databasetype");
    if (type != null)
      parameters.setParameter(JDBCConstants.providerParameter,type);

    String accessMethod = variableContext.getParameter("accessmethod");
    if (accessMethod != null)
      parameters.setParameter(JDBCConstants.methodParameter,accessMethod);

    String host = variableContext.getParameter("databasehost");
    if (host != null)
      parameters.setParameter(JDBCConstants.hostParameter,host);

    String databaseName = variableContext.getParameter("databasename");
    if (databaseName != null)
      parameters.setParameter(JDBCConstants.databaseNameParameter,databaseName);

    String rawJDBCString = variableContext.getParameter("rawjdbcstring");
    if (rawJDBCString != null)
      parameters.setParameter(JDBCConstants.driverStringParameter,rawJDBCString);

    String sslTrustAll = variableContext.getParameter(JDBCConstants.sslTrustAll);
    if (sslTrustAll != null)
      parameters.setParameter(JDBCConstants.sslTrustAll,sslTrustAll);

    String userName = variableContext.getParameter("username");
    if (userName != null)
      parameters.setParameter(JDBCConstants.databaseUserName,userName);

    String password = variableContext.getParameter("password");
    if (password != null)
      parameters.setObfuscatedParameter(JDBCConstants.databasePassword,variableContext.mapKeyToPassword(password));
    
    return null;
  }
  
  /** View configuration.
  * This method is called in the body section of the connector's view configuration page.  Its purpose is to present the connection information to the user.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate &lt;html&gt; and &lt;body&gt;tags.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters)
    throws ManifoldCFException, IOException
  {
    out.print(
"<table class=\"displaytable\">\n"+
"  <tr>\n"+
"    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.Parameters") + "</nobr></td>\n"+
"    <td class=\"value\" colspan=\"3\">\n"
    );
    Iterator iter = parameters.listParameters();
    while (iter.hasNext())
    {
      String param = (String)iter.next();
      String value = parameters.getParameter(param);
      if (param.length() >= "password".length() && param.substring(param.length()-"password".length()).equalsIgnoreCase("password"))
      {
        out.print(
"      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=********</nobr><br/>\n"
        );
      }
      else if (param.length() >="keystore".length() && param.substring(param.length()-"keystore".length()).equalsIgnoreCase("keystore"))
      {
        IKeystoreManager kmanager = KeystoreManagerFactory.make("",value);
        out.print(
"      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=&lt;"+Integer.toString(kmanager.getContents().length)+" certificate(s)&gt;</nobr><br/>\n"
        );
      }
      else
      {
        out.print(
"      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value)+"</nobr><br/>\n"
        );
      }
    }
    out.print(
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
    );
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
    throws ManifoldCFException, IOException
  {
    tabsArray.add(Messages.getString(locale,"JDBCConnector.Queries"));
    tabsArray.add(Messages.getString(locale,"JDBCConnector.Security"));

    String seqPrefix = "s"+connectionSequenceNumber+"_";

    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"+
"\n"+
"function "+seqPrefix+"SpecOp(n, opValue, anchorvalue)\n"+
"{\n"+
"  eval(\"editjob.\"+n+\".value = \\\"\"+opValue+\"\\\"\");\n"+
"  postFormSetAnchor(anchorvalue);\n"+
"}\n"+
"\n"+
"function "+seqPrefix+"DeleteAttr(index)\n"+
"{\n"+
"  "+seqPrefix+"SpecOp(\""+seqPrefix+"attr_\"+index+\"_op\", \"Delete\", \""+seqPrefix+"attr_\" + index);\n"+
"}\n"+
"\n"+
"function "+seqPrefix+"AddAttr(index)\n"+
"{\n"+
"  if (editjob."+seqPrefix+"attr_name.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.TypeInAnAttributeName") + "\");\n"+
"    editjob."+seqPrefix+"attr_name.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"attr_query.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.AttributeQueryCannotBeNull") + "\");\n"+
"    editjob."+seqPrefix+"attr_query.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"attr_query.value.indexOf(\"$(IDCOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnIDCOLUMNInTheResult") + "\");\n"+
"    editjob."+seqPrefix+"attr_query.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"attr_query.value.indexOf(\"$(DATACOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnDATACOLUMNInTheResult") + "\");\n"+
"    editjob."+seqPrefix+"attr_query.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"attr_query.value.indexOf(\"$(IDLIST)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustUseIDLISTInWHEREClause") + "\");\n"+
"    editjob."+seqPrefix+"attr_query.focus();\n"+
"    return;\n"+
"  }\n"+
"  "+seqPrefix+"SpecOp(\""+seqPrefix+"attr_op\", \"Add\", \""+seqPrefix+"attr_\"+(index+1));\n"+
"}\n"+
"\n"+
"function "+seqPrefix+"SpecAddToken(anchorvalue)\n"+
"{\n"+
"  if (editjob."+seqPrefix+"spectoken.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.TypeInAnAccessToken") + "\");\n"+
"    editjob."+seqPrefix+"spectoken.focus();\n"+
"    return;\n"+
"  }\n"+
"  "+seqPrefix+"SpecOp(\""+seqPrefix+"accessop\",\"Add\",anchorvalue);\n"+
"}\n"+
"\n"+
"function "+seqPrefix+"checkSpecification()\n"+
"{\n"+
"  if (editjob."+seqPrefix+"idquery.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.EnterASeedingQuery") + "\");\n"+
"    editjob."+seqPrefix+"idquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"idquery.value.indexOf(\"$(IDCOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnIDCOLUMNInTheResult") + "\");\n"+
"    editjob."+seqPrefix+"idquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"versionquery.value != \"\")\n"+
"  {\n"+
"    if (editjob."+seqPrefix+"versionquery.value.indexOf(\"$(IDCOLUMN)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnIDCOLUMNInTheResult") + "\");\n"+
"      editjob."+seqPrefix+"versionquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"    if (editjob."+seqPrefix+"versionquery.value.indexOf(\"$(VERSIONCOLUMN)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnVERSIONCOLUMNInTheResult") + "\");\n"+
"      editjob."+seqPrefix+"versionquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"    if (editjob."+seqPrefix+"versionquery.value.indexOf(\"$(IDLIST)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustUseIDLISTInWHEREClause") + "\");\n"+
"      editjob."+seqPrefix+"versionquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"aclquery.value != \"\")\n"+
"  {\n"+
"    if (editjob."+seqPrefix+"aclquery.value.indexOf(\"$(IDCOLUMN)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnIDCOLUMNInTheResult") + "\");\n"+
"      editjob."+seqPrefix+"aclquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"    if (editjob."+seqPrefix+"aclquery.value.indexOf(\"$(TOKENCOLUMN)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnTOKENCOLUMNInTheResult") + "\");\n"+
"      editjob."+seqPrefix+"aclquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"    if (editjob."+seqPrefix+"aclquery.value.indexOf(\"$(IDLIST)\") == -1)\n"+
"    {\n"+
"      alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustUseIDLISTInWHEREClause") + "\");\n"+
"      editjob."+seqPrefix+"aclquery.focus();\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"dataquery.value == \"\")\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.EnterADataQuery") + "\");\n"+
"    editjob."+seqPrefix+"dataquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"dataquery.value.indexOf(\"$(IDCOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnIDCOLUMNInTheResult2") + "\");\n"+
"    editjob."+seqPrefix+"dataquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"dataquery.value.indexOf(\"$(URLCOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnURLCOLUMNInTheResult") + "\");\n"+
"    editjob."+seqPrefix+"dataquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"dataquery.value.indexOf(\"$(DATACOLUMN)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustReturnDATACOLUMNInTheResult") + "\");\n"+
"    editjob."+seqPrefix+"dataquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editjob."+seqPrefix+"dataquery.value.indexOf(\"$(IDLIST)\") == -1)\n"+
"  {\n"+
"    alert(\"" + Messages.getBodyJavascriptString(locale,"JDBCConnector.MustUseIDLISTInWHEREClause") + "\");\n"+
"    editjob."+seqPrefix+"dataquery.focus();\n"+
"    return false;\n"+
"  }\n"+
"\n"+
"  return true;\n"+
"}\n"+
"\n"+
"//-->\n"+
"</script>\n"
    );
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
    throws ManifoldCFException, IOException
  {
    String seqPrefix = "s"+connectionSequenceNumber+"_";

    String idQuery = "SELECT idfield AS $(IDCOLUMN) FROM documenttable WHERE modifydatefield > $(STARTTIME) AND modifydatefield <= $(ENDTIME)";
    String versionQuery = "SELECT idfield AS $(IDCOLUMN), versionfield AS $(VERSIONCOLUMN) FROM documenttable WHERE idfield IN $(IDLIST)";
    String dataQuery = "SELECT idfield AS $(IDCOLUMN), urlfield AS $(URLCOLUMN), datafield AS $(DATACOLUMN) FROM documenttable WHERE idfield IN $(IDLIST)";
    String aclQuery = "SELECT docidfield AS $(IDCOLUMN), aclfield AS $(TOKENCOLUMN) FROM acltable WHERE docidfield IN $(IDLIST)";
    
    final Map<String, String> attributeQueryMap = new HashMap<String, String>();
    int i = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(JDBCConstants.idQueryNode))
      {
        idQuery = sn.getValue();
        if (idQuery == null)
          idQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.versionQueryNode))
      {
        versionQuery = sn.getValue();
        if (versionQuery == null)
          versionQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.dataQueryNode))
      {
        dataQuery = sn.getValue();
        if (dataQuery == null)
          dataQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.aclQueryNode))
      {
        aclQuery = sn.getValue();
        if (aclQuery == null)
          aclQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.attributeQueryNode))
      {
        String attributeName = sn.getAttributeValue(JDBCConstants.attributeName);
        String attributeQuery = sn.getValue();
        attributeQueryMap.put(attributeName, attributeQuery);
      }
    }

    // Sort the attribute query list
    final String[] attributeNames = attributeQueryMap.keySet().toArray(new String[0]);
    Arrays.sort(attributeNames);
    
    // The Queries tab

    if (tabName.equals(Messages.getString(locale,"JDBCConnector.Queries")) && connectionSequenceNumber == actualSequenceNumber)
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.SeedingQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.returnIdsThatNeedToBeChecked") + "</nobr></td>\n"+
"    <td class=\"value\"><textarea name=\""+seqPrefix+"idquery\" cols=\"64\" rows=\"6\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(idQuery)+"</textarea></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.VersionCheckQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.returnIdsAndVersionsForASetOfDocuments") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.leaveBlankIfNoVersioningCapability") + "</nobr></td>\n"+
"    <td class=\"value\"><textarea name=\""+seqPrefix+"versionquery\" cols=\"64\" rows=\"6\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(versionQuery)+"</textarea></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AccessTokenQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.returnIdsAndAccessTokensForASetOfDocuments") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.leaveBlankIfNoSecurityCapability") + "</nobr></td>\n"+
"    <td class=\"value\"><textarea name=\""+seqPrefix+"aclquery\" cols=\"64\" rows=\"6\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(aclQuery)+"</textarea></td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.DataQuery") + "</nobr><br/><nobr>" + Messages.getBodyString(locale,"JDBCConnector.returnIdsUrlsAndDataForASetOfDocuments") + "</nobr></td>\n"+
"    <td class=\"value\"><textarea name=\""+seqPrefix+"dataquery\" cols=\"64\" rows=\"6\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(dataQuery)+"</textarea></td>\n"+
"  </tr>\n");
      out.print(
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeQueries") + "</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeName") + "</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeQuery") + "</nobr></td>\n"+
"        </tr>\n"
      );
      int attributeIndex = 0;
      for (final String attributeName : attributeNames) {
        final String attributeQuery = attributeQueryMap.get(attributeName);
        if (attributeIndex % 2 == 0)
        {
          out.print(
"        <tr class=\"evenformrow\">\n"
          );
        }
        else 
        {
          out.print(
"        <tr class=\"oddformrow\">\n"
          );
        }
        // Delete button
        out.print(
"          <td class=\"formcolumncell\">\n"+
"            <a name=\""+seqPrefix+"attr_"+attributeIndex+"\">\n"+
"              <nobr>\n"+
"                <input type=\"button\" value=\""+Messages.getAttributeString(locale,"JDBCConnector.Delete")+"\"\n"+
"                alt=\""+Messages.getAttributeString(locale,"JDBCConnector.DeleteAttributeQueryNumber")+attributeIndex+"\" onclick=\"javascript:"+seqPrefix+"DeleteAttr("+attributeIndex+");\"/>\n"+
"              </nobr>\n"+
"            </a>\n"+
"            <input type=\"hidden\" name=\""+seqPrefix+"attr_"+attributeIndex+"_op"+"\" value=\"Continue\"/>\n"+
"            <input type=\"hidden\" name=\""+seqPrefix+"attr_"+attributeIndex+"_name\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(attributeName)+"\"/>\n"+
"          </td>\n"
        );
        // Attribute name
        out.print(
"          <td class=\"formcolumncell\">\n"+
"            "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(attributeName)+"\n"+
"          </td>\n"
        );
        // Query
        out.print(
"          <td class=\"formcolumncell\">\n"+
"            <textarea name=\""+seqPrefix+"attr_"+attributeIndex+"_query\" cols=\"64\" rows=\"6\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(attributeQuery)+"</textarea>\n"+
"          </td>\n"
        );
        out.print(
"        </tr>\n"
        );
        attributeIndex++;
      }
      if (attributeIndex == 0)
      {
        out.print(
"        <tr><td class=\"formmessage\" colspan=\"3\">"+Messages.getBodyString(locale,"JDBCConnector.NoAttributeQueries")+"</td></tr>\n"
        );
      }
      // Add button
      out.print(
"        <tr><td class=\"formseparator\" colspan=\"3\"><hr/></td></tr>\n"+
"        <tr class=\"formrow\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <a name=\""+seqPrefix+"attr_"+attributeIndex+"\">\n"+
"              <input type=\"button\" value=\""+Messages.getAttributeString(locale,"JDBCConnector.Add")+"\"\n"+
"              alt=\""+Messages.getAttributeString(locale,"JDBCConnector.AddAttribute")+"\" onclick=\"javascript:"+seqPrefix+"AddAttr("+attributeIndex+");\"/>\n"+
"            </a>\n"+
"            <input type=\"hidden\" name=\""+seqPrefix+"attr_count\" value=\""+attributeIndex+"\"/>\n"+
"            <input type=\"hidden\" name=\""+seqPrefix+"attr_op\" value=\"Continue\"/>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\"><nobr><input name=\""+seqPrefix+"attr_name\" type=\"text\" size=\"16\" value=\"\"/></nobr></td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <textarea name=\""+seqPrefix+"attr_query\" cols=\"64\" rows=\"6\">SELECT idfield AS $(IDCOLUMN), datafield AS $(DATACOLUMN) FROM attributetable WHERE idfield IN $(IDLIST)</textarea>\n"+
"          </td>\n"+
"        </tr>\n"
      );
      out.print(
"      </table>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"idquery\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(idQuery)+"\"/>\n"+
"<input type=\"hidden\" name=\""+seqPrefix+"versionquery\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(versionQuery)+"\"/>\n"+
"<input type=\"hidden\" name=\""+seqPrefix+"aclquery\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(aclQuery)+"\"/>\n"+
"<input type=\"hidden\" name=\""+seqPrefix+"dataquery\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(dataQuery)+"\"/>\n"
      );
      int attributeIndex = 0;
      for (final String attributeName : attributeNames) {
        final String attributeQuery = attributeQueryMap.get(attributeName);
        out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"attr_"+attributeIndex+"_name\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(attributeName)+"\"/>\n"+
"<input type=\"hidden\" name=\""+seqPrefix+"attr_"+attributeIndex+"_query\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(attributeQuery)+"\"/>\n"
        );
        attributeIndex++;
      }
      out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"attr_count\" value=\""+attributeIndex+"\"/>\n"
      );
    }
	
    // Security tab
    // There is no native security, so all we care about are the tokens.
    i = 0;
    boolean securityOn = true;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals("security"))
      {
        String securityValue = sn.getAttributeValue("value");
        if (securityValue.equals("off"))
          securityOn = false;
        else if (securityValue.equals("on"))
          securityOn = true;
      }
    }

    if (tabName.equals(Messages.getString(locale,"JDBCConnector.Security")) && connectionSequenceNumber == actualSequenceNumber)
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>"+Messages.getBodyString(locale,"JDBCConnector.SecurityColon")+"</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      <input type=\"radio\" name=\""+seqPrefix+"specsecurity\" value=\"on\" "+(securityOn?"checked=\"true\"":"")+" />"+Messages.getBodyString(locale,"JDBCConnector.Enabled")+"\n"+
"      <input type=\"radio\" name=\""+seqPrefix+"specsecurity\" value=\"off\" "+((securityOn==false)?"checked=\"true\"":"")+" />"+Messages.getBodyString(locale,"JDBCConnector.Disabled")+"\n"+
"    </td>\n"+
"  </tr>\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
      );
      // Go through forced ACL
      i = 0;
      int k = 0;
      while (i < ds.getChildCount())
      {
        SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals("access"))
        {
          String accessDescription = "_"+Integer.toString(k);
          String accessOpName = seqPrefix+"accessop"+accessDescription;
          String token = sn.getAttributeValue("token");
          out.print(
"  <tr>\n"+
"    <td class=\"description\">\n"+
"      <input type=\"hidden\" name=\""+accessOpName+"\" value=\"\"/>\n"+
"      <input type=\"hidden\" name=\""+seqPrefix+"spectoken"+accessDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token)+"\"/>\n"+
"      <a name=\""+seqPrefix+"token_"+Integer.toString(k)+"\">\n"+
"        <input type=\"button\" value=\"Delete\" onClick='Javascript:"+seqPrefix+"SpecOp(\""+accessOpName+"\",\"Delete\",\""+seqPrefix+"token_"+Integer.toString(k)+"\")' alt=\"" + Messages.getAttributeString(locale,"JDBCConnector.DeleteToken") + "\""+Integer.toString(k)+"\"/>\n"+
"      </a>&nbsp;\n"+
"    </td>\n"+
"    <td class=\"value\">\n"+
"      "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token)+"\n"+
"    </td>\n"+
"  </tr>\n"
          );
          k++;
        }
      }
      if (k == 0)
      {
        out.print(
"  <tr>\n"+
"    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"JDBCConnector.NoAccessTokensPresent") + "</td>\n"+
"  </tr>\n"
        );
      }
      out.print(
"  <tr><td class=\"lightseparator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\">\n"+
"      <input type=\"hidden\" name=\""+seqPrefix+"tokencount\" value=\""+Integer.toString(k)+"\"/>\n"+
"      <input type=\"hidden\" name=\""+seqPrefix+"accessop\" value=\"\"/>\n"+
"      <a name=\""+seqPrefix+"token_"+Integer.toString(k)+"\">\n"+
"        <input type=\"button\" value=\"Add\" onClick='Javascript:"+seqPrefix+"SpecAddToken(\""+seqPrefix+"token_"+Integer.toString(k+1)+"\")' alt=\"" + Messages.getAttributeString(locale,"JDBCConnector.AddAccessToken") + "\"/>\n"+
"      </a>&nbsp;\n"+
"    </td>\n"+
"    <td class=\"value\">\n"+
"      <input type=\"text\" size=\"30\" name=\""+seqPrefix+"spectoken\" value=\"\"/>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"specsecurity\" value=\""+(securityOn?"on":"off")+"\"/>\n"
      );
      // Finally, go through forced ACL
      i = 0;
      int k = 0;
      while (i < ds.getChildCount())
      {
        SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals("access"))
        {
          String accessDescription = "_"+Integer.toString(k);
          String token = sn.getAttributeValue("token");
          out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"spectoken"+accessDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token)+"\"/>\n"
          );
          k++;
        }
      }
      out.print(
"<input type=\"hidden\" name=\""+seqPrefix+"tokencount\" value=\""+Integer.toString(k)+"\"/>\n"
      );
    }
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
    throws ManifoldCFException
  {
    String seqPrefix = "s"+connectionSequenceNumber+"_";

    String idQuery = variableContext.getParameter(seqPrefix+"idquery");
    String versionQuery = variableContext.getParameter(seqPrefix+"versionquery");
    String dataQuery = variableContext.getParameter(seqPrefix+"dataquery");
    String aclQuery = variableContext.getParameter(seqPrefix+"aclquery");
    
    SpecificationNode sn;
    if (idQuery != null)
    {
      int i = 0;
      while (i < ds.getChildCount())
      {
        if (ds.getChild(i).getType().equals(JDBCConstants.idQueryNode))
          ds.removeChild(i);
        else
          i++;
      }
      sn = new SpecificationNode(JDBCConstants.idQueryNode);
      sn.setValue(idQuery);
      ds.addChild(ds.getChildCount(),sn);
    }
    if (versionQuery != null)
    {
      int i = 0;
      while (i < ds.getChildCount())
      {
        if (ds.getChild(i).getType().equals(JDBCConstants.versionQueryNode))
          ds.removeChild(i);
        else
          i++;
      }
      sn = new SpecificationNode(JDBCConstants.versionQueryNode);
      sn.setValue(versionQuery);
      ds.addChild(ds.getChildCount(),sn);
    }
    if (aclQuery != null)
    {
      int i = 0;
      while (i < ds.getChildCount())
      {
        if (ds.getChild(i).getType().equals(JDBCConstants.aclQueryNode))
          ds.removeChild(i);
        else
          i++;
      }
      sn = new SpecificationNode(JDBCConstants.aclQueryNode);
      sn.setValue(aclQuery);
      ds.addChild(ds.getChildCount(),sn);
    }
    if (dataQuery != null)
    {
      int i = 0;
      while (i < ds.getChildCount())
      {
        if (ds.getChild(i).getType().equals(JDBCConstants.dataQueryNode))
          ds.removeChild(i);
        else
          i++;
      }
      sn = new SpecificationNode(JDBCConstants.dataQueryNode);
      sn.setValue(dataQuery);
      ds.addChild(ds.getChildCount(),sn);
    }

    String xc;
    xc = variableContext.getParameter(seqPrefix+"attr_count");
    if (xc != null)
    {
      // Delete all attribute queries first
      int i = 0;
      while (i < ds.getChildCount())
      {
        sn = ds.getChild(i);
        if (sn.getType().equals(JDBCConstants.attributeQueryNode))
          ds.removeChild(i);
        else
          i++;
      }

      // Now, maybe do add
      final String newAttributeName;
      final String newAttributeOp = variableContext.getParameter(seqPrefix+"attr_op");
      if (newAttributeOp != null && newAttributeOp.equals("Add"))
      {
        final String attributeName = variableContext.getParameter(seqPrefix+"attr_name");
        final String attributeQuery = variableContext.getParameter(seqPrefix+"attr_query");
        SpecificationNode node = new SpecificationNode(JDBCConstants.attributeQueryNode);
        node.setAttribute(JDBCConstants.attributeName, attributeName);
        newAttributeName = attributeName;
        node.setValue(attributeQuery);
        ds.addChild(ds.getChildCount(),node);
      }
      else
      {
        newAttributeName = null;
      }

      int attributeCount = Integer.parseInt(xc);
      for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++)
      {
        final String attributeOp = variableContext.getParameter(seqPrefix+"attr_"+attributeIndex+"_op");
        if (!(attributeOp != null && attributeOp.equals("Delete")))
        {
          // Include this!!
          final String attributeName = variableContext.getParameter(seqPrefix+"attr_"+attributeIndex+"_name");
          if (newAttributeName == null || !attributeName.equals(newAttributeName)) {
            final String attributeQuery = variableContext.getParameter(seqPrefix+"attr_"+attributeIndex+"_query");
            SpecificationNode node = new SpecificationNode(JDBCConstants.attributeQueryNode);
            node.setAttribute(JDBCConstants.attributeName, attributeName);
            node.setValue(attributeQuery);
            ds.addChild(ds.getChildCount(),node);
          }
        }
      }
      
    }
    
    xc = variableContext.getParameter(seqPrefix+"specsecurity");
    if (xc != null)
    {
      // Delete all security entries first
      int i = 0;
      while (i < ds.getChildCount())
      {
        sn = ds.getChild(i);
        if (sn.getType().equals("security"))
          ds.removeChild(i);
        else
          i++;
      }

      SpecificationNode node = new SpecificationNode("security");
      node.setAttribute("value",xc);
      ds.addChild(ds.getChildCount(),node);

    }

    xc = variableContext.getParameter(seqPrefix+"tokencount");
    if (xc != null)
    {
      // Delete all tokens first
      int i = 0;
      while (i < ds.getChildCount())
      {
        sn = ds.getChild(i);
        if (sn.getType().equals("access"))
          ds.removeChild(i);
        else
          i++;
      }

      int accessCount = Integer.parseInt(xc);
      i = 0;
      while (i < accessCount)
      {
        String accessDescription = "_"+Integer.toString(i);
        String accessOpName = seqPrefix+"accessop"+accessDescription;
        xc = variableContext.getParameter(accessOpName);
        if (xc != null && xc.equals("Delete"))
        {
          // Next row
          i++;
          continue;
        }
        // Get the stuff we need
        String accessSpec = variableContext.getParameter(seqPrefix+"spectoken"+accessDescription);
        SpecificationNode node = new SpecificationNode("access");
        node.setAttribute("token",accessSpec);
        ds.addChild(ds.getChildCount(),node);
        i++;
      }

      String op = variableContext.getParameter(seqPrefix+"accessop");
      if (op != null && op.equals("Add"))
      {
        String accessspec = variableContext.getParameter(seqPrefix+"spectoken");
        SpecificationNode node = new SpecificationNode("access");
        node.setAttribute("token",accessspec);
        ds.addChild(ds.getChildCount(),node);
      }
    }
    return null;
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
    throws ManifoldCFException, IOException
  {
    String idQuery = "";
    String versionQuery = "";
    String dataQuery = "";
    String aclQuery = "";
    
    final Map<String, String> attributeQueryMap = new HashMap<String, String>();
    
    int i = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(JDBCConstants.idQueryNode))
      {
        idQuery = sn.getValue();
        if (idQuery == null)
          idQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.versionQueryNode))
      {
        versionQuery = sn.getValue();
        if (versionQuery == null)
          versionQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.dataQueryNode))
      {
        dataQuery = sn.getValue();
        if (dataQuery == null)
          dataQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.aclQueryNode))
      {
        aclQuery = sn.getValue();
        if (aclQuery == null)
          aclQuery = "";
      }
      else if (sn.getType().equals(JDBCConstants.attributeQueryNode))
      {
        String attributeName = sn.getAttributeValue(JDBCConstants.attributeName);
        String attributeQuery = sn.getValue();
        attributeQueryMap.put(attributeName, attributeQuery);
      }
    }

    // Sort the attribute query list
    final String[] attributeNames = attributeQueryMap.keySet().toArray(new String[0]);
    Arrays.sort(attributeNames);

    out.print(
"<table class=\"displaytable\">\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.SeedingQuery") + "</nobr></td>\n"+
"    <td class=\"value\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(idQuery)+"</td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.VersionCheckQuery") + "</nobr></td>\n"+
"    <td class=\"value\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(versionQuery)+"</td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AccessTokenQuery") + "</nobr></td>\n"+
"    <td class=\"value\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(aclQuery)+"</td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.DataQuery") + "</nobr></td>\n"+
"    <td class=\"value\">"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(dataQuery)+"</td>\n"+
"  </tr>\n"+
"\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>"+
"    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeQueries") + "</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeName") + "</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AttributeQuery") + "</nobr></td>\n"+
"        </tr>\n"
    );
    int attributeIndex = 0;
    for (final String attributeName : attributeNames) {
      final String attributeQuery = attributeQueryMap.get(attributeName);
      if (attributeIndex % 2 == 0)
      {
        out.print(
"        <tr class=\"evenformrow\">\n"
        );
      }
      else 
      {
        out.print(
"        <tr class=\"oddformrow\">\n"
        );
      }
      // Attribute name
      out.print(
"          <td class=\"formcolumncell\">\n"+
"            "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(attributeName)+"\n"+
"          </td>\n"
      );
      // Query
      out.print(
"          <td class=\"formcolumncell\">\n"+
"            "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(attributeQuery)+"\n"+
"          </td>\n"
      );
      out.print(
"        </tr>\n"
      );
      attributeIndex++;
    }
    if (attributeIndex == 0)
    {
      out.print(
"        <tr><td class=\"formmessage\" colspan=\"2\">"+Messages.getBodyString(locale,"JDBCConnector.NoAttributeQueries")+"</td></tr>\n"
      );
    }
    out.print(
"      </table>\n"+
"    </td>\n"+
"  </tr>\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
    );
    // Find whether security is on or off
    i = 0;
    boolean securityOn = true;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals("security"))
      {
        String securityValue = sn.getAttributeValue("value");
        if (securityValue.equals("off"))
          securityOn = false;
        else if (securityValue.equals("on"))
          securityOn = true;
      }
    }
    out.print(
"  <tr>\n"+
"    <td class=\"description\">"+Messages.getBodyString(locale,"JDBCConnector.SecurityColon")+"</td>\n"+
"    <td class=\"value\">"+(securityOn?Messages.getBodyString(locale,"JDBCConnector.Enabled"):Messages.getBodyString(locale,"JDBCConnector.Disabled"))+"</td>\n"+
"  </tr>\n"+
"\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
    );

    // Go through looking for access tokens
    boolean seenAny = false;
    i = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals("access"))
      {
        if (seenAny == false)
        {
          out.print(
"  <tr><td class=\"description\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.AccessTokens") + "</nobr></td>\n"+
"    <td class=\"value\">\n"
          );
          seenAny = true;
        }
        String token = sn.getAttributeValue("token");
        out.print(
"      "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token)+"<br/>\n"
        );
      }
    }

    if (seenAny)
    {
      out.print(
"    </td>\n"+
"  </tr>\n"
      );
    }
    else
    {
      out.print(
"  <tr><td class=\"message\" colspan=\"2\"><nobr>" + Messages.getBodyString(locale,"JDBCConnector.NoAccessTokensSpecified") + "</nobr></td></tr>\n"
      );
    }

    out.print(
"</table>\n"
    );
  }

  /** Special column names, as far as document queries are concerned */
  protected static HashMap documentKnownColumns;
  static
  {
    documentKnownColumns = new HashMap();
    documentKnownColumns.put(JDBCConstants.idReturnColumnName,"");
    documentKnownColumns.put(JDBCConstants.urlReturnColumnName,"");
    documentKnownColumns.put(JDBCConstants.dataReturnColumnName,"");
    documentKnownColumns.put(JDBCConstants.contentTypeReturnColumnName,"");
  }
  
  /** Apply metadata to a repository document.
  *@param rd is the repository document to apply the metadata to.
  *@param row is the resultset row to use to get the metadata.  All non-special columns from this row will be considered to be metadata.
  */
  protected void applyMetadata(RepositoryDocument rd, IResultRow row)
    throws ManifoldCFException
  {
    // Cycle through the row's columns
    Iterator iter = row.getColumns();
    while (iter.hasNext())
    {
      String columnName = (String)iter.next();
      if (documentKnownColumns.get(columnName) == null)
      {
        // Consider this column to contain metadata.
        // We can only accept non-binary metadata at this time.
        Object metadata = row.getValue(columnName);
        rd.addField(columnName,JDBCConnection.readAsString(metadata));
      }
    }
  }

  /** Apply multi-valued attribute values to a repository document.
  */
  protected void applyMultiAttributeValues(final RepositoryDocument rd, final Map<String, Set<String>> values)
    throws ManifoldCFException
  {
    if (values == null)
    {
      return;
    }
    
    for (final String attributeName : values.keySet())
    {
      final Set<String> attributes = values.get(attributeName);
      final String[] attributeValues = new String[attributes.size()];
      int i = 0;
      for (final String attributeValue : attributes)
      {
        attributeValues[i++] = attributeValue;
      }
      rd.addField(attributeName, attributeValues);
    }
  }
  
  /** Apply access tokens to a repository document.
  *@param rd is the repository document to apply the access tokens to.
  */
  protected void applyAccessTokens(RepositoryDocument rd, Set<String> accessTokens)
    throws ManifoldCFException
  {
    if (accessTokens == null)
      return;
    
    String[] accessAcls = new String[accessTokens.size()];
    int i = 0;
    for (String accessToken : accessTokens)
    {
      accessAcls[i++] = accessToken;
    }
    Arrays.sort(accessAcls);
    String[] denyAcls = new String[]{defaultAuthorityDenyToken};
      
    rd.setSecurity(RepositoryDocument.SECURITY_TYPE_DOCUMENT,accessAcls,denyAcls);

  }
  
  /** Get the maximum number of documents to amalgamate together into one batch, for this connector.
  *@return the maximum number. 0 indicates "unlimited".
  */
  @Override
  public int getMaxDocumentRequest()
  {
    // This is a number that is comfortably processed by the query processor as part of an IN clause.
    return 100;
  }


  // These are protected helper methods

  /** Add starttime and endtime query variables
  */
  protected static void addVariable(VariableMap map, String varName, long variable)
  {
    ArrayList params = new ArrayList();
    params.add(new Long(variable));
    map.addVariable(varName,"?",params);
  }

  /** Add string query variables
  */
  protected static void addVariable(VariableMap map, String varName, String variable)
  {
    ArrayList params = new ArrayList();
    params.add(variable);
    map.addVariable(varName,"?",params);
  }

  /** Add string query constants
  */
  protected static void addConstant(VariableMap map, String varName, String value)
  {
    map.addVariable(varName,value,null);
  }

  /** Build an idlist variable, and add it to the specified variable map.
  */
  protected static boolean addIDList(VariableMap map, String varName, String[] documentIdentifiers, Set<String> fetchDocuments)
  {
    ArrayList params = new ArrayList();
    StringBuilder sb = new StringBuilder(" (");
    int k = 0;
    for (String documentIdentifier : documentIdentifiers)
    {
      if (fetchDocuments == null || fetchDocuments.contains(documentIdentifier))
      {
        if (k > 0)
          sb.append(",");
        sb.append("?");
        params.add(documentIdentifier);
        k++;
      }
    }
    sb.append(") ");
    map.addVariable(varName,sb.toString(),params);
    return (k > 0);
  }

  /** Given a query, and a parameter map, substitute it.
  * Each variable substitutes the string, and it also substitutes zero or more query parameters.
  */
  protected static void substituteQuery(String inputString, VariableMap inputMap, StringBuilder outputQuery, ArrayList outputParams)
    throws ManifoldCFException
  {
    // We are looking for strings that look like this: $(something)
    // Right at the moment we don't care even about quotes, so we just want to look for $(.
    int startIndex = 0;
    while (true)
    {
      int nextIndex = inputString.indexOf("$(",startIndex);
      if (nextIndex == -1)
      {
        outputQuery.append(inputString.substring(startIndex));
        break;
      }
      int endIndex = inputString.indexOf(")",nextIndex);
      if (endIndex == -1)
      {
        outputQuery.append(inputString.substring(startIndex));
        break;
      }
      String variableName = inputString.substring(nextIndex+2,endIndex);
      VariableMapItem item = inputMap.getVariable(variableName);
      if (item == null)
        throw new ManifoldCFException("No such substitution variable: $("+variableName+")");
      outputQuery.append(inputString.substring(startIndex,nextIndex));
      outputQuery.append(item.getValue());
      ArrayList inputParams = item.getParameters();
      if (inputParams != null)
      {
        int i = 0;
        while (i < inputParams.size())
        {
          Object x = inputParams.get(i++);
          outputParams.add(x);
        }
      }
      startIndex = endIndex+1;
    }
  }

  /** Create an entity identifier from a querystring and a parameter list.
  */
  protected static String createQueryString(String queryText, ArrayList paramList)
  {
    StringBuilder sb = new StringBuilder(queryText);
    sb.append("; arguments = (");
    int i = 0;
    while (i < paramList.size())
    {
      if (i > 0)
        sb.append(",");
      Object parameter = paramList.get(i++);
      if (parameter instanceof String)
        sb.append(quoteSQLString((String)parameter));
      else
        sb.append(parameter.toString());
    }
    sb.append(")");
    return sb.toString();
  }

  /** Quote a sql string.
  */
  protected static String quoteSQLString(String input)
  {
    StringBuilder sb = new StringBuilder("\'");
    int i = 0;
    while (i < input.length())
    {
      char x = input.charAt(i++);
      if (x == '\'')
        sb.append('\'').append(x);
      else if (x >= 0 && x < ' ')
        sb.append(' ');
      else
        sb.append(x);
    }
    sb.append("\'");
    return sb.toString();
  }


  /** Variable map entry.
  */
  protected static class VariableMapItem
  {
    protected String value;
    protected ArrayList params;

    /** Constructor.
    */
    public VariableMapItem(String value, ArrayList params)
    {
      this.value = value;
      this.params = params;
    }

    /** Get value.
    */
    public String getValue()
    {
      return value;
    }

    /** Get parameters.
    */
    public ArrayList getParameters()
    {
      return params;
    }
  }

  /** Variable map.
  */
  protected static class VariableMap
  {
    protected Map variableMap = new HashMap();

    /** Constructor
    */
    public VariableMap()
    {
    }

    /** Add a variable map entry */
    public void addVariable(String variableName, String value, ArrayList parameters)
    {
      VariableMapItem e = new VariableMapItem(value,parameters);
      variableMap.put(variableName,e);
    }

    /** Get a variable map entry */
    public VariableMapItem getVariable(String variableName)
    {
      return (VariableMapItem)variableMap.get(variableName);
    }
  }

  /** This class represents data gleaned from a document specification, in a more usable form.
  */
  protected static class TableSpec
  {
    public final String idQuery;
    public final String versionQuery;
    public final String dataQuery;
    public final String aclQuery;
    public final boolean securityOn;
    public final Set<String> aclMap = new HashSet<String>();

    public TableSpec(Specification ds)
    {
      String idQuery = null;
      String versionQuery = null;
      String dataQuery = null;
      String aclQuery = null;
      boolean securityOn = false;
      for (int i = 0; i < ds.getChildCount(); i++)
      {
        SpecificationNode sn = ds.getChild(i);
        if (sn.getType().equals(JDBCConstants.idQueryNode))
        {
          idQuery = sn.getValue();
          if (idQuery == null)
            idQuery = "";
        }
        else if (sn.getType().equals(JDBCConstants.versionQueryNode))
        {
          versionQuery = sn.getValue();
          if (versionQuery == null)
            versionQuery = "";
        }
        else if (sn.getType().equals(JDBCConstants.dataQueryNode))
        {
          dataQuery = sn.getValue();
          if (dataQuery == null)
            dataQuery = "";
        }
        else if (sn.getType().equals(JDBCConstants.aclQueryNode))
        {
          aclQuery = sn.getValue();
          if (aclQuery == null)
            aclQuery = "";
        }
        else if (sn.getType().equals("access"))
        {
          String token = sn.getAttributeValue("token");
          aclMap.add(token);
        }
        else if (sn.getType().equals("security"))
        {
          String value = sn.getAttributeValue("value");
          securityOn = value.equals("on");
        }
      }
      this.idQuery = idQuery;
      this.versionQuery = versionQuery;
      this.dataQuery = dataQuery;
      this.aclQuery = aclQuery;
      this.securityOn = securityOn;
    }

    public Set<String> getAcls()
    {
      return aclMap;
    }

    public boolean isSecurityOn()
    {
      return securityOn;
    }
    
  }

}


