/* $Id: ThrottleDescription.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.connectorcommon.interfaces.*;
import java.util.*;
import java.util.regex.*;

/** This class describes complex throttling criteria pulled from a configuration.
* The data contained is organized by regular expression performed on a bin.  What we store
* for each regular expression is a Pattern, for efficiency.
*
* This structure deals with bandwidth limits, maximum connection limits, and maximum fetch rate
* limits.  Average fetch rate limits are handled in the infrastructure.
*
* Generally it is a good thing to limit the number of regexps that need to be evaluated against
* any given bin value as much as possible.  For that reason I've organized this structure
* accordingly.
*/
public class ThrottleDescription implements IThrottleSpec
{
  public static final String _rcsid = "@(#)$Id: ThrottleDescription.java 988245 2010-08-23 18:39:35Z kwright $";

  /** This is the hash that contains everything.  It's keyed by the regexp string itself.
  * Values are ThrottleItem's. */
  protected Map<String,ThrottleItem> patternHash = new HashMap<String,ThrottleItem>();

  /** Constructor.  Build the description from the ConfigParams. */
  public ThrottleDescription(ConfigParams configData)
    throws ManifoldCFException
  {
    // Scan, looking for bin description nodes
    int i = 0;
    while (i < configData.getChildCount())
    {
      ConfigNode node = configData.getChild(i++);
      if (node.getType().equals(WebcrawlerConfig.NODE_BINDESC))
      {
        // Get the bin regexp
        String binDescription = node.getAttributeValue(WebcrawlerConfig.ATTR_BINREGEXP);
        // Get the case sensitivity flag
        String caseSensitive = node.getAttributeValue(WebcrawlerConfig.ATTR_INSENSITIVE);
        boolean isInsensitive = false;
        if (caseSensitive != null && caseSensitive.equalsIgnoreCase("true"))
          isInsensitive = true;
        // Now, go through this node's children looking for values we know about.
        Integer maxConnectionCount = null;
        Double minMillisecondsPerByte = null;
        Long minMillisecondsPerFetch = null;
        int j = 0;
        while (j < node.getChildCount())
        {
          ConfigNode childNode = node.getChild(j++);
          if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXCONNECTIONS))
          {
            String value = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
            if (value != null)
            {
              try
              {
                maxConnectionCount = new Integer(value);
              }
              catch (NumberFormatException e)
              {
                throw new ManifoldCFException("Bad number",e);
              }
            }
          }
          else if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXKBPERSECOND))
          {
            String value = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
            if (value != null)
            {
              try
              {
                double kbPerSecond = new Double(value).doubleValue();
                if (kbPerSecond > 0)
                  minMillisecondsPerByte = new Double(1.0/(double)kbPerSecond);
              }
              catch (NumberFormatException e)
              {
                throw new ManifoldCFException("Bad number",e);
              }
            }
          }
          else if (childNode.getType().equals(WebcrawlerConfig.NODE_MAXFETCHESPERMINUTE))
          {
            String value = childNode.getAttributeValue(WebcrawlerConfig.ATTR_VALUE);
            if (value != null)
            {
              try
              {
                double fetchesPerMinute = new Double(value).doubleValue();
                if (fetchesPerMinute > 0)
                  minMillisecondsPerFetch = new Long((long)(((double)60000.0)/(double)fetchesPerMinute));
              }
              catch (NumberFormatException e)
              {
                throw new ManifoldCFException("Bad number: "+e.getMessage(),e);
              }
            }
          }
        }
        try
        {
          int flags = Pattern.UNICODE_CASE;
          if (isInsensitive)
            flags |= Pattern.CASE_INSENSITIVE;
          Pattern p;
          try
          {
            p = Pattern.compile(binDescription,flags);
          }
          catch (java.util.regex.PatternSyntaxException e)
          {
            throw new ManifoldCFException("Bin regular expression '"+binDescription+"' is illegal: "+e.getMessage(),e);
          }
          ThrottleItem ti = new ThrottleItem(p);
          ti.setMaxOpenConnections(maxConnectionCount);
          ti.setMinimumMillisecondsPerByte(minMillisecondsPerByte);
          ti.setMinimumMillisecondsPerFetch(minMillisecondsPerFetch);
          patternHash.put(binDescription,ti);
        }
        catch (PatternSyntaxException e)
        {
          throw new ManifoldCFException("Bad pattern syntax in '"+binDescription+"'",e);
        }
      }
    }
  }

  /** Given a bin name, find the max open connections to use for that bin.
  *@return Integer.MAX_VALUE if no limit found.
  */
  @Override
  public int getMaxOpenConnections(String binName)
  {
    // Go through the regexps and match; for each match, find the maximum possible.
    int maxCount = -1;
    for (ThrottleItem ti : patternHash.values())
    {
      Integer limit = ti.getMaxOpenConnections();
      if (limit != null)
      {
        Pattern p = ti.getPattern();
        Matcher m = p.matcher(binName);
        if (m.find())
        {
          if (maxCount == -1 || limit.intValue() > maxCount)
            maxCount = limit.intValue();
        }
      }
    }
    if (maxCount == -1)
      maxCount = Integer.MAX_VALUE;
    else if (maxCount == 0)
      maxCount = 1;
    return maxCount;
  }

  /** Look up minimum milliseconds per byte for a bin.
  *@return 0.0 if no limit found.
  */
  @Override
  public double getMinimumMillisecondsPerByte(String binName)
  {
    // Go through the regexps and match; for each match, find the maximum possible.
    double minMilliseconds = 0.0;
    boolean seenSomething = false;
    for (ThrottleItem ti : patternHash.values())
    {
      Double limit = ti.getMinimumMillisecondsPerByte();
      if (limit != null)
      {
        Pattern p = ti.getPattern();
        Matcher m = p.matcher(binName);
        if (m.find())
        {
          if (seenSomething == false || limit.doubleValue() < minMilliseconds)
          {
            seenSomething = true;
            minMilliseconds = limit.doubleValue();
          }
        }
      }
    }
    return minMilliseconds;
  }

  /** Look up minimum milliseconds for a fetch for a bin.
  *@return 0 if no limit found.
  */
  @Override
  public long getMinimumMillisecondsPerFetch(String binName)
  {
    // Go through the regexps and match; for each match, find the maximum possible.
    long minMilliseconds = 0L;
    boolean seenSomething = false;
    for (ThrottleItem ti : patternHash.values())
    {
      Long limit = ti.getMinimumMillisecondsPerFetch();
      if (limit != null)
      {
        Pattern p = ti.getPattern();
        Matcher m = p.matcher(binName);
        if (m.find())
        {
          if (seenSomething == false || limit.longValue() < minMilliseconds)
          {
            seenSomething = true;
            minMilliseconds = limit.longValue();
          }
        }
      }
    }
    return minMilliseconds;
  }

  /** Class representing an individual throttle item.
  */
  protected static class ThrottleItem
  {
    /** The bin-matching pattern. */
    protected final Pattern pattern;
    /** The minimum milliseconds between bytes, or null if no limit. */
    protected Double minimumMillisecondsPerByte = null;
    /** The minimum milliseconds per fetch, or null if no limit */
    protected Long minimumMillisecondsPerFetch = null;
    /** The maximum open connections, or null if no limit. */
    protected Integer maxOpenConnections = null;

    /** Constructor. */
    public ThrottleItem(Pattern p)
    {
      pattern = p;
    }

    /** Get the pattern. */
    public Pattern getPattern()
    {
      return pattern;
    }

    /** Set minimum milliseconds per byte. */
    public void setMinimumMillisecondsPerByte(Double value)
    {
      minimumMillisecondsPerByte = value;
    }

    /** Get minimum milliseconds per byte. */
    public Double getMinimumMillisecondsPerByte()
    {
      return minimumMillisecondsPerByte;
    }

    /** Set minimum milliseconds per fetch */
    public void setMinimumMillisecondsPerFetch(Long value)
    {
      minimumMillisecondsPerFetch = value;
    }

    /** Get minimum milliseconds per fetch */
    public Long getMinimumMillisecondsPerFetch()
    {
      return minimumMillisecondsPerFetch;
    }

    /** Set maximum open connections. */
    public void setMaxOpenConnections(Integer value)
    {
      maxOpenConnections = value;
    }

    /** Get maximum open connections. */
    public Integer getMaxOpenConnections()
    {
      return maxOpenConnections;
    }
  }

}
