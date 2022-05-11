/* $Id: ThrottledFetcher.java 989847 2010-08-26 17:52:30Z kwright $ */

/**`
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
import org.apache.manifoldcf.connectorcommon.common.DeflateInputStream;
import org.apache.manifoldcf.connectorcommon.common.XThreadInputStream;
import org.apache.manifoldcf.connectorcommon.common.InterruptibleSocketFactory;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.core.util.URLEncoder;
import org.apache.manifoldcf.crawler.interfaces.*;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.crawler.system.ManifoldCF;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.config.SocketConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpStatus;
import org.apache.http.HttpHost;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeader;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.impl.cookie.LaxBrowserCompatSpec;
import org.apache.http.impl.cookie.RFC6265CookieSpecProvider;
import org.apache.http.impl.cookie.BasicPathHandler;
import org.apache.http.impl.cookie.LaxMaxAgeHandler;
import org.apache.http.impl.cookie.BasicDomainHandler;
import org.apache.http.impl.cookie.BasicSecureHandler;
import org.apache.http.impl.cookie.LaxExpiresHandler;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.client.CookieStore;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.CookieIdentityComparator;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.Registry;
import org.apache.http.client.config.CookieSpecs;

import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.HttpException;

/** This class uses httpclient to fetch stuff from webservers.  However, it additionally controls the fetch
* rate in two ways: first, controlling the overall bandwidth used per server, and second, limiting the number
* of simultaneous open connections per server.
* An instance of this class would very probably need to have a lifetime consistent with the long-term nature
* of these values, and be static.
*/
public class ThrottledFetcher
{
  public static final String _rcsid = "@(#)$Id: ThrottledFetcher.java 989847 2010-08-26 17:52:30Z kwright $";

  /** Web throttle group type */
  protected static final String webThrottleGroupType = "_WEB_";
  
  /** Idle timeout */
  protected static final long idleTimeout = 300000L;
  
  /** This flag determines whether we record everything to the disk, as a means of doing a web snapshot */
  protected static final boolean recordEverything = false;

  protected static final long TIME_2HRS = 7200000L;
  protected static final long TIME_5MIN = 300000L;
  protected static final long TIME_15MIN = 1500000L;
  protected static final long TIME_6HRS = 6L * 60L * 60000L;
  protected static final long TIME_1DAY = 24L * 60L * 60000L;

  /** The read chunk length */
  protected static final int READ_CHUNK_LENGTH = 4096;

  /** Connection pools.
  /* This is a static hash of the connection pools in existence.  Each connection pool represents a set of identical connections. */
  protected final static Map<ConnectionPoolKey,ConnectionPool> connectionPools = new HashMap<ConnectionPoolKey,ConnectionPool>();
  
  /** Current host name */
  private static String currentHost = null;
  static
  {
    // Find the current host name
    try
    {
      java.net.InetAddress addr = java.net.InetAddress.getLocalHost();

      // Get hostname
      currentHost = addr.getHostName();
    }
    catch (java.net.UnknownHostException e)
    {
    }
  }

  private static final Registry<CookieSpecProvider> cookieSpecRegistry =
    RegistryBuilder.<CookieSpecProvider>create()
      .register(CookieSpecs.STANDARD, new LaxBrowserCompatSpecProvider())
      .build();

  /** Constructor.  Private since we never instantiate.
  */
  private ThrottledFetcher()
  {
  }


  /** Obtain a connection to specified protocol, server, and port.  We use the protocol because the
  * setup for some protocols is extensive (e.g. https) and hopefully would not need to be repeated if
  * we distinguish connections based on that.
  *@param protocol is the protocol, e.g. "http"
  *@param server is the server IP address, e.g. "10.32.65.1"
  *@param port is the port to connect to, e.g. 80.  Pass -1 if the default port for the protocol is desired.
  *@param authentication is the page credentials object to use for the fetch.  If null, no credentials are available.
  *@param trustStore is the current trust store in effect for the fetch.
  *@param binNames is the set of bins, in order, that should be used for throttling this connection.
  *       Note that the bin names for a given IP address and port MUST be the same for every connection!
  *       This must be enforced by whatever it is that builds the bins - it must do so given an IP and port.
  *@param throttleDescription is the description of all the throttling that should take place.
  *@param connectionLimit isthe maximum number of connections permitted.
  *@return an IThrottledConnection object that can be used to fetch from the port.
  */
  public static IThrottledConnection getConnection(IThreadContext threadContext, String throttleGroupName,
    String protocol, String server, int port,
    PageCredentials authentication,
    IKeystoreManager trustStore,
    IThrottleSpec throttleDescription, String[] binNames,
    int connectionLimit,
    String proxyHost, int proxyPort, String proxyAuthDomain, String proxyAuthUsername, String proxyAuthPassword,
    int socketTimeoutMilliseconds, int connectionTimeoutMilliseconds,
    IAbortActivity activities)
    throws ManifoldCFException, ServiceInterruption
  {
    // Get a throttle groups handle
    IThrottleGroups throttleGroups = ThrottleGroupsFactory.make(threadContext);
    
    // Create the appropruate throttle group, or update the throttle description for an existing one
    throttleGroups.createOrUpdateThrottleGroup(webThrottleGroupType,throttleGroupName,throttleDescription);
    
    // Create the https scheme and trust store string for this connection
    javax.net.ssl.SSLSocketFactory baseFactory;
    String trustStoreString;
    if (trustStore != null)
    {
      baseFactory = trustStore.getSecureSocketFactory();
      trustStoreString = trustStore.getHashString();
    }
    else
    {
      baseFactory = KeystoreManagerFactory.getTrustingSecureSocketFactory();
      trustStoreString = null;
    }

    // Construct a connection pool key
    ConnectionPoolKey poolKey = new ConnectionPoolKey(protocol,server,port,authentication,
      trustStoreString,proxyHost,proxyPort,proxyAuthDomain,proxyAuthUsername,proxyAuthPassword,
      socketTimeoutMilliseconds,connectionTimeoutMilliseconds);
    
    ConnectionPool p;
    synchronized (connectionPools)
    {
      p = connectionPools.get(poolKey);
      if (p == null)
      {
        // Construct a new IConnectionThrottler.
        IConnectionThrottler connectionThrottler =
          throttleGroups.obtainConnectionThrottler(webThrottleGroupType,throttleGroupName,binNames);
        p = new ConnectionPool(connectionThrottler,protocol,server,port,authentication,baseFactory,
          proxyHost,proxyPort,proxyAuthDomain,proxyAuthUsername,proxyAuthPassword,
          socketTimeoutMilliseconds,connectionTimeoutMilliseconds);
        connectionPools.put(poolKey,p);
      }
    }
    
    return p.grab(activities);
  }

  /** Flush connections that have timed out from inactivity. */
  public static void flushIdleConnections(IThreadContext threadContext)
    throws ManifoldCFException
  {
    // Go through outstanding connection pools and clean them up.
    synchronized (connectionPools)
    {
      for (ConnectionPool pool : connectionPools.values())
      {
        pool.flushIdleConnections();
      }
    }
  }

  /** Throttled connections.  Each instance of a connection describes the bins to which it belongs,
  * along with the actual open connection itself, and the last time the connection was used. */
  protected static class ThrottledConnection implements IThrottledConnection
  {
    /** Connection pool */
    protected final ConnectionPool myPool;
    /** Fetch throttler */
    protected final IFetchThrottler fetchThrottler;
    /** Protocol */
    protected final String protocol;
    /** Server */
    protected final String server;
    /** Port */
    protected final int port;
    /** Authentication */
    protected final PageCredentials authentication;

    /** This is when the connection will expire.  Only valid if connection is in the pool. */
    protected long expireTime = -1L;

    /** The http connection manager.  The pool is of size 1.  */
    protected HttpClientConnectionManager connManager = null;
    /** The http client object. */
    protected HttpClient httpClient = null;
    /** The method object */
    protected HttpRequestBase fetchMethod = null;
    /** The error trace, if any */
    protected Throwable throwable = null;
    /** The current URL being fetched */
    protected String myUrl = null;
    /** The status code fetched, if any */
    protected int statusCode = FETCH_NOT_TRIED;
    /** The kind of fetch we are doing */
    protected String fetchType = null;
    /** The current bytes in the current fetch */
    protected long fetchCounter = 0L;
    /** The start of the current fetch */
    protected long startFetchTime = -1L;
    /** The cookies from the last fetch */
    protected LoginCookies lastFetchCookies = null;
    /** Proxy host */
    protected final String proxyHost;
    /** Proxy port */
    protected final int proxyPort;
    /** Proxy auth domain */
    protected final String proxyAuthDomain;
    /** Proxy auth user name */
    protected final String proxyAuthUsername;
    /** Proxy auth password */
    protected final String proxyAuthPassword;
    /** Https protocol */
    protected final javax.net.ssl.SSLSocketFactory httpsSocketFactory;
    /** Socket timeout milliseconds */
    protected final int socketTimeoutMilliseconds;
    /** Connection timeout milliseconds */
    protected final int connectionTimeoutMilliseconds;

    /** The thread that is actually doing the work */
    protected ExecuteMethodThread methodThread = null;
    /** Set if thread has been started */
    protected boolean threadStarted = false;
    
    /** Abort checker */
    protected AbortChecker abortCheck = null;
    
    /** Constructor.  Create a connection with a specific server and port, and
    * register it as active against all bins. */
    public ThrottledConnection(ConnectionPool myPool, IFetchThrottler fetchThrottler,
      String protocol, String server, int port, PageCredentials authentication,
      javax.net.ssl.SSLSocketFactory httpsSocketFactory,
      String proxyHost, int proxyPort, String proxyAuthDomain, String proxyAuthUsername, String proxyAuthPassword,
      int socketTimeoutMilliseconds, int connectionTimeoutMilliseconds)
    {
      this.myPool = myPool;
      this.fetchThrottler = fetchThrottler;
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
      this.proxyAuthDomain = proxyAuthDomain;
      this.proxyAuthUsername = proxyAuthUsername;
      this.proxyAuthPassword = proxyAuthPassword;
      this.protocol = protocol;
      this.server = server;
      this.port = port;
      this.authentication = authentication;
      this.httpsSocketFactory = httpsSocketFactory;
      this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
      this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
    }

    /** Set the abort checker.  This must be done before the connection is actually used.
    */
    @Override
    public void setAbortChecker(AbortChecker abortCheck)
    {
      this.abortCheck = abortCheck;
    }
    
    /** Check whether the connection has expired.
    *@param currentTime is the current time to use to judge if a connection has expired.
    *@return true if the connection has expired, and should be closed.
    */
    @Override
    public boolean hasExpired(long currentTime)
    {
      if (connManager != null)
      {
        connManager.closeIdleConnections(idleTimeout, TimeUnit.MILLISECONDS);
        connManager.closeExpiredConnections();
      }
      return (currentTime > expireTime);
    }

    /** Log the fetch of a number of bytes, from within a stream. */
    public void logFetchCount(int count)
    {
      fetchCounter += (long)count;
    }

    /** Destroy the connection forever */
    @Override
    public void destroy()
    {
      // Kill the actual connection object.
      if (connManager != null)
      {
        connManager.shutdown();
        connManager = null;
      }

    }


    /** Begin the fetch process.
    * @param fetchType is a short descriptive string describing the kind of fetch being requested.  This
    *        is used solely for logging purposes.
    */
    @Override
    public void beginFetch(String fetchType)
      throws ManifoldCFException, ServiceInterruption
    {
      this.fetchType = fetchType;
      this.fetchCounter = 0L;
      try
      {
        if (fetchThrottler.obtainFetchDocumentPermission(abortCheck) == false)
          throw new IllegalStateException("Unexpected return value from obtainFetchDocumentPermission()");
      }
      catch (InterruptedException e)
      {
        throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
      }
      catch (BreakException e)
      {
        abortCheck.rethrowExceptions();
      }
    }

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
    @Override
    public void executeFetch(String urlPath, String userAgent, String from,
      boolean redirectOK, String host, FormData formData,
      LoginCookies loginCookies)
      throws ManifoldCFException, ServiceInterruption
    {
      // Set up scheme
      SSLConnectionSocketFactory myFactory = new SSLConnectionSocketFactory(new InterruptibleSocketFactory(httpsSocketFactory,connectionTimeoutMilliseconds),
        NoopHostnameVerifier.INSTANCE);

      int hostPort;
      String displayedPort;
      if (port != -1)
      {
        if (!(protocol.equals("http") && port == 80) &&
          !(protocol.equals("https") && port == 443))
        {
          displayedPort = ":"+Integer.toString(port);
          hostPort = port;
        }
        else
        {
          displayedPort = "";
          hostPort = -1;
        }
      }
      else
      {
        displayedPort = "";
        hostPort = -1;
      }

      StringBuilder sb = new StringBuilder(protocol);
      sb.append("://").append(server).append(displayedPort).append(urlPath);
      String fetchUrl = sb.toString();

      HttpHost fetchHost = new HttpHost(server,hostPort,protocol);
      HttpHost hostHost;
      
      if (host != null)
      {
        sb.setLength(0);
        sb.append(protocol).append("://").append(host).append(displayedPort).append(urlPath);
        myUrl = sb.toString();
        hostHost = new HttpHost(host,hostPort,protocol);
      }
      else
      {
        myUrl = fetchUrl;
        hostHost = fetchHost;
      }
      
      if (connManager == null)
      {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
          .register("http", PlainConnectionSocketFactory.getSocketFactory())
          .register("https", myFactory)
          .build());
        poolingConnManager.setDefaultMaxPerRoute(1);
        poolingConnManager.setValidateAfterInactivity(2000);
        poolingConnManager.setDefaultSocketConfig(SocketConfig.custom()
          .setTcpNoDelay(true)
          .setSoTimeout(socketTimeoutMilliseconds)
          .build());
        connManager = poolingConnManager;
      }
      
      long startTime = 0L;
      if (Logging.connectors.isDebugEnabled())
      {
        startTime = System.currentTimeMillis();
        Logging.connectors.debug("WEB: Waiting for an HttpClient object");
      }

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      // Set up authentication to use
      if (authentication != null)
      {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: For "+myUrl+", discovered matching authentication credentials");
        credentialsProvider.setCredentials(AuthScope.ANY,
          authentication.makeCredentialsObject(host));
      }

      RequestConfig.Builder requestBuilder = RequestConfig.custom()
        .setCircularRedirectsAllowed(true)
        .setSocketTimeout(socketTimeoutMilliseconds)
        .setExpectContinueEnabled(true)
        .setConnectTimeout(connectionTimeoutMilliseconds)
        .setConnectionRequestTimeout(socketTimeoutMilliseconds)
        .setCookieSpec(CookieSpecs.STANDARD)
        .setRedirectsEnabled(redirectOK);

      // If there's a proxy, set that too.
      if (proxyHost != null && proxyHost.length() > 0)
      {
        // Configure proxy authentication
        if (proxyAuthUsername != null && proxyAuthUsername.length() > 0)
        {
          credentialsProvider.setCredentials(
            new AuthScope(proxyHost, proxyPort),
            new NTCredentials(proxyAuthUsername, (proxyAuthPassword==null)?"":proxyAuthPassword, currentHost, (proxyAuthDomain==null)?"":proxyAuthDomain));
        }

        HttpHost proxy = new HttpHost(proxyHost, proxyPort);

        requestBuilder.setProxy(proxy);
      }


      httpClient = HttpClients.custom()
        .setConnectionManager(connManager)
        .disableAutomaticRetries()
        .setDefaultRequestConfig(requestBuilder.build())
        .setDefaultCredentialsProvider(credentialsProvider)
        .setRequestExecutor(new HttpRequestExecutor(socketTimeoutMilliseconds))
        .setRedirectStrategy(new LaxRedirectStrategy())
        .build();

        /*
        BasicHttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.DEFAULT_HOST,fetchHost);
        params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY,true);
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK,true);
        params.setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS,true);
        // MEDIUM_SECURITY compatibility level not supported in HttpComponents.  Try BROWSER_NETSCAPE?
        HttpClientParams.setCookiePolicy(params,CookiePolicy.BROWSER_COMPATIBILITY);
        params.setBooleanParameter(CookieSpecPNames.SINGLE_COOKIE_HEADER,new Boolean(true));

        DefaultHttpClient localHttpClient = new DefaultHttpClient(connManager,params);
        // No retries
        localHttpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler()
          {
            public boolean retryRequest(
              IOException exception,
              int executionCount,
              HttpContext context)
            {
              return false;
            }
         
          });
        localHttpClient.setRedirectStrategy(new LaxRedirectStrategy());
        localHttpClient.getCookieSpecs().register(CookiePolicy.BROWSER_COMPATIBILITY, new CookieSpecFactory()
          {

            public CookieSpec newInstance(HttpParams params)
            {
              return new LaxBrowserCompatSpec();
            }
    
          }
        );


          
        httpClient = localHttpClient;
        */


      // Set the parameters we haven't keyed on (so these can change from request to request)

      if (host != null)
      {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: For "+myUrl+", setting virtual host to "+host);
      }


      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("WEB: Got an HttpClient object after "+new Long(System.currentTimeMillis()-startTime).toString()+" ms.");

      startFetchTime = System.currentTimeMillis();

      int pageFetchMethod = FormData.SUBMITMETHOD_GET;
      if (formData != null)
        pageFetchMethod = formData.getSubmitMethod();
      switch (pageFetchMethod)
      {
      case FormData.SUBMITMETHOD_GET:
        // MUST be just the path, or apparently we wind up resetting the HostConfiguration
        // Add additional parameters to url path
        String fullUrlPath;
        if (formData != null)
        {
          StringBuilder psb = new StringBuilder(urlPath);
          Iterator iter = formData.getElementIterator();
          char appendChar;
          if (urlPath.indexOf("?") == -1)
            appendChar = '?';
          else
            appendChar = '&';

            while (iter.hasNext())
            {
              FormDataElement el = (FormDataElement)iter.next();
              psb.append(appendChar);
              appendChar = '&';
              String param = el.getElementName();
              String value = el.getElementValue();
              psb.append(URLEncoder.encode(param));
              if (value != null)
              {
                psb.append('=').append(URLEncoder.encode(value));
              }
            }


          fullUrlPath = psb.toString();
        }
        else
        {
          fullUrlPath = urlPath;
        }
        // Hack; apparently httpclient treats // as a protocol specifier and so it rips off the first section of the path in that case.
        while (fullUrlPath.startsWith("//"))
          fullUrlPath = fullUrlPath.substring(1);
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Get method for '"+fullUrlPath+"'");
        fetchMethod = new HttpGet(fullUrlPath);
        break;
      case FormData.SUBMITMETHOD_POST:
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Post method for '"+urlPath+"'");
        // MUST be just the path, or apparently we wind up resetting the HostConfiguration
        HttpPost postMethod = new HttpPost(urlPath);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        // Add parameters to post variables
        if (formData != null)
        {
          Iterator iter = formData.getElementIterator();
          while (iter.hasNext())
          {
            FormDataElement e = (FormDataElement)iter.next();
            String param = e.getElementName();
            String value = e.getElementValue();
            if (Logging.connectors.isDebugEnabled())
              Logging.connectors.debug("WEB: Post parameter name '"+param+"' value '"+value+"' for '"+urlPath+"'");
            nvps.add(new BasicNameValuePair(param,value));
          }
        }
        postMethod.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
        fetchMethod = postMethod;
        break;
      default:
        throw new ManifoldCFException("Illegal method type: "+Integer.toString(pageFetchMethod));
      }

      // Set all appropriate headers and parameters
      fetchMethod.setHeader(new BasicHeader("User-Agent",userAgent));
      fetchMethod.setHeader(new BasicHeader("From",from));
      fetchMethod.setHeader(new BasicHeader("Accept","*/*"));
      fetchMethod.setHeader(new BasicHeader("Accept-Encoding","gzip,deflate"));

      // Use a custom cookie store
      CookieStore cookieStore = new OurBasicCookieStore();
      // If we have any cookies to set, set them.
      if (loginCookies != null)
      {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("WEB: Adding "+Integer.toString(loginCookies.getCookieCount())+" cookies for '"+urlPath+"'");
        int h = 0;
        while (h < loginCookies.getCookieCount())
        {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB:  Cookie '"+loginCookies.getCookie(h)+"' added");
          cookieStore.addCookie(loginCookies.getCookie(h++));
        }
      }


      // Copy out the current cookies, in case the fetch fails
      lastFetchCookies = loginCookies;

      // Create the thread
      methodThread = new ExecuteMethodThread(this, fetchThrottler, httpClient, hostHost, fetchMethod, cookieStore);
      try
      {
        methodThread.start();
        threadStarted = true;
        try
        {
          statusCode = methodThread.getResponseCode();
          lastFetchCookies = methodThread.getCookies();
          switch (statusCode)
          {
          case HttpStatus.SC_REQUEST_TIMEOUT:
          case HttpStatus.SC_GATEWAY_TIMEOUT:
          case HttpStatus.SC_SERVICE_UNAVAILABLE:
            // Temporary service interruption
            // May want to make the retry time a parameter someday
            long currentTime = System.currentTimeMillis();
            throw new ServiceInterruption("Http response temporary error on '"+myUrl+"': "+Integer.toString(statusCode),new ManifoldCFException("Service unavailable (code "+Integer.toString(statusCode)+")"),
              currentTime + TIME_2HRS, currentTime + TIME_1DAY, -1, false);
          case HttpStatus.SC_UNAUTHORIZED:
          case HttpStatus.SC_USE_PROXY:
          case HttpStatus.SC_OK:
          case HttpStatus.SC_GONE:
          case HttpStatus.SC_NOT_FOUND:
          case HttpStatus.SC_BAD_GATEWAY:
          case HttpStatus.SC_BAD_REQUEST:
          case HttpStatus.SC_FORBIDDEN:
          case HttpStatus.SC_INTERNAL_SERVER_ERROR:
          default:
            return;
          }
        }
        catch (InterruptedException e)
        {
          methodThread.interrupt();
          methodThread = null;
          threadStarted = false;
          throw e;
        }

      }
      catch (InterruptedException e)
      {
        // Drop the current connection on the floor, so it cannot be reused.
        fetchMethod = null;
        throwable = new ManifoldCFException("Interrupted: "+e.getMessage(),e);
        statusCode = FETCH_INTERRUPTED;
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (java.net.SocketTimeoutException e)
      {
        throwable = e;
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out waiting for IO for '"+myUrl+"': "+e.getMessage(), e, currentTime + TIME_5MIN,
          currentTime + TIME_2HRS,-1,false);
      }
      catch (ConnectTimeoutException e)
      {
        throwable = e;
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out waiting for connection for '"+myUrl+"': "+e.getMessage(), e, currentTime + TIME_5MIN,
          currentTime + TIME_2HRS,-1,false);
      }
      catch (InterruptedIOException e)
      {
        //Logging.connectors.warn("IO interruption seen",e);
        throwable = new ManifoldCFException("Interrupted: "+e.getMessage(),e);
        statusCode = FETCH_INTERRUPTED;
        throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
      }
      catch (RedirectException e)
      {
        throwable = e;
        statusCode = FETCH_CIRCULAR_REDIRECT;
        return;
      }
      catch (NoHttpResponseException e)
      {
        throwable = e;
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out waiting for response for '"+myUrl+"': "+e.getMessage(), e, currentTime + TIME_15MIN,
          currentTime + TIME_2HRS,-1,false);
      }
      catch (java.net.ConnectException e)
      {
        throwable = e;
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out waiting for a connection for '"+myUrl+"': "+e.getMessage(), e, currentTime + TIME_2HRS,
          currentTime + TIME_6HRS,-1,false);
      }
      catch (javax.net.ssl.SSLException e)
      {
        // Probably this is an incorrectly configured trust store
        throwable = new ManifoldCFException("SSL handshake error: "+e.getMessage()+"; check your connection's Certificate configuration",e);
        statusCode = FETCH_IO_ERROR;
        return;
      }
      catch (IOException e)
      {
        // Treat this as a bad url.  We don't know what happened, but it isn't something we are going to naively
        // retry on.
        throwable = e;
        statusCode = FETCH_IO_ERROR;
        return;
      }
      catch (Throwable e)
      {
        Logging.connectors.debug("WEB: Caught an unexpected exception: "+e.getMessage(),e);
        throwable = e;
        statusCode = FETCH_UNKNOWN_ERROR;
        return;
      }

    }

    /** Get the http response code.
    *@return the response code.  This is either an HTTP response code, or one of the codes above.
    */
    @Override
    public int getResponseCode()
      throws ManifoldCFException, ServiceInterruption
    {
      return statusCode;
    }

    /** Get the last fetch cookies.
    *@return the cookies now in effect from the last fetch.
    */
    @Override
    public LoginCookies getLastFetchCookies()
      throws ManifoldCFException, ServiceInterruption
    {
      if (Logging.connectors.isDebugEnabled())
      {
        Logging.connectors.debug("WEB: Retrieving cookies...");
        for (int i = 0; i < lastFetchCookies.getCookieCount(); i++)
        {
          Logging.connectors.debug("WEB:   Cookie '"+lastFetchCookies.getCookie(i)+"'");
        }
      }
      return lastFetchCookies;
    }

    /** Get response headers
    *@return a map keyed by header name containing a list of values.
    */
    @Override
    public Map<String,List<String>> getResponseHeaders()
      throws ManifoldCFException, ServiceInterruption
    {
      if (fetchMethod == null)
        throw new ManifoldCFException("Attempt to get headers when there is no method");
      if (methodThread == null || threadStarted == false)
        throw new ManifoldCFException("Attempt to get headers when no method thread");
      try
      {
        return methodThread.getResponseHeaders();
      }
      catch (InterruptedException e)
      {
        methodThread.interrupt();
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (HttpException e)
      {
        handleHTTPException(e,"reading headers");
      }
      catch (IOException e)
      {
        handleIOException(e,"reading headers");
      }
      return null;
    }

    /** Get a specified response header, if it exists.
    *@param headerName is the name of the header.
    *@return the header value, or null if it doesn't exist.
    */
    @Override
    public String getResponseHeader(String headerName)
      throws ManifoldCFException, ServiceInterruption
    {
      if (fetchMethod == null)
        throw new ManifoldCFException("Attempt to get a header when there is no method");
      if (methodThread == null || threadStarted == false)
        throw new ManifoldCFException("Attempt to get a header when no method thread");
      try
      {
        return methodThread.getFirstHeader(headerName);
      }
      catch (InterruptedException e)
      {
        methodThread.interrupt();
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (HttpException e)
      {
        handleHTTPException(e,"reading header");
      }
      catch (IOException e)
      {
        handleIOException(e,"reading header");
      }
      return null;
    }

    /** Get the response input stream.  It is the responsibility of the caller
    * to close this stream when done.
    */
    @Override
    public InputStream getResponseBodyStream()
      throws ManifoldCFException, ServiceInterruption
    {
      if (fetchMethod == null)
        throw new ManifoldCFException("Attempt to get an input stream when there is no method");
      if (methodThread == null || threadStarted == false)
        throw new ManifoldCFException("Attempt to get an input stream when no method thread");
      try
      {
        InputStream bodyStream = methodThread.getSafeInputStream();
        if (methodThread.isGZipStream())
          bodyStream = new GZIPInputStream(bodyStream);
        else if (methodThread.isDeflateStream())
          bodyStream = new DeflateInputStream(bodyStream);
        return bodyStream;
      }
      catch (InterruptedException e)
      {
        methodThread.interrupt();
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (IOException e)
      {
        handleIOException(e, "reading response stream");
      }
      catch (HttpException e)
      {
        handleHTTPException(e, "reading response stream");
      }
      return null;
    }

    /** Get limited response as a string.
    */
    @Override
    public String getLimitedResponseBody(int maxSize, String encoding)
      throws ManifoldCFException, ServiceInterruption
    {
      try
      {
        InputStream is = getResponseBodyStream();
        try
        {
          Reader r = new InputStreamReader(is,encoding);
          char[] buffer = new char[maxSize];
          int amt = r.read(buffer);
          if (amt == -1)
            return "";
          return new String(buffer,0,amt);
        }
        finally
        {
          is.close();
        }
      }
      catch (IOException e)
      {
        handleIOException(e,"reading limited response");
      }
      return null;
    }

    /** Note that the connection fetch was interrupted by something.
    */
    @Override
    public void noteInterrupted(Throwable e)
    {
      if (statusCode > 0)
      {
        throwable = new ManifoldCFException("Fetch interrupted: "+e.getMessage(),e);
        statusCode = FETCH_INTERRUPTED;
      }
    }

    /** Done with the fetch.  Call this when the fetch has been completed.  A log entry will be generated
    * describing what was done.
    */
    @Override
    public void doneFetch(IProcessActivity activities)
      throws ManifoldCFException
    {
      if (fetchType != null)
      {
        // Abort the connection, if not already complete
        if (methodThread != null && threadStarted)
          methodThread.abort();

        long endTime = System.currentTimeMillis();

        activities.recordActivity(new Long(startFetchTime),WebcrawlerConnector.ACTIVITY_FETCH,
          new Long(fetchCounter),myUrl,Integer.toString(statusCode),(throwable==null)?null:throwable.getMessage(),null);

        Logging.connectors.info("WEB: FETCH "+fetchType+"|"+myUrl+"|"+new Long(startFetchTime).toString()+"+"+new Long(endTime-startFetchTime).toString()+"|"+
          Integer.toString(statusCode)+"|"+new Long(fetchCounter).toString()+"|"+((throwable==null)?"":(throwable.getClass().getName()+"| "+throwable.getMessage())));
        if (throwable != null)
        {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("WEB: Fetch exception for '"+myUrl+"'",throwable);
        }

        // Shut down (join) the connection thread, if any, and if it started
        if (methodThread != null)
        {
          if (threadStarted)
          {
            try
            {
              methodThread.finishUp();
            }
            catch (InterruptedException e)
            {
              throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
            }
            threadStarted = false;
          }
          methodThread = null;
        }
        
        fetchMethod = null;
        throwable = null;
        startFetchTime = -1L;
        myUrl = null;
        statusCode = FETCH_NOT_TRIED;
        lastFetchCookies = null;
        fetchType = null;
      }

    }

    /** Close the connection.  Call this to return the connection to its pool.
    */
    @Override
    public void close()
    {
      expireTime = System.currentTimeMillis() + idleTimeout;
      myPool.release(this);
    }
    
    protected void handleHTTPException(HttpException e, String activity)
      throws ServiceInterruption, ManifoldCFException
    {
      long currentTime = System.currentTimeMillis();
      Logging.connectors.debug("Web: HTTP exception "+activity+" for '"+myUrl+"', retrying");
      throw new ServiceInterruption("HTTP exception "+activity+": "+e.getMessage(),e,currentTime+TIME_5MIN,-1L,2,false);
    }

    protected void handleIOException(IOException e, String activity)
      throws ServiceInterruption, ManifoldCFException
    {
      if (e instanceof java.net.SocketTimeoutException)
      {
        long currentTime = System.currentTimeMillis();
        Logging.connectors.debug("Web: Socket timeout exception "+activity+" for '"+myUrl+"', retrying");
        throw new ServiceInterruption("Socket timeout exception "+activity+": "+e.getMessage(),e,currentTime+TIME_5MIN,-1L,2,false);
      }
      if (e instanceof ConnectTimeoutException)
      {
        long currentTime = System.currentTimeMillis();
        Logging.connectors.debug("Web: Connect timeout exception "+activity+" for '"+myUrl+"', retrying");
        throw new ServiceInterruption("Connect timeout exception "+activity+": "+e.getMessage(),e,currentTime+TIME_5MIN,-1L,2,false);
      }
      if (e instanceof InterruptedIOException)
      {
        methodThread.interrupt();
        throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
      }
      if (e instanceof NoHttpResponseException)
      {
        // Give up after 2 hours.
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out "+activity+" for '"+myUrl+"'", e, currentTime + 15L * 60000L,
          currentTime + 120L * 60000L,-1,false);
      }
      if (e instanceof java.net.ConnectException)
      {
        // Give up after 6 hours.
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("Timed out "+activity+" for '"+myUrl+"'", e, currentTime + 1000000L,
          currentTime + 720L * 60000L,-1,false);
      }
      if (e instanceof java.net.NoRouteToHostException)
      {
        // This exception means we know the IP address but can't get there.  That's either a firewall issue, or it's something transient
        // with the network.  Some degree of retry is probably wise.
        long currentTime = System.currentTimeMillis();
        throw new ServiceInterruption("No route to host during "+activity+" for '"+myUrl+"'", e, currentTime + 1000000L,
          currentTime + 720L * 60000L,-1,false);
      }
      long currentTime = System.currentTimeMillis();
      Logging.connectors.debug("Web: IO exception ("+e.getClass().getName()+")"+activity+" for '"+myUrl+"', retrying",e);
      throw new ServiceInterruption("IO exception ("+e.getClass().getName()+")"+activity+": "+e.getMessage(),e,currentTime+TIME_5MIN,-1L,2,false);
    }

  }


  /** This class throttles an input stream based on the specified byte rate parameters.  The
  * throttling takes place across all streams that are open to the server in question.
  */
  protected static class ThrottledInputstream extends InputStream
  {
    /** Stream throttler */
    protected final IStreamThrottler streamThrottler;
    /** The throttled connection we belong to */
    protected final ThrottledConnection throttledConnection;
    /** The stream we are wrapping. */
    protected final InputStream inputStream;

    /** Constructor.
    */
    public ThrottledInputstream(IStreamThrottler streamThrottler, ThrottledConnection connection, InputStream is)
    {
      this.streamThrottler = streamThrottler;
      this.throttledConnection = connection;
      this.inputStream = is;
    }

    /** Read a byte.
    */
    @Override
    public int read()
      throws IOException
    {
      byte[] byteArray = new byte[1];
      int count = read(byteArray,0,1);
      if (count == -1)
        return count;
      return ((int)byteArray[0]) & 0xff;
    }

    /** Read lots of bytes.
    */
    @Override
    public int read(byte[] b)
      throws IOException
    {
      return read(b,0,b.length);
    }

    /** Read lots of specific bytes.
    */
    @Override
    public int read(byte[] b, int off, int len)
      throws IOException
    {
      int totalCount = 0;
      while (len > ThrottledFetcher.READ_CHUNK_LENGTH)
      {
        int amt = basicRead(b,off,ThrottledFetcher.READ_CHUNK_LENGTH,totalCount);
        if (amt == -1)
        {
          if (totalCount == 0)
            return amt;
          return totalCount;
        }
        totalCount += amt;
        off += amt;
        len -= amt;
      }
      if (len > 0)
      {
        int amt = basicRead(b,off,len,totalCount);
        if (amt == -1)
        {
          if (totalCount == 0)
            return amt;
          return totalCount;
        }
        return totalCount + amt;
      }
      return totalCount;
    }

    /** Basic read, which uses the server object to throttle activity.
    */
    protected int basicRead(byte[] b, int off, int len, int totalSoFar)
      throws IOException
    {
      try
      {
        if (streamThrottler.obtainReadPermission(len) == false)
          throw new IllegalStateException("Unexpected result calling obtainReadPermission()");
        int amt = 0;
        try
        {
          amt = inputStream.read(b,off,len);
          return amt;
        }
        finally
        {
          if (amt == -1)
            streamThrottler.releaseReadPermission(len,0);
          else
          {
            streamThrottler.releaseReadPermission(len,amt);
            throttledConnection.logFetchCount(amt);
          }
        }
      }
      catch (InterruptedException e)
      {
        InterruptedIOException e2 = new InterruptedIOException("Interrupted");
        e2.bytesTransferred = totalSoFar;
        throw e2;
      }
    }

    /** Skip
    */
    @Override
    public long skip(long n)
      throws IOException
    {
      // Not sure whether we should bother doing anything with this; it's not used.
      return inputStream.skip(n);
    }

    /** Get available.
    */
    @Override
    public int available()
      throws IOException
    {
      return inputStream.available();
    }

    /** Mark.
    */
    @Override
    public void mark(int readLimit)
    {
      inputStream.mark(readLimit);
    }

    /** Reset.
    */
    @Override
    public void reset()
      throws IOException
    {
      inputStream.reset();
    }

    /** Check if mark is supported.
    */
    @Override
    public boolean markSupported()
    {
      return inputStream.markSupported();
    }

    /** Close.
    */
    @Override
    public void close()
      throws IOException
    {
      try
      {
        inputStream.close();
      }
      catch (java.net.SocketTimeoutException e)
      {
        Logging.connectors.debug("Socket timeout exception trying to close connection: "+e.getMessage(),e);
      }
      catch (ConnectTimeoutException e)
      {
        Logging.connectors.debug("Socket connection timeout exception trying to close connection: "+e.getMessage(),e);
      }
      catch (InterruptedIOException e)
      {
        throw e;
      }
      catch (java.net.SocketException e)
      {
        Logging.connectors.debug("Connection reset while I was closing it: "+e.getMessage(),e);
      }
      catch (IOException e)
      {
        Logging.connectors.debug("IO Exception trying to close connection: "+e.getMessage(),e);
      }
      finally
      {
        streamThrottler.closeStream();
      }
    }

  }

  /** Pool exception class */
  protected static class PoolException extends Exception
  {
    public PoolException(String message)
    {
      super(message);
    }
  }

  /** Wait exception class */
  protected static class WaitException extends Exception
  {
    protected long amt;

    public WaitException(long amt)
    {
      super("Wait needed");
      this.amt = amt;
    }

    public long getWaitAmount()
    {
      return amt;
    }
  }

  /** Class to create a cookie spec.
  */
  protected static class LaxBrowserCompatSpecProvider extends RFC6265CookieSpecProvider
  {
    @Override
    public CookieSpec create(HttpContext context)
    {
      return new LaxBrowserCompatSpec();
    }
  }

  /** This thread does the actual socket communication with the server.
  * It's set up so that it can be abandoned at shutdown time.
  *
  * The way it works is as follows:
  * - it starts the transaction
  * - it receives the response, and saves that for the calling class to inspect
  * - it transfers the data part to an input stream provided to the calling class
  * - it shuts the connection down
  *
  * If there is an error, the sequence is aborted, and an exception is recorded
  * for the calling class to examine.
  *
  * The calling class basically accepts the sequence above.  It starts the
  * thread, and tries to get a response code.  If instead an exception is seen,
  * the exception is thrown up the stack.
  */
  protected static class ExecuteMethodThread extends Thread
  {
    /** The connection */
    protected final ThrottledConnection theConnection;
    /** The fetch throttler */
    protected final IFetchThrottler fetchThrottler;
    /** Client and method, all preconfigured */
    protected final HttpClient httpClient;
    protected final HttpHost target;
    protected final HttpRequestBase executeMethod;
    protected final CookieStore cookieStore;
    
    protected HttpResponse response = null;
    protected Throwable responseException = null;
    protected LoginCookies cookies = null;
    protected Throwable cookieException = null;
    protected XThreadInputStream threadStream = null;
    protected InputStream bodyStream = null;
    protected boolean streamCreated = false;
    protected Throwable streamException = null;
    protected boolean abortThread = false;
    protected boolean gzip = false;
    protected boolean deflate = false;

    protected Throwable shutdownException = null;

    protected Throwable generalException = null;
    
    public ExecuteMethodThread(ThrottledConnection theConnection, IFetchThrottler fetchThrottler,
      HttpClient httpClient, HttpHost target, HttpRequestBase executeMethod, CookieStore cookieStore)
    {
      super();
      setDaemon(true);
      this.theConnection = theConnection;
      this.fetchThrottler = fetchThrottler;
      this.httpClient = httpClient;
      this.target = target;
      this.executeMethod = executeMethod;
      this.cookieStore = cookieStore;
    }

    public void run()
    {
      try
      {
        try
        {
          // Call the execute method appropriately
          synchronized (this)
          {
            if (!abortThread)
            {
              try
              {
                HttpContext context = new BasicHttpContext();
                context.setAttribute(HttpClientContext.COOKIE_STORE,cookieStore);
                response = httpClient.execute(target,executeMethod,context);
              }
              catch (java.net.SocketTimeoutException e)
              {
                responseException = e;
              }
              catch (ConnectTimeoutException e)
              {
                responseException = e;
              }
              catch (InterruptedIOException e)
              {
                throw e;
              }
              catch (Throwable e)
              {
                responseException = e;
              }
              this.notifyAll();
            }
          }
          
          // Fetch the cookies
          if (responseException == null)
          {
            synchronized (this)
            {
              if (!abortThread)
              {
                try
                {
                  cookies = new CookieSet(cookieStore.getCookies());
                }
                catch (Throwable e)
                {
                  cookieException = e;
                }
                this.notifyAll();
              }
            }
          }

          // Start the transfer of the content
          if (cookieException == null && responseException == null)
          {
            synchronized (this)
            {
              if (!abortThread)
              {
                try
                {
                  Header ceheader = response.getEntity().getContentEncoding();
                  if (ceheader != null)
                  {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++)
                    {
                      if (codecs[i].getName().equalsIgnoreCase("gzip"))
                      {
                        // GZIP
                        gzip = true;
                        break;
                      }
                      else if (codecs[i].getName().equalsIgnoreCase("deflate"))
                      {
                        // Deflate
                        deflate = true;
                        break;
                      }
                    }
                  }
                  bodyStream = response.getEntity().getContent();
                  if (bodyStream != null)
                  {
                    bodyStream = new ThrottledInputstream(fetchThrottler.createFetchStream(),theConnection,bodyStream);
                    threadStream = new XThreadInputStream(bodyStream);
                  }
                  streamCreated = true;
                }
                catch (java.net.SocketTimeoutException e)
                {
                  streamException = e;
                }
                catch (ConnectTimeoutException e)
                {
                  streamException = e;
                }
                catch (InterruptedIOException e)
                {
                  throw e;
                }
                catch (Throwable e)
                {
                  streamException = e;
                }
                this.notifyAll();
              }
            }
          }
          
          if (cookieException == null && responseException == null && streamException == null)
          {
            if (threadStream != null)
            {
              // Stuff the content until we are done
              threadStream.stuffQueue();
            }
          }
          
        }
        finally
        {
          if (bodyStream != null)
          {
            try
            {
              bodyStream.close();
            }
            catch (IOException e)
            {
            }
            bodyStream = null;
          }
          synchronized (this)
          {
            try
            {
              executeMethod.abort();
            }
            catch (Throwable e)
            {
              shutdownException = e;
            }
            this.notifyAll();
          }
        }
      }
      catch (Throwable e)
      {
        // We catch exceptions here that should ONLY be InterruptedExceptions, as a result of the thread being aborted.
        this.generalException = e;
      }
    }

    public int getResponseCode()
      throws InterruptedException, IOException, HttpException
    {
      // Must wait until the response object is there
      while (true)
      {
        synchronized (this)
        {
          checkException(responseException);
          if (response != null)
            return response.getStatusLine().getStatusCode();
          wait();
        }
      }
    }

    public Map<String,List<String>> getResponseHeaders()
      throws InterruptedException, IOException, HttpException
    {
      // Must wait for the response object to appear
      while (true)
      {
        synchronized (this)
        {
          checkException(responseException);
          if (response != null)
          {
            Header[] headers = response.getAllHeaders();
            Map<String,List<String>> rval = new HashMap<String,List<String>>();
            int i = 0;
            while (i < headers.length)
            {
              Header h = headers[i++];
              String name = h.getName();
              String value = h.getValue();
              List<String> values = rval.get(name);
              if (values == null)
              {
                values = new ArrayList<String>();
                rval.put(name,values);
              }
              values.add(value);
            }
            return rval;
          }
          wait();
        }
      }

    }
    
    public String getFirstHeader(String headerName)
      throws InterruptedException, IOException, HttpException
    {
      // Must wait for the response object to appear
      while (true)
      {
        synchronized (this)
        {
          checkException(responseException);
          if (response != null)
          {
            Header h = response.getFirstHeader(headerName);
            if (h == null)
              return null;
            return h.getValue();
          }
          wait();
        }
      }
    }

    public LoginCookies getCookies()
      throws InterruptedException, IOException, HttpException
    {
      while (true)
      {
        synchronized (this)
        {
          if (responseException != null)
            throw new IllegalStateException("Check for response before getting cookies");
          checkException(cookieException);
          if (cookies != null)
            return cookies;
          wait();
        }
      }
    }

    public boolean isGZipStream()
      throws InterruptedException, IOException, HttpException
    {
      // Must wait until stream is created, or until we note an exception was thrown.
      while (true)
      {
        synchronized (this)
        {
          if (responseException != null)
            throw new IllegalStateException("Check for response before getting stream");
          if (cookieException != null)
            throw new IllegalStateException("Check for cookies before getting stream");
          checkException(streamException);
          if (streamCreated)
            return gzip;
          wait();
        }
      }
    }    

    public boolean isDeflateStream()
      throws InterruptedException, IOException, HttpException
    {
      // Must wait until stream is created, or until we note an exception was thrown.
      while (true)
      {
        synchronized (this)
        {
          if (responseException != null)
            throw new IllegalStateException("Check for response before getting stream");
          if (cookieException != null)
            throw new IllegalStateException("Check for cookies before getting stream");
          checkException(streamException);
          if (streamCreated)
            return deflate;
          wait();
        }
      }
    }    
    
    public InputStream getSafeInputStream()
      throws InterruptedException, IOException, HttpException
    {
      // Must wait until stream is created, or until we note an exception was thrown.
      while (true)
      {
        synchronized (this)
        {
          if (responseException != null)
            throw new IllegalStateException("Check for response before getting stream");
          if (cookieException != null)
            throw new IllegalStateException("Check for cookies before getting stream");
          checkException(streamException);
          if (streamCreated)
            return threadStream;
          wait();
        }
      }
    }
    
    public void abort()
    {
      // This will be called during the finally
      // block in the case where all is well (and
      // the stream completed) and in the case where
      // there were exceptions.
      synchronized (this)
      {
        if (streamCreated)
        {
          if (threadStream != null)
            threadStream.abort();
        }
        abortThread = true;
      }
    }
    
    public void finishUp()
      throws InterruptedException
    {
      join();
    }
    
    protected synchronized void checkException(Throwable exception)
      throws IOException, HttpException
    {
      if (exception != null)
      {
        // Throw the current exception, but clear it, so no further throwing is possible on the same problem.
        Throwable e = exception;
        if (e instanceof IOException)
          throw (IOException)e;
        else if (e instanceof HttpException)
          throw (HttpException)e;
        else if (e instanceof RuntimeException)
          throw (RuntimeException)e;
        else if (e instanceof Error)
          throw (Error)e;
        else
          throw new RuntimeException("Unhandled exception of type: "+e.getClass().getName(),e);
      }
    }

  }

  protected static class OurBasicCookieStore implements CookieStore, Serializable {

    private static final long serialVersionUID = -7581093305228232025L;

    private final TreeSet<Cookie> cookies;

    public OurBasicCookieStore() {
      super();
      this.cookies = new TreeSet<Cookie>(new CookieIdentityComparator());
    }

    /**
     * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent cookies.
     * If the given cookie has already expired it will not be added, but existing
     * values will still be removed.
     *
     * @param cookie the {@link Cookie cookie} to be added
     *
     * @see #addCookies(Cookie[])
     *
     */
    public synchronized void addCookie(Cookie cookie) {
      if (cookie != null) {
        // first remove any old cookie that is equivalent
        cookies.remove(cookie);
        cookies.add(cookie);
      }
    }

    /**
     * Adds an array of {@link Cookie HTTP cookies}. Cookies are added individually and
     * in the given array order. If any of the given cookies has already expired it will
     * not be added, but existing values will still be removed.
     *
     * @param cookies the {@link Cookie cookies} to be added
     *
     * @see #addCookie(Cookie)
     *
     */
    public synchronized void addCookies(Cookie[] cookies) {
      if (cookies != null) {
        for (Cookie cooky : cookies) {
          this.addCookie(cooky);
        }
      }
    }

    /**
     * Returns an immutable array of {@link Cookie cookies} that this HTTP
     * state currently contains.
     *
     * @return an array of {@link Cookie cookies}.
     */
    public synchronized List<Cookie> getCookies() {
      //create defensive copy so it won't be concurrently modified
      return new ArrayList<Cookie>(cookies);
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state
     * that have expired by the specified {@link java.util.Date date}.
     *
     * @return true if any cookies were purged.
     *
     * @see Cookie#isExpired(Date)
     */
    public synchronized boolean clearExpired(final Date date) {
      if (date == null) {
        return false;
      }
      boolean removed = false;
      for (Iterator<Cookie> it = cookies.iterator(); it.hasNext();) {
        if (it.next().isExpired(date)) {
          it.remove();
            removed = true;
        }
      }
      return removed;
    }

    /**
     * Clears all cookies.
     */
    public synchronized void clear() {
      cookies.clear();
    }

    @Override
    public synchronized String toString() {
      return cookies.toString();
    }

  }

  /** Connection pool key */
  protected static class ConnectionPoolKey
  {
    protected final String protocol;
    protected final String server;
    protected final int port;
    protected final PageCredentials authentication;
    protected final String trustStoreString;
    protected final String proxyHost;
    protected final int proxyPort;
    protected final String proxyAuthDomain;
    protected final String proxyAuthUsername;
    protected final String proxyAuthPassword;
    protected final int socketTimeoutMilliseconds;
    protected final int connectionTimeoutMilliseconds;
    
    public ConnectionPoolKey(String protocol,
      String server, int port, PageCredentials authentication,
      String trustStoreString, String proxyHost, int proxyPort,
      String proxyAuthDomain, String proxyAuthUsername, String proxyAuthPassword,
      int socketTimeoutMilliseconds, int connectionTimeoutMilliseconds)
    {
      this.protocol = protocol;
      this.server = server;
      this.port = port;
      this.authentication = authentication;
      this.trustStoreString = trustStoreString;
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
      this.proxyAuthDomain = proxyAuthDomain;
      this.proxyAuthUsername = proxyAuthUsername;
      this.proxyAuthPassword = proxyAuthPassword;
      this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
      this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
    }
    
    public int hashCode()
    {
      return protocol.hashCode() +
        server.hashCode() +
        (port * 31) +
        ((authentication==null)?0:authentication.hashCode()) +
        ((trustStoreString==null)?0:trustStoreString.hashCode()) +
        ((proxyHost==null)?0:proxyHost.hashCode()) +
        (proxyPort * 29) +
        ((proxyAuthDomain==null)?0:proxyAuthDomain.hashCode()) +
        ((proxyAuthUsername==null)?0:proxyAuthUsername.hashCode()) +
        ((proxyAuthPassword==null)?0:proxyAuthPassword.hashCode()) +
        new Integer(socketTimeoutMilliseconds).hashCode() +
        new Integer(connectionTimeoutMilliseconds).hashCode();
    }
    
    public boolean equals(Object o)
    {
      if (!(o instanceof ConnectionPoolKey))
        return false;
      ConnectionPoolKey other = (ConnectionPoolKey)o;
      if (!server.equals(other.server) ||
        port != other.port)
        return false;
      if (authentication == null || other.authentication == null)
      {
        if (authentication != other.authentication)
          return false;
      }
      else
      {
        if (!authentication.equals(other.authentication))
          return false;
      }
      if (trustStoreString == null || other.trustStoreString == null)
      {
        if (trustStoreString != other.trustStoreString)
          return false;
      }
      else
      {
        if (!trustStoreString.equals(other.trustStoreString))
          return false;
      }
      if (proxyHost == null || other.proxyHost == null)
      {
        if (proxyHost != other.proxyHost)
          return false;
      }
      else
      {
        if (!proxyHost.equals(other.proxyHost))
          return false;
      }
      if (proxyPort != other.proxyPort)
        return false;
      if (proxyAuthDomain == null || other.proxyAuthDomain == null)
      {
        if (proxyAuthDomain != other.proxyAuthDomain)
          return false;
      }
      else
      {
        if (!proxyAuthDomain.equals(other.proxyAuthDomain))
          return false;
      }
      if (proxyAuthUsername == null || other.proxyAuthUsername == null)
      {
        if (proxyAuthUsername != other.proxyAuthUsername)
          return false;
      }
      else
      {
        if (!proxyAuthUsername.equals(other.proxyAuthUsername))
          return false;
      }
      if (proxyAuthPassword == null || other.proxyAuthPassword == null)
      {
        if (proxyAuthPassword != other.proxyAuthPassword)
          return false;
      }
      else
      {
        if (!proxyAuthPassword.equals(other.proxyAuthPassword))
          return false;
      }
      return socketTimeoutMilliseconds == other.socketTimeoutMilliseconds &&
        connectionTimeoutMilliseconds == other.connectionTimeoutMilliseconds;
    }
  }
  
  /** Each connection pool has identical connections we can draw on.
  */
  protected static class ConnectionPool
  {
    /** Throttler */
    protected final IConnectionThrottler connectionThrottler;
    
    // If we need to create a connection, these are what we use
    
    protected final String protocol;
    protected final String server;
    protected final int port;
    protected final PageCredentials authentication;
    protected final javax.net.ssl.SSLSocketFactory baseFactory;
    protected final String proxyHost;
    protected final int proxyPort;
    protected final String proxyAuthDomain;
    protected final String proxyAuthUsername;
    protected final String proxyAuthPassword;
    protected final int socketTimeoutMilliseconds;
    protected final int connectionTimeoutMilliseconds;

    /** The actual pool of connections */
    protected final List<IThrottledConnection> connections = new ArrayList<IThrottledConnection>();
    
    public ConnectionPool(IConnectionThrottler connectionThrottler,
      String protocol,
      String server, int port, PageCredentials authentication,
      javax.net.ssl.SSLSocketFactory baseFactory,
      String proxyHost, int proxyPort,
      String proxyAuthDomain, String proxyAuthUsername, String proxyAuthPassword,
      int socketTimeoutMilliseconds, int connectionTimeoutMilliseconds)
    {
      this.connectionThrottler = connectionThrottler;
      
      this.protocol = protocol;
      this.server = server;
      this.port = port;
      this.authentication = authentication;
      this.baseFactory = baseFactory;
      this.proxyHost = proxyHost;
      this.proxyPort = proxyPort;
      this.proxyAuthDomain = proxyAuthDomain;
      this.proxyAuthUsername = proxyAuthUsername;
      this.proxyAuthPassword = proxyAuthPassword;
      this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
      this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
    }
    
    public IThrottledConnection grab(IAbortActivity activities)
      throws ManifoldCFException, ServiceInterruption
    {
      AbortChecker abortCheck = new AbortChecker(activities);
      try
      {
        // Wait for a connection
        IThrottledConnection connection;
        int result = connectionThrottler.waitConnectionAvailable(abortCheck);
        if (result == IConnectionThrottler.CONNECTION_FROM_POOL)
        {
          // We are guaranteed to have a connection in the pool, unless there's a coding error.
          synchronized (connections)
          {
            connection = connections.remove(connections.size()-1);
          }
        }
        else if (result == IConnectionThrottler.CONNECTION_FROM_CREATION)
        {
          connection = new ThrottledConnection(this,connectionThrottler.getNewConnectionFetchThrottler(),
            protocol,server,port,authentication,baseFactory,
            proxyHost,proxyPort,
            proxyAuthDomain,proxyAuthUsername,proxyAuthPassword,
            socketTimeoutMilliseconds,connectionTimeoutMilliseconds);
        }
        else
          throw new IllegalStateException("Unexpected return value from waitConnectionAvailable(): "+result);
        connection.setAbortChecker(abortCheck);
        return connection;
      }
      catch (InterruptedException e)
      {
        throw new ManifoldCFException("Interrupted: "+e.getMessage(),ManifoldCFException.INTERRUPTED);
      }
      catch (BreakException e)
      {
        abortCheck.rethrowExceptions();
        return null;
      }
    }
    
    public void release(IThrottledConnection connection)
    {
      if (connectionThrottler.noteReturnedConnection())
      {
        // Destroy this connection
        connection.destroy();
        connectionThrottler.noteConnectionDestroyed();
      }
      else
      {
        // Return to pool
        connection.setAbortChecker(null);
        synchronized (connections)
        {
          connections.add(connection);
        }
        connectionThrottler.noteConnectionReturnedToPool();
      }
    }
    
    public void flushIdleConnections()
    {
      long currentTime = System.currentTimeMillis();
      // First, remove connections that are over the quota
      while (connectionThrottler.checkDestroyPooledConnection())
      {
        // Destroy the oldest ones first
        IThrottledConnection connection;
        synchronized (connections)
        {
          connection = connections.remove(0);
        }
        connection.destroy();
        connectionThrottler.noteConnectionDestroyed();
      }
      // Now, get rid of expired connections
      while (true)
      {
        boolean expired;
        synchronized (connections)
        {
          expired = connections.size() > 0 && connections.get(0).hasExpired(currentTime);
        }
        if (!expired)
          break;
        // We found an expired connection!  Now tell the throttler that, and see if it agrees.
        if (connectionThrottler.checkExpireConnection())
        {
          // Remove a connection from the pool, and destroy it.
          // It's not guaranteed to be an expired one, but that's a rare occurrence, we expect.
          IThrottledConnection connection;
          synchronized (connections)
          {
            connection = connections.remove(0);
          }
          connection.destroy();
          connectionThrottler.noteConnectionDestroyed();
        }
        else
          break;
      }
    }
    
  }
}
