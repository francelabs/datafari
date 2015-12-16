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
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.user.User;


/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/deleteRole")
public class DeleteRole extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(DeleteRole.class.getName());  
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DeleteRole() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		try{
			if (request.getParameter(UserDataService.USERNAMECOLUMN)!=null && request.getParameter(UserDataService.ROLECOLUMN)!=null){
				User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(),"");
				int code = user.deleteRole(request.getParameter(UserDataService.ROLECOLUMN).toString());
				if (code == CodesReturned.ALLOK)
					jsonResponse.put("code", CodesReturned.ALLOK).put("statut", "User deleted with success");
				else
					jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE).put("statut", "Datafari isn't connected to Database");
			}else{
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Problem with query");
			}
		}catch (JSONException e) {
			logger.error(e);
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}
}
