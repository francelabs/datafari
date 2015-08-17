package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.user.CodesUser;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.user.StringsUser;
import com.mongodb.realm.MongoDBRunning;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/GetFavorites")
public class GetFavorites extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GetFavorites.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetFavorites() {
        super();
        // TODO Auto-generated constructor stub
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
		try{
			Principal userPrincipal = request.getUserPrincipal(); 
			//checking if the user is connected
			if (userPrincipal == null){
				jsonResponse.put("code", CodesUser.NOTCONNECTED)
				.put("statut", "Please reload the page, you'r not connected");
			}else{
				String username = userPrincipal.getName();
				if (!(new MongoDBRunning(StringsUser.FAVORITEDB).isConnected())){
					jsonResponse.put("code",CodesUser.PROBLEMCONNECTIONMONGODB);
				}
				else{
					ArrayList<String> favoritesList = Favorite.getFavorites(username);
					jsonResponse.put("code",CodesUser.ALLOK);
					if (favoritesList!=null)
						jsonResponse.put("favoritesList", favoritesList);
				}
			}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}
		PrintWriter out = response.getWriter();
		out.print(jsonResponse);
	}
}
