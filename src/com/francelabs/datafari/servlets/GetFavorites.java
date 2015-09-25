/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.Favorite;
import com.francelabs.datafari.user.UserConstants;

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
				jsonResponse.put("code", CodesReturned.NOTCONNECTED)
				.put("statut", "Please reload the page, you're not connected");
			}else{
				String username = userPrincipal.getName();
				List<String> favoritesList = Favorite.getFavorites(username);
				if (favoritesList==null){
					jsonResponse.put("code",CodesReturned.PROBLEMCONNECTIONDATABASE);
				}else{
					jsonResponse.put("code",CodesReturned.ALLOK);
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
