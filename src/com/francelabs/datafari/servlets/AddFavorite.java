package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.user.CodesUser;
import com.francelabs.datafari.user.Favorite;


/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/addFavorite")
public class AddFavorite extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AddFavorite.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddFavorite() {
        super();
        BasicConfigurator.configure();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject jsonResponse = new JSONObject();
		request.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		if (request.getParameter("idDocument")!=null ){
			try {
				Principal userPrincipal = request.getUserPrincipal(); 
				//checking if the user is connected
				if (userPrincipal == null){
					jsonResponse.put("code", CodesUser.NOTCONNECTED)
					.put("statut", "Please reload the page, you'r not connected");
				}else{
					String username = request.getUserPrincipal().getName();
					if (Favorite.addFavorite(username, request.getParameter("idDocument"))){
						jsonResponse.put("code", 0);
					}else{
						// MongoDB is not running
						jsonResponse.put("code", CodesUser.PROBLEMCONNECTIONMONGODB)
						.append("statut", "Problem while connecting to database");
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
		}else{
			try {
				jsonResponse.put("code", -1)
				.put("statut", "Query malformed");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e);
			}
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}

}
