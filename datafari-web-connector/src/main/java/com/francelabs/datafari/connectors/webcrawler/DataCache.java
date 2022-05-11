/* $Id: DataCache.java 988245 2010-08-23 18:39:35Z kwright $ */

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
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.system.ManifoldCF;
import java.util.*;
import java.io.*;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.HttpException;

/** This class is a cache of a specific URL's data.  It's fetched early and kept,
* so that (1) an accurate data length can be found, and (2) we can compute a version
* checksum.
*/
public class DataCache
{
  public static final String _rcsid = "@(#)$Id: DataCache.java 988245 2010-08-23 18:39:35Z kwright $";

  // Hashmap containing the cache of files.
  // This is keyed by document identifier, and contains DocumentData objects.
  protected Map<String,DocumentData> cacheData = new HashMap<String,DocumentData>();

  /** Constructor.
  */
  public DataCache()
  {
  }


  /** Add a data entry into the cache.
  * This method is called whenever the data from a fetch is considered interesting or useful, and will
  * be thus passed on from getDocumentVersions() to the processDocuments() phase.  At the moment that's
  * usually a 200 or a 302 response.
  *@param documentIdentifier is the document identifier (url).
  *@param connection is the connection, upon which a fetch has been done that needs to be
  * cached.
  *@return a "checksum" value, to use as a version string.
  */
  public String addData(IProcessActivity activities, String documentIdentifier, IThrottledConnection connection)
    throws ManifoldCFException, ServiceInterruption
  {
    // Grab the response code, and the content-type header
    int responseCode = connection.getResponseCode();
    String contentType = connection.getResponseHeader("Content-Type");
    String referralURI = connection.getResponseHeader("Location");

    // Create a temporary file; that's what we will cache
    try
    {
      // First, get the stream.
      InputStream dataStream = connection.getResponseBodyStream();
      if (dataStream == null)
        return null;
      try
      {
        File tempFile = File.createTempFile("_webcache_","tmp");
        try
        {
          // Causes memory leaks if left around; there's no way to release
          // the record specifying that the file should be deleted, even
          // after it's removed.  So disable this and live with the occasional
          // dangling file left as a result of shutdown or error. :-(
          // tempFile.deleteOnExit();
          ManifoldCF.addFile(tempFile);

          // Transfer data to temporary file
          long checkSum = 0L;
          OutputStream os = new FileOutputStream(tempFile);
          try
          {
            byte[] byteArray = new byte[65536];
            while (true)
            {
              int amt;
              try
              {
                amt = dataStream.read(byteArray,0,byteArray.length);
              }
              catch (java.net.SocketTimeoutException e)
              {
                Logging.connectors.warn("Socket timeout exception reading socket stream: "+e.getMessage(),e);
                long currentTime = System.currentTimeMillis();
                throw new ServiceInterruption("Socket timeout: "+e.getMessage(),e,currentTime + 300000L,
                  currentTime + 12 * 60 * 60000L,-1,false);
              }
              catch (ConnectTimeoutException e)
              {
                Logging.connectors.warn("Socket connect timeout exception reading socket stream: "+e.getMessage(),e);
                long currentTime = System.currentTimeMillis();
                throw new ServiceInterruption("Socket timeout: "+e.getMessage(),e,currentTime + 300000L,
                  currentTime + 12 * 60 * 60000L,-1,false);
              }
              catch (InterruptedIOException e)
              {
                //Logging.connectors.warn("IO interruption seen",e);
                throw new ManifoldCFException("Interrupted: "+e.getMessage(),ManifoldCFException.INTERRUPTED);
              }
              catch (IOException e)
              {
                Logging.connectors.warn("IO exception reading socket stream: "+e.getMessage(),e);
                long currentTime = System.currentTimeMillis();
                throw new ServiceInterruption("Read timeout: "+e.getMessage(),e,currentTime + 300000L,
                  currentTime + 12 * 60 * 60000L,-1,false);
              }
              if (amt == -1)
                break;
              int i = 0;
              while (i < amt)
              {
                byte x = byteArray[i++];
                long bytevalue = (long)x;
                checkSum = (checkSum << 5) ^ (checkSum >> 3) ^ (bytevalue << 2) ^ (bytevalue >> 3);
              }

              os.write(byteArray,0,amt);
              // Check if job is alive before looping
              activities.checkJobStillActive();
            }
          }
          finally
          {
            os.close();
          }

          synchronized(this)
          {
            deleteData(documentIdentifier);
            cacheData.put(documentIdentifier,new DocumentData(tempFile,responseCode,contentType,referralURI));
            return new Long(checkSum).toString();
          }

        }
        catch (IOException e)
        {
          ManifoldCF.deleteFile(tempFile);
          throw e;
        }
        catch (ManifoldCFException e)
        {
          ManifoldCF.deleteFile(tempFile);
          throw e;
        }
        catch (ServiceInterruption e)
        {
          ManifoldCF.deleteFile(tempFile);
          throw e;
        }
        catch (Error e)
        {
          ManifoldCF.deleteFile(tempFile);
          throw e;
        }
      }
      finally
      {
        try
        {
          dataStream.close();
        }
        catch (java.net.SocketTimeoutException e)
        {
          Logging.connectors.warn("WEB: Socket timeout exception closing data stream, ignoring: "+e.getMessage(),e);
        }
        catch (ConnectTimeoutException e)
        {
          Logging.connectors.warn("WEB: Socket connect timeout exception closing data stream, ignoring: "+e.getMessage(),e);
        }
        catch (InterruptedIOException e)
        {
          throw e;
        }
        catch (IOException e)
        {
          // We can get this if the socket was unexpectedly closed by the server; treat this
          // as a Service Interruption.  Generally, this is ok - warn but don't do anything else.
          Logging.connectors.warn("WEB: IO exception closing data stream, ignoring: "+e.getMessage(),e);
        }
      }
    }
    catch (java.net.SocketTimeoutException e)
    {
      throw new ManifoldCFException("Socket timeout exception creating temporary file: "+e.getMessage(),e);
    }
    catch (ConnectTimeoutException e)
    {
      throw new ManifoldCFException("Socket connect timeout exception creating temporary file: "+e.getMessage(),e);
    }
    catch (InterruptedIOException e)
    {
      //Logging.connectors.warn("IO interruption seen",e);
      throw new ManifoldCFException("Interrupted: "+e.getMessage(),ManifoldCFException.INTERRUPTED);
    }
    catch (IOException e)
    {
      throw new ManifoldCFException("IO exception creating temporary file: "+e.getMessage(),e);
    }
  }

  /** Get the response code.
  *@param documentIdentifier is the document identifier.
  *@return the code.
  */
  public synchronized int getResponseCode(String documentIdentifier)
  {
    DocumentData dd = cacheData.get(documentIdentifier);
    if (dd == null)
      return IThrottledConnection.FETCH_NOT_TRIED;
    return dd.getResponseCode();
  }

  /** Get the content type.
  *@param documentIdentifier is the document identifier.
  *@return the content type, or null if there is none.
  */
  public synchronized String getContentType(String documentIdentifier)
  {
    DocumentData dd = cacheData.get(documentIdentifier);
    if (dd == null)
      return null;
    return dd.getContentType();
  }

  /** Get the referral URI.
  *@param documentIdentifier is the document identifier.
  *@return the referral URI, or null if none.
  */
  public synchronized String getReferralURI(String documentIdentifier)
  {
    DocumentData dd = cacheData.get(documentIdentifier);
    if (dd == null)
      return null;
    return dd.getReferralURI();
  }

  /** Fetch binary data length.
  *@param documentIdentifier is the document identifier.
  *@return the length.
  */
  public synchronized long getDataLength(String documentIdentifier)
  {
    DocumentData dd = cacheData.get(documentIdentifier);
    if (dd == null)
      return 0L;
    return dd.getData().length();
  }

  /** Fetch binary data entry from the cache.
  *@param documentIdentifier is the document identifier (url).
  *@return a binary data stream.
  */
  public synchronized InputStream getData(String documentIdentifier)
    throws ManifoldCFException
  {
    DocumentData dd = cacheData.get(documentIdentifier);
    if (dd == null)
      return null;
    try
    {
      return new FileInputStream(dd.getData());
    }
    catch (FileNotFoundException e)
    {
      throw new ManifoldCFException("File not found exception opening data: "+e.getMessage(),e);
    }
  }

  /** Delete specified item of data.
  *@param documentIdentifier is the document identifier (url).
  */
  public synchronized void deleteData(String documentIdentifier)
  {
    DocumentData dd = cacheData.remove(documentIdentifier);
    if (dd != null)
    {
      ManifoldCF.deleteFile(dd.getData());
    }
  }

  // Protected classes

  /** This class represents everything we need to know about a document that's getting passed from the
  * getDocumentVersions() phase to the processDocuments() phase.
  */
  protected static class DocumentData
  {
    /** The cache file for the data */
    protected File data;
    /** The response code */
    protected int responseCode;
    /** The content-type header value */
    protected String contentType;
    /** The referral URI */
    protected String referralURI;

    // More will probably go here later, but I can't think of much else at the moment.

    /** Constructor. */
    public DocumentData(File data, int responseCode, String contentType, String referralURI)
    {
      this.data = data;
      this.responseCode = responseCode;
      this.contentType = contentType;
      this.referralURI = referralURI;
    }

    /** Get the data */
    public File getData()
    {
      return data;
    }

    /** Get the response code */
    public int getResponseCode()
    {
      return responseCode;
    }

    /** Get the contentType */
    public String getContentType()
    {
      return contentType;
    }

    /** Get the referral URI */
    public String getReferralURI()
    {
      return referralURI;
    }

  }

}
