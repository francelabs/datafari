package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.elk.ActivateELK;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.ELKConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Servlet implementation class getAllUsersAndRolesApache
 */
@WebServlet("/SearchAdministrator/changePasswordKibana")
public class ChangePasswordKibana extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ChangePasswordKibana.class.getName());
  private static String elasticsearchPath;
  private static String kibanaPath;
  private static String toolsPath;
  private static File internalUsersFile;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ChangePasswordKibana() {
    super();
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    elasticsearchPath = environnement + "/elk/elasticsearch/";
    kibanaPath = environnement + "/elk/kibana/";
    toolsPath = elasticsearchPath + "plugins/opendistro_security/tools/";
    internalUsersFile = new File(elasticsearchPath + "plugins/opendistro_security/securityconfig/internal_users.yml");
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final List<String> usersList = new ArrayList<String>();
    usersList.add("admin");
    usersList.add("searchexpert");
    usersList.add("searchadmin");
    if (usersList != null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("users", usersList);
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Datafari isn't connected to Cassandra");
      logger.error("Datafari isn't connected to Cassandra");
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter("username") != null && request.getParameter("password") != null) {
      final String username = request.getParameter("username");
      final String password = request.getParameter("password");
      try {

        // Prepare the process that will hash the password
        final ProcessBuilder hashProcessBuilder = new ProcessBuilder();
        hashProcessBuilder.command(toolsPath + "hash.sh", "-p", password);
        hashProcessBuilder.redirectErrorStream(true);
        final Map<String, String> hashEnvironment = hashProcessBuilder.environment();
        final List<String> varToRemove = new ArrayList<String>();
        for (final String key : hashEnvironment.keySet()) {
          // The PATH, the JAVA_HOME and the LANG variables are mandatory for the opendistro script, the others might introduce bugs so we remove them from the environment of the process
          if (!key.contentEquals("PATH") && !key.contentEquals("JAVA_HOME") && !key.contentEquals("LANG")) {
            varToRemove.add(key);
          }
        }
        for (final String toRemove : varToRemove) {
          hashEnvironment.remove(toRemove);
        }

        // Prepare the process that will apply new password
        final ProcessBuilder securityAdminProcessBuilder = new ProcessBuilder();
        securityAdminProcessBuilder.command(elasticsearchPath + "securityadmin_datafari.sh");
        securityAdminProcessBuilder.redirectErrorStream(true);
        final Map<String, String> securityAdminEnvironment = securityAdminProcessBuilder.environment();
        for (final String toRemove : varToRemove) {
          securityAdminEnvironment.remove(toRemove);
        }

        // Start the hash password process and retrieve the hashed password
        final Process hashProcess = hashProcessBuilder.start();
        final StringWriter hashProccessStringWriter = new StringWriter();
        IOUtils.copy(hashProcess.getInputStream(), hashProccessStringWriter, StandardCharsets.UTF_8);
        final int hashExitCode = hashProcess.waitFor();
        final String hashedPasswordOutput = hashProccessStringWriter.toString().trim();
        hashProccessStringWriter.close();
        // If the hash process ended with no errors, then set the hashed password to the config file
        if (hashExitCode == 0) {

          // Replace in the internal users file
          final StringBuffer newConfElastic = new StringBuffer();
          BufferedReader br = new BufferedReader(new FileReader(internalUsersFile));

          String line = br.readLine();
          boolean modifyNextline = false;
          while (line != null) {
            if (modifyNextline) {
              newConfElastic.append("  hash: \"" + hashedPasswordOutput + "\"");
              newConfElastic.append("\n");
              modifyNextline = false;
            } else {
              if (line.startsWith(username)) {
                modifyNextline = true;
              }
              newConfElastic.append(line);
              newConfElastic.append("\n");
            }

            line = br.readLine();
          }
          br.close();
          FileOutputStream fos = new FileOutputStream(internalUsersFile);
          fos.write(newConfElastic.toString().getBytes());
          fos.close();

          // If we change the password of the admin user, then we need to also replace the password in the Kibana config file
          if (username.contentEquals("admin")) {
            final String kibanaConfig = kibanaPath + "config/kibana.yml";
            final StringBuffer newConfKibana = new StringBuffer();
            br = new BufferedReader(new FileReader(kibanaConfig));

            line = br.readLine();
            while (line != null) {
              if (line.startsWith("elasticsearch.password:")) {
                newConfKibana.append("elasticsearch.password: " + password);
              } else {
                newConfKibana.append(line);
              }
              newConfKibana.append("\n");

              line = br.readLine();
            }
            br.close();
            fos = new FileOutputStream(kibanaConfig);
            fos.write(newConfKibana.toString().getBytes());
            fos.close();

            // Kibana must be stopped before applying the new admin password
            stopKibana();
            // Wait 5 seconds that Kibana stops
            Thread.sleep(5000);
          }

          // Apply new pasword
          final Process securityAdminProcess = securityAdminProcessBuilder.start();
          final StringWriter securityAdminStringWriter = new StringWriter();
          IOUtils.copy(securityAdminProcess.getInputStream(), securityAdminStringWriter, StandardCharsets.UTF_8);
          final int securityAdminExitCode = securityAdminProcess.waitFor();
          final String securityAdminOut = securityAdminStringWriter.toString().trim();
          securityAdminStringWriter.close();

          // If user admin then Kibana must be restarted
          if (username.contentEquals("admin")) {
            startKibana();
          }
          if (securityAdminExitCode != 0) {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, securityAdminOut);
            logger.error(securityAdminOut);
          } else {
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put(OutputConstants.STATUS, username + " password changed with success");
          }
        } else {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, hashedPasswordOutput);
          logger.error(hashedPasswordOutput);
        }

      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem to change the password");
        logger.error("Impossible to change the password", e);
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Problem with query");
      logger.error("Problem with query, some parameters are empty or missing: " + request.getQueryString());
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  private void stopKibana() {
    final String externalELK = ELKConfiguration.getInstance().getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF);
    if (externalELK.contentEquals("true")) {
      final String elkServer = ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SERVER);
      final String elkScriptDir = ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SCRIPTS_DIR);
      ActivateELK.getInstance().deactivateKibanaRemote(elkServer, elkScriptDir);
    } else {
      ActivateELK.getInstance().deactivateKibana();
    }
  }

  private void startKibana() {
    final String externalELK = ELKConfiguration.getInstance().getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF);
    if (externalELK.contentEquals("true")) {
      final String elkServer = ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SERVER);
      final String elkScriptDir = ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SCRIPTS_DIR);
      ActivateELK.getInstance().activateKibanaRemote(elkServer, elkScriptDir);
    } else {
      ActivateELK.getInstance().activateKibana();
    }
  }
}
