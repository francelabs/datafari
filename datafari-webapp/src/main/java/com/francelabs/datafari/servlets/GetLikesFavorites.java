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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.user.Like;

/**
 * Servlet implementation class GetLikesFavorites
 */
@WebServlet("/getLikesFavorites")
public class GetLikesFavorites extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetLikesFavorites.class.getName());
  private static final String FAVORITESLIST = "favoritesList";
  private static final String LIKESLIST = "likesList";

  private static final String DOCUMENTSID = "documentsID";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetLikesFavorites() {
    super();
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
    final String[] documentIDs = request.getParameterValues(DOCUMENTSID);

    if (request.getUserPrincipal() != null) {
      final String username = request.getUserPrincipal().getName();
      try {
        jsonResponse.put(FAVORITESLIST, Favorite.getFavorites(username, documentIDs));

        jsonResponse.put(LIKESLIST, Like.getLikes(username, documentIDs));

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      } catch (final DatafariServerException e) {
        jsonResponse.put(OutputConstants.CODE, e.getErrorCode().getValue());
        jsonResponse.put(OutputConstants.STATUS, "Not connected yet");
        logger.error("Error getting likes and favorites", e);
      }
    } else {
      jsonResponse.put("code", CodesReturned.NOTCONNECTED.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Not connected yet");
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
