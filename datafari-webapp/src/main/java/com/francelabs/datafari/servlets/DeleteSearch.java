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
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.user.SavedSearch;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/deleteSearch")
public class DeleteSearch extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(DeleteSearch.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DeleteSearch() {
    super();
    BasicConfigurator.configure();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    if (request.getParameter("request") != null) {
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal == null) {
        jsonResponse.put("code", CodesReturned.NOTCONNECTED);
        jsonResponse.put("statut", "Please reload the page, you're not connected");
      } else {
        final String username = request.getUserPrincipal().getName();
        final String requestName = request.getParameter("name");
        if (SavedSearch.deleteSearch(username, requestName, request.getParameter("request")) == CodesReturned.ALLOK
            .getValue()) {
          jsonResponse.put("code", CodesReturned.ALLOK);
          AuditLogUtil.log("postgresql", authenticatedUserName, request.getRemoteAddr(),
              "Deleted a saved search from " + username);
        } else {
          jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE);
          jsonResponse.put("statut", "Problem while connecting to database");
          logger.error("Delete search error: database problem");
        }
      }
    } else {
      jsonResponse.put("code", -1);
      jsonResponse.put("statut", "Query malformed");
      logger.error("Delete search error, query malformed: " + request.getQueryString());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
