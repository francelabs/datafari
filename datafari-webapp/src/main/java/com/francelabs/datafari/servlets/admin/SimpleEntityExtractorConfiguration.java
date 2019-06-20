/*******************************************************************************
 *  * Copyright 2019 France Labs
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

package com.francelabs.datafari.servlets.admin;

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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.SolrAPI;
import com.francelabs.datafari.utils.SolrConfiguration;

@WebServlet("/SearchAdministrator/simpleEntityExtractorConfiguration")
public class SimpleEntityExtractorConfiguration extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LogManager.getLogger(SimpleEntityExtractorConfiguration.class.getName());

  private final String DEFAULT_SOLR_SERVER = "localhost";
  private final String DEFAULT_SOLR_PORT = "8983";
  private final String DEFAULT_SOLR_PROTOCOL = "http";

  private final String addCopyField = "add-copy-field";
  private final String deleteCopyField = "delete-copy-field";
  private final String fieldSource = "content_*";
  private final String fieldDestination = "entity_person";

  private final String entityExtractActivationVariable = "entity.extract";
  private final String entityExtractNameVariable = "entity.name";
  private final String entityExtractPhoneVariable = "entity.phone";
  private final String entityExtractSpecialVariable = "entity.special";

  private final String solrserver;
  private final String solrport;
  private final String protocol;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public SimpleEntityExtractorConfiguration() {
    super();
    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST, DEFAULT_SOLR_SERVER);
    solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT, DEFAULT_SOLR_PORT);
    protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL, DEFAULT_SOLR_PROTOCOL);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();

    boolean isActivated = false;
    boolean isNamesActivated = false;
    boolean isPhonesActivated = false;
    boolean isSpecialActivated = false;
    // Perform retrieve operations to get the actual values


    JSONObject jsonresponseAPI = new JSONObject();

    try {
      jsonresponseAPI = SolrAPI.readConfigOverlay(Core.FILESHARE.toString());

      isActivated = Boolean.parseBoolean(SolrAPI.getUserProp(jsonresponseAPI, entityExtractActivationVariable));

      isNamesActivated = (Boolean) Boolean.parseBoolean(SolrAPI.getUserProp(jsonresponseAPI, entityExtractNameVariable));
      isPhonesActivated = (Boolean) Boolean.parseBoolean(SolrAPI.getUserProp(jsonresponseAPI, entityExtractPhoneVariable));
      isSpecialActivated = (Boolean) Boolean.parseBoolean(SolrAPI.getUserProp(jsonresponseAPI, entityExtractSpecialVariable));

      LOGGER.debug("Entity extraction "+isActivated);

    } catch (final Exception e) {
      LOGGER.error("Error while querying Solr config API to get entity extraction parameters.", e);
      response.setStatus(500);
      return;
    }

    // Write the values to the response object and send
    jsonResponse.put("isActivated", isActivated);
    jsonResponse.put("isNamesActivated", isNamesActivated);
    jsonResponse.put("isPhonesActivated", isPhonesActivated);
    jsonResponse.put("isSpecialActivated", isSpecialActivated);
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    try {

      SolrAPI.setUserProp(Core.FILESHARE.toString(), entityExtractActivationVariable, request.getParameter("isActivated"));
      SolrAPI.setUserProp(Core.FILESHARE.toString(), entityExtractNameVariable, request.getParameter("isNamesActivated"));
      SolrAPI.setUserProp(Core.FILESHARE.toString(), entityExtractPhoneVariable, request.getParameter("isPhonesActivated"));
      SolrAPI.setUserProp(Core.FILESHARE.toString(), entityExtractSpecialVariable, request.getParameter("isSpecialActivated"));

      boolean isActivated = (Boolean) Boolean.parseBoolean(request.getParameter("isActivated"));
      boolean isNamesActivated = (Boolean) Boolean.parseBoolean(request.getParameter("isNamesActivated"));

      boolean copyFieldPresent = false ;
      copyFieldPresent = SolrAPI.containsCopyFields(Core.FILESHARE.toString());
      if ((isActivated == true) && (isNamesActivated == true) && (copyFieldPresent == false) ) {
        SolrAPI.manageCopyField(Core.FILESHARE.toString(), addCopyField,fieldSource, fieldDestination);
      }

      if ((isActivated == false) || (isNamesActivated == false)) {
        while (copyFieldPresent == true) {
          SolrAPI.manageCopyField(Core.FILESHARE.toString(), deleteCopyField,fieldSource, fieldDestination);
          copyFieldPresent = SolrAPI.containsCopyFields(Core.FILESHARE.toString());
          Thread.sleep(1000);
        }

      }

      jsonResponse.put("code", CodesReturned.ALLOK.getValue());

    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
      LOGGER.error("Entity extractor error ", e);
      response.setStatus(500);
      return;
    }

    // Dummy always OK response
    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    jsonResponse.put(OutputConstants.STATUS, "Configuration successfully updated.");

    // Send the response
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }


}
