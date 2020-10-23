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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.external.search.api.DropboxSource;
import com.francelabs.datafari.external.search.api.IExternalSource;
import com.francelabs.datafari.external.search.api.ManagedAPIs;
import com.francelabs.datafari.external.search.api.ResultDocumentList;
import com.francelabs.datafari.service.db.AccessTokenDataService;
import com.francelabs.datafari.service.db.AccessTokenDataService.AccessToken;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/ExternalSourceProxy/*")
public class ExternalSourceProxy extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(ExternalSourceProxy.class.getName());
  private static final int defaultRows = 10;

  private Set<String> getAllowedHandlers() {
    final Set<String> allowedHandlers = new HashSet<>();
    allowedHandlers.add(ManagedAPIs.DROPBOX);
    return allowedHandlers;
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    String user = "no_user";
    if (request.getUserPrincipal() != null) {
      user = request.getUserPrincipal().getName();
    }
    final String handler = getHandler(request);

    final Set<String> allowedHandlers = getAllowedHandlers();

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

    JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final Map<String, String[]> searchRequest = new HashMap<>(request.getParameterMap());
    searchRequest.remove("q");
    searchRequest.remove("rows");
    searchRequest.remove("start");
    final String query = request.getParameter("q");

    int rows = defaultRows;
    int start = 0;
    final JSONParser parser = new JSONParser();
    JSONObject parameters;
    try {
      parameters = (JSONObject) parser.parse(request.getParameter("parameters"));
    } catch (final ParseException e) {
      LOGGER.warn("Unable to parse provided parameters: " + request.getParameter("parameters"));
      parameters = new JSONObject();
    }

    if (parameters.containsKey("start")) {
      final long startL = (long) parameters.get("start");
      start = Math.toIntExact(startL);
    }

    if (parameters.containsKey("rows")) {
      final long rowsL = (long) parameters.get("rows");
      rows = Math.toIntExact(rowsL);
    }

    switch (handler) {
    case ManagedAPIs.DROPBOX:
      try {
        final AccessTokenDataService atds = AccessTokenDataService.getInstance();
        final AccessToken token = atds.getToken(user, ManagedAPIs.DROPBOX);
        if (token != null) {
          final IExternalSource dropboxSrc = new DropboxSource(token.getIdentifier(), token.getToken(), parameters);
          final ResultDocumentList rdl = dropboxSrc.executeQuery(query, start, rows, searchRequest);
          jsonResponse = rdl.getAsJSONResponse();
          jsonResponse.put("code", CodesReturned.ALLOK.getValue());
        } else {
          jsonResponse.put("code", CodesReturned.NOTCONNECTED.getValue());
          jsonResponse.put("status", "Current user is not allowed to send requests to this external source");
        }
      } catch (final Exception e) {
        jsonResponse.put("code", CodesReturned.GENERALERROR.getValue());
        jsonResponse.put("status", e.getMessage());
      }
      break;
    default:
      jsonResponse.put("handler", handler);
      jsonResponse.put("searchRequest", searchRequest);
      jsonResponse.put("parameters", parameters);
      break;
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

  private String getHandler(final HttpServletRequest servletRequest) {
    final String pathInfo = servletRequest.getPathInfo();
    return pathInfo.substring(pathInfo.lastIndexOf("/") + 1, pathInfo.length());
  }

}