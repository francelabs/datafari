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

import com.francelabs.datafari.elk.ActivateELK;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.ELKConfiguration;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/ELKAdmin")
public class ELKAdmin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ELKAdmin.class);

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

		jsonResponse.put(ELKConfiguration.KIBANA_URI, ELKConfiguration.getProperty(ELKConfiguration.KIBANA_URI));
		jsonResponse.put(ELKConfiguration.EXTERNAL_ELK_ON_OFF, ELKConfiguration.getProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF));
		jsonResponse.put(ELKConfiguration.ELK_SERVER, ELKConfiguration.getProperty(ELKConfiguration.ELK_SERVER));
		jsonResponse.put(ELKConfiguration.ELK_SCRIPTS_DIR, ELKConfiguration.getProperty(ELKConfiguration.ELK_SCRIPTS_DIR));
		final boolean activated = Boolean.parseBoolean(ELKConfiguration.getProperty(ELKConfiguration.ELK_ACTIVATION));
		final boolean urlUp = isURLUp(ELKConfiguration.getProperty(ELKConfiguration.KIBANA_URI));
		try {
			if (activated) {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put(ELKConfiguration.ELK_ACTIVATION, "true");
			} else {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put(ELKConfiguration.ELK_ACTIVATION, "false");
			}
			if (urlUp) {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put("isELKUp", "true");
			} else {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put("isELKUp", "false");
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
			if (req.getParameter(ELKConfiguration.ELK_ACTIVATION) == null) {
				jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY.getValue()).put(OutputConstants.STATUS, "Query Malformed");
			} else {
				String elkActivation = req.getParameter(ELKConfiguration.ELK_ACTIVATION);

				try {
					if (elkActivation.equals("true")) {
						if (req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null
								&& req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).toString().equals("true")) {
							if (req.getParameter(ELKConfiguration.ELK_SERVER) != null && req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) != null) {
								ActivateELK.getInstance().activateRemote(req.getParameter(ELKConfiguration.ELK_SERVER),
										req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR));
							} else {
								logger.warn("Unable to activate ELK : wrong parameters");
								elkActivation = "false";
							}
						} else {
							ActivateELK.getInstance().activate();
						}
					} else {
						if (req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null
								&& req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).toString().equals("true")) {
							if (req.getParameter(ELKConfiguration.ELK_SERVER) != null && req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) != null) {
								ActivateELK.getInstance().deactivateRemote(req.getParameter(ELKConfiguration.ELK_SERVER),
										req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR));
							} else {
								logger.warn("Unable to unactivate ELK : wrong parameters");
								elkActivation = "true";
							}
						} else {
							ActivateELK.getInstance().deactivate();
						}
					}
					if (ELKConfiguration.setProperty(ELKConfiguration.ELK_ACTIVATION, elkActivation)) {
						jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
					} else {
						jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue()).put(ELKConfiguration.ELK_ACTIVATION, elkActivation);
					}
				} catch (final Exception e) {
					jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
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