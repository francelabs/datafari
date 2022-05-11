/* $Id: DNSManager.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import java.util.*;
import java.io.*;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.apache.manifoldcf.authorities.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.CacheKeyFactory;
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import org.apache.manifoldcf.crawler.system.Logging;


/** This class manages the database table into which we DNS entries for hosts.  The data resides in the database,
* as well as in cache (up to a certain point).  The result is that there is a memory limited, database-backed repository
* of DNS entries that we can draw on.
* Note that this code is also responsible for efficiently caching the mapping of IP address to a canonical host name.
* 
* <br><br>
* <b>dnsdata</b>
* <table border="1" cellpadding="3" cellspacing="0" summary="">
* <tr class="TableHeadingColor">
* <th>Field</th><th>Type</th><th>Description&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
* <tr><td>hostname</td><td>VARCHAR(255)</td><td>Primary Key</td></tr>
* <tr><td>canonicalhostname</td><td>VARCHAR(255)</td><td></td></tr>
* <tr><td>ipaddress</td><td>VARCHAR(16)</td><td></td></tr>
* <tr><td>expirationtime</td><td>BIGINT</td><td></td></tr>
* </table>
* <br><br>
* 
*/
public class DNSManager extends org.apache.manifoldcf.core.database.BaseTable
{
  public static final String _rcsid = "@(#)$Id: DNSManager.java 988245 2010-08-23 18:39:35Z kwright $";

  // Robots cache class.  Only one needed.
  protected static DNSCacheClass dnsCacheClass = new DNSCacheClass();

  // Database fields
  protected final static String hostField = "hostname";
  protected final static String fqdnField = "canonicalhostname";
  protected final static String ipaddressField = "ipaddress";
  protected final static String expirationField = "expirationtime";

  // Cache manager.  This handle is set up during the constructor.
  ICacheManager cacheManager;

  /** Constructor.  Note that one robotsmanager handle is only useful within a specific thread context,
  * so the calling connector object logic must recreate the handle whenever the thread context changes.
  *@param tc is the thread context.
  *@param database is the database handle.
  */
  public DNSManager(IThreadContext tc, IDBInterface database)
    throws ManifoldCFException
  {
    super(database,"dnsdata");
    cacheManager = CacheManagerFactory.make(tc);
  }

  /** Install the manager.
  */
  public void install()
    throws ManifoldCFException
  {
    // Standard practice: outer loop, no transactions
    while (true)
    {
      Map existing = getTableSchema(null,null);
      if (existing == null)
      {
        // Install the table.
        HashMap map = new HashMap();
        map.put(hostField,new ColumnDescription("VARCHAR(255)",true,false,null,null,false));
        map.put(fqdnField,new ColumnDescription("VARCHAR(255)",false,true,null,null,false));
        map.put(ipaddressField,new ColumnDescription("VARCHAR(16)",false,true,null,null,false));
        map.put(expirationField,new ColumnDescription("BIGINT",false,false,null,null,false));
        performCreate(map,null);
      }
      else
      {
        // Upgrade code, if needed, goes here
      }

      // Handle indexes

      // I thought at one point this index might be useful, but it doesn't seem necessary after all
      // ArrayList list = new ArrayList();
      // list.add(ipaddressField);
      // addTableIndex(false,list);

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

  /** Given a host name, look up the ip address and fqdn.
  *@return null if there is no available cached version of this info.
  */
  public DNSInfo lookup(String hostName, long currentTime)
    throws ManifoldCFException
  {
    // Build description objects
    HostDescription[] objectDescriptions = new HostDescription[1];
    StringSetBuffer ssb = new StringSetBuffer();
    ssb.add(getDNSKey(hostName));
    objectDescriptions[0] = new HostDescription(hostName,new StringSet(ssb));

    HostExecutor exec = new HostExecutor(this,objectDescriptions[0]);
    cacheManager.findObjectsAndExecute(objectDescriptions,null,exec,getTransactionID());

    // DNSInfo object must be built if it isn't yet present.
    DNSInfo rd = exec.getResults();
    if (rd == null || rd.getExpirationTime() <= currentTime)
      return null;
    return rd;
  }

  /** Write DNS data, replacing any existing row.
  *@param hostName is the host.
  *@param fqdn is the canonical host name.
  *@param ipaddress is the host ip address, in standard form.
  *@param expirationTime is the time this data should expire.
  */
  public void writeDNSData(String hostName, String fqdn, String ipaddress, long expirationTime)
    throws ManifoldCFException
  {
    StringSetBuffer ssb = new StringSetBuffer();
    ssb.add(getDNSKey(hostName));
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
        if (fqdn == null)
          fqdn = "";
        values.put(fqdnField,fqdn);
        if (ipaddress == null)
          ipaddress = "";
        values.put(ipaddressField, ipaddress);
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

  // Protected methods and classes

  /** Construct a key which represents an individual host name.
  *@param hostName is the name of the connector.
  *@return the cache key.
  */
  protected static String getDNSKey(String hostName)
  {
    return "DNS_"+hostName;
  }

  /** Read DNS data, if it exists.
  *@return null if the data doesn't exist at all.  Return DNS data if it does.
  */
  protected DNSInfo readDNSInfo(String hostName)
    throws ManifoldCFException
  {
    ArrayList list = new ArrayList();
    list.add(hostName);
    IResultSet set = performQuery("SELECT "+ipaddressField+","+fqdnField+","+expirationField+" FROM "+getTableName()+
      " WHERE "+hostField+"=?",list,null,null);
    if (set.getRowCount() == 0)
      return null;
    IResultRow row = set.getRow(0);
    long expiration = ((Long)row.getValue(expirationField)).longValue();
    String ipaddress = (String)row.getValue(ipaddressField);
    if (ipaddress != null && ipaddress.length() == 0)
      ipaddress = null;
    String fqdn = (String)row.getValue(fqdnField);
    if (fqdn != null && fqdn.length() == 0)
      fqdn = null;
    return new DNSInfo(ipaddress,fqdn,expiration,hostName);
  }

  /** This is a cached data item.
  */
  protected static class DNSInfo
  {
    protected long expiration;
    protected String hostName;
    protected String ipaddress;
    protected String fqdn;

    /** Constructor. */
    public DNSInfo(String ipaddress, String fqdn, long expiration, String hostName)
    {
      this.ipaddress = ipaddress;
      this.fqdn = fqdn;
      this.expiration = expiration;
      this.hostName = hostName;
    }

    /** Get the ipaddress */
    public String getIPAddress()
    {
      return ipaddress;
    }

    /** Get the fqdn */
    public String getFQDN()
    {
      return fqdn;
    }

    /** Get the expiration time. */
    public long getExpirationTime()
    {
      return expiration;
    }

    /** Get the host name */
    public String getHostName()
    {
      return hostName;
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
      super("dnscache");
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
      return dnsCacheClass;
    }
  }

  /** Cache class for robots.
  * An instance of this class describes the cache class for robots data caching.  There's
  * only ever a need for one, so that will be created statically.
  */
  protected static class DNSCacheClass implements ICacheClass
  {
    /** Get the name of the object class.
    * This determines the set of objects that are treated in the same
    * LRU pool.
    *@return the class name.
    */
    public String getClassName()
    {
      // We count all the robot data, so this is a constant string.
      return "DNSCLASS";
    }

    /** Get the maximum LRU count of the object class.
    *@return the maximum number of the objects of the particular class
    * allowed.
    */
    public int getMaxLRUCount()
    {
      // Hardwired for the moment; 2000 dns data records will be cached,
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
    protected DNSManager thisManager;
    protected DNSInfo returnValue;
    protected HostDescription thisHost;

    /** Constructor.
    *@param manager is the RobotsManager class instance.
    *@param objectDescription is the desired object description.
    */
    public HostExecutor(DNSManager manager, HostDescription objectDescription)
    {
      super();
      thisManager = manager;
      thisHost = objectDescription;
      returnValue = null;
    }

    /** Get the result.
    *@return the looked-up or read cached instance.
    */
    public DNSInfo getResults()
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
      DNSInfo[] rval = new DNSInfo[objectDescriptions.length];
      int i = 0;
      while (i < rval.length)
      {
        HostDescription desc = (HostDescription)objectDescriptions[i];
        // I need to cache both the data and the expiration date, and pick up both when I
        // do the query.  This is because I don't want to cache based on request time, since that
        // would screw up everything!
        rval[i] = thisManager.readDNSInfo(desc.getHostName());
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
      DNSInfo data = (DNSInfo)cachedObject;
      if (objectDesc.equals(thisHost))
        returnValue = data;
    }

    /** Perform the desired operation.  This method is called after either createGetObject()
    * or exists() is called for every requested object.
    */
    public void execute() throws ManifoldCFException
    {
      // Does nothing; we only want to fetch objects in this cacher.
    }


  }


}
