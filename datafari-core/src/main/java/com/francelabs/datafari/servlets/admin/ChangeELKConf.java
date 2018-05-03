package com.francelabs.datafari.servlets.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.ELKConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/changeELKConf")
public class ChangeELKConf extends HttpServlet {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -4261065947276998520L;
  private static final Logger logger = Logger.getLogger(ChangeELKConf.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ChangeELKConf() {
    super();
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    req.setCharacterEncoding("utf8");
    resp.setContentType("application/json");
    String datafari_home;
    datafari_home = Environment.getEnvironmentVariable("DATAFARI_HOME");
    // environment
    if (datafari_home == null) { // If in development environment
      datafari_home = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    final String kibana_conf_path = datafari_home + "/elk/kibana/config/kibana.yml";
    try {
      if (req.getParameter(ELKConfiguration.KIBANA_URI) == null || req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null && req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).equals("true")
          && (req.getParameter(ELKConfiguration.ELK_SERVER) == null || req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) == null)) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue()).put(OutputConstants.STATUS, "Query Malformed");
      } else {
        try {
          final ELKConfiguration elkConf = ELKConfiguration.getInstance();
          elkConf.setProperty(ELKConfiguration.KIBANA_URI, req.getParameter(ELKConfiguration.KIBANA_URI));
          elkConf.setProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF, req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF));
          elkConf.setProperty(ELKConfiguration.ELK_SERVER, req.getParameter(ELKConfiguration.ELK_SERVER));
          elkConf.setProperty(ELKConfiguration.ELK_SCRIPTS_DIR, req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR));
          elkConf.setProperty(ELKConfiguration.AUTH_USER, req.getParameter(ELKConfiguration.AUTH_USER));
          elkConf.saveProperties();

          final String kibana_url = ELKConfiguration.KIBANA_URI;
          String kibana_clean_url = null;
          List<String> list = new ArrayList<>();
          if (kibana_url != null && kibana_url.contains("http")) {
            final Pattern p = Pattern.compile("http://(.*?)/");
            final Matcher m = p.matcher(kibana_url);
            if (m.find()) {
              kibana_clean_url = m.group(1);
            }
            if (kibana_clean_url.contains(":")) {
              kibana_clean_url = kibana_clean_url.split(":")[0];
            }
            ELKConfiguration.getInstance().setProperty(ELKConfiguration.ELK_SERVER, kibana_clean_url);

            final File f = new File(kibana_conf_path);
            if (f.exists() && !f.isDirectory()) {
              try (BufferedReader br = Files.newBufferedReader(Paths.get(kibana_conf_path))) {
                list = br.lines().collect(Collectors.toList());
              } catch (final IOException e) {
                e.printStackTrace();
              }
              int index_position = -1;
              for (int i = 0; i < list.size(); i++) {
                if (list.get(i).startsWith("server.host")) {
                  System.out.println(list.get(i));
                  index_position = i;
                }
              }
              if (index_position != -1) {
                list.remove(index_position);
                list.add(index_position, "server.host: " + kibana_clean_url);
                Files.write(Paths.get(kibana_conf_path), list);

              }
            }
          }
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        } catch (final IOException e) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        }

      }
    } catch (final JSONException e) {
      logger.error("Error", e);
    }
    final PrintWriter out = resp.getWriter();
    out.print(jsonResponse);
  }

}