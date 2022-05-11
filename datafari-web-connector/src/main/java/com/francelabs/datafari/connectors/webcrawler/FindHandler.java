/* $Id: FindHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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

/** This class is used to discover links in a session login context */
public class FindHandler implements IDiscoveredLinkHandler
{
  protected String parentURI;
  protected String targetURI = null;

  public FindHandler(String parentURI)
  {
    this.parentURI = parentURI;
  }

  @Override
  public void noteDiscoveredBase(String rawURL)
    throws ManifoldCFException
  {
    // MHL
  }
  
  /** Inform the world of a discovered link.
  *@param rawURL is the raw discovered url.  This may be relative, malformed, or otherwise unsuitable for use until final form is acheived.
  */
  @Override
  public void noteDiscoveredLink(String rawURL)
    throws ManifoldCFException
  {
    // Build a complete url, but don't filter or anything
    try
    {
      java.net.URI url;
      if (parentURI != null)
      {
        java.net.URI parentURL = new java.net.URI(parentURI);
        url = parentURL.resolve(rawURL);
      }
      else
        url = new java.net.URI(rawURL);

      String protocol = url.getScheme();
      String host = url.getHost();

      // The new URL better darn well have a host and a protocol, and we only know how to deal with
      // http and https.
      if (protocol == null || host == null)
      {
        return;
      }
      if (!WebcrawlerConnector.understoodProtocols.contains(protocol))
      {
        return;
      }

      String id = url.toASCIIString();
      if (id == null)
        return;

      // As a last basic legality check, go through looking for illegal characters.
      int i = 0;
      while (i < id.length())
      {
        char x = id.charAt(i++);
        // Only 7-bit ascii is allowed in URLs - and that has limits too (no control characters)
        if (x < ' ' || x > 127)
        {
          return;
        }
      }

      // Set the target.
      targetURI = id;
    }
    catch (java.net.URISyntaxException e)
    {
      return;
    }
    catch (java.lang.IllegalArgumentException e)
    {
      return;
    }
    catch (java.lang.NullPointerException e)
    {
      // This gets tossed by url.toAsciiString() for reasons I don't understand, but which have to do with a malformed URL.
      return;
    }
  }

  public String getTargetURI()
  {
    return targetURI;
  }
}
