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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.Favorite;

/**
 * Servlet implementation class addFavorite
 */
@WebServlet("/deleteFavorite")
public class DeleteFavorite extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(DeleteFavorite.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DeleteFavorite() {
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
				if (userPrincipal == null){
					jsonResponse.put("code", CodesReturned.NOTCONNECTED)
					.put("statut", "Please reload the page, you're not connected");
				}else{
					String username = request.getUserPrincipal().getName();
					if (Favorite.deleteFavorite(username, request.getParameter("idDocument")) == CodesReturned.ALLOK){
						jsonResponse.put("code", CodesReturned.ALLOK);
					}else{
						jsonResponse.put("code", CodesReturned.PROBLEMCONNECTIONDATABASE)
						.put("statut", "Problem while connecting to database");
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
