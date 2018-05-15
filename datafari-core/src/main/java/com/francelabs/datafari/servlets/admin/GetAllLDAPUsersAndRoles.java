package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.ActiveDirectoryUtils;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/getAllLDAPUsersAndRoles")
public class GetAllLDAPUsersAndRoles extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(GetAllLDAPUsersAndRoles.class);
  private static boolean performingRefresh = false;
  private static List<String> currentUsersList = new ArrayList<>();

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetAllLDAPUsersAndRoles() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String getParam = request.getParameter("get");

    if (getParam.equals(OutputConstants.STATUS)) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, performingRefresh);
    } else if (getParam.equals("currentList")) {
      final Map<String, List<String>> usersRoles = User.getAllUsers();
      final Map<String, List<String>> ldapUsersRoles = new HashMap<>();
      for (final String ldapUser : currentUsersList) {
        final List<String> roles = new ArrayList<>();
        if (usersRoles.containsKey(ldapUser)) {
          roles.addAll(usersRoles.get(ldapUser));
        }
        ldapUsersRoles.put(ldapUser, roles);
      }

      // Write the LDAP users (& roles) list in the response in
      // JSON
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, ldapUsersRoles);
    } else {
      if (!performingRefresh) {
        performingRefresh = true;
        try {
          // Get the LDAP configuration parameters
          final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);
          // Retrueve the LDAP context from the LDAP configuration
          // parameters
          final LdapContext ctx = ActiveDirectoryUtils.getLdapContext(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL), h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));

          // Retrieve the LDAP users list
          final List<String> ldapUsersList = ActiveDirectoryUtils.listAllusers(ctx, h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME), Boolean.parseBoolean(h.get(RealmLdapConfiguration.ATTR_SUBTREE)));
          currentUsersList = ldapUsersList;
          // Close the context (never forget this)
          ctx.close();
          if (ldapUsersList.isEmpty()) { // No LDAP users found
            jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
            jsonResponse.put(OutputConstants.STATUS, "No users found !");
          } else { // Retrieve user roles for LDAP users (if possible)
            final Map<String, List<String>> usersRoles = User.getAllUsers();
            final Map<String, List<String>> ldapUsersRoles = new HashMap<>();
            for (final String ldapUser : ldapUsersList) {
              final List<String> roles = new ArrayList<>();
              if (usersRoles.containsKey(ldapUser)) {
                roles.addAll(usersRoles.get(ldapUser));
              }
              ldapUsersRoles.put(ldapUser, roles);
            }

            // Write the LDAP users (& roles) list in the response
            // in
            // JSON
            jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
            jsonResponse.put(OutputConstants.STATUS, ldapUsersRoles);
          }
        } catch (SAXException | ParserConfigurationException e) {
          performingRefresh = false;

          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem with XML Manipulation");
          logger.error(e);

        } catch (final NamingException e) {
          performingRefresh = false;

          jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem with AD connection");
          logger.error(e);

        } catch (final ManifoldCFException e) {
          performingRefresh = false;

          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put(OutputConstants.STATUS, "Problem with password obfuscation");
          logger.error(e);

        }
      } else {
        while (performingRefresh) {
          try {
            Thread.sleep(1000);
          } catch (final InterruptedException e) {
            logger.error(e);
          }
        }
        final Map<String, List<String>> usersRoles = User.getAllUsers();
        final Map<String, List<String>> ldapUsersRoles = new HashMap<>();
        for (final String ldapUser : currentUsersList) {
          final List<String> roles = new ArrayList<>();
          if (usersRoles.containsKey(ldapUser)) {
            roles.addAll(usersRoles.get(ldapUser));
          }
          ldapUsersRoles.put(ldapUser, roles);
        }

        // Write the LDAP users (& roles) list in the response in
        // JSON
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        jsonResponse.put(OutputConstants.STATUS, ldapUsersRoles);
      }
    }
    performingRefresh = false;
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}