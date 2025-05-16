/* $Id: JiraSession.java 1490586 2013-06-07 11:14:52Z kwright $ */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
* http://www.apache.org/licenses/LICENSE-2.0
 * 
* Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.manifoldcf.crawler.connectors.jira;

import org.apache.manifoldcf.core.common.*;
import org.apache.manifoldcf.connectorcommon.common.*;
import org.apache.manifoldcf.connectorcommon.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.Reader;
import java.io.Writer;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.manifoldcf.core.util.URLEncoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.client.AuthCache;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

/**
 *
 * @author andrew
 */
public class JiraSession {

  private final HttpHost host;
  private final String path;
  private final String clientId;
  private final String clientSecret;
  private String baseUrl;
  
  private HttpClientConnectionManager connectionManager;
  private HttpClient httpClient;
  
  // Current host name
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

  /**
   * Constructor. Create a session.
   */
  public JiraSession(String clientId, String clientSecret,
    String protocol, String host, int port, String path,
    String proxyHost, int proxyPort, String proxyDomain, String proxyUsername, String proxyPassword)
    throws ManifoldCFException {
    this.host = new HttpHost(host,port,protocol);
    this.path = path;
    this.clientId = clientId;
    this.clientSecret = clientSecret;

    int socketTimeout = 900000;
    int connectionTimeout = 60000;

    javax.net.ssl.SSLSocketFactory httpsSocketFactory = KeystoreManagerFactory.getTrustingSecureSocketFactory();
    SSLConnectionSocketFactory myFactory = new SSLConnectionSocketFactory(new InterruptibleSocketFactory(httpsSocketFactory,connectionTimeout),
      NoopHostnameVerifier.INSTANCE);

    PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", myFactory)
        .build());
    poolingConnectionManager.setDefaultMaxPerRoute(1);
    poolingConnectionManager.setValidateAfterInactivity(2000);
    poolingConnectionManager.setDefaultSocketConfig(SocketConfig.custom()
      .setTcpNoDelay(true)
      .setSoTimeout(socketTimeout)
      .build());
    connectionManager = poolingConnectionManager;

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    // If authentication needed, set that
    if (clientId != null)
    {
      credentialsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(clientId,clientSecret));
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom()
      .setCircularRedirectsAllowed(true)
      .setSocketTimeout(socketTimeout)
      .setExpectContinueEnabled(true)
      .setConnectTimeout(connectionTimeout)
      .setConnectionRequestTimeout(socketTimeout);

    // If there's a proxy, set that too.
    if (proxyHost != null && proxyHost.length() > 0)
    {


      // Configure proxy authentication
      if (proxyUsername != null && proxyUsername.length() > 0)
      {
        if (proxyPassword == null)
          proxyPassword = "";
        if (proxyDomain == null)
          proxyDomain = "";

        credentialsProvider.setCredentials(
          new AuthScope(proxyHost, proxyPort),
          new NTCredentials(proxyUsername, proxyPassword, currentHost, proxyDomain));
      }

      HttpHost proxy = new HttpHost(proxyHost, proxyPort);
      requestBuilder.setProxy(proxy);
    }

    httpClient = HttpClients.custom()
      .setConnectionManager(connectionManager)
      .disableAutomaticRetries()
      .setDefaultRequestConfig(requestBuilder.build())
      .setDefaultCredentialsProvider(credentialsProvider)
      .setRequestExecutor(new HttpRequestExecutor(socketTimeout))
      .setRedirectStrategy(new LaxRedirectStrategy())
      .build();

  }

  /**
   * Close session.
   */
  public void close() {
    httpClient = null;
    if (connectionManager != null)
      connectionManager.shutdown();
    connectionManager = null;
  }

  private static Object convertToJSON(HttpResponse httpResponse)
    throws IOException {
    HttpEntity entity = httpResponse.getEntity();
    if (entity != null) {
      InputStream is = entity.getContent();
      try {
        Reader r = new InputStreamReader(is,getCharSet(entity));
        return JSONValue.parse(r);
      } finally {
        is.close();
      }
    }
    return null;
  }

  private static String convertToString(HttpResponse httpResponse)
    throws IOException {
    HttpEntity entity = httpResponse.getEntity();
    if (entity != null) {
      InputStream is = entity.getContent();
      try {
        char[] buffer = new char[65536];
        Reader r = new InputStreamReader(is,getCharSet(entity));
        Writer w = new StringWriter();
        try {
          while (true) {
            int amt = r.read(buffer);
            if (amt == -1)
              break;
            w.write(buffer,0,amt);
          }
        } finally {
          w.flush();
        }
        return w.toString();
      } finally {
        is.close();
      }
    }
    return "";
  }

  private static Charset getCharSet(HttpEntity entity)
  {
    Charset charSet;
    try {
      final ContentType ct = ContentType.get(entity);
      if (ct == null || ct.getCharset() == null) {
        charSet = StandardCharsets.UTF_8;
      } else {
        charSet = ct.getCharset();
      }
    } catch (final ParseException | UnsupportedCharsetException e) {
      charSet = StandardCharsets.UTF_8;
    }
    return charSet;
  }
  
  private void getRest(String rightside, JiraJSONResponse response) 
    throws IOException, ResponseException {

    // Create AuthCache instance
    AuthCache authCache = new BasicAuthCache();
    // Generate BASIC scheme object and add it to the local
    // auth cache
    BasicScheme basicAuth = new BasicScheme();
    authCache.put(host, basicAuth);

    // Add AuthCache to the execution context
    HttpClientContext localContext = HttpClientContext.create();
    localContext.setAuthCache(authCache);

    final HttpRequestBase method = new HttpGet(host.toURI() + path + rightside);
    method.addHeader("Accept", "application/json");

    try {
      HttpResponse httpResponse = httpClient.execute(method,localContext);
      int resultCode = httpResponse.getStatusLine().getStatusCode();
      if (resultCode != 200)
        throw new IOException("Unexpected result code "+resultCode+": "+convertToString(httpResponse));
      Object jo = convertToJSON(httpResponse);
      response.acceptJSONObject(jo);
    } finally {
      method.abort();
    }
  }

  /**
   * Obtain repository information.
   */
  public Map<String, String> getRepositoryInfo()
    throws IOException, ResponseException {
    HashMap<String, String> statistics = new HashMap<String, String>();
    JiraQueryResults qr = new JiraQueryResults();
    getRest("search?maxResults=1&jql=", qr);
    statistics.put("Total Issues", qr.getTotal().toString());
    return statistics;
  }
  
  /**
   * Get baseUrl via serverInfo API call
   * @return
   * @throws IOException
   * @throws ResponseException
   */
  public String getBaseUrl() throws IOException, ResponseException {
    if (this.baseUrl == null) {
      JiraServerInfo jiraServerInfo = new JiraServerInfo();
      getRest("serverInfo",jiraServerInfo);
      this.baseUrl = jiraServerInfo.getBaseUrl();
    }
    return this.baseUrl;
  }

  /**
   * Get the list of matching root documents, e.g. seeds.
   */
  public void getSeeds(XThreadStringBuffer idBuffer, String jiraDriveQuery)
      throws IOException, ResponseException, InterruptedException, ManifoldCFException {
    long startAt = 0L;
    long setSize = 800L;
    long totalAmt = 0L;
    do {
      JiraQueryResults qr = new JiraQueryResults();
      getRest("search?maxResults=" + setSize + "&startAt=" + startAt + "&jql=" + URLEncoder.encode(jiraDriveQuery), qr);
      Long total = qr.getTotal();
      if (total == null)
        return;
      totalAmt = total.longValue();
      qr.pushIds(idBuffer);
      startAt += setSize;
    } while (startAt < totalAmt);
  }

  /**
  * Get the list of users that can see the specified issue.
  */
  public List<String> getUsers(String issueKey)
    throws IOException, ResponseException, ManifoldCFException {
    List<String> rval = new ArrayList<String>();
    long startAt = 0L;
    long setSize = 800L;
    while (true) {
      JiraUserQueryResults qr = new JiraUserQueryResults();
      getRest("user/viewissue/search?username=%27%27&issueKey="+URLEncoder.encode(issueKey)+"&maxResults=" + setSize + "&startAt=" + startAt, qr);
      qr.getNames(rval);
      startAt += setSize;
      if (rval.size() < startAt)
        break;
    }
    return rval;
  }

  /**
   * Get an individual issue.
   */
  public JiraIssue getIssue(String issueKey)
    throws IOException, ResponseException, ManifoldCFException {
    JiraIssue ji = new JiraIssue();
    getRest("issue/" + URLEncoder.encode(issueKey), ji);
    return ji;
  }


}
