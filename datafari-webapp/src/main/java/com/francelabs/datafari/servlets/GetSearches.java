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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.user.SavedSearch;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/GetSearches")
public class GetSearches extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetSearches.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetSearches() {
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
    final Principal userPrincipal = request.getUserPrincipal();
    // checking if the user is connected
    if (userPrincipal == null) {
      jsonResponse.put("code", CodesReturned.NOTCONNECTED);
      jsonResponse.put("statut", "Please reload the page, you're not connected");
    } else {
      final String username = userPrincipal.getName();
      final Map<String, String> searchesList = SavedSearch.getSearches(username);
      if (searchesList == null) {
        jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE);
        logger.error("Impossible to retrieve searches");
      } else {
        jsonResponse.put("code", CodesReturned.ALLOK);
        jsonResponse.put("searchesList", searchesList);
        AuditLogUtil.log("cassandra", authenticatedUserName, request.getRemoteAddr(),
            "Accessed all saved searches of user " + username);
      }
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
