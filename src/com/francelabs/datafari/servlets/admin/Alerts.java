package com.francelabs.datafari.servlets.admin;

/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.service.db.AlertDataService;

/** Javadoc
 * 
 * This servlet is used to add new alerts and print/edit/delete the existing alerts in the database.
 * It is only called by the Alerts.html.
 * doGet is used to print the Alerts.
 * doPost is used to add/edit/delete Alerts.
 * The connection with the database is made in the constructor.
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/Alerts") 
public class Alerts extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(Alerts.class
			.getName());
	/**
	 * @throws IOException 
	 * @see HttpServlet#HttpServlet()
	 * Connect with the database
	 */
	public Alerts() throws IOException {
		super();							
	} 
	/**
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Used to print the existing alerts.
	 * Makes a request and put the results into a JSON file.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		try{
			PrintWriter pw = response.getWriter();

			int i=0;
			JSONObject superJson = new JSONObject();
			try{
				List<Properties> alerts = AlertDataService.getInstance().getAlerts();								//Get all the existing Alerts
				for (Properties alert : alerts) {										//Get the next Alert
					if(!request.getParameter("keyword").equals("")){		//If the user have typed something in the search field
						if(alert.get("keyword").equals(request.getParameter("keyword"))){	//then only the Alerts with a corresponding keyword are put into the Json		
							if(request.getRemoteUser().equals(alert.get("user")) || request.isUserInRole("SearchAdministrator")){	//Only the Alerts with the correct user, except if it's the admin
								try{
									superJson.append("alerts", put(alert, request.isUserInRole("SearchAdministrator")));			//put the jsonObject in an other so that this superJSON will contain all the Alerts
									i++;										//count the number of alerts
								}catch(JSONException e){
									pw.append("Error while getting one or more alerts, please retry, if the problem persists contact your system administrator. Error code : 69007"); 	
									pw.close();
									LOGGER.error("Error while building the JSON answer in the Alerts Servlet's doGet. Error 69007", e);
									continue;
								} 
							}
						}
					}else{													//If nothing was typed in the search field
						try{
							if(request.getRemoteUser().equals(alert.get("user")) || request.isUserInRole("SearchAdministrator")){	//Only the Alerts with the correct user, except if it's the admin		
								superJson.append("alerts", put(alert, request.isUserInRole("SearchAdministrator")));
								i++;
							}
						}catch(JSONException e){
							pw.append("Error while getting one or more alerts, please retry, if the problem persists contact your system administrator. Error code : 69007"); 	
							pw.close();
							LOGGER.error("Error while building the JSON answer in the Alerts Servlet's doGet. Error 69007", e);
							continue;
						}
					}
				}
				try {
					superJson.put("length", i);								//Put the number of alerts at the end of the JSON object (handy to print the alerts back in the HTML)
				} catch (JSONException e) {
					pw.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69009"); 	
					pw.close();
					LOGGER.error("Error while building the final JSON answer in the Alerts Servlet's doGet. Error 69009 ", e);
					return;
				}
				pw.write(superJson.toString());								//Send the JSON back to the HTML page
				response.setStatus(200);
				response.setContentType("text/json;charset=UTF-8");
			} catch(Exception e){
				pw.append("Error connecting to the database, please retry, if the problem persists contact your system administrator. Error code : 69010"); 	
				pw.close();
				LOGGER.error("Error connecting to the database in Alerts Servlet's doGet. Error 69010", e);
				return;
			} 
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69502");
			out.close();
			LOGGER.error("Unindentified error in Alerts doGet. Error 69502", e);
		}
	} 

	private Object put(Properties alert, boolean admin) throws JSONException {
		JSONObject json = new JSONObject();			//Creates a json object
		json.put("_id", alert.get("_id"));				//gets the id
		json.put("keyword",alert.get("keyword"));		//gets the keyword
		json.put("subject",alert.get("subject"));		//gets the subject
		json.put("core",alert.get("core"));			//gets the core
		json.put("frequency",alert.get("frequency"));	//gets the frequency
		json.put("mail",alert.get("mail"));			//gets the mail
		if(admin){
			json.put("user",alert.get("user"));		//gets the user
		}
		return json;

	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Used to delete/add/edit an Alert
	 * Directly change the database and returns nothing
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			PrintWriter pw = response.getWriter();
			try{
				if(request.getParameter("_id")!=null){
					AlertDataService.getInstance().deleteAlert(request.getParameter("_id"));//Deleting part									//Execute the query in the collection
				}
				if(request.getParameter("keyword")!=null){	
					Properties alert = new Properties();//Adding part
					for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){	//For all the parameters passed, we put the parameter name as the key and the content as the value
						String elem = e.nextElement();
						if(!elem.equals("_id")){									//Do not put the _id manually so if the parameter is "_id" we do not put it in,
							alert.put(elem, request.getParameter(elem));				//otherwise there will be an exception at the 2nd modification or at a removal after a modification.
						}															//This loop can only be triggered by an edit.
					} 
					alert.put("user", request.getRemoteUser());
					AlertDataService.getInstance().addAlert(alert);
					//insert the object composed of all the parameters
				}
				//If this is an edit the two parts (Delete and Add) will be executed successively
			} catch(Exception e){
				pw.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69011"); 	
				pw.close();
				LOGGER.error("Error connecting to the database in Alerts Servlet's doPost. Error 69011", e);
			}
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69503");
			out.close();
			LOGGER.error("Unindentified error in Alerts doPost. Error 69503", e);
		}
	}
		static String readFile(String path, Charset encoding) 					//Read the file
				throws IOException 
		{
				byte[] encoded = Files.readAllBytes(Paths.get(path));
				return new String(encoded, encoding);
		}
	}