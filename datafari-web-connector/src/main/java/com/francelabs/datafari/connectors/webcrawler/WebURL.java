/* $Id: WebURL.java 1158475 2011-08-17 00:08:43Z kwright $ */

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

import java.net.URI;
import java.net.URISyntaxException;

/** Replacement class for java.net.URI, which is broken in many ways.
*/
public class WebURL
{
  protected URI theURL;
  protected String rawQueryPart;
  
  public WebURL(String url)
    throws URISyntaxException
  {
    theURL = new URI(url);
    rawQueryPart = null;
  }
  
  public WebURL(String scheme, String host, int port, String path, String queryPart)
    throws URISyntaxException
  {
    theURL = new URI(scheme, null, host, port, path, null, null);
    rawQueryPart = queryPart;
  }
  
  public WebURL(URI theURL)
  {
    this(theURL,null);
  }
  
  public WebURL(URI theURL, String rawQueryPart)
  {
    this.theURL = theURL;
    this.rawQueryPart = rawQueryPart;
  }
  
  public WebURL resolve(String raw)
    throws URISyntaxException
  {
    URI rawURL = new URI(raw);
    if (rawURL.isAbsolute())
      return new WebURL(rawURL);
    URI fixedURL = theURL;
    if (theURL.getPath() == null || theURL.getPath().length() == 0)
      fixedURL = new URI(theURL.getScheme(),null,theURL.getHost(),theURL.getPort(),"/",null,null);

    if (raw.startsWith("?"))
      return new WebURL(fixedURL.getScheme(),fixedURL.getHost(),fixedURL.getPort(),fixedURL.getPath(),rawURL.getRawQuery());
    
    return new WebURL(fixedURL.resolve(rawURL));
  }
  
  public String getPath()
  {
    return theURL.getPath();
  }
  
  public String getHost()
  {
    return theURL.getHost();
  }
  
  public String getScheme()
  {
    return theURL.getScheme();
  }
  
  public int getPort()
  {
    return theURL.getPort();
  }
  
  public String getRawQuery()
  {
    if (rawQueryPart != null)
      return rawQueryPart;
    return theURL.getRawQuery();
  }
  
  public String toASCIIString()
  {
    String rval = theURL.toASCIIString();
    if (rval != null && rawQueryPart != null && rawQueryPart.length() > 0)
      rval += "?" + rawQueryPart;
    return rval;
  }
  
  public String toString()
  {
    String rval = theURL.toString();
    if (rval != null && rawQueryPart != null && rawQueryPart.length() > 0)
      rval += "?" + rawQueryPart;
    return rval;
  }
}
