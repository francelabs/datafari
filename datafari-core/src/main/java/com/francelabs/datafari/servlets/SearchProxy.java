/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryManager;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.RealmLdapConfiguration;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchProxy/*")
public class SearchProxy extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static String domain = "corp.francelabs.com";

  private static final List<String> allowedHandlers = Arrays.asList("/select", "/suggest", "/stats", "/statsQuery");

  private static final Logger LOGGER = Logger.getLogger(SearchProxy.class.getName());

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";

    if (!allowedHandlers.contains(handler)) {
      log("Unauthorized handler");
      response.setStatus(401);
      response.setContentType("text/html");
      final PrintWriter out = response.getWriter();
      out.println("<HTML>");
      out.println("<HEAD><TITLE>Unauthorized Handler</TITLE></HEAD>");
      out.println("<BODY>");
      out.println("The handler is not authorized.");
      out.print("Only these handlers are authorized : ");
      for (final String allowedHandler : allowedHandlers) {
        out.print(allowedHandler + " ");
      }
      out.println("</BODY></HTML>");
      return;
    }

    IndexerServer solr;
    IndexerServer promolinkCore = null;
    IndexerQueryResponse queryResponse = null;
    IndexerQueryResponse queryResponsePromolink = null;
    IndexerQuery queryPromolink = null;

    final IndexerQuery params;
    try {
      // get the AD domain
      final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);
      if (h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME) != null) {
        final String userBase = h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME).toLowerCase();
        final String[] parts = userBase.split(",");
        domain = "";
        for (int i = 0; i < parts.length; i++) {
          if (parts[i].indexOf("dc=") != -1) { // Check if the current
            // part is a domain
            // component
            if (!domain.isEmpty()) {
              domain += ".";
            }
            domain += parts[i].substring(parts[i].indexOf('=') + 1);
          }
        }
      }

      switch (handler) {
      case "/stats":
      case "/statsQuery":
        solr = IndexerServerManager.getIndexerServer(Core.STATISTICS);
        params = IndexerQueryManager.createQuery();
        break;
      default:
        solr = IndexerServerManager.getIndexerServer(Core.FILESHARE);
        params = IndexerQueryManager.createQuery();
        promolinkCore = IndexerServerManager.getIndexerServer(Core.PROMOLINK);
        queryPromolink = IndexerQueryManager.createQuery();

        // Add authentication
        if (request.getUserPrincipal() != null) {
          String AuthenticatedUserName = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
          if (AuthenticatedUserName.contains("@")) {
            AuthenticatedUserName = AuthenticatedUserName.substring(0, AuthenticatedUserName.indexOf("@"));
          }
          if (!domain.equals("")) {
            AuthenticatedUserName += "@" + domain;
          }
          params.setParam("AuthenticatedUserName", AuthenticatedUserName);
        }

        final String queryParam = params.getParamValue("query");
        if (queryParam != null) {
          params.setParam("q", queryParam);
          params.removeParam("query");
        }

        break;
      }

      params.addParams(request.getParameterMap());

      try {
        if (ScriptConfiguration.getProperty("ontologyEnabled").toLowerCase().equals("true")
            && ScriptConfiguration.getProperty("ontologyEnabled").toLowerCase().equals("true")
            && handler.equals("/select")) {
          final boolean languageSelection = Boolean
              .valueOf(ScriptConfiguration.getProperty("ontologyLanguageSelection"));
          String parentsLabels = ScriptConfiguration.getProperty("ontologyParentsLabels");
          String childrenLabels = ScriptConfiguration.getProperty("ontologyChildrenLabels");
          if (languageSelection) {
            parentsLabels += "_fr";
            childrenLabels += "_fr";
          }
          final int facetFieldLength = params.getParamValues("facet.field").length;
          final String[] facetFields = Arrays.copyOf(params.getParamValues("facet.field"), facetFieldLength + 2);
          facetFields[facetFieldLength] = "{!ex=" + parentsLabels + "}" + parentsLabels;
          facetFields[facetFieldLength + 1] = "{!ex=" + childrenLabels + "}" + childrenLabels;
          params.setParam("facet.field", facetFields);
        }
      } catch (final IOException e) {
        LOGGER.warn("Ignored ontology facets because of error: " + e.toString());
      }

      // perform query
      // define the request handler which may change if a specific source
      // has been provided
      String requestHandler = handler;
      if (request.getParameter("source") != null && !request.getParameter("source").isEmpty()
          && !request.getParameter("source").equalsIgnoreCase("all")) {
        requestHandler += "-" + request.getParameter("source");
      }
      params.removeParam("source");
      params.setRequestHandler(handler);
      queryResponse = solr.executeQuery(params);
      if (promolinkCore != null && !params.getParamValue("q").toString().equals("*:*")) { // launch
        // a
        // request
        // in
        // the
        // promolink
        // core
        // only
        // if
        // it's
        // a
        // request
        // onZ
        // the
        // FileShare
        // core

        queryPromolink.setQuery(params.getParamValue("q"));
        queryPromolink.setFilterQueries("-dateBeginning:[NOW/DAY+1DAY TO *]", "-dateEnd:[* TO NOW/DAY]");
        queryResponsePromolink = promolinkCore.executeQuery(queryPromolink);
      }
      switch (handler) {
      case "/select":
        // If there is no id there is no need to record stats
        if (params.getParamValue("id") != null && !params.getParamValue("id").equals("")) {
          // index
          final long numFound = queryResponse.getNumFound();
          final int QTime = queryResponse.getQTime();
          final IndexerQuery statsParams = IndexerQueryManager.createQuery();
          statsParams.addParams(params.getParams());
          statsParams.setParam("numFound", Long.toString(numFound));
          if (numFound == 0) {
            statsParams.setParam("noHits", "1");
          }
          statsParams.setParam("QTime", Integer.toString(QTime));

          StatsPusher.pushQuery(statsParams, protocol);
        }
        break;
      case "/stats":
        solr.processStatsResponse(queryResponse);
        break;
      }

      if (promolinkCore != null) {
        writeSolrJResponse(request, response, params, queryResponse, queryPromolink, queryResponsePromolink);
      } else {
        writeSolrJResponse(request, response, params, queryResponse, null, null);
      }

    } catch (final Exception e) {
      // TODO fine handling of exception
      LOGGER.error("Unknown error " + e.getMessage());
    }

  }

  private void writeSolrJResponse(final HttpServletRequest request, final HttpServletResponse response,
      final IndexerQuery query, final IndexerQueryResponse queryResponse, final IndexerQuery queryPromolink,
      final IndexerQueryResponse queryResponsePromolink) throws IOException, JSONException, ParseException {

    if (queryResponsePromolink != null) { // If it was a request on
      // FileShare
      // therefore on promolink
      final String jsonStrQueryResponse = queryResponse.getStrJSONResponse();
      final JSONObject json = new JSONObject(jsonStrQueryResponse.substring(jsonStrQueryResponse.indexOf("{"))); // Creating
      // a
      // valid
      // json
      // object
      // from
      // the
      // results

      // Write the result of the query on
      // promolink
      final String jsonStrPromolinkResponse = queryResponsePromolink.getStrJSONResponse();

      if (queryResponsePromolink.getNumFound() != 0) { // If
        // there
        // are
        // a
        // result
        // for
        // the
        // promolink

        final JSONObject promoResponseJSON = new JSONObject();
        final JSONArray jsonPromolinkDocs = queryResponsePromolink.getResults();
        final JSONObject jsonPromolinkDoc = jsonPromolinkDocs.getJSONObject(0);
        final String[] fieldNames = JSONObject.getNames(jsonPromolinkDoc);
        for (int i = 0; i < fieldNames.length; i++) {
          final String fieldName = fieldNames[i];
          promoResponseJSON.put(fieldName, jsonPromolinkDoc.get(fieldName));
        }

        // Taking
        // just
        // the
        // results
        // without
        // the
        // header

        json.put("promolinkSearchComponent", promoResponseJSON);

      }
      final String wrapperFunction = request.getParameter("json.wrf");
      final String finalString = wrapperFunction + "(" + json.toString() + ")";
      response.setStatus(200);
      response.setCharacterEncoding("utf-8");
      response.setContentType("text/json;charset=utf-8");
      response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
      response.getWriter().write(finalString); // Send the answer to the
      // jsp page

    } else {
      response.setStatus(200);
      response.setCharacterEncoding("utf-8");
      response.setContentType("text/json;charset=utf-8");
      response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
      response.getWriter().write(queryResponse.getStrJSONResponse());

    }
  }

  private String getHandler(final HttpServletRequest servletRequest) {
    final String pathInfo = servletRequest.getPathInfo();
    return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
  }

}