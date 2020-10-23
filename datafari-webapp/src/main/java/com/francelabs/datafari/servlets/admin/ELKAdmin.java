package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.elk.ActivateELK;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.ELKConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchExpert/ELKAdmin")
public class ELKAdmin extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ELKAdmin.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ELKAdmin() {
    super();
  }

  /**
   * Check if the provided URL exists or return an error code
   *
   * @param urlString
   *          the URL to check
   * @return true if URL exists, false otherwise
   * @throws MalformedURLException
   * @throws IOException
   */
  private boolean isURLUp(final String urlString) {
    try {
      final URL u = new URL(urlString);
      if (u.getProtocol().toLowerCase().equals("https")) {
        // If protocol is https ensure the trustStore is correctly set
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
          System.setProperty("javax.net.ssl.trustStore", System.getenv("TRUSTSTORE_PATH"));
          System.setProperty("javax.net.ssl.trustStorePassword", System.getenv("TRUSTSTORE_PASSWORD"));
        }

        final HttpsURLConnection huc = (HttpsURLConnection) u.openConnection();
        // Deactivate the hostname verification
        huc.setHostnameVerifier(new HostnameVerifier() {

          @Override
          public boolean verify(final String hostname, final SSLSession session) {
            return true;
          }
        });
        huc.setRequestMethod("HEAD");
        huc.setConnectTimeout(1000);
        huc.connect();
        huc.getResponseCode();
        return true;
      } else {
        final HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        huc.setRequestMethod("HEAD");
        huc.setConnectTimeout(1000);
        huc.connect();
        huc.getResponseCode();
        return true;
      }
    } catch (final Exception e) {
      logger.error("Unable to connect to kibana", e);
      return false;
    }

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    jsonResponse.put(ELKConfiguration.KIBANA_URI, ELKConfiguration.getInstance().getProperty(ELKConfiguration.KIBANA_URI));
    jsonResponse.put(ELKConfiguration.EXTERNAL_ELK_ON_OFF, ELKConfiguration.getInstance().getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF));
    jsonResponse.put(ELKConfiguration.ELK_SERVER, ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SERVER));
    jsonResponse.put(ELKConfiguration.ELK_SCRIPTS_DIR, ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_SCRIPTS_DIR));
    final boolean activated = Boolean.parseBoolean(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_ACTIVATION));
    final boolean urlUp = isURLUp(ELKConfiguration.getInstance().getProperty(ELKConfiguration.KIBANA_URI));

    if (activated) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(ELKConfiguration.ELK_ACTIVATION, "true");
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(ELKConfiguration.ELK_ACTIVATION, "false");
    }
    if (urlUp) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("isELKUp", "true");
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("isELKUp", "false");
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    req.setCharacterEncoding("utf8");
    resp.setContentType("application/json");
    if (req.getParameter(ELKConfiguration.ELK_ACTIVATION) == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Query Malformed");
      logger.error("Query Malformed, some parameters are empty or missing: " + req.getQueryString());
    } else {
      String elkActivation = req.getParameter(ELKConfiguration.ELK_ACTIVATION);

      try {
        if (elkActivation.equals("true")) {
          if (req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null && req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).toString().equals("true")) {
            if (req.getParameter(ELKConfiguration.ELK_SERVER) != null && req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) != null) {
              ActivateELK.getInstance().activateRemote(req.getParameter(ELKConfiguration.ELK_SERVER), req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR));
            } else {
              logger.warn("Unable to activate ELK : wrong parameters");
              elkActivation = "false";
            }
          } else {
            ActivateELK.getInstance().activate();
          }
        } else {
          if (req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null && req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).toString().equals("true")) {
            if (req.getParameter(ELKConfiguration.ELK_SERVER) != null && req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) != null) {
              ActivateELK.getInstance().deactivateRemote(req.getParameter(ELKConfiguration.ELK_SERVER), req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR));
            } else {
              logger.warn("Unable to unactivate ELK : wrong parameters");
              elkActivation = "true";
            }
          } else {
            ActivateELK.getInstance().deactivate();
          }
        }
        try {
          ELKConfiguration.getInstance().setProperty(ELKConfiguration.ELK_ACTIVATION, elkActivation);
          ELKConfiguration.getInstance().saveProperties();
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
          jsonResponse.put(ELKConfiguration.ELK_ACTIVATION, elkActivation);
        } catch (final IOException e) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          logger.error("Unable to save ELK properties", e);
        }

      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        logger.error("Fatal Error", e);
      }
    }
    final PrintWriter out = resp.getWriter();
    out.print(jsonResponse);
  }

}