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

import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.WidgetManagerConfiguration;

/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/WidgetManager")
public class WidgetManager extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(WidgetManager.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public WidgetManager() {
    super();
    BasicConfigurator.configure();
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

    final WidgetManagerConfiguration conf = WidgetManagerConfiguration.getInstance();

    String activated = "false";

    final String id = request.getParameter("id");
    final String strActivated = request.getParameter("activated");
    if (id != null) {
      if (conf.getProperty(id) != null) {
        activated = conf.getProperty(id);
      } else {
        if (strActivated != null) {
          activated = strActivated;
        }
        conf.setProperty(id, activated);
        conf.saveProperties();
      }
    }

    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    jsonResponse.put("activated", activated);

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final WidgetManagerConfiguration conf = WidgetManagerConfiguration.getInstance();

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    final String id = request.getParameter("id");
    final String strActivated = request.getParameter("activated");

    if (id != null && strActivated != null) {
      conf.setProperty(id, strActivated);
      conf.saveProperties();
      jsonResponse.put("activated", strActivated);
    }

    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

}
