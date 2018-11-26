/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.utils.HighlightConfiguration;
import com.francelabs.datafari.utils.SolrAPI;
import com.francelabs.datafari.utils.SolrConfiguration;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/GetAutocompleteThreshold")
public class GetAutocompleteThreshold extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetAutocompleteThreshold.class.getName());

  private final String DEFAULT_SOLR_SERVER = "localhost";
  private final String DEFAULT_SOLR_PORT = "8983";
  private final String DEFAULT_SOLR_PROTOCOL = "http";

  private final String solrserver;
  private final String solrport;
  private final String protocol;

  /**
   * @throws IOException 
   * @see HttpServlet#HttpServlet()
   */
  public GetAutocompleteThreshold() throws IOException {
    super();
   
    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
   solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
   protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    double autocompleteThreshold = 0;
   JSONObject jsonresponse = new JSONObject();
    
    try {
     jsonresponse =  SolrAPI.readConfigOverlay();
     autocompleteThreshold = SolrAPI.getAutocompleteThreshold(jsonresponse);
    } catch (ManifoldCFException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (ParseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    // Perform retrieve operations to get the actual values

    logger.debug("overlay"+jsonresponse.toJSONString());
    // Write the values to the response object and send
    jsonResponse.put("autoCompleteThreshold", autocompleteThreshold);
    jsonResponse.put("code", CodesReturned.ALLOK.getValue());
   
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    if (request.getParameter("hl.maxAnalyzedChars") != null) {
      logger.debug(request.getParameter("hl.maxAnalyzedChars"));
      System.out.println(request.getParameter("hl.maxAnalyzedChars"));
      
    }
    
  }
  
 
 

}
