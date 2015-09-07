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
package com.francelabs.datafari.servlets.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.datafari.alerts.AlertsManager;
/**
 * 
 * This servlet is used to configure Alerts
 * It is only called by the alertsAdmin.html
 * doGet is called at the loading of the AlertsAdmin, to get the parameters from datafari.properties.
 * doPost is called when clicking on the on/off button, turns off and on the alerts
 * Or when the save parameters button is clicked, saves the parameters in datafari.properties 
 * If you are in development environment, the path towards the datafari.properties is hardcoded
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/alertsAdmin")
public class alertsAdmin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String env;
	private final static Logger LOGGER = Logger.getLogger(alertsAdmin.class
			.getName());
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public alertsAdmin() {
		super();
	}
	/**
	 * Gets the required parameters parameters
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException  {
		try{
			env = System.getProperty("catalina.home");		//Gets the installation directory if in standard environment 
			env += "/conf/datafari.properties";
			String content ="";
			PrintWriter out = response.getWriter();				
			JSONObject json = new JSONObject();
			try {
				content = readFile(env, StandardCharsets.UTF_8);
			} catch (NoSuchFileException e1) {
				LOGGER.error("Error while reading the datafari.properties file in the doGet of the alerts administration Servlet . Error 69020 ", e1);
				out.append("Error while reading the datafari.properties file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69020");
				out.close();
				return;
			}
			String[] lines = content.split(System.getProperty("line.separator"));	//read the file line by line
			DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");
			DateTimeFormatter formatterbis = DateTimeFormat.forPattern("dd/MM/yyyy/ HH:mm");
			for(int i = 0 ; i < lines.length ; i++){				//for each line
				try{
					if(lines[i].startsWith("ALERTS")){			
						if(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("o")).equals("on")){
							json.put("on", "on");					//If the alerts are activated puts "on" in the JSON (so the button will print "turn off" in the UI)
						}else{
							json.put("on", "off");					//If the alerts are deactivated puts "off" in the JSON (so the button will print "turn on" in the UI)
						}
					}else if(lines[i].startsWith("HOURLYDELAY"))	//Gets the date for the hourly alerts
						json.put("hourlyDate", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("DAILYDELAY"))		//Gets the date for the daily alerts
						json.put("dailyDate", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("WEEKLYDELAY"))		//Gets the date for the weekly alerts
						json.put("weeklyDate", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("HOST"))			//Gets the address of the host
						json.put("host", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("PORT"))			//Gets the port
						json.put("port", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("DATABASE"))		//Gets the name of the database
						json.put("database", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("COLLECTION"))		//Gets the name of the collection
						json.put("collection", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""));
					else if(lines[i].startsWith("Hourly")){		//Gets the previous execution date for the hourly alerts
						DateTime next = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""))).plusHours(1);
						DateTime previous = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", "")));
						json.put("nextHourly", next.toString(formatterbis));
						json.put("hourly",  previous.toString(formatterbis));
					}else if(lines[i].startsWith("Daily")){		//Gets the previous execution date for the hourly alerts
						DateTime next = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""))).plusDays(1);
						DateTime previous = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", "")));
						json.put("nextDaily",  next.toString(formatterbis));
						json.put("daily", previous.toString(formatterbis));
					}else if(lines[i].startsWith("Weekly")){	//Gets the previous execution date for the hourly alerts
						DateTime next = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", ""))).plusWeeks(1);
						DateTime previous = new DateTime(formatter.parseDateTime(lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()).replaceAll("(\\r|\\n)", "")));
						json.put("nextWeekly",  next.toString(formatterbis));
						json.put("weekly", previous.toString(formatterbis));
					}
				}catch(JSONException | IllegalArgumentException e){
					LOGGER.error("Error while building the JSON answer in the doGet of the alerts administration servlets, make sure the fields are filled correctly and that datafari.properties have the correct encoding charset(UTF_8). Error 69021", e);
					out.append("Error while getting the parameters, please retry, if the problem persists contact your system administrator. Error code : 69021");
					out.close();
					return;
				}
			}
			out.write(json.toString());						//Send all the parameters to the client
			response.setStatus(200);
			response.setContentType("text/json;charset=UTF-8");
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69508");
			out.close();
			LOGGER.error("Unindentified error in alertsAdmin doGet. Error 69508", e);
		}
	}

	/**
	 * Two uses :
	 * When user clicks on turn on/off button, starts or stops the alerts
	 * When user clicks on the parameter saving button, saves all the parameters
	 * @throws IOException 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
		try{
			int launch=0;
			String content ="";
			try {
				content = readFile(env, StandardCharsets.UTF_8);
			} catch (IOException e1) {
				LOGGER.error("Error while reading the datafari.properties of the doPost of the alerts administration servlet doPost. Error 69022 ", e1);
				PrintWriter out = response.getWriter();
				out.append("Error while reading the datafari.properties, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69022");
				out.close();
				return;
			}
			String[] lines = content.split(System.getProperty("line.separator")); 		//read the file line by line
			String linesBis = "";
			if(request.getParameter("on/off")!=null){									//If the user clicked on turn on/off button
				for(int i = 0 ; i < lines.length ; i++){							
					if(lines[i].startsWith("ALERTS")){									//search for the ALERTS line
						if(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("o")).equals("on")){
							launch=2;
							linesBis += lines[i].substring(0, lines[i].indexOf("o"))+"off\n";	//If it was set to on, then set it to off
						}else{
							linesBis += lines[i].substring(0, lines[i].indexOf("o"))+"on\n";	//If it was set to off, then set it to on
							launch=1;
						}
					}else{
						linesBis += lines[i]+"\n";										//for every other lines just get it as is
					}
				}
			}else{																		//If the user clicked on save parameters button
				DateFormat df = new SimpleDateFormat("dd/MM/yyyy/ HH:mm");				//Create a date format and get current time
				DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");
				for(int i = 0 ; i < lines.length ; i++){								//for each line
					try{
						if(lines[i].startsWith("HOURLYDELAY")){							//If it's the HOURLYDELAY line
							if(!request.getParameter("hourlyDelay").equals("")){		//If the parameter is not empty
								DateTime date = new DateTime(df.parse(request.getParameter("hourlyDelay")));
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+date.toString(formatter)+"\n";	//Replaces the delay in the file
							}else
								linesBis += lines[i]+"\n";								//If the parameter is empty keep the line intact
						}else if(lines[i].startsWith("DAILYDELAY")){					//Same but with DAILYDELAY
							if(!request.getParameter("dailyDelay").equals("")){
								DateTime date = new DateTime(df.parse(request.getParameter("dailyDelay")));
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+date.toString(formatter)+"\n";	//Replaces the delay in the file
							}
							else
								linesBis += lines[i]+"\n";
						}else if(lines[i].startsWith("WEEKLYDELAY")){				//Same but with WEEKLYDELAY
							if(!request.getParameter("weeklyDelay").equals("")){
								DateTime date = new DateTime(df.parse(request.getParameter("weeklyDelay")));
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+date.toString(formatter)+"\n";	//Replaces the delay in the file
							}else
								linesBis += lines[i]+"\n";
						}else if(lines[i].startsWith("HOST")){					//Replaces the host if the parameter is not empty
							if(!request.getParameter("host").equals(""))
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("host").replaceAll("\\s+","")+"\n";
							else
								linesBis += lines[i]+"\n";
						}else if(lines[i].startsWith("PORT")){					//Replaces the port if the parameter is not empty
							if(!request.getParameter("port").equals(""))
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("port").replaceAll("\\s+","")+"\n";
							else
								linesBis += lines[i]+"\n";
						}else if(lines[i].startsWith("DATABASE")){				//Replaces the name of the database the parameter is not empty
							if(!request.getParameter("database").equals(""))
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("database").replaceAll("\\s+","")+"\n";
							else
								linesBis += lines[i]+"\n";
						}else if(lines[i].startsWith("COLLECTION")){			//Replaces the name of the collection if the parameter is not empty
							if(!request.getParameter("collection").equals(""))
								linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("collection").replaceAll("\\s+","")+"\n";
							else
								linesBis += lines[i]+"\n";
						}
						else{
							linesBis += lines[i]+"\n";
						}
					}catch(ParseException e){
						LOGGER.error("Error while parsing the dates in doPost, check if the parameters passed are formatted as necessary alerts administration servlet doPost. Error 69023 ", e);
						PrintWriter out = response.getWriter();
						out.append("Error wrong format for the dates, please retry, if the problem persists contact your system administrator. Error code : 69023");
						out.close();
						return;
					}
				}
			}
			try {	
				FileOutputStream fooStream = new FileOutputStream(new File(env), false); 
				byte[] myBytes = linesBis.getBytes();
				fooStream.write(myBytes);										//rewrite the file
				fooStream.close();
			}catch(IOException e){
				LOGGER.error("Error while rewriting the datafari.properties file in doPost, alerts administration servlet. Error 69024", e);
				PrintWriter out = response.getWriter();
				out.append("Error while rewriting the datafari.properties file, please retry, if the problem persists contact your system administrator. Error code : 69024");
				out.close();
				return;
			}
			if(launch == 1)																//If it has been set to on
				AlertsManager.getInstance().turnOn();								//Start Alerts
			else if(launch == 2)
				AlertsManager.getInstance().turnOff();								//Stop Alerts
		}catch(Exception e){
			PrintWriter out = response.getWriter();
			out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69509");
			out.close();
			LOGGER.error("Unindentified error in alertsAdmin doPost. Error 69509", e);
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
