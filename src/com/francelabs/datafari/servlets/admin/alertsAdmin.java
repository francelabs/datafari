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
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
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

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public alertsAdmin() {
		super();
	}

	/**
	 * Gets the required parameters parameters
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)  {
		env = System.getenv("DATAFARI_HOME");							//Gets the directory of installation if in standard environment	
		if(env==null){													//If in development environment
			env = "/home/youp/workspaceTest/Servers/Datafari-config/datafari.properties";	//Hardcoded path
		}else{
			env = env+"tomcat/conf/datafari.properties";
		}
		String content ="";
		PrintWriter pw = null;
		try {
			content = readFile(env, StandardCharsets.UTF_8); 
			pw = response.getWriter();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[] lines = content.split(System.getProperty("line.separator"));	//read the file line by line
		JSONObject SuperJson = new JSONObject();						
		JSONObject json = new JSONObject();
		for(int i = 0 ; i < lines.length ; i++){				//for each line
			try{
				if(lines[i].startsWith("ALERTS")){			
					if(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("o")).equals("on")){
						json.put("on", "on");					//If the alerts are activated puts "on" in the JSON (so the button will print "turn off" in the UI)
					}else{
						json.put("on", "off");					//If the alerts are deactivated puts "off" in the JSON (so the button will print "turn on" in the UI)
					}

				}else if(lines[i].startsWith("HOST")){			//Gets the address of the host
					json.put("host", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()));
				}else if(lines[i].startsWith("PORT")){			//Gets the port
					json.put("port", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()));
				}else if(lines[i].startsWith("DATABASE")){		//Gets the name of the database
					json.put("database", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()));
				}else if(lines[i].startsWith("COLLECTION")){	//Gets the name of the collection
					json.put("collection", lines[i].substring(lines[i].indexOf("=")+1, lines[i].length()));
				}
			}catch(Exception e){
				e.printStackTrace(); 
			}
		}
		try {
			SuperJson.put("response", json);
			pw.write(json.toString());						//Send all the parameters to the client
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.setStatus(200);
		response.setContentType("text/json;charset=UTF-8");
	}

	/**
	 * Two uses :
	 * When user clicks on turn on/off button, starts or stops the alerts
	 * When user clicks on the parameter saving button, saves all the parameters
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response){
		boolean launch=false;
		String content ="";
		try {
			content = readFile(env, StandardCharsets.UTF_8);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[] lines = content.split(System.getProperty("line.separator")); 		//read the file line by line
		String linesBis = "";
		if(request.getParameter("on/off")!=null){									//If the user clicked on turn on/off button
			for(int i = 0 ; i < lines.length ; i++){							
				if(lines[i].startsWith("ALERTS")){									//search for the ALERTS line
					if(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("o")).equals("on")){
						linesBis += lines[i].substring(0, lines[i].indexOf("o"))+"off\n";	//If it was set to on, then set it to off
					}else{
						linesBis += lines[i].substring(0, lines[i].indexOf("o"))+"on\n";	//If it was set to off, then set it to on
						launch=true;
					}
				}else{
					linesBis += lines[i]+"\n";										//for every other lines just get it as is
				}
			}
			if(launch)																//If it has been set to on
				AlertsManager.getInstance().getParameter();							//Start Alerts
			else
				AlertsManager.getInstance().turnOff();								//Stop Alerts
		}else{																		//If the user clicked on save parameters button
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");				//Create a date format and get current time
			DateTime currentDate = new DateTime();
			long delay;
			for(int i = 0 ; i < lines.length ; i++){								//for each line
				try{
					if(lines[i].startsWith("HOURLYDELAY")){							//If it's the HOURLYDELAY line
						if(!request.getParameter("hourlyDelay").equals("")){		//If the parameter is not empty
							DateTime hourlyDate = new DateTime(df.parse(request.getParameter("hourlyDelay")));
							if(hourlyDate.compareTo(currentDate.minusMinutes(5))>0 && hourlyDate.compareTo(currentDate)<0){
								delay=0;											//If the given hour is less than five minutes past, the alert will be launched instantly
							}else{													//Else we get the interval in minute as a delay
								try{												
									Interval interval = new Interval(currentDate, hourlyDate);
									Duration duration = interval.toDuration();
									delay = duration.getStandardMinutes();
								}catch(IllegalArgumentException e){					//If the given date is anterior to the current date then the AlertsManager will calculate the delay in function of the previous execution
									delay = -1;
								}
							}
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+delay+"\n";	//Replaces the delay in the file
						}else
							linesBis += lines[i]+"\n";								//If the parameter is empty keep the line intact
					}else if(lines[i].startsWith("DAILYDELAY")){					//Same but with DAILYDELAY
						if(!request.getParameter("dailyDelay").equals("")){
							DateTime dailyDate = new DateTime(df.parse(request.getParameter("dailyDelay")));
							if(dailyDate.compareTo(currentDate.minusMinutes(5))>0 && dailyDate.compareTo(currentDate)<0){
								delay=0;
							}else{
								try{
									Interval interval = new Interval(currentDate, dailyDate);
									Duration duration = interval.toDuration();
									delay = duration.getStandardMinutes();
								}catch(IllegalArgumentException e){
									delay = -1;
								}
							}
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+delay+"\n";	
						}
						else
							linesBis += lines[i]+"\n";
					}else if(lines[i].startsWith("WEEKLYDELAY")){				//Same but with WEEKLYDELAY
						if(!request.getParameter("weeklyDelay").equals("")){
							DateTime weeklyDate = new DateTime(df.parse(request.getParameter("weeklyDelay")));
							if(weeklyDate.compareTo(currentDate.minusMinutes(5))>0 && weeklyDate.compareTo(currentDate)<0){
								delay=0;
							}else{
								try{
									Interval interval = new Interval(currentDate, weeklyDate);
									Duration duration = interval.toDuration();
									delay = duration.getStandardMinutes();
								}catch(IllegalArgumentException e){
									delay = -1;
								}
							}
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+delay+"\n";	
						}
						else
							linesBis += lines[i]+"\n";
					}else if(lines[i].startsWith("HOST")){					//Replaces the host if the parameter is not empty
						if(!request.getParameter("host").equals(""))
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("host")+"\n";
						else
							linesBis += lines[i]+"\n";
					}else if(lines[i].startsWith("PORT")){					//Replaces the port if the parameter is not empty
						if(!request.getParameter("port").equals(""))
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("port")+"\n";
						else
							linesBis += lines[i]+"\n";
					}else if(lines[i].startsWith("DATABASE")){				//Replaces the name of the database the parameter is not empty
						if(!request.getParameter("database").equals(""))
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("database")+"\n";
						else
							linesBis += lines[i]+"\n";
					}else if(lines[i].startsWith("COLLECTION")){			//Replaces the name of the collection if the parameter is not empty
						if(!request.getParameter("collection").equals(""))
							linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+request.getParameter("collection")+"\n";
						else
							linesBis += lines[i]+"\n";
					}
					else{
						linesBis += lines[i]+"\n";
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		try {	
			FileOutputStream fooStream = new FileOutputStream(new File(env), false); 
			byte[] myBytes = linesBis.getBytes();
			fooStream.write(myBytes);										//rewrite the file
			fooStream.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
