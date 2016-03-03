package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;
import com.francelabs.datafari.user.UserConstants;

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
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Problem with query");
			}
		} catch (final JSONException e) {
			logger.error(e);
		}
		final PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}
