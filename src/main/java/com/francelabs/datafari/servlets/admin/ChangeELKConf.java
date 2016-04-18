package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.utils.ELKConfiguration;

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
		try {
			if (req.getParameter(ELKConfiguration.KIBANA_URI) == null || (req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF) != null
					&& req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF).equals("true")
					&& (req.getParameter(ELKConfiguration.ELK_SERVER) == null || req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR) == null))) {
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Query Malformed");
			} else {
				if (ELKConfiguration.setProperty(ELKConfiguration.KIBANA_URI, req.getParameter(ELKConfiguration.KIBANA_URI))
						|| ELKConfiguration.setProperty(ELKConfiguration.EXTERNAL_ELK_ON_OFF, req.getParameter(ELKConfiguration.EXTERNAL_ELK_ON_OFF))
						|| ELKConfiguration.setProperty(ELKConfiguration.ELK_SERVER, req.getParameter(ELKConfiguration.ELK_SERVER))
						|| ELKConfiguration.setProperty(ELKConfiguration.ELK_SCRIPTS_DIR, req.getParameter(ELKConfiguration.ELK_SCRIPTS_DIR))) {
					jsonResponse.put("code", CodesReturned.GENERALERROR);
				} else {
					jsonResponse.put("code", CodesReturned.ALLOK);
				}
			}
		} catch (final JSONException e) {
			logger.error("Error", e);
		}
		final PrintWriter out = resp.getWriter();
		out.print(jsonResponse);
	}

}