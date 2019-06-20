/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.Favorite;

/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/deleteFavorite")
public class DeleteFavorite extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(DeleteFavorite.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DeleteFavorite() {
    super();
    BasicConfigurator.configure();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
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
    if (request.getParameter("idDocument") != null) {
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal == null) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.NOTCONNECTED.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Please reload the page, you're not connected");
      } else {
        final String username = request.getUserPrincipal().getName();
        try {
          Favorite.deleteFavorite(username, request.getParameter("idDocument"));
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        } catch (final DatafariServerException e) {
          jsonResponse.put(OutputConstants.CODE, e.getErrorCode().getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem while connecting to database");
          logger.error("Delete favorite error", e);
        }
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Query malformed");
      logger.error("Delete favorite error, query malformed: " + request.getQueryString());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
