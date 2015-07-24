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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/** Javadoc
 * 
 * This servlet is used to add new alerts and print/edit/delete the existing alerts in the MongoDB database.
 * It is only called by the Alerts.html.
 * doGet is used to print the Alerts.
 * doPost is used to add/edit/delete Alerts.
 * The connection with the mongoDB database is made in the constructor.
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/Alerts") 
public class Alerts extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String host;
	private int port;
	private String database;
	private String collection;
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> coll1;
	private String env;
	private final static Logger LOGGER = Logger.getLogger(Alerts.class
			.getName());
	/**
	 * @throws IOException 
	 * @see HttpServlet#HttpServlet()
	 * Connect with the database
	 */
	public Alerts() throws IOException {
		super();
		host = "localhost";
		port = 27017;													//Default address/port of the database
		database = "Datafari";											//Default name of the Database
		collection = "Alerts";											//Default name of the collection
		env = System.getenv("DATAFARI_HOME");							//Gets the directory of installation if in standard environment	
		if(env==null){													//If in development environment
			env = "/home/youp/workspaceTest/Servers/Datafari-config/datafari.properties";	//Hardcoded path
		}else{
			env = env+"tomcat/conf/datafari.properties";
		}
		String content ="";
		try {
			content = readFile(env, StandardCharsets.UTF_8); 
		} catch (IOException e1) {
			LOGGER.error("Error while reading the datafari.properties in the Alerts Servlet's constructor ", e1);
			throw e1;
		}
		String[] lines = content.split(System.getProperty("line.separator"));	//read the file line by line
		for(int i = 0 ; i < lines.length ; i++){				//for each line
			if(lines[i].startsWith("HOST")){			//Gets the address of the host
				host = lines[i].substring(lines[i].indexOf("=")+1, lines[i].length());
			}else if(lines[i].startsWith("PORT")){	//Gets the port
				try{
					port = Integer.parseInt(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()));
				}catch(NumberFormatException e){
					LOGGER.error("Error while reading the \"port\" line of datafari.properties in the Alerts Servlet's constructor using default port ", e);
				}
			}else if(lines[i].startsWith("DATABASE")){		//Gets the name of the database
				database = lines[i].substring(lines[i].indexOf("=")+1, lines[i].length());
			}else if(lines[i].startsWith("COLLECTION")){	//Gets the name of the collection
				collection = lines[i].substring(lines[i].indexOf("=")+1, lines[i].length());
			}
		}
		mongoClient = new MongoClient(host, port);						//Connect to the mongoDB database
		db = mongoClient.getDatabase(database);							//Switch to the right Database
		coll1 = db.getCollection(collection);								
	} 
	/**
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Used to print the existing alerts.
	 * Makes a request and put the results into a JSON file.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		PrintWriter pw = null; 
		try {
			pw = response.getWriter();
		} catch (IOException e1) {
			LOGGER.error("Error while reading the datafari.properties in the Alerts Servlet's doGet ", e1);
			throw e1;
		}
		int i=0;
		JSONObject superJson = new JSONObject();
		try{
			FindIterable<Document> cursor = coll1.find();								//Get all the existing Alerts
			for (Document d : cursor) {										//Get the next Alert
				if(!request.getParameter("keyword").equals("")){		//If the user have typed something in the search field
					if(d.get("keyword").equals(request.getParameter("keyword"))){	//then only the Alerts with a corresponding keyword are put into the Json		
						if(request.getRemoteUser().equals(d.get("user")) || request.isUserInRole("SearchAdministrator")){	//Only the Alerts with the correct user, except if it's the admin
							try{
								superJson.append("alerts", put(d, request.isUserInRole("SearchAdministrator")));			//put the jsonObject in an other so that this superJSON will contain all the Alerts
								i++;										//count the number of alerts
							}catch(JSONException e){
								LOGGER.error("Error while building the JSON answer in the Alerts Servlet's doGet ", e);
								continue;
							} 
						}
					}
				}else{													//If nothing was typed in the search field
					try{
						if(request.getRemoteUser().equals(d.get("user")) || request.isUserInRole("SearchAdministrator")){	//Only the Alerts with the correct user, except if it's the admin		
							superJson.append("alerts", put(d, request.isUserInRole("SearchAdministrator")));
							i++;
						}
					}catch(JSONException e){
						LOGGER.error("Error while building the JSON answer in the Alerts Servlet's doGet ", e);
						continue;
					}
				}
			}
			try {
				superJson.put("length", i);								//Put the number of alerts at the end of the JSON object (handy to print the alerts back in the HTML)
			} catch (JSONException e) {
				LOGGER.error("Error while building the final JSON answer in the Alerts Servlet's doGet ", e);
				throw new RuntimeException();
			}
			pw.write(superJson.toString());								//Send the JSON back to the HTML page
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
		} catch(MongoSocketOpenException | MongoTimeoutException e){
			pw.write("error connecting to the database");
			LOGGER.error("Error connecting to the database", e);
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
		} 
	} 

	private Object put(Document db, boolean admin) {
		try{
			JSONObject json = new JSONObject();					//Creates a json object
			json.put("_id", db.get("_id"));				//gets the id
			json.put("keyword",db.get("keyword"));		//gets the keyword
			json.put("subject",db.get("subject"));		//gets the subject
			json.put("core",db.get("core"));			//gets the core
			json.put("frequency",db.get("frequency"));	//gets the frequency
			json.put("mail",db.get("mail"));			//gets the mail
			if(admin){
				json.put("user",db.get("user"));			//gets the user
			}
			return json;
		}catch(JSONException e){
			LOGGER.error("", e);
			throw new RuntimeException();
		}
	}

	/**
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Used to delete/add/edit an Alert
	 * Directly change the database and returns nothing
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter pw = null;
		try {
			pw = response.getWriter();
		} catch (IOException e1) {
			LOGGER.error("Error while getting the writer in the Alerts Servlet's doPost ", e1);
			throw e1;
		}
		try{
			if(request.getParameter("_id")!=null){								//Deleting part
				BasicDBObject query = new BasicDBObject();						
				query.put("_id", new ObjectId(request.getParameter("_id")));	//Create a query where we put the id of the alerts that must be deleted
				coll1.findOneAndDelete(query);										//Execute the query in the collection
			}
			if(request.getParameter("keyword")!=null){							//Adding part
				Document obj = new Document();
				for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){	//For all the parameters passed, we put the parameter name as the key and the content as the value
					String elem = e.nextElement();
					if(!elem.equals("_id")){									//Do not put the _id manually so if the parameter is "_id" we do not put it in,
						obj.put(elem, request.getParameter(elem));				//otherwise there will be an exception at the 2nd modification or at a removal after a modification.
					}															//This loop can only be triggered by an edit.
				} 
				obj.put("user", request.getRemoteUser());
				coll1.insertOne(obj);												//insert the object composed of all the parameters
			}
			//If this is an edit the two parts (Delete and Add) will be executed successively
		} catch(MongoSocketOpenException | MongoTimeoutException e){
			pw.write("error connecting to the database");
			LOGGER.error("Error connecting to the database", e);
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
			throw new RuntimeException();
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		try{
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);
		}catch(IOException e){
			LOGGER.error("Error while reading the datafari.properties file in readFile, alerts administration servlet", e);
			throw e;
		}
	}


}
