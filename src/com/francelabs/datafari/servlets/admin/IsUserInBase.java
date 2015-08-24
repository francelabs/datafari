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
import com.francelabs.realm.MongoDBRunning;
import com.francelabs.realm.User;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchAdministrator/isUserInBase")
public class IsUserInBase extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(IsUserInBase.class.getName());  
    /**
     * @see HttpServlet#HttpServlet()
     */
    public IsUserInBase() {
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
			if (request.getParameter(User.USERNAMECOLUMN)!=null){
				MongoDBRunning mongoDBRunning = new MongoDBRunning(User.IDENTIFIERSDB);
				if (mongoDBRunning.isConnected()){
					User user = new User(request.getParameter(User.USERNAMECOLUMN).toString(),"",mongoDBRunning.getDb());
					String result;
					if (user.isInBase()){
						result = "true";
					}else{
						result = "false";
					}
					jsonResponse.put("code", CodesReturned.ALLOK).put("statut", result);
				}else{
					jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONMONGODB).put("statut", "Problem with database");
				}
			}else{
				jsonResponse.put("code", CodesReturned.PROBLEMQUERY).put("statut", "Problem with query");
			}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}
			PrintWriter out = response.getWriter();
			out.print(jsonResponse);
	}

}
