package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.*;

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
	private MongoClient mongoClient;
	private DB db;
	private DBCollection coll1;

	/**
	 * @see HttpServlet#HttpServlet()
	 * Connect with the database
	 */
	@SuppressWarnings("deprecation")
	public Alerts() {
		super();
		mongoClient = new MongoClient("localhost", 27017);			//Hardcoded address/port of the database
		db = 	mongoClient.getDB("Datafari");						//Hardcoded name of the Database
		coll1 = db.getCollection("Alerts");							//Hardcoded name of the collection
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Used to print the existing alerts.
	 * Makes a request and put the results into a JSON file.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter pw = response.getWriter();
		int i=0;
		JSONObject json;
		JSONObject superJson = new JSONObject();
		DBCursor cursor = coll1.find();								//Get all the existing Alerts
		while (cursor.hasNext()) {
			DBObject db = cursor.next();							//Get the next Alert
			if(!request.getParameter("keyword").equals("")){		//If the user have typed something in the search field
				if(db.get("keyword").equals(request.getParameter("keyword"))){	//then only the Alerts with a corresponding keyword are put into the Json
					try{
						json = new JSONObject();					//Creates a json object
						json.put("_id", db.get("_id"));				//gets the id
						json.put("keyword",db.get("keyword"));		//gets the keyword
						json.put("subject",db.get("subject"));		//gets the subject
						json.put("core",db.get("core"));			//gets the core
						json.put("frequency",db.get("frequency"));	//gets the frequency
						superJson.append("alerts", json);			//put the jsonObject in an other so that this superJSON will contain all the Alerts
						i++;										//count the number of alerts
					}catch(Exception e){}
				}
			}else{													//If nothing was typed in the search field
				try{
					json = new JSONObject();						//put all the alerts in a jsonObject the same way as higher
					json.put("_id", db.get("_id"));
					json.put("keyword",db.get("keyword"));
					json.put("subject",db.get("subject"));
					json.put("core",db.get("core"));
					json.put("frequency",db.get("frequency"));
					superJson.append("alerts", json);
					i++;
				}catch(Exception e){}
			}
		}
		try {
			superJson.put("length", i);								//Put the number of alerts at the end of the JSON object (handy to print the alerts back in the HTML)
		} catch (JSONException e) {}
		pw.write(superJson.toString());								//Send the JSON back to the HTML page
		response.setStatus(200);
		response.setContentType("text/json;charset=UTF-8");
	} 

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * Used to delete/add/edit an Alert
	 * Directly change the database and returns nothing
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(request.getParameter("_id")!=null){								//Deleting part
			BasicDBObject query = new BasicDBObject();						
			query.put("_id", new ObjectId(request.getParameter("_id")));	//Create a query where we put the id of the alerts that must be deleted
			coll1.findAndRemove(query);										//Execute the query in the collection
		}
		if(request.getParameter("keyword")!=null){							//Adding part
			BasicDBObject obj = new BasicDBObject();
			for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();){	//For all the parameters passed, we put the parameter name as the key and the content as the value
				String elem = e.nextElement();
				if(!elem.equals("_id")){									//Do not put the _id manually so if the parameter is "_id" we do not put it in,
					obj.put(elem, request.getParameter(elem));				//otherwise there will be an exception at the 2nd modification or at a removal after a modification.
				}															//This loop can only be triggered by an edit.
			}
			obj.put("address", "exemple.eg@ex.com");						//When you will pass an address in the ajax request suppress this line
			coll1.insert(obj);												//insert the object composed of all the parameters
		}
		//If this is an edit the two parts (Delete and Add) will be executed successively
	}

}
