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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import com.francelabs.datafari.utils.ZKUtils;

@WebServlet("/SearchAdministrator/zookeeperConf")
public class ZooKeeperConf extends HttpServlet {
  private final String env;
  private static final String confname = "FileShare";
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = Logger.getLogger(ZooKeeperConf.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ZooKeeperConf() {
    super();

    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/solr/solrcloud/FileShare/conf";

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
    final String actionParam = request.getParameter("action");

    try {
      if (actionParam.toLowerCase().equals("download")) {
        ZKUtils.configZK("downloadconfigzk.sh", confname);
      } else if (actionParam.toLowerCase().equals("upload")) {
        ZKUtils.configZK("uploadconfigzk.sh", confname);
      } else if (actionParam.toLowerCase().equals("reload")) {
        ZKUtils.configZK("reloadCollections.sh", confname);
      } else if (actionParam.toLowerCase().equals("upload_and_reload")) {
        ZKUtils.configZK("uploadconfigzk.sh", confname);
        Thread.sleep(3000);
        ZKUtils.configZK("reloadCollections.sh", confname);
      }

      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    } catch (final IOException e) {
      LOGGER.error("Exception during action " + actionParam, e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    } catch (final InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
