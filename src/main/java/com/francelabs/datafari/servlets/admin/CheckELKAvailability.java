package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.elk.ActivateELK;
import com.francelabs.datafari.utils.ScriptConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/checkELKAvailability")
public class CheckELKAvailability extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(CheckELKAvailability.class);
	private static final String ELKACTIVATION = "ELKactivation";
	private static final String KIBANAURI = "KibanaURI";
	private static final String EXTERNALELK = "externalELK";
	private static final String ELKSERVER = "ELKServer";
	private static final String ELKSCRIPTSDIR = "ELKScriptsDir";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CheckELKAvailability() {
		super();
	}

	/**
	 * Check if the provided URL exists or return an error code
	 *
	 * @param urlString
	 *            the URL to check
	 * @return true if URL exists, false otherwise
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private boolean isURLUp(final String urlString) {
		try {
			final URL u = new URL(urlString);
			final HttpURLConnection huc = (HttpURLConnection) u.openConnection();
			huc.setRequestMethod("HEAD");
			huc.connect();
			huc.getResponseCode();
			return true;
		} catch (final Exception e) {
			return false;
		}
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

		jsonResponse.put(KIBANAURI, ScriptConfiguration.getProperty(KIBANAURI));
		jsonResponse.put(EXTERNALELK, ScriptConfiguration.getProperty(EXTERNALELK));
		jsonResponse.put(ELKSERVER, ScriptConfiguration.getProperty(ELKSERVER));
		jsonResponse.put(ELKSCRIPTSDIR, ScriptConfiguration.getProperty(ELKSCRIPTSDIR));
		final boolean activated = Boolean.parseBoolean(ScriptConfiguration.getProperty(ELKACTIVATION));
		final boolean urlUp = isURLUp(ScriptConfiguration.getProperty(KIBANAURI));
		try {
			if (activated) {
				jsonResponse.put("code", CodesReturned.ALLOK).put(ELKACTIVATION, "true");
			} else {
				jsonResponse.put("code", CodesReturned.ALLOK).put(ELKACTIVATION, "false");
			}
			if (urlUp) {
				jsonResponse.put("code", CodesReturned.ALLOK).put("isELKUp", "true");
			} else {
				jsonResponse.put("code", CodesReturned.ALLOK).put("isELKUp", "false");
			}
		} catch (final JSONException e) {
			logger.error("Error", e);
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
			if (req.getParameter(ELKACTIVATION) == null) {
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Query Malformed");
			} else {
				String elkActivation = req.getParameter(ELKACTIVATION);

				try {
					int returnCode;
					if (elkActivation.equals("true")) {
						if (req.getParameter(EXTERNALELK) != null && req.getParameter(EXTERNALELK).toString().equals("true")) {
							if (req.getParameter(ELKSERVER) != null && req.getParameter(ELKSCRIPTSDIR) != null) {
								returnCode = ActivateELK.getInstance().activateRemote(req.getParameter(ELKSERVER), req.getParameter(ELKSCRIPTSDIR));
							} else {
								returnCode = CodesReturned.GENERALERROR;
								elkActivation = "false";
							}
						} else {
							returnCode = ActivateELK.getInstance().activate();
						}
					} else {
						if (req.getParameter(EXTERNALELK) != null && req.getParameter(EXTERNALELK).toString().equals("true")) {
							if (req.getParameter(ELKSERVER) != null && req.getParameter(ELKSCRIPTSDIR) != null) {
								returnCode = ActivateELK.getInstance().deactivateRemote(req.getParameter(ELKSERVER), req.getParameter(ELKSCRIPTSDIR));
							} else {
								returnCode = CodesReturned.GENERALERROR;
								elkActivation = "true";
							}
						} else {
							returnCode = ActivateELK.getInstance().deactivate();
						}
					}
					if (ScriptConfiguration.setProperty(ELKACTIVATION, elkActivation)) {
						jsonResponse.put("code", CodesReturned.GENERALERROR);
					} else {
						jsonResponse.put("code", returnCode).put(ELKACTIVATION, elkActivation);
					}
				} catch (final Exception e) {
					jsonResponse.put("code", CodesReturned.GENERALERROR);
					logger.error("Fatal Error", e);
				}
			}
		} catch (final JSONException e) {
			logger.error("Error", e);
		}
		final PrintWriter out = resp.getWriter();
		out.print(jsonResponse);
	}

}