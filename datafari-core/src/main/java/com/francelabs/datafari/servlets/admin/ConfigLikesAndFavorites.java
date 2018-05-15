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
package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.startup.LikesLauncher;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class ConfigureLikesAndFacorites
 */
@WebServlet("/ConfigureLikesAndFavorites")
public class ConfigLikesAndFavorites extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(ConfigLikesAndFavorites.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ConfigLikesAndFavorites() {
    super();
    // TODO Auto-generated constructor stub
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
    BasicConfigurator.configure();
    final JSONObject jsonResponse = new JSONObject();
    if (request.getParameter("enable") != null) {
      final String enable = request.getParameter("enable");
      request.setCharacterEncoding("utf8");
      response.setContentType("application/json");
      boolean error = false;
      if (enable.equals("true")) {
        error = ScriptConfiguration.setProperty(StringsDatafariProperties.LIKESANDFAVORTES, "true");
        LikesLauncher.startScheduler();
      } else {
        error = ScriptConfiguration.setProperty(StringsDatafariProperties.LIKESANDFAVORTES, "false");
        LikesLauncher.shutDown();
      }

      if (error) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      } else {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      }

    } else if (request.getParameter("initiate") != null) {
      final String isEnabled = ScriptConfiguration.getProperty(StringsDatafariProperties.LIKESANDFAVORTES);

      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("isEnabled", isEnabled);

    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
