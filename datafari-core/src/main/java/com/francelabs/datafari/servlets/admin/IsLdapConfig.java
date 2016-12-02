package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.ldap.LDAPService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.ActivateLDAPSolr;
import com.francelabs.datafari.utils.RealmLdapConfiguration;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/isLdapConfig")
public class IsLdapConfig extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(IsLdapConfig.class);
	private static final String LDAPACTIVATED = "ISLDAPACTIVATED";

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
			String isLdapActivated = ScriptConfiguration.getProperty(LDAPACTIVATED);
			if (isLdapActivated == null) {
				isLdapActivated = "false";
			} else if (isLdapActivated.equals("true")) {
				// Test the connection
				final HashMap<String, String> h = RealmLdapConfiguration.getConfig(request);
				LDAPService.getInstance().testLDAPConnection(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL),
						h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));
			}
			jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put("isActivated", isLdapActivated);
		} catch (final JSONException | ParserConfigurationException | SAXException | ManifoldCFException e) {
			jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
			logger.error("Fatal Error", e);
		} catch (final NamingException e) {
			try {
				ActivateLDAPSolr.disactivate();
			} catch (final Exception e1) {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
				logger.error("Fatal Error", e);
			} finally {
				ScriptConfiguration.setProperty(LDAPACTIVATED, "false");
				jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue())
						.put(OutputConstants.STATUS, "Fail to connect to AD with the given settings ").put("isActivated", false);
			}

		}
		final PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final JSONObject jsonResponse = new JSONObject();
		req.setCharacterEncoding("utf8");
		resp.setContentType("application/json");
		try {
			if (req.getParameter("isLdapActivated") == null) {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue()).put(OutputConstants.STATUS, "Query Malformed");
			} else {
				try {
					if (req.getParameter("isLdapActivated").toString().equals("true")) {
						// Test the connection
						final HashMap<String, String> h = RealmLdapConfiguration.getConfig(req);
						LDAPService.getInstance().testLDAPConnection(h.get(RealmLdapConfiguration.ATTR_CONNECTION_URL),
								h.get(RealmLdapConfiguration.ATTR_CONNECTION_NAME), h.get(RealmLdapConfiguration.ATTR_CONNECTION_PW));

						ActivateLDAPSolr.activate();
						if (ScriptConfiguration.setProperty(LDAPACTIVATED, req.getParameter("isLdapActivated"))) {
							jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
						}
					} else {
						ActivateLDAPSolr.disactivate();
					}
					jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put("isActivated", req.getParameter("isLdapActivated"));
				} catch (final NamingException e) {
					jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONAD.getValue()).put(OutputConstants.STATUS,
							"Fail to connect to AD with the given settings");
				} catch (final Exception e) {
					jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
					logger.error("Fatal Error", e);
				}
			}

		} catch (

		final JSONException e) {
			logger.error("Error", e);
		}
		final PrintWriter out = resp.getWriter();
		out.print(jsonResponse);
	}

}
