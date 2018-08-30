package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONObject;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.user.UserConstants;
import com.francelabs.datafari.utils.ActiveDirectoryUtils;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/addUser")
public class AddUser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(AddUser.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public AddUser() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.LDAPCOLUMN).toString().equals("true")) {

      boolean userExists = false;
      // Check if user exists
      try {
        final String username = request.getParameter("username");

        // Get the LDAP configuration parameters
        final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);

        // Retrieve the LDAP context from the LDAP configuration
        // parameters
        final DirContext ctx = ActiveDirectoryUtils.getLdapContext(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL), h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));

        userExists = ActiveDirectoryUtils.checkUser(username, h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME), ctx);

        if (userExists) {
          final User user = new User(request.getParameter(UserConstants.USERNAMECOLUMN).toString(), "");
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
          }

        } else {

          jsonResponse.put(OutputConstants.CODE, CodesReturned.ADUSERNOTEXISTS.getValue());
          jsonResponse.put(OutputConstants.STATUS, "AD user does not exist");

        }
      } catch (ParserConfigurationException | SAXException | DOMException | ManifoldCFException e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with XML Manipulation");
        logger.error("Problem with XML Manipulation", e);
      } catch (final NamingException e) {

        jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Problem with AD connection");
        logger.error("Problem with AD connection", e);
      }
    } else if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.PASSWORDCOLUMN) != null && request.getParameter(UserDataService.ROLECOLUMN + "[]") != null) {
      final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(), request.getParameter(UserDataService.PASSWORDCOLUMN).toString());
      try {
        user.signup(Arrays.asList(request.getParameterValues(UserDataService.ROLECOLUMN + "[]")));
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, "User successfully added");
      } catch (final DatafariServerException e) {
        if (e.getErrorCode().equals(CodesReturned.USERALREADYINBASE)) {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.USERALREADYINBASE.getValue());
          jsonResponse.put(OutputConstants.STATUS, "User already Signed up");
          logger.error("User already Signed up", e);
        } else {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem with database");
          logger.error("Problem with database", e);
        }
      }
    } else {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue());
      jsonResponse.put("statut", "Problem with query");
      logger.error("Problem with query, some parameters are empty or missing: " + request.getQueryString());
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
