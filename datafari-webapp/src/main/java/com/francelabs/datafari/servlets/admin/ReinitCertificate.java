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
import com.francelabs.datafari.utils.ExecutionEnvironment;

@WebServlet("/admin/ReinitCertificate")
public class ReinitCertificate extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private String env;
  private final static Logger LOGGER = LogManager.getLogger(ReinitCertificate.class.getName());

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    String environnement = System.getenv("DATAFARI_HOME");
    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/ssl-keystore/customerCertificates";

    try {

      final String scriptname = "reinitApacheCertificates.sh";
      final String scriptEnvironment = env + "/../apache/config";

      final String[] command = { "/bin/bash", "-c", "cd " + scriptEnvironment + " && bash " + scriptname };
      final ProcessBuilder p = new ProcessBuilder(command);
      final Process p2 = p.start();

      final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

      final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

      // read the output from the command
      String s = null;
      String errorCode = null;
      while ((s = stdInput.readLine()) != null) {
        LOGGER.info(s);
      }

      // read any errors from the attempted command
      // TODO too verbose, display messages that are not errors
      while ((s = stdError.readLine()) != null) {
        LOGGER.warn(s);
        errorCode = s;
      }

    } catch (final Exception e) {
      LOGGER.error("general exception");
      LOGGER.error(e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    }

    try {

      final String scriptname = "reinitApacheCertificates.sh";
      final String scriptEnvironment = env + "/../apache/config";

      final String[] command = { "/bin/bash", "-c", "cd " + scriptEnvironment + " && bash " + scriptname };
      final ProcessBuilder p = new ProcessBuilder(command);
      final Process p2 = p.start();

      final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

      final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

      // read the output from the command
      String s = null;
      String errorCode = null;
      while ((s = stdInput.readLine()) != null) {
        LOGGER.info(s);
      }

      // read any errors from the attempted command
      // TODO too verbose, display messages that are not errors
      while ((s = stdError.readLine()) != null) {
        LOGGER.warn(s);
        errorCode = s;
      }

    } catch (final Exception e) {
      LOGGER.error("general exception");
      LOGGER.error(e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    }

    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

    LOGGER.info(jsonResponse.toJSONString());
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

}
