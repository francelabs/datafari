package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Servlet implementation class getAllUsersAndRolesApache
 */
@WebServlet("/SearchAdministrator/changePasswordApache")
public class ChangePasswordApache extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ChangePasswordApache.class.getName());
  private static String filePassword;
  private static final String realm = "datafari";
  private static String datafariHome;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ChangePasswordApache() {
    super();
    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    filePassword = environnement + "/apache/password/htpasswd";

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
        com.francelabs.datafari.utils.FileUtils.changePassApache(filePassword, username, realm, password);

        // stop apache
        final String[] command = { "/bin/bash", "-c", datafariHome + "/bin/monitorUtils/monit-stop-apache.sh" };
        final ProcessBuilder p = new ProcessBuilder(command);
        final Process p2 = p.start();

        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

        final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

        // read the output from the command
        String s = null;
        String errorCode = null;
        while ((s = stdInput.readLine()) != null) {
          logger.info(s);
        }

        // read any errors from the attempted command
        // TODO too verbose, display messages that are not errors
        while ((s = stdError.readLine()) != null) {
          logger.warn(s);
          errorCode = s;
        }

        // start apache
        final String[] commandStart = { "/bin/bash", "-c", datafariHome + "/bin/monitorUtils/monit-start-apache.sh" };
        final ProcessBuilder p3 = new ProcessBuilder(commandStart);
        final Process p4 = p3.start();

        final BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(p4.getInputStream()));

        final BufferedReader stdError2 = new BufferedReader(new InputStreamReader(p4.getErrorStream()));

        // read the output from the command
        String s2 = null;
        String errorCode2 = null;
        while ((s2 = stdInput2.readLine()) != null) {
          logger.info(s2);
        }

        // read any errors from the attempted command
        // TODO too verbose, display messages that are not errors
        while ((s2 = stdError2.readLine()) != null) {
          logger.warn(s);
          errorCode2 = s2;
        }

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "User password changed with success");
      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem to store the password in htpasswd file");
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
}
