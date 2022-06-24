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
import com.francelabs.datafari.utils.Environment;

/**
 *
 */
@WebServlet("/admin/MCFUISimplified/Db")
public class MCFUISimplifiedDb extends HttpServlet {

  private static final long serialVersionUID = -6561976993995634818L;

  private String env;

  private final static Logger LOGGER = LogManager.getLogger(MCFUISimplifiedDb.class);

  /**
   * @see HttpServlet#HttpServlet() Gets the environment path of Datafari installation
   */
  public MCFUISimplifiedDb() {
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
      final String dbType = request.getParameter("dbType");
      final String dbHost = request.getParameter("dbHost");
      final String dbName = request.getParameter("dbName");
      final String dbConnStr = request.getParameter("dbConnStr");
      final String dbUsername = request.getParameter("dbUsername");
      final String dbPassword = request.getParameter("dbPassword");
      final String dbSeeding = request.getParameter("dbSeeding");
      final String dbVersion = request.getParameter("dbVersion");
      final String dbAccessToken = request.getParameter("dbAccessToken");
      final String dbData = request.getParameter("dbData");
      final String sourcename = request.getParameter("dbSourcename");
      final String reponame = request.getParameter("dbReponame");
      final String security = request.getParameter("dbSecurity");
      final String startJob = request.getParameter("dbStartJob");

      // Checking if the reponame is valid (alphanumerical and undescores only)
      final Pattern repoNamePattern = Pattern.compile("^\\w+$");
      final Matcher repoNameMatcher = repoNamePattern.matcher(reponame);
      if (repoNameMatcher.find()) {
        if (!repoNameMatcher.group().contentEquals(reponame)) {
          jsonResponse.put("OK", "OK");
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "The repository name is not valid");
          LOGGER.error("The repository name is not valid");
        } else {
          createDbRepo(jsonResponse, dbType, dbHost, dbName, dbConnStr, dbUsername, dbPassword, dbSeeding, dbVersion, dbAccessToken, dbData, reponame, sourcename, security, startJob);
        }
      } else {
        jsonResponse.put("OK", "OK");
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "The repository name is not valid");
        LOGGER.error("The repository name is not valid");
      }

    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, e.getMessage());
      LOGGER.error("Unknown error during process", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  private void createDbRepo(final JSONObject jsonResponse, final String dbType, final String dbHost, final String dbName, final String dbConnStr, final String dbUsername, final String dbPassword, final String dbSeeding, final String dbVersion,
      final String dbAccessToken, final String dbData, final String reponame, final String sourcename, final String security, final String startJob) throws Exception {
    // Create dbRepository
    // final DbRepository dbRepo = new DbRepository();
    // dbRepo.setServer(server);
    // dbRepo.setUser(user);
    // dbRepo.setPassword(password);
    // dbRepo.setReponame(reponame);
    // final String filerRepoName = DbRepoConfig.getInstance().createRepoConnection(dbRepo);
    //
    // if (filerRepoName == null) {
    // jsonResponse.put("OK", "OK");
    // jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    // LOGGER.error("Cannot create Share Repository Connection");
    //
    // } else {
    //
    // // Create filerJob
    // final DbJob filerJob = new DbJob();
    // filerJob.setRepositoryConnection(filerRepoName);
    // filerJob.setPaths(paths);
    // filerJob.setSourcename(sourcename);
    // if (security != null) {
    // filerJob.setSecurity(true);
    // }
    // final String jobId = FilerJobConfig.getInstance().createJob(filerJob);
    //
    // if (jobId != null) {
    // if (startJob != null) {
    // ManifoldAPI.startJob(jobId);
    // }
    // jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    // jsonResponse.put("job_id", jobId);
    // } else {
    // jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    // LOGGER.error("Cannot create Filer job");
    // }
    // jsonResponse.put("OK", "OK");
    // }
  }
}
