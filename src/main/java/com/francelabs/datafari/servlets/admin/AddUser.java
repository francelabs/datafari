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

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.user.UserConstants;
import com.francelabs.datafari.utils.AcitveDirectoryUtils;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/addUser")
public class AddUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AddUser.class.getName());

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
		try {
			if (request.getParameter(UserDataService.USERNAMECOLUMN) != null && request.getParameter(UserDataService.PASSWORDCOLUMN) != null
					&& request.getParameter(UserDataService.ROLECOLUMN + "[]") != null) {
				final User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(),
						request.getParameter(UserDataService.PASSWORDCOLUMN).toString());
				final int code = user.signup(Arrays.asList(request.getParameterValues(UserDataService.ROLECOLUMN + "[]")));
				if (code == CodesReturned.ALLOK) {
					jsonResponse.put("code", CodesReturned.ALLOK).put("statut", "User successfully added");
				} else if (code == CodesReturned.USERALREADYINBASE) {
					jsonResponse.put("code", CodesReturned.USERALREADYINBASE).put("statut", "User already Signed up");
				} else {
					jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE).put("statut", "Problem with database");
				}
			} else if (request.getParameter(UserDataService.USERNAMECOLUMN) != null
					&& request.getParameter(UserDataService.LDAPCOLUMN).toString().equals("true")) {

				boolean userExists = false;
				// Check if user exists
				try {
					final String username = request.getParameter("username");

					// Get the LDAP configuration parameters
					final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);

					// Retrueve the LDAP context from the LDAP configuration
					// parameters
					final DirContext ctx = AcitveDirectoryUtils.getDirContext(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL),
							h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));

					userExists = AcitveDirectoryUtils.checkUser(username, h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME), ctx);

					if (userExists) {
						final User user = new User(request.getParameter(UserConstants.USERNAMECOLUMN).toString(), "");
						final int code = user.signup(Arrays.asList(request.getParameterValues(UserDataService.ROLECOLUMN + "[]")));
						if (code == CodesReturned.ALLOK) {
							jsonResponse.put("code", CodesReturned.ALLOK).put("statut", "User successfully added");
						} else if (code == CodesReturned.USERALREADYINBASE) {
							jsonResponse.put("code", CodesReturned.USERALREADYINBASE).put("statut", "User already Signed up");
						} else {
							jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE).put("statut", "Problem with database");
						}
					} else {
						try {
							jsonResponse.put("code", CodesReturned.ADUSERNOTEXISTS).put("statut", "AD user does not exist");
						} catch (final JSONException e) {
							logger.error(e);
						}
					}
				} catch (ParserConfigurationException | SAXException e) {
					try {
						jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML Manipulation");
						logger.error(e);
					} catch (final JSONException e1) {
						logger.error(e);
					}
				} catch (final NamingException e) {
					try {
						jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONAD).put("statut", "Problem with AD connection");
						logger.error(e);
					} catch (final JSONException e1) {
						logger.error(e);
					}
				}
			} else {
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Problem with query");
			}
		} catch (final JSONException e) {
			logger.error(e);
		}
		final PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}
