package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.w3c.dom.DOMException;

import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.ldap.LdapUsers;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.user.UserConstants;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/addUser")
public class AddUser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(AddUser.class.getName());

  boolean keycloakEnabled = false;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public AddUser() {
    super();
    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties");) {
      final Properties appProps = new Properties();
      appProps.load(is);
      keycloakEnabled = Boolean.parseBoolean(appProps.getProperty("keycloak.enabled", "false"));
    } catch (final Exception e) {
      keycloakEnabled = false;
    }
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    boolean allOK = true;
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.IMPORTCOLUMN).toString().equals("true")) {

      boolean userExists = false;
      // Check if user exists
      try {
        String username = request.getParameter(UserConstants.USERNAMECOLUMN).toLowerCase();
        // Remove domain from username if any, as it is not considered in Cassandra
        if (username.contains("@")) {
          username = username.substring(0, username.indexOf("@"));
        }

        // If keycloak is enabled, as we cannot check if the user exists in Keycloak, no other choice to believe it does
        if (!keycloakEnabled) {
          final String userDomain = LdapUsers.getInstance().getUserDomain(username);
          if (userDomain != null && !userDomain.isEmpty()) {
            userExists = true;
          }
        } else {
          userExists = true;
        }

        if (userExists) {
          final User user = new User(username, "", true);
          try {
            user.signup(Arrays.asList(request.getParameterValues(UserDataService.ROLECOLUMN + "[]")));
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put(OutputConstants.STATUS, "User successfully added");
          } catch (final DatafariServerException e) {
            if (e.getErrorCode().equals(CodesReturned.USERALREADYINBASE)) {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.USERALREADYINBASE.getValue());
              jsonResponse.put(OutputConstants.STATUS, "User already Signed up");
            } else {
              jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
              jsonResponse.put(OutputConstants.STATUS, "Problem with database");
            }
            logger.error("Impossible to add user", e);
            allOK = false;
          }

        } else {

          jsonResponse.put(OutputConstants.CODE, CodesReturned.ADUSERNOTEXISTS.getValue());
          jsonResponse.put(OutputConstants.STATUS, "AD user does not exist");
          allOK = false;

        }
      } catch (final DOMException e) {
        logger.error(e);
        allOK = false;
      }
    } else if (hasValidUsernameAndPassword(request.getParameter(UserDataService.USERNAMECOLUMN), request.getParameter(UserDataService.PASSWORDCOLUMN))
            && request.getParameter(UserDataService.ROLECOLUMN + "[]") != null) {
      final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(), request.getParameter(UserDataService.PASSWORDCOLUMN).toString(), false);
      try {
        user.signup(Arrays.asList(request.getParameterValues(UserDataService.ROLECOLUMN + "[]")));
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "User successfully added");
      } catch (final DatafariServerException e) {
        if (e.getErrorCode().equals(CodesReturned.USERALREADYINBASE)) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.USERALREADYINBASE.getValue());
          jsonResponse.put(OutputConstants.STATUS, "User already Signed up");
        } else {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem with database");
        }
        logger.error("Impossible to add user", e);
        allOK = false;
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put("statut", "Problem with query");
      logger.error("Problem with query, some parameters are missing: " + request.getQueryString());
      allOK = false;
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    final String createdUser = request.getParameter(UserDataService.USERNAMECOLUMN) == null ? "null" : request.getParameter(UserDataService.USERNAMECOLUMN).toString();
    final String userRoles = request.getParameterValues(UserDataService.ROLECOLUMN + "[]") == null ? "null" : request.getParameterValues(UserDataService.ROLECOLUMN + "[]").toString();
    if (allOK) {
      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(), "Created user " + createdUser + " with roles " + userRoles);
    } else {

      AuditLogUtil.log("Cassandra", authenticatedUserName, request.getRemoteAddr(), "Error trying to create user " + createdUser + " with roles " + userRoles);
    }
  }

  boolean hasValidUsernameAndPassword(String username, String password) {
    return username != null
            && password != null
            && !StringUtils.hasUppercaseLetters(username);
  }

}
