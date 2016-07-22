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

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.user.User;

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
			if (request.getParameter(UserDataService.USERNAMECOLUMN)!=null){
				try {
				User user = new User(request.getParameter(UserDataService.USERNAMECOLUMN).toString(),"");
				String result = null;
				if (user.isInBase()){
					result = "true";
				}else {
					result = "false";
				}
				jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK).put(OutputConstants.STATUS, result);
				} catch (DatafariServerException e){
					jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMCONNECTIONDATABASE).put(OutputConstants.STATUS, "Problem with database");
				}
		}else{
			jsonResponse.put(OutputConstants.CODE, CodesReturned.PROBLEMQUERY).put(OutputConstants.STATUS, "Problem with query");
		}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}
			PrintWriter out = response.getWriter();
			out.print(jsonResponse);
	}

}
