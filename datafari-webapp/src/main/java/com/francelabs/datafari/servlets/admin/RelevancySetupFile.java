/*******************************************************************************
 *  * Copyright 2016 France Labs
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

package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//<<<<<<< 687d5d61d2619e10e69db6ae2eeecb34211afb1e
import org.json.simple.JSONObject;
/* =======
import org.json.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
>>>>>>> Relevancy setup files editing threw UI */
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.RelevancySetupConfiguration;

@WebServlet("/admin/relevancySetupFile")
public class RelevancySetupFile extends HttpServlet {

  private static final long serialVersionUID = -6561976993995634818L;

  private final static Logger LOGGER = LogManager.getLogger(RelevancySetupFile.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RelevancySetupFile() {

  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");

    final String action = request.getParameter("action");

    if (action != null && action.equals("getRelevancySetup")) {
      response.setContentType("application/json");
      final JSONObject result = RelevancySetupConfiguration.getInstance().getRelevancySetup().getParametersJSONObject();
      final PrintWriter out = response.getWriter();
      out.print(result);
      return;
    }

    response.setContentType("application/json");
    jsonResponse.put("relevancySetupFilePath", RelevancySetupConfiguration.getInstance().getRelevancySetupFilePath());
    jsonResponse.put("goldenQueriesSetupFilePath", RelevancySetupConfiguration.getInstance().getGoldenQueriesFilePath());
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    try {

      final String action = request.getParameter("action");

      if (action != null && !action.trim().isEmpty()) {

        final String relevancySetupFile = request.getParameter("relevancySetupFile");
        final String goldenQueriesSetupFile = request.getParameter("goldenQueriesSetupFile");

        if (relevancySetupFile != null && !relevancySetupFile.isEmpty() && goldenQueriesSetupFile != null && !goldenQueriesSetupFile.isEmpty() && action.trim().equalsIgnoreCase("save")) {
          RelevancySetupConfiguration.getInstance().changeFilePath(relevancySetupFile, goldenQueriesSetupFile);
        }
        if (action.trim().equalsIgnoreCase("saveconfig")) {
          try {
            final JSONParser parser = new JSONParser();
            final JSONObject configObject = (JSONObject) parser.parse(request.getParameter("params"));
            RelevancySetupConfiguration.getInstance().getRelevancySetup().updateParams(configObject);
            RelevancySetupConfiguration.getInstance().getRelevancySetup().saveSetup();
          } catch (final Exception e) {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Error parsing JSON request string");
            LOGGER.error("Error parsing JSON request string", e);
          }

        }
      }
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69253");
      LOGGER.error("Error in relevancySetupFile doPost. Error 69253", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
