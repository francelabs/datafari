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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.db.DocumentDataService;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;

@WebServlet("/SearchExpert/queryEvaluator")
public class QueryEvaluator extends HttpServlet {
  private final String server = Core.FILESHARE.toString();
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = Logger.getLogger(QueryEvaluator.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public QueryEvaluator() {
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
      String[] docIDs = request.getParameterValues("docids[]");
      if (docIDs == null) {
        docIDs = request.getParameterValues("docids");
      }
      final String query = request.getParameter("query");

      // TODO : should use a "real" service layer
      final Map<String, Integer> rankedDocuments = DocumentDataService.getInstance().getRank(query, docIDs);
      jsonResponse.put("docs", rankedDocuments);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      // }
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
    if (request.getParameter("query") != null && !request.getParameter("query").equals("")) {
      try {
        // Retrieve the query used for the search
        final String queryReq = request.getParameter("query");

        // Retrieve the docId and the action to
        // perform (elevate or remove from elevate)
        final String docId = request.getParameter("item");
        final String rank = request.getParameter("rank");

        // TODO : should use a "real" service layer
        synchronized (this) {
          if (rank != null) {
            DocumentDataService.getInstance().deleteRank(queryReq, docId);
            DocumentDataService.getInstance().addRank(queryReq, docId, Integer.parseInt(rank));

          } else {
            DocumentDataService.getInstance().deleteRank(queryReq, docId);
          }
        }

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        LOGGER.error("Error on marshal/unmarshal elevate.xml file in solr/solrcloud/" + server + "/conf", e);
      }
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
