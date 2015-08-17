package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.servlets.admin.ConfigDeduplication;
import com.francelabs.datafari.user.CodesUser;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.user.Like;
import com.mongodb.realm.MongoDBRunning;

/**
 * Servlet implementation class GetLikesFavorites
 */
@WebServlet("/getLikesFavorites")
public class GetLikesFavorites extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger  = Logger.getLogger(GetLikesFavorites.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetLikesFavorites() {
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
			try {
				if (request.getUserPrincipal()!=null){
					if (new MongoDBRunning(Favorite.FAVORITEDB).isConnected()){
						String username = request.getUserPrincipal().getName();
						ArrayList<String> likeList = Like.getLikes(username);	
						ArrayList<String> favoritesList = Favorite.getFavorites(username);	
						jsonResponse.put("favorites", favoritesList)
							.put("code", 0)
							.put("likes", likeList);
					}else{
						jsonResponse.put("code", CodesUser.PROBLEMCONNECTIONMONGODB)
						.put("statut", "MongoDB Doesn't respound");		
					}
				}else{
					jsonResponse.put("code", CodesUser.NOTCONNECTED)
						.put("statut","Not connected yet");
				}
			} catch (JSONException e) {
					logger.error(e);
			}			

			PrintWriter out = response.getWriter();
			out.print(jsonResponse);
	}

}
