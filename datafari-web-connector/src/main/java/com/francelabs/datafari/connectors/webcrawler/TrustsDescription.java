/* $Id: TrustsDescription.java 988245 2010-08-23 18:39:35Z kwright $ */

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
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import java.util.*;
import java.util.regex.*;

/** This class describes trust information pulled from a configuration.
* The data contained is organized by regular expression performed on a url.  What we store
* for each regular expression is a Pattern, for efficiency.
*
* This structure deals with trusts as applied to a matching set of urls.
*
* Generally it is a good thing to limit the number of regexps that need to be evaluated against
* any given url value as much as possible.  For that reason I've organized this structure
* accordingly.
*/
public class TrustsDescription
{
  public static final String _rcsid = "@(#)$Id: TrustsDescription.java 988245 2010-08-23 18:39:35Z kwright $";

  /** This is the hash that contains everything.  It's keyed by the regexp string itself.
  * Values are TrustsItem objects. */
  protected HashMap patternHash = new HashMap();

  /** Constructor.  Build the description from the ConfigParams. */
  public TrustsDescription(ConfigParams configData)
    throws ManifoldCFException
  {
    // Scan, looking for bin description nodes
    int i = 0;
    while (i < configData.getChildCount())
    {
      ConfigNode node = configData.getChild(i++);
      if (node.getType().equals(WebcrawlerConfig.NODE_TRUST))
      {
        // Get the url regexp
        String urlDescription = node.getAttributeValue(WebcrawlerConfig.ATTR_URLREGEXP);
        String trustEverything = node.getAttributeValue(WebcrawlerConfig.ATTR_TRUSTEVERYTHING);
        try
        {
          Pattern p;
          try
          {
            p = Pattern.compile(urlDescription,Pattern.UNICODE_CASE);
          }
          catch (java.util.regex.PatternSyntaxException e)
          {
            throw new ManifoldCFException("Trust regular expression '"+urlDescription+"' is illegal: "+e.getMessage(),e);
          }
          if (trustEverything != null && trustEverything.equals("true"))
          {
            TrustsItem ti = new TrustsItem(p,null);
            patternHash.put(urlDescription,ti);
          }
          else
          {
            String trustStore = node.getAttributeValue(WebcrawlerConfig.ATTR_TRUSTSTORE);
            TrustsItem ti = new TrustsItem(p,trustStore);
            patternHash.put(urlDescription,ti);
          }
        }
        catch (PatternSyntaxException e)
        {
          throw new ManifoldCFException("Bad pattern syntax in '"+urlDescription+"': "+e.getMessage(),e);
        }
      }
    }
  }

  /** Given a URL, build the right trust certificate store, or return null if all certs should be accepted.
  */
  public IKeystoreManager getTrustStore(String url)
    throws ManifoldCFException
  {
    IKeystoreManager rval = KeystoreManagerFactory.make("");

    int certNumber = 0;
    Iterator iter = patternHash.keySet().iterator();
    while (iter.hasNext())
    {
      String urlDescription = (String)iter.next();
      TrustsItem ti = (TrustsItem)patternHash.get(urlDescription);
      Pattern p = ti.getPattern();
      Matcher m = p.matcher(url);
      if (m.find())
      {
        IKeystoreManager trustStore = ti.getTrustStore();
        if (trustStore == null)
          return null;
        String[] aliases = trustStore.getContents();
        int j = 0;
        while (j < aliases.length)
        {
          rval.addCertificate(Integer.toString(certNumber),trustStore.getCertificate(aliases[j++]));
          certNumber++;
        }
      }
    }
    return rval;
  }

  /** Class representing an individual credential item.
  */
  protected static class TrustsItem
  {
    /** The bin-matching pattern. */
    protected Pattern pattern;
    /** The credential, or null if this is a "trust everything" item */
    protected IKeystoreManager trustStore;

    /** Constructor. */
    public TrustsItem(Pattern p, String trustStoreString)
      throws ManifoldCFException
    {
      pattern = p;
      if (trustStoreString != null)
        trustStore = KeystoreManagerFactory.make("",trustStoreString);
      else
        trustStore = null;
    }

    /** Get the pattern. */
    public Pattern getPattern()
    {
      return pattern;
    }

    /** Get keystore */
    public IKeystoreManager getTrustStore()
    {
      return trustStore;
    }

  }

}
