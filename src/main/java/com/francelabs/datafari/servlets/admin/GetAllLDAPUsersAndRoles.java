package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
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
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.utils.RealmLdapConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/getAllLDAPUsersAndRoles")
public class GetAllLDAPUsersAndRoles extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GetAllLDAPUsersAndRoles.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetAllLDAPUsersAndRoles() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets the directory context of the provided url
	 *
	 * @param url
	 *            the url ("ldap://serverName:serverPort")
	 * @param user
	 *            the user
	 * @param password
	 *            the user's password
	 * @return the directory context
	 * @throws NamingException
	 *             .
	 */
	private DirContext getDirContext(final String url, final String user, final String password) throws NamingException {
		// create an initial directory context
		final Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("com.sun.jndi.ldap.connect.timeout", "1000");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, password);

		// Create the initial directory context
		return new InitialDirContext(env);
	}

	private List<String> getUsersList(final DirContext ctx, final String userBase, final boolean userSubTree) throws NamingException {
		final List<String> usersList = new ArrayList<String>();
		final String[] attributeNames = { "userAccountControl", "sAMAccountName" };
		final NamingEnumeration<NameClassPair> contentsEnum = ctx.list(userBase);
		while (contentsEnum.hasMore()) {
			final NameClassPair ncp = contentsEnum.next();
			final String userName = ncp.getName();
			final Attributes attr1 = ctx.getAttributes(userName + "," + userBase, new String[] { "objectcategory" });
			if (attr1.get("objectcategory").toString().indexOf("CN=Person") == -1) { // Test
																						// if
																						// current
																						// object
																						// is
																						// a
																						// user
				if (userSubTree) {
					// Recurse sub-contexts
					usersList.addAll(getUsersList(ctx, userName + "," + userBase, userSubTree));
				}
			} else {

				final Attributes attrs = ctx.getAttributes(userName + "," + userBase, attributeNames);
				final Attribute bitsAttribute = attrs.get("userAccountControl");

				if (bitsAttribute != null) {
					final long lng = Long.parseLong(bitsAttribute.get(0).toString());
					final long secondBit = lng & 2; // get bit 2
					if (secondBit == 0) {
						final Attribute accountAttribute = attrs.get("sAMAccountName");
						if (accountAttribute != null) {
							usersList.add((String) accountAttribute.get(0));
						}
					}
				}

			}
		}
		return usersList;
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
		try {
			final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);
			final DirContext ctx = getDirContext(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL),
					h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));
			final List<String> ldapUsersList = getUsersList(ctx, h.get(RealmLdapConfiguration.ATTR_DOMAIN_NAME), false);
			ctx.close();
			if (ldapUsersList.isEmpty()) {
				jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "No LDAP users found !");
			} else {
				final Map<String, List<String>> usersRoles = User.getAllUsers();
				final Map<String, List<String>> ldapUsersRoles = new HashMap<>();
				for (final String ldapUser : ldapUsersList) {
					final List<String> roles = new ArrayList<>();
					if (usersRoles.containsKey(ldapUser)) {
						roles.addAll(usersRoles.get(ldapUser));
					}
					ldapUsersRoles.put(ldapUser, roles);
				}
				jsonResponse.put("code", CodesReturned.ALLOK).put("statut", ldapUsersRoles);
			}
		} catch (SAXException | ParserConfigurationException | NamingException | JSONException e) {
			try {
				jsonResponse.put("code", CodesReturned.GENERALERROR).put("statut", "Problem with XML Manipulation");
				logger.error(e);
			} catch (final JSONException e1) {
				logger.error(e);
			}
		}
		final PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}