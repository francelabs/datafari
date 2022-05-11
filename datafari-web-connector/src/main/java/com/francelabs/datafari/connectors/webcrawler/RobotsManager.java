/* $Id: RobotsManager.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package com.francelabs.datafari.connectors.webcrawler;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.apache.manifoldcf.authorities.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.CacheKeyFactory;
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import org.apache.manifoldcf.crawler.system.Logging;


/** This class manages the database table into which we write robots.txt files for hosts.  The data resides in the database,
* as well as in cache (up to a certain point).  The result is that there is a memory limited, database-backed repository
* of robots files that we can draw on.
* 
* <br><br>
* <b>robotsdata</b>
* <table border="1" cellpadding="3" cellspacing="0" summary="">
* <tr class="TableHeadingColor">
* <th>Field</th><th>Type</th><th>Description&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
* <tr><td>hostname</td><td>VARCHAR(255)</td><td>Primary Key</td></tr>
* <tr><td>robotsdata</td><td>BIGINT</td><td></td></tr>
* <tr><td>expirationtime</td><td>BLOB</td><td></td></tr>
* </table>
* <br><br>
* 
*/
public class RobotsManager extends org.apache.manifoldcf.core.database.BaseTable
{
  public static final String _rcsid = "@(#)$Id: RobotsManager.java 988245 2010-08-23 18:39:35Z kwright $";

  // Robots cache class.  Only one needed.
  protected static RobotsCacheClass robotsCacheClass = new RobotsCacheClass();

  // Database fields
  protected final static String hostField = "hostname";
  protected final static String robotsField = "robotsdata";
  protected final static String expirationField = "expirationtime";

  // Cache manager.  This handle is set up during the constructor.
  ICacheManager cacheManager;

  /** Constructor.  Note that one robotsmanager handle is only useful within a specific thread context,
  * so the calling connector object logic must recreate the handle whenever the thread context changes.
  *@param tc is the thread context.
  *@param database is the database handle.
  */
  public RobotsManager(IThreadContext tc, IDBInterface database)
    throws ManifoldCFException
  {
    super(database,"robotsdata");
    cacheManager = CacheManagerFactory.make(tc);
  }

  /** Install the manager.
  */
  public void install()
    throws ManifoldCFException
  {
    // Standard practice: outer loop on install methods, no transactions
    while (true)
    {
      Map existing = getTableSchema(null,null);
      if (existing == null)
      {
        // Install the table.
        HashMap map = new HashMap();
        map.put(hostField,new ColumnDescription("VARCHAR(255)",true,false,null,null,false));
        map.put(expirationField,new ColumnDescription("BIGINT",false,false,null,null,false));
        map.put(robotsField,new ColumnDescription("BLOB",false,true,null,null,false));
        performCreate(map,null);
      }
      else
      {
        // Upgrade code, if needed, goes here
      }

      // Handle indexes, if needed

      break;
    }
  }

  /** Uninstall the manager.
  */
  public void deinstall()
    throws ManifoldCFException
  {
    performDrop(null);
  }


  /** Read robots.txt data from the cache or from the database.
  *@param hostName is the host for which the data is desired.
  *@param currentTime is the time of the check.
  *@return null if the record needs to be fetched, true if fetch is allowed.
  */
  public Boolean checkFetchAllowed(String userAgent, String hostName, long currentTime, String pathString,
    IProcessActivity activities)
    throws ManifoldCFException
  {
    // Build description objects
    HostDescription[] objectDescriptions = new HostDescription[1];
    StringSetBuffer ssb = new StringSetBuffer();
    ssb.add(getRobotsKey(hostName));
    objectDescriptions[0] = new HostDescription(hostName,new StringSet(ssb));

    HostExecutor exec = new HostExecutor(this,activities,objectDescriptions[0]);
    cacheManager.findObjectsAndExecute(objectDescriptions,null,exec,getTransactionID());

    // We do the expiration check here, rather than in the query, so that caching
    // is possible.
    RobotsData rd = exec.getResults();
    if (rd == null || rd.getExpirationTime() <= currentTime)
      return null;
    return new Boolean(rd.isFetchAllowed(userAgent,pathString));
  }

  /** Write robots.txt, replacing any existing row.
  *@param hostName is the host.
  *@param expirationTime is the time this data should expire.
  *@param data is the robots data stream.  May be null.
  */
  public void writeRobotsData(String hostName, long expirationTime, InputStream data)
    throws ManifoldCFException, IOException
  {
    TempFileInput tfi = null;
    try
    {
      if (data != null)
      {
        try
        {
          tfi = new TempFileInput(data);
        }
        catch (ManifoldCFException e)
        {
          if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
            throw e;
          throw new IOException("Fetch failed: "+e.getMessage());
        }
      }

      StringSetBuffer ssb = new StringSetBuffer();
      ssb.add(getRobotsKey(hostName));
      StringSet cacheKeys = new StringSet(ssb);
      ICacheHandle ch = cacheManager.enterCache(null,cacheKeys,getTransactionID());
      try
      {

        beginTransaction();
        try
        {
          // See whether the instance exists
          ArrayList params = new ArrayList();
          params.add(hostName);
          IResultSet set = performQuery("SELECT * FROM "+getTableName()+" WHERE "+
            hostField+"=?",params,null,null);
          HashMap values = new HashMap();
          values.put(expirationField,new Long(expirationTime));
          if (tfi != null)
            values.put(robotsField,tfi);
          if (set.getRowCount() > 0)
          {
            // Update
            params.clear();
            params.add(hostName);
            performUpdate(values," WHERE "+hostField+"=?",params,null);
          }
          else
          {
            // Insert
            values.put(hostField,hostName);
            // We only need the general key because this is new.
            performInsert(values,null);
          }
          cacheManager.invalidateKeys(ch);
        }
        catch (ManifoldCFException e)
        {
          signalRollback();
          throw e;
        }
        catch (Error e)
        {
          signalRollback();
          throw e;
        }
        finally
        {
          endTransaction();
        }
      }
      finally
      {
        cacheManager.leaveCache(ch);
      }
    }
    finally
    {
      if (tfi != null)
        tfi.discard();
    }
  }

  // Protected methods and classes

  /** Construct a key which represents an individual host name.
  *@param hostName is the name of the connector.
  *@return the cache key.
  */
  protected static String getRobotsKey(String hostName)
  {
    return "ROBOTS_"+hostName;
  }

  /** Read robots data, if it exists.
  *@return null if the data doesn't exist at all.  Return robots data if it does.
  */
  protected RobotsData readRobotsData(String hostName, IProcessActivity activities)
    throws ManifoldCFException
  {
    try
    {
      ArrayList list = new ArrayList();
      list.add(hostName);
      IResultSet set = performQuery("SELECT "+robotsField+","+expirationField+" FROM "+getTableName()+
        " WHERE "+hostField+"=?",list,null,null);
      if (set.getRowCount() == 0)
        return null;
      if (set.getRowCount() > 1)
        throw new ManifoldCFException("Unexpected number of robotsdata rows matching '"+hostName+"': "+Integer.toString(set.getRowCount()));
      IResultRow row = set.getRow(0);
      long expiration = ((Long)row.getValue(expirationField)).longValue();
      BinaryInput bi = (BinaryInput)row.getValue(robotsField);
      if (bi == null)
        return new RobotsData(null,expiration,hostName,activities);
      try
      {
        InputStream is = bi.getStream();
        return new RobotsData(is,expiration,hostName,activities);
      }
      finally
      {
        bi.discard();
      }
    }
    catch (InterruptedIOException e)
    {
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (IOException e)
    {
      throw new ManifoldCFException("IO error reading robots data for "+hostName+": "+e.getMessage(),e);
    }
  }

  /** Convert a string from the robots file into a readable form that does NOT contain NUL characters (since postgresql does not accept those).
  */
  protected static String makeReadable(String inputString)
  {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < inputString.length())
    {
      char y = inputString.charAt(i++);
      if (y >= ' ')
        sb.append(y);
      else
      {
        sb.append('^');
        sb.append((char)(y + '@'));
      }
    }
    return sb.toString();
  }

  /** This is a cached data item.
  */
  protected static class RobotsData
  {
    protected long expiration;
    protected ArrayList records = null;

    /** Constructor. */
    public RobotsData(InputStream is, long expiration, String hostName, IProcessActivity activities)
      throws IOException, ManifoldCFException
    {
      this.expiration = expiration;
      if (is == null)
      {
        records = null;
        return;
      }
      Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);
      try
      {
        BufferedReader br = new BufferedReader(r);
        try
        {
          parseRobotsTxt(br,hostName,activities);
        }
        finally
        {
          br.close();
        }
      }
      finally
      {
        r.close();
      }
    }

    /** Check if fetch is allowed */
    public boolean isFetchAllowed(String userAgent, String pathString)
    {
      if (records == null)
        return true;

      boolean wasDisallowed = false;
      boolean wasAllowed = false;

      // First matching user-agent takes precedence, according to the following chunk of spec:
      // "These name tokens are used in User-agent lines in /robots.txt to
      // identify to which specific robots the record applies. The robot
      // must obey the first record in /robots.txt that contains a User-
      // Agent line whose value contains the name token of the robot as a
      // substring. The name comparisons are case-insensitive. If no such
      // record exists, it should obey the first record with a User-agent
      // line with a "*" value, if present. If no record satisfied either
      // condition, or no records are present at all, access is unlimited."

      boolean sawAgent = false;

      String userAgentUpper = userAgent.toUpperCase(Locale.ROOT);

      int i = 0;
      while (i < records.size())
      {
        Record r = (Record)records.get(i++);
        if (r.isAgentMatch(userAgentUpper,false))
        {
          if (r.isDisallowed(pathString))
            wasDisallowed = true;
          if (r.isAllowed(pathString))
            wasAllowed = true;

          sawAgent = true;
          break;
        }
      }
      if (sawAgent == false)
      {
        i = 0;
        while (i < records.size())
        {
          Record r = (Record)records.get(i++);
          if (r.isAgentMatch("*",true))
          {
            if (r.isDisallowed(pathString))
              wasDisallowed = true;
            if (r.isAllowed(pathString))
              wasAllowed = true;

            sawAgent = true;
            break;
          }
        }
      }

      if (sawAgent == false)
        return true;

      // Allowed always overrides disallowed
      if (wasAllowed)
        return true;
      if (wasDisallowed)
        return false;

      // No match -> crawl allowed
      return true;
    }

    /** Get expiration */
    public long getExpirationTime()
    {
      return expiration;
    }

    /** Parse the robots.txt file using a reader.
    * Is NOT expected to close the stream.
    */
    protected void parseRobotsTxt(BufferedReader r, String hostName, IProcessActivity activities)
      throws IOException, ManifoldCFException
    {
      boolean parseCompleted = false;
      boolean robotsWasHtml = false;
      boolean foundErrors = false;
      String description = null;

      long startParseTime = System.currentTimeMillis();
      try
      {
        records = new ArrayList();
        Record record = null;
        boolean seenAction = false;
        while (true)
        {
          String x = r.readLine();
          if (x == null)
            break;
          int numSignPos = x.indexOf("#");
          if (numSignPos != -1)
            x = x.substring(0,numSignPos);
          String lowercaseLine = x.toLowerCase(Locale.ROOT).trim();
          if (lowercaseLine.startsWith("user-agent:"))
          {
            if (seenAction)
            {
              records.add(record);
              record = null;
              seenAction = false;
            }
            if (record == null)
              record = new Record();

            String agentName = x.substring("User-agent:".length()).trim();
            record.addAgent(agentName);
          }
          else if (lowercaseLine.startsWith("user-agent"))
          {
            if (seenAction)
            {
              records.add(record);
              record = null;
              seenAction = false;
            }
            if (record == null)
              record = new Record();

            String agentName = x.substring("User-agent".length()).trim();
            record.addAgent(agentName);
          }
          else if (lowercaseLine.startsWith("disallow:"))
          {
            if (record == null)
            {
              description = "Disallow without User-agent";
              Logging.connectors.warn("Web: Bad robots.txt file format from '"+hostName+"': "+description);
              foundErrors = true;
            }
            else
            {
              String disallowPath = x.substring("Disallow:".length()).trim();
              // The spec says that a blank disallow means let everything through.
              if (disallowPath.length() > 0)
                record.addDisallow(disallowPath);
              seenAction = true;
            }
          }
          else if (lowercaseLine.startsWith("disallow"))
          {
            if (record == null)
            {
              description = "Disallow without User-agent";
              Logging.connectors.warn("Web: Bad robots.txt file format from '"+hostName+"': "+description);
              foundErrors = true;
            }
            else
            {
              String disallowPath = x.substring("Disallow".length()).trim();
              // The spec says that a blank disallow means let everything through.
              if (disallowPath.length() > 0)
                record.addDisallow(disallowPath);
              seenAction = true;
            }
          }
          else if (lowercaseLine.startsWith("allow:"))
          {
            if (record == null)
            {
              description = "Allow without User-agent";
              Logging.connectors.warn("Web: Bad robots.txt file format from '"+hostName+"': "+description);
              foundErrors = true;
            }
            else
            {
              String allowPath = x.substring("Allow:".length()).trim();
              // The spec says that a blank disallow means let everything through.
              if (allowPath.length() > 0)
                record.addAllow(allowPath);
              seenAction = true;
            }
          }
          else if (lowercaseLine.startsWith("allow"))
          {
            if (record == null)
            {
              description = "Allow without User-agent";
              Logging.connectors.warn("Web: Bad robots.txt file format from '"+hostName+"': "+description);
              foundErrors = true;
            }
            else
            {
              String allowPath = x.substring("Allow".length()).trim();
              // The spec says that a blank disallow means let everything through.
              if (allowPath.length() > 0)
                record.addAllow(allowPath);
              seenAction = true;
            }
          }
          else if (lowercaseLine.startsWith("crawl-delay:"))
          {
            // We don't complain about this, but right now we don't listen to it either.
          }
          else if (lowercaseLine.startsWith("crawl-delay"))
          {
            // We don't complain about this, but right now we don't listen to it either.
          }
          else
          {
            // If it's not just a blank line, complain
            if (x.trim().length() > 0)
            {
              String problemLine = makeReadable(x);
              description = "Unknown robots.txt line: '"+problemLine+"'";
              Logging.connectors.warn("Web: Unknown robots.txt line from '"+hostName+"': '"+problemLine+"'");
              if (x.indexOf("<html") != -1 || x.indexOf("<HTML") != -1)
              {
                // Looks like some kind of an html file, probably as a result of a redirection, so just abort as if we have a page error
                robotsWasHtml = true;
                parseCompleted = true;
                break;
              }
              foundErrors = true;
            }
          }
        }
        if (record != null)
          records.add(record);
        parseCompleted = true;
      }
      finally
      {
        // Log the fact that we attempted to parse robots.txt, as well as what happened
        // These are the following situations we will report:
        // (1) INCOMPLETE - Parsing did not complete - if the stream was interrupted
        // (2) HTML - Robots was html - if the robots data seemed to be html
        // (3) ERRORS - Robots had errors - if the robots data was accepted but had errors in it
        // (4) SUCCESS - Robots parsed successfully - if the robots data was parsed without problem
        String status;
        if (parseCompleted)
        {
          if (robotsWasHtml)
          {
            status = "HTML";
            description = "Robots file contained HTML, skipped";
          }
          else
          {
            if (foundErrors)
            {
              status = "ERRORS";
              // description should already be set
            }
            else
            {
              status = "SUCCESS";
              description = null;
            }
          }
        }
        else
        {
          status = "INCOMPLETE";
          description = "Parsing was interrupted";
        }

        activities.recordActivity(new Long(startParseTime),WebcrawlerConnector.ACTIVITY_ROBOTSPARSE,
          null,hostName,status,description,null);

      }
    }

  }

  /** Check if path matches specification */
  protected static boolean doesPathMatch(String path, String spec)
  {
    // For robots 1.0, this function would do just this:
    // return path.startsWith(spec);
    // However, we implement the "google bot" spec, which allows wildcard matches that are, in fact, regular-expression-like in some ways.
    // The "specification" can be found here: http://www.google.com/support/webmasters/bin/answer.py?hl=en&answer=40367
    return doesPathMatch(path,0,spec,0);
  }

  /** Recursive method for matching specification to path. */
  protected static boolean doesPathMatch(String path, int pathIndex, String spec, int specIndex)
  {
    while (true)
    {
      if (specIndex == spec.length())
        // Hit the end of the specification!  We're done.
        return true;
      char specChar = spec.charAt(specIndex++);
      if (specChar == '*')
      {
        // Found a specification wildcard.
        // Eat up all the '*' characters at this position - otherwise each additional one increments the exponent of how long this can take,
        // making denial-of-service via robots parsing a possibility.
        while (specIndex < spec.length())
        {
          if (spec.charAt(specIndex) != '*')
            break;
          specIndex++;
        }
        // It represents zero or more characters, so we must recursively try for a match against all remaining characters in the path string.
        while (true)
        {
          boolean match = doesPathMatch(path,pathIndex,spec,specIndex);
          if (match)
            return true;
          if (path.length() == pathIndex)
            // Nothing further to try, and no match
            return false;
          pathIndex++;
          // Try again
        }
      }
      else if (specChar == '$' && specIndex == spec.length())
      {
        // Found a specification end-of-path character.
        // (It can only be legitimately the last character of the specification.)
        return pathIndex == path.length();
      }
      if (pathIndex == path.length())
        // Hit the end of the path! (but not the end of the specification!)
        return false;
      if (path.charAt(pathIndex) != specChar)
        return false;
      // On to the next match
      pathIndex++;
    }
  }

  /** This is the object description for a robots host object.
  * This is the key that is used to look up cached data.
  */
  protected static class HostDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription
  {
    protected String hostName;
    protected String criticalSectionName;
    protected StringSet cacheKeys;

    public HostDescription(String hostName, StringSet invKeys)
    {
      super("robotscache");
      this.hostName = hostName;
      criticalSectionName = getClass().getName()+"-"+hostName;
      cacheKeys = invKeys;
    }

    public String getHostName()
    {
      return hostName;
    }

    public int hashCode()
    {
      return hostName.hashCode();
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof HostDescription))
        return false;
      HostDescription d = (HostDescription)o;
      return d.hostName.equals(hostName);
    }

    public String getCriticalSectionName()
    {
      return criticalSectionName;
    }

    /** Get the cache keys for an object (which may or may not exist yet in
    * the cache).  This method is called in order for cache manager to throw the correct locks.
    * @return the object's cache keys, or null if the object should not
    * be cached.
    */
    public StringSet getObjectKeys()
    {
      return cacheKeys;
    }

    /** Get the object class for an object.  The object class is used to determine
    * the group of objects treated in the same LRU manner.
    * @return the newly created object's object class, or null if there is no
    * such class, and LRU behavior is not desired.
    */
    public ICacheClass getObjectClass()
    {
      return robotsCacheClass;
    }
  }

  /** Cache class for robots.
  * An instance of this class describes the cache class for robots data caching.  There's
  * only ever a need for one, so that will be created statically.
  */
  protected static class RobotsCacheClass implements ICacheClass
  {
    /** Get the name of the object class.
    * This determines the set of objects that are treated in the same
    * LRU pool.
    *@return the class name.
    */
    public String getClassName()
    {
      // We count all the robot data, so this is a constant string.
      return "ROBOTSCLASS";
    }

    /** Get the maximum LRU count of the object class.
    *@return the maximum number of the objects of the particular class
    * allowed.
    */
    public int getMaxLRUCount()
    {
      // Hardwired for the moment; 2000 robots data records will be cached,
      // and no more.
      return 2000;
    }

  }

  /** This is the executor object for locating robots host objects.
  * This object furnishes the operations the cache manager needs to rebuild objects that it needs that are
  * not in the cache at the moment.
  */
  protected static class HostExecutor extends org.apache.manifoldcf.core.cachemanager.ExecutorBase
  {
    // Member variables
    protected RobotsManager thisManager;
    protected RobotsData returnValue;
    protected HostDescription thisHost;
    protected IProcessActivity activities;

    /** Constructor.
    *@param manager is the RobotsManager class instance.
    *@param objectDescription is the desired object description.
    */
    public HostExecutor(RobotsManager manager, IProcessActivity activities, HostDescription objectDescription)
    {
      super();
      thisManager = manager;
      this.activities = activities;
      thisHost = objectDescription;
      returnValue = null;
    }

    /** Get the result.
    *@return the looked-up or read cached instance.
    */
    public RobotsData getResults()
    {
      return returnValue;
    }

    /** Create a set of new objects to operate on and cache.  This method is called only
    * if the specified object(s) are NOT available in the cache.  The specified objects
    * should be created and returned; if they are not created, it means that the
    * execution cannot proceed, and the execute() method will not be called.
    * @param objectDescriptions is the set of unique identifier of the object.
    * @return the newly created objects to cache, or null, if any object cannot be created.
    *  The order of the returned objects must correspond to the order of the object descriptinos.
    */
    public Object[] create(ICacheDescription[] objectDescriptions) throws ManifoldCFException
    {
      // I'm not expecting multiple values to be request, so it's OK to walk through the objects
      // and do a request at a time.
      RobotsData[] rval = new RobotsData[objectDescriptions.length];
      int i = 0;
      while (i < rval.length)
      {
        HostDescription desc = (HostDescription)objectDescriptions[i];
        // I need to cache both the data and the expiration date, and pick up both when I
        // do the query.  This is because I don't want to cache based on request time, since that
        // would screw up everything!
        rval[i] = thisManager.readRobotsData(desc.getHostName(),activities);
        i++;
      }

      return rval;
    }


    /** Notify the implementing class of the existence of a cached version of the
    * object.  The object is passed to this method so that the execute() method below
    * will have it available to operate on.  This method is also called for all objects
    * that are freshly created as well.
    * @param objectDescription is the unique identifier of the object.
    * @param cachedObject is the cached object.
    */
    public void exists(ICacheDescription objectDescription, Object cachedObject) throws ManifoldCFException
    {
      // Cast what came in as what it really is
      HostDescription objectDesc = (HostDescription)objectDescription;
      RobotsData robotsData = (RobotsData)cachedObject;
      if (objectDesc.equals(thisHost))
        returnValue = robotsData;
    }

    /** Perform the desired operation.  This method is called after either createGetObject()
    * or exists() is called for every requested object.
    */
    public void execute() throws ManifoldCFException
    {
      // Does nothing; we only want to fetch objects in this cacher.
    }


  }

  /** This class represents a record in a robots.txt file.  It contains one or
  * more user-agents, and one or more disallows.
  */
  protected static class Record
  {
    protected ArrayList userAgents = new ArrayList();
    protected ArrayList disallows = new ArrayList();
    protected ArrayList allows = new ArrayList();

    /** Constructor.
    */
    public Record()
    {
    }

    /** Add a user-agent.
    */
    public void addAgent(String agentName)
    {
      userAgents.add(agentName);
    }

    /** Add a disallow.
    */
    public void addDisallow(String disallowPath)
    {
      disallows.add(disallowPath);
    }

    /** Add an allow.
    */
    public void addAllow(String allowPath)
    {
      allows.add(allowPath);
    }

    /** See if user-agent matches.
    */
    public boolean isAgentMatch(String agentNameUpper, boolean exactMatch)
    {
      int i = 0;
      while (i < userAgents.size())
      {
        String agent = ((String)userAgents.get(i++)).toUpperCase(Locale.ROOT);
        if (exactMatch && agent.trim().equals(agentNameUpper))
          return true;
        if (!exactMatch && agentNameUpper.indexOf(agent) != -1)
          return true;
      }
      return false;
    }

    /** See if path is disallowed.  Only called if user-agent has already
    * matched.  (This checks if there's an explicit match with one of the
    * Disallows clauses.)
    */
    public boolean isDisallowed(String path)
    {
      int i = 0;
      while (i < disallows.size())
      {
        String disallow = (String)disallows.get(i++);
        if (doesPathMatch(path,disallow))
          return true;
      }
      return false;
    }

    /** See if path is allowed.  Only called if user-agent has already
    * matched.  (This checks if there's an explicit match with one of the
    * Allows clauses).
    */
    public boolean isAllowed(String path)
    {
      int i = 0;
      while (i < allows.size())
      {
        String allow = (String)allows.get(i++);
        if (doesPathMatch(path,allow))
          return true;
      }
      return false;
    }

  }

}
