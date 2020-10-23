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
package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.RelevancySetupConfiguration;
import com.francelabs.datafari.utils.relevancy.RelevancySetup;

@WebServlet("/SearchExpert/queryRelevancy")
public class QueryRelevancy extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = LogManager.getLogger(QueryRelevancy.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public QueryRelevancy() {
    super();
  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    try {
      final RelevancySetup rs = RelevancySetupConfiguration.getInstance().getRelevancySetup();
      final String queryParam = request.getParameter("query");
      final List<String> relevantDocsList = rs.getRelevantDocsList(queryParam);
      jsonResponse.put("relevantDocsList", relevantDocsList);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      LOGGER.error("Error on marshal/unmarshal elevate.xml file ", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final JSONObject jsonResponse = new JSONObject();

    // Retrieve the relevancy setup
    final RelevancySetup rs = RelevancySetupConfiguration.getInstance().getRelevancySetup();

    if (request.getParameter("action") != null && request.getParameter("action").equals("create")) {
      // Retrieve the query name if available
      String queryName = "Query-";
      if (request.getParameter("name") != null && !request.getParameter("name").isEmpty()) {
        queryName += request.getParameter("name");
      } else {
        queryName += "created";
      }

      // Retrieve the query used for the search
      final String queryReq = request.getParameter("query");

      // Create a new Relevancy Query entry
      rs.newQuery(queryName, queryReq);

      // Set the response code
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

    } else if (request.getParameter("action") != null && request.getParameter("action").equals("save")) {
      // Save the new setup
      rs.saveSetup();

      // Set the response code
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    } else if (request.getParameter("action") != null && !request.getParameter("action").isEmpty() && request.getParameter("query") != null && !request.getParameter("query").equals("")) {
      // Retrieve the query used for the search
      final String queryReq = request.getParameter("query");

      // Retrieve the docId and the action to
      // perform (elevate or remove from elevate)
      final String docId = request.getParameter("item");
      final String action = request.getParameter("action");

      if (action.equals("add")) { // Add the relevant doc to the corresponding
                                  // query

        rs.addRelevantDoc(docId);

        // Set the response code
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      } else if (action.equals("remove")) { // Remove the relevant doc from
                                            // the corresponding query

        rs.removeRelevantDoc(docId);

        // Set the response code
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      }
    } else if (request.getParameter("action") != null && request.getParameter("action").equals("save")) {
      rs.saveSetup();

      // Set the response code
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
