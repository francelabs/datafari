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
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.francelabs.datafari.service.db.StatisticsDataService.UserActions;
import com.francelabs.datafari.statistics.StatsPusher;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class Search
 */
@WebServlet("/Preview")
public class Preview extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final String redirectUrl = "previewUI.jsp";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public Preview() {
    super();

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
    final String docID = request.getParameter("docId");
    final String queryId = request.getParameter("id");
    final String action = request.getParameter("action");
    String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (authenticatedUserName == null) {
      authenticatedUserName = "";
    }

    if (action != null && action.equals("PREVIEW_CHANGE_DOC")) {
      // Changing doc using previous / next buttons in the preview interface
      final JSONObject parameters = new JSONObject();
      final String docPosParam = request.getParameter("docPos");
      int docPos = -1;
      try {
        docPos = Integer.parseInt(docPosParam);
      } catch (final Exception e) {

      }
      parameters.put("dest_doc_id", docID);
      parameters.put("dest_rank", docPos);
      StatsPusher.pushUserAction(queryId, authenticatedUserName, UserActions.PREVIEW_CHANGE_DOC, parameters, new Date().toInstant());
    } else if (queryId == null) {
      // The preview is loaded from a shared link
      final JSONObject parameters = new JSONObject();
      parameters.put("doc_id", docID);
      StatsPusher.pushUserAction("00000000-0000-0000-0000-000000000000", authenticatedUserName, UserActions.OPEN_PREVIEW_SHARED, parameters, new Date().toInstant());
    } else {
      // The preview is loaded from a results page
      final JSONObject parameters = new JSONObject();
      final String docPosParam = request.getParameter("docPos");
      int docPos = -1;
      try {
        docPos = Integer.parseInt(docPosParam);
      } catch (final Exception e) {

      }
      parameters.put("doc_id", docID);
      parameters.put("rank", docPos);
      StatsPusher.pushUserAction(queryId, authenticatedUserName, UserActions.OPEN_PREVIEW, parameters, new Date().toInstant());
    }

    rd.forward(request, response);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final RequestDispatcher rd = request.getRequestDispatcher(redirectUrl);
    rd.forward(request, response);
  }

}
