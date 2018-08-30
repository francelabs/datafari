package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.ldap.LDAPService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/isLdapConfig")
public class IsLdapConfig extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(IsLdapConfig.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public IsLdapConfig() {
    super();
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    try {
      // Test the connection
      final String connectionName = request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_NAME);
      final String connectionPassword = request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_PW);
      final String connectionURL = request.getParameter(RealmLdapConfiguration.ATTR_CONNECTION_URL);
      LDAPService.getInstance().testLDAPConnection(connectionURL, connectionName, connectionPassword);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put("isActivated", true);
    } catch (final NamingException e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Fail to connect to AD with the given settings");
      jsonResponse.put("isActivated", false);
      logger.error("Fail to connect to AD with the given settings", e);

    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
