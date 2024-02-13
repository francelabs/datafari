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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.francelabs.datafari.simplifiedui.utils.WebJob;
import com.francelabs.datafari.simplifiedui.utils.WebJobConfig;
import com.francelabs.datafari.simplifiedui.utils.WebRepoConfig;
import com.francelabs.datafari.simplifiedui.utils.WebRepository;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

/**
 *
 */
@WebServlet("/admin/MCFUISimplified/Web")
public class MCFUISimplifiedWeb extends HttpServlet {

  private static final long serialVersionUID = -6561976993995634818L;

  private String env;

  private final static Logger LOGGER = LogManager.getLogger(MCFUISimplifiedWeb.class);

  /**
   * @see HttpServlet#HttpServlet() Gets the environment path of Datafari installation
   */
  public MCFUISimplifiedWeb() {
    env = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (env == null) {
      // if no variable is set, use the default installation path
      env = "/opt/datafari";
    }
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response) It saves the MCF connections if action parameter is save It restores the MCF connections if action parameter is
   *      restore It uses the backup directory in input (if specified) or a default path
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    try {
      final String seeds = request.getParameter("seeds").replaceAll("\r", ""); // Clean the paths to remove windows CR char (end of line)
      final String email = request.getParameter("email");
      final String sourcename = request.getParameter("webSourcename");
      final String reponame = request.getParameter("webReponame");
      final String timezone = request.getParameter("webTimeZone");
      final String startJob = request.getParameter("startJobWeb");
      final String mode = request.getParameter("webMode");

      boolean duplicatesDetection = false;
      if (request.getParameter("webDuplicatesDetection") != null) {
        duplicatesDetection = true;
      }
      boolean createOCR = false;
      if (request.getParameter("webCreateOCR") != null) {
        createOCR = true;
      }
      final String tikaOCRHost = request.getParameter("webTikaOCRHost");
      final String tikaOCRPort = request.getParameter("webTikaOCRPort");
      final String tikaOCRName = request.getParameter("webTikaOCRName");

      // Create webRepository
      final WebRepository webRepo = new WebRepository();
      webRepo.setEmail(email);
      webRepo.setReponame(reponame);
      final String webRepoName = WebRepoConfig.getInstance().createRepoConnection(webRepo);

      if (webRepoName == null) {
        jsonResponse.put("OK", "OK");
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Cannot create Web Repository Connection");
        LOGGER.error("Cannot create Web Repository Connection");
      } else {
        // Checking if the reponame is valid (alphanumerical and undescores only)
        final Pattern repoNamePattern = Pattern.compile("^\\w+$");
        final Matcher repoNameMatcher = repoNamePattern.matcher(reponame);
        if (!repoNameMatcher.find() || !repoNameMatcher.group().contentEquals(reponame)) {
          jsonResponse.put("OK", "OK");
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "The repository name is not valid");
          LOGGER.error("The repository name is not valid");
        } else {
          // Create webJob
          final WebJob webJob = new WebJob();
          webJob.setRepositoryConnection(webRepoName);
          webJob.setSeeds(seeds);
          webJob.setSourcename(sourcename);
          webJob.setTimezone(timezone);
          webJob.setMode(mode);
          webJob.setDuplicatesDetection(duplicatesDetection);
          webJob.setCreateOCR(createOCR);
          webJob.setTikaOCRHost(tikaOCRHost);
          webJob.setTikaOCRPort(tikaOCRPort);
          webJob.setTikaOCRName(tikaOCRName);
          final String jobId = WebJobConfig.getInstance().createJob(webJob);

          if (jobId != null) {
            if (startJob != null) {
              ManifoldAPI.startJob(jobId);
            }
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put("job_id", jobId);
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "Cannot create Web job");
            LOGGER.error("Cannot create Web job");
          }
          jsonResponse.put("OK", "OK");
        }
      }
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, e.getMessage());
      LOGGER.error("Unknown error during process", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
