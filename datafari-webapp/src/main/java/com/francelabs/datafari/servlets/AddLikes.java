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

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.Like;
import com.francelabs.datafari.utils.UpdateNbLikes;

/**
 * Servlet implementation class addLikes
 */
@WebServlet("/addLikes")
public class AddLikes extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(AddLikes.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public AddLikes() {
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
    // TODO Auto-generated method stub
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String documentId = request.getParameter("idDocument");
    if (documentId != null) {
      final Principal userPrincipal = request.getUserPrincipal();
      // checking if the user is connected
      if (userPrincipal == null) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.NOTCONNECTED.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Please reload the page, you'r not connected");
      } else {
        final String username = request.getUserPrincipal().getName(); // get
        // the
        // username
        try {
          Like.addLike(username, documentId);
          UpdateNbLikes.getInstance().increment(documentId);
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

        } catch (final DatafariServerException e) {
          jsonResponse.put(OutputConstants.CODE, e.getErrorCode().getValue());
          if (e.getErrorCode().equals(CodesReturned.PROBLEMCONNECTIONDATABASE)) {
            // if the like was already done (attempt to increase
            // illegaly the likes)
            jsonResponse.put(OutputConstants.STATUS, "Problem while connecting to database");
          }
          logger.error("Add like error", e);
        }
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR);
      jsonResponse.put(OutputConstants.STATUS, "Query malformed");
      logger.error("Add like error, no idDocument provided: " + request.getQueryString());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
