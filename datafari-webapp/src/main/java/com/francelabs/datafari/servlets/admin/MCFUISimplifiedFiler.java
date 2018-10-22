/*******************************************************************************
 *  * Copyright 2018 France Labs
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
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.simplifiedui.utils.FilerJob;
import com.francelabs.datafari.simplifiedui.utils.FilerJobConfig;
import com.francelabs.datafari.simplifiedui.utils.FilerRepoConfig;
import com.francelabs.datafari.simplifiedui.utils.FilerRepository;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

/**
 *
 */
@WebServlet("/admin/MCFUISimplified/Filer")
public class MCFUISimplifiedFiler extends HttpServlet {

  private static final long serialVersionUID = -6561976993995634818L;

  private String env;

  private final static Logger LOGGER = LogManager.getLogger(MCFUISimplifiedFiler.class);

  /**
   * @see HttpServlet#HttpServlet() Gets the environment path of Datafari
   *      installation
   */
  public MCFUISimplifiedFiler() {
    env = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (env == null) {
      // if no variable is set, use the default installation path
      env = "/opt/datafari";
    }
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response) It saves the MCF connections if action parameter is save It
   *      restores the MCF connections if action parameter is restore It uses the
   *      backup directory in input (if specified) or a default path
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    try {
      final String server = request.getParameter("server");
      final String user = request.getParameter("user");
      final String password = request.getParameter("password");
      final String paths = request.getParameter("paths");
      final String security = request.getParameter("security");
      final String startJob = request.getParameter("startJob");

      // Create webRepository
      final FilerRepository filerRepo = new FilerRepository();
      filerRepo.setServer(server);
      filerRepo.setUser(user);
      filerRepo.setPassword(password);
      final String filerRepoName = FilerRepoConfig.getInstance().createRepoConnection(filerRepo);

      if (filerRepoName == null) {
        jsonResponse.put("OK", "OK");
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        LOGGER.error("Cannot create Share Repository Connection");

      } else {

        // Create filerJob
        final FilerJob filerJob = new FilerJob();
        filerJob.setRepositoryConnection(filerRepoName);
        filerJob.setPaths(paths);
        if (security != null) {
          filerJob.setSecurity(true);
        }
        final String jobId = FilerJobConfig.getInstance().createJob(filerJob);

        if (jobId != null) {
          if (startJob != null) {
            ManifoldAPI.startJob(jobId);
          }
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
          jsonResponse.put("job_id", jobId);
        } else {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          LOGGER.error("Cannot create Filer job");
        }
        jsonResponse.put("OK", "OK");
      }

    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append(
          "Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69253");
      out.close();
      LOGGER.error("Unknown error during process", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
