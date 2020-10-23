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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.external.search.api.DropboxSource;
import com.francelabs.datafari.service.db.AccessTokenDataService;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/ExternalSources")
public class ExternalSources extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(ExternalSources.class.getName());

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getUserPrincipal() != null) {
      final String username = request.getUserPrincipal().getName();
      try {
        final JSONArray sourcesTokens = AccessTokenDataService.getInstance().getTokens(username);
        jsonResponse.put("code", CodesReturned.ALLOK.getValue());
        jsonResponse.put("sourcesTokens", sourcesTokens);
      } catch (final Exception e) {
        jsonResponse.put("code", CodesReturned.GENERALERROR.getValue());
        jsonResponse.put("status", e.getMessage());
      }
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getUserPrincipal() != null) {
      final String username = request.getUserPrincipal().getName();
      final String source = request.getParameter("source");
      final String action = request.getParameter("action");

      if (source != null && !source.isEmpty()) {
        if (source.equals("dropbox")) {
          final String clientIdentifier = request.getParameter("clientIdentifier");
          final String accessToken = request.getParameter("access-token");
          if (clientIdentifier != null && !clientIdentifier.isEmpty() && accessToken != null && !accessToken.isEmpty()) {
            if (DropboxSource.validAccessToken(clientIdentifier, accessToken)) {
              try {
                if (action != null) {
                  if (action.equals("add")) {
                    AccessTokenDataService.getInstance().setToken(username, source, clientIdentifier, accessToken);
                  } else if (action.equals("update")) {
                    AccessTokenDataService.getInstance().updateToken(username, source, clientIdentifier, accessToken);
                  } else if (action.equals("delete")) {
                    AccessTokenDataService.getInstance().deleteToken(username, source, clientIdentifier);
                  }
                  jsonResponse.put("code", CodesReturned.ALLOK.getValue());
                }
              } catch (final DatafariServerException e) {
                jsonResponse.put("code", CodesReturned.GENERALERROR.getValue());
                jsonResponse.put("status", e.getMessage());
              }
            } else {
              jsonResponse.put("code", CodesReturned.GENERALERROR.getValue());
              jsonResponse.put("status", "The provided token does not work, please check it !");
            }
          }
        }
      }
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

}