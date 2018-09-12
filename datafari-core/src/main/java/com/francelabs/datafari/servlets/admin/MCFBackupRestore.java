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

import java.io.File;
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
import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript;

/**
 * This Servlet is used to save and restore the MCF connections from AdminUI It
 * is called by MCFBackupRestore.html DoGet is not used DoPost is used to save
 * and restore the MCF connections, given the action parameter (save or restore)
 *
 * @author Giovanni Usai
 */
@WebServlet("/admin/MCFBackupRestore")
public class MCFBackupRestore extends HttpServlet {

  private static final long serialVersionUID = -6561976993995634818L;

  private String env;

  private static final String DEFAULT_MCF_BACKUP_DIR = "/bin/backup/mcf";

  private final static Logger LOGGER = LogManager.getLogger(MCFBackupRestore.class);

  /**
   * @see HttpServlet#HttpServlet() Gets the environment path of Datafari
   *      installation
   */
  public MCFBackupRestore() {
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
   *      restores the MCF connections if action parameter is restore It uses
   *      the backup directory in input (if specified) or a default path
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final String action = request.getParameter("action");
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final PrintWriter out = response.getWriter();

    if (action != null && !action.trim().isEmpty()) {

      final String backupDirTmp = request.getParameter("backupDir");

      String backupDirectory;

      // Check if the backup dir has been given in input, otherwise take the
      // default path
      if (backupDirTmp != null && !backupDirTmp.trim().isEmpty()) {
        backupDirectory = backupDirTmp.trim();
      } else {
        backupDirectory = env + DEFAULT_MCF_BACKUP_DIR;
      }

      // Test directory
      final File dir = new File(backupDirectory);
      try {
        if (!dir.exists()) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "The provided directory does not exist");
        } else if (!dir.isDirectory()) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "The provided directory is not a directory");
        } else {
          if (action.trim().equalsIgnoreCase("save")) {
            if (dir.list().length != 0) {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
              jsonResponse.put(OutputConstants.STATUS, "The provided directory is not empty");
              LOGGER.warn("The provided directory is not empty : " + backupDirectory);
            } else {
              try {
                BackupManifoldCFConnectorsScript.doSave(backupDirectory);
                jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
              } catch (final Exception e) {
                jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
                jsonResponse.put(OutputConstants.STATUS, e.getMessage());
                LOGGER.error("Error during MCF config save", e);
              }
            }
          } else if (action.trim().equalsIgnoreCase("restore")) {
            if (dir.list().length == 0) {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
              jsonResponse.put(OutputConstants.STATUS, "The provided directory does not contain any file to restore");
            } else {
              try {
                BackupManifoldCFConnectorsScript.doRestore(backupDirectory);
                jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
              } catch (final Exception e) {
                jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
                jsonResponse.put(OutputConstants.STATUS, e.getMessage());
                LOGGER.error("Error during MCF config restore", e);
              }
            }
          }
        }
      } catch (final SecurityException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "The provided directory is not accessible");
        LOGGER.warn("The provided directory and/or subfiles is/are not accessible/readable :" + backupDirectory);
      }

    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "No action has been provided");
      LOGGER.warn("No action has been provided");
    }

    out.print(jsonResponse);

  }
}
