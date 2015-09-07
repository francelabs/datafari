package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.User;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/getAllUsersAndRoles")
public class GetAllUsersAndRoles extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GetAllUsersAndRoles.class);
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetAllUsersAndRoles() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		try {
			Map<String, List<String>> usersList = User.getAllUsers();
			if (usersList!=null)
				jsonResponse.put("code",CodesReturned.ALLOK).put("statut",User.getAllUsers());
			else
				jsonResponse.put("code",CodesReturned.PROBLEMCONNECTIONDATABASE).put("statut","Datafari isn't connected to Cassandra");
		}catch (JSONException e) {
				logger.error(e);
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}
