/* $Id: IThrottledConnection.java 988245 2010-08-23 18:39:35Z kwright $ */

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
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;
import java.io.*;
import java.util.*;

/** This interface represents an established connection to a URL.
*/
public interface IThrottledConnection
{
  public static final String _rcsid = "@(#)$Id: IThrottledConnection.java 988245 2010-08-23 18:39:35Z kwright $";

  // Issue codes.
  public static final int FETCH_NOT_TRIED = -1;
  public static final int FETCH_CIRCULAR_REDIRECT = -100;
  public static final int FETCH_BAD_URI = -101;
  public static final int FETCH_SEQUENCE_ERROR = -102;
  public static final int FETCH_IO_ERROR = -103;
  public static final int FETCH_INTERRUPTED = -104;
  public static final int FETCH_UNKNOWN_ERROR = -999;

  /** Set the abort checker.  This must be done before the connection is actually used.
  */
  public void setAbortChecker(AbortChecker abortCheck);

  /** Check whether the connection has expired.
  *@param currentTime is the current time to use to judge if a connection has expired.
  *@return true if the connection has expired, and should be closed.
  */
  public boolean hasExpired(long currentTime);

  /** Begin the fetch process.
  * @param fetchType is a short descriptive string describing the kind of fetch being requested.  This
  *        is used solely for logging purposes.
  */
  public void beginFetch(String fetchType)
    throws ManifoldCFException, ServiceInterruption;

  /** Execute the fetch and get the return code.  This method uses the
  * standard logging mechanism to keep track of the fetch attempt.  It also
  * signals the following conditions: ServiceInterruption (if a dynamic
  * error occurs), or ManifoldCFException if a fatal error occurs, or nothing if
  * a standard protocol error occurs.
  * Note that, for proxies etc, the idea is for this fetch request to handle whatever
  * redirections are needed to support proxies.
  * @param urlPath is the path part of the url, e.g. "/robots.txt"
  * @param userAgent is the value of the userAgent header to use.
  * @param from is the value of the from header to use.
  * @param redirectOK should be set to true if you want redirects to be automatically followed.
  * @param host is the value to use as the "Host" header, or null to use the default.
  * @param formData describes additional form arguments and how to fetch the page.
  * @param loginCookies describes the cookies that should be in effect for this page fetch.
  */
  public void executeFetch(String urlPath, String userAgent, String from,
    boolean redirectOK, String host, FormData formData,
    LoginCookies loginCookies)
    throws ManifoldCFException, ServiceInterruption;

  /** Get the http response code.
  *@return the response code.  This is either an HTTP response code, or one of the codes above.
  */
  public int getResponseCode()
    throws ManifoldCFException, ServiceInterruption;

  /** Get the last fetch cookies.
  *@return the cookies now in effect from the last fetch.
  */
  public LoginCookies getLastFetchCookies()
    throws ManifoldCFException, ServiceInterruption;

  /** Get response headers
  *@return a map keyed by header name containing a list of values.
  */
  public Map<String,List<String>> getResponseHeaders()
    throws ManifoldCFException, ServiceInterruption;
    
  /** Get a specified response header, if it exists.
  *@param headerName is the name of the header.
  *@return the header value, or null if it doesn't exist.
  */
  public String getResponseHeader(String headerName)
    throws ManifoldCFException, ServiceInterruption;

  /** Get the response input stream.  It is the responsibility of the caller
  * to close this stream when done.
  */
  public InputStream getResponseBodyStream()
    throws ManifoldCFException, ServiceInterruption;

  /** Get limited response as a string.
  */
  public String getLimitedResponseBody(int maxSize, String encoding)
    throws ManifoldCFException, ServiceInterruption;

  /** Note that the connection fetch was interrupted by something.
  */
  public void noteInterrupted(Throwable e);

  /** Done with the fetch.  Call this when the fetch has been completed.  A log entry will be generated
  * describing what was done.
  */
  public void doneFetch(IProcessActivity activities)
    throws ManifoldCFException;

  /** Close the connection.  Call this to return the connection to
  * its pool.
  */
  public void close();
  
  /** Destroy the connection.  Call this to close the connection.
  */
  public void destroy();
  
}
