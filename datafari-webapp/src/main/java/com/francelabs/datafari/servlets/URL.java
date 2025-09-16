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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.URLConnection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.UrlValidator;

/**
 * Servlet implementation class URL
 */
@WebServlet("/URL")
public class URL extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(URL.class.getName());

  private static final String redirectUrl = "/url.jsp";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public URL() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
        performGet(request, response);
  }

  public static void performGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    request.setCharacterEncoding("UTF-8");

    final Map<String, String[]> requestMap = new HashMap<>();
    requestMap.putAll(request.getParameterMap());
    if (requestMap.get("id") == null) {
      if (request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
        String id[] = { (String) request.getAttribute("id") };
        requestMap.put("id", id);
      } else {
        // Lets create a new ID else it will be registered under the "undefined" id
        // which we don't want
        String id[] = { UUID.randomUUID().toString() };
        requestMap.put("id", id);
      }
    }

    final IndexerQuery query = IndexerServerManager.createQuery();
    query.addParams(requestMap);

    try {

      // Add authentication
      String authenticatedUserName = AuthenticatedUserName.getName(request);
      if (authenticatedUserName != null) {
        query.setParam("AuthenticatedUserName", authenticatedUserName);
      }
    } catch (final Exception e) {
      logger.error("Unable to add AuthenticatedUserName to query", e);
    }

    final String action = query.getParamValue("action");
    if (action != null && action.equals("OPEN_FROM_PREVIEW")) {
      // TODO: Gather information and call pushUserAction
    } else {
      StatsPusher.pushDocument(query);
    }
 // URL validation
    final String rawUrl = request.getParameter("url");
    

    if (rawUrl == null || rawUrl.isBlank() || !UrlValidator.isAllowed(rawUrl)) {
      logger.warn("Blocked or missing 'url' param: {}", rawUrl);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST); // 400 -> /WEB-INF/jsp/error400.jsp
      return;
    }

    final RequestDispatcher rd = request.getRequestDispatcher(redirectUrl); // "/url.jsp"
    rd.forward(request, response);
  }
  
}
