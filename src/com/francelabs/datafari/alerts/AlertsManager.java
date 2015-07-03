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
package com.francelabs.datafari.alerts;
/**
 * 
 * This class is used to get the parameters for the alerts and then launch them.
 * It is called at the start of Datafari and when you turn off or on the alerts in the Alerts Administration UI
 * It is a singleton
 * getParameter reads the file and take what it needs then if necessary make the connection with the mongoDB database
 * startScheduled creates the runnable calculates the delays or the first launch and starts the schedules the tasks
 * alerts is used to run all the alerts of a given frequency
 * If you are in development environment the path to the datafari.properties is hardcoded
 * @author Alexis Karassev
 *
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.francelabs.datafari.solrj.SolrServers;
import com.francelabs.datafari.solrj.SolrServers.Core;

import org.apache.solr.client.solrj.SolrServer;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class AlertsManager {
	private static AlertsManager INSTANCE = new AlertsManager();
	private MongoClient mongoClient;
	private DB db;
	private DBCollection coll1;
	private boolean onOff=false;
	private int delayH, delayD, delayW, port;
	private DateFormat df = new SimpleDateFormat("dd/MM/yyyy/HH:mm:ss");
	private customBool Hourly = new customBool("Hourly", false), Daily = new customBool("Daily", false), Weekly = new customBool("Weekly", false);
	private String HourlyHour = "", DailyHour = "", WeeklyHour = "", IP="", filePath="", database="", collection="" ;
	private List<customBool> HDW = new ArrayList<customBool>(); 
	private ScheduledFuture<?> alertHandleH, alertHandleD, alertHandleW;
	private ScheduledExecutorService scheduler;
	private AlertsManager(){							//Booleans to know if there has been a previous execution for a given frequency
		HDW.add(Hourly);
		HDW.add(Daily);
		HDW.add(Weekly);
	}
	public static AlertsManager getInstance(){			//Singleton 
		return INSTANCE;
	}

	/**
	 * Gets the path of datafari.properties file
	 * Reads the file to fill the variables
	 * if the ALERTS line was set to true, then it establishes the connection with the MongoDB database
	 */
	@SuppressWarnings("deprecation")
	public void getParameter(){
		filePath = System.getenv("DATAFARI_HOME");		//Gets the installation directory if in standard environment 
		if(filePath==null)								//If development environment
			filePath = "/home/youp/workspace/Servers/Datafari-config/datafari.properties";	//hardcoded path
		else
			filePath += "tomcat/conf/datafari.properties";	//completing the path 
		String content;
		String[] lines = new String[0];
		try {
			content = readFile(filePath, StandardCharsets.UTF_8);
			lines = content.split(System.getProperty("line.separator"));	//Read the file line by line
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(int i = 0; i < lines.length ; i++){								//For each line
			try{
				if(lines[i].startsWith("ALERTS")){								//If it starts with alerts, reads if it's on "on" of "off"
					if(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("o")).equals("on"))
						onOff=true;
				}else if(lines[i].startsWith("HOURLYDELAY"))					//Gets the delay for the hourly alerts
					delayH = Integer.parseInt(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1));
				else if(lines[i].startsWith("DAILYDELAY"))						//Gets the delay for the daily alerts
					delayD = Integer.parseInt(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1));
				else if(lines[i].startsWith("WEEKLYDELAY"))						//Gets the delay for the weekly alerts
					delayW = Integer.parseInt(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1));
				else if(lines[i].startsWith("HOST"))							//Gets the address of the host
					IP = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				else if(lines[i].startsWith("PORT"))							//Gets the port
					port = Integer.parseInt(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1));
				else if(lines[i].startsWith("DATABASE"))						//Gets the name of the database
					database = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				else if(lines[i].startsWith("COLLECTION"))						//Gets the name of the collection
					collection = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				else if(lines[i].startsWith("Hourly=")){						//Checks if there has been a previous execution for hourly alerts
					Hourly.setBool(true);
					HourlyHour = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				}else if(lines[i].startsWith("Daily=")){						//Checks if there has been a previous execution for daily alerts
					Daily.setBool(true);
					DailyHour = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				}else if(lines[i].startsWith("Weekly=")){						//Checks if there has been a previous execution for weekly alerts
					Weekly.setBool(true);
					WeeklyHour = lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(onOff){															//If the ALERTS line was set to "on"
			mongoClient = new MongoClient(IP, port);						//Connect to the mongoDB database
			db = mongoClient.getDB(database);								//Switch to the right Database
			coll1 = db.getCollection(collection);							//Switch to the right Collection
			this.startScheduled();										//Starts the scheduled task
		}
	}
	/**
	 * Creates the runnables
	 * Calculates the delays if necessary
	 * Schedules the runnables
	 * 
	 */
	private void startScheduled() {
		scheduler = Executors.newScheduledThreadPool(1);
		final Runnable alertHourly = new Runnable() {					//Runnable that runs every hour
			public void run() {
				alerts("Hourly"); }
		};
		final Runnable alertDaily = new Runnable() {					//Runnable that runs every Day
			public void run() {
				alerts("Daily"); }
		};
		final Runnable alertWeekly = new Runnable() {					//Runnable that runs every Week
			public void run() {
				alerts("Weekly"); }
		};
		long diff = 0;
		try{
			if(Hourly.isBool() && delayH==-1){								//If there has been a previous execution and the date typed tin the UI was invalid
				DateTime dt1 = new DateTime(df.parse(HourlyHour));			//Parses the previous execution date
				String now = df.format(new Date());		
				DateTime dt2 = new DateTime(df.parse(now));					//Gets the current date
				Interval interval = new Interval(dt1, dt2);					//Gets the interval
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();						
				diff = 60 - diff;											//Gets the number of minutes that has still to go before launch
				if(diff<0){													//if it's under 0 the it is set to 0
					diff = 0;
				}
				alertHandleH = scheduler.scheduleAtFixedRate(alertHourly, diff, 60, TimeUnit.MINUTES);	//Launches alerts() every hour with the "Hourly" parameter and as an initial delay, the difference calculated previously 
			}else if(Hourly.isBool()){
				alertHandleH = scheduler.scheduleAtFixedRate(alertHourly, delayH, 60, TimeUnit.MINUTES);//Launches alerts() every hour with the "Hourly" parameter and as an initial delay, the difference between the current date and the date set in the UI 
			}else{
				alertHandleH = scheduler.scheduleAtFixedRate(alertHourly, 0, 60, TimeUnit.MINUTES);		//Launches alerts() every hour with the "Hourly" parameter instantly
			}
			if(Daily.isBool() && delayD==-1){								//Same as above but with Daily alerts
				DateTime dt1 = new DateTime(df.parse(DailyHour));
				String now = df.format(new Date());
				DateTime dt2 = new DateTime(df.parse(now));
				Interval interval = new Interval(dt1, dt2);
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();
				diff = 1440 - diff;
				if(diff<0){
					diff = 0;
				};
				alertHandleD = scheduler.scheduleAtFixedRate(alertDaily, diff, 1440, TimeUnit.MINUTES);	
			}else if(Daily.isBool()){
				alertHandleD = scheduler.scheduleAtFixedRate(alertDaily, delayD, 1440, TimeUnit.MINUTES);	
			}else{
				alertHandleD = scheduler.scheduleAtFixedRate(alertDaily, 0, 1440, TimeUnit.MINUTES);	
			}
			if(Weekly.isBool() && delayW==-1){								//Same as above but with Weekly alerts
				DateTime dt1 = new DateTime(df.parse(WeeklyHour));
				String now = df.format(new Date());
				DateTime dt2 = new DateTime(df.parse(now));
				Interval interval = new Interval(dt1, dt2);
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();
				diff = 10080 - diff;
				if(diff<0){
					diff = 0;
				}
				alertHandleW = scheduler.scheduleAtFixedRate(alertWeekly, diff, 10080, TimeUnit.MINUTES);
			}else if(Weekly.isBool()){
				alertHandleW = scheduler.scheduleAtFixedRate(alertWeekly, delayW, 10080, TimeUnit.MINUTES);
			}else{
				alertHandleW = scheduler.scheduleAtFixedRate(alertWeekly, 0, 10080, TimeUnit.MINUTES);
			}
		}catch(ParseException e){
			e.printStackTrace();
		}
	}
	/**
	 * Updates the datafari.properties file
	 * Run the alerts with the correct frequency
	 * @param frequency : Hourly/Daily/Weekly
	 */
	private void alerts(String frequency) {
		boolean bool = false;
		for(customBool c : HDW){								//Checks if alerts with the correct frequency have already run at least once
			if(c.getFrequency().equals(frequency) && c.isBool()){
				bool = true;
			}
		}
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy/HH:mm:ss");
		Date date = new Date();
		try {
			if(bool){											//If alerts have already run
				String content = readFile(filePath, StandardCharsets.UTF_8);
				String[] lines = content.split(System.getProperty("line.separator"));
				String linesBis = "";							//Reads the file line by line
				for(int i = 0 ; i < lines.length ; i++){
					if(lines[i].startsWith(frequency)){			//If it's the correct frequency line
						linesBis += lines[i].substring(0, lines[i].indexOf("=")+1)+df.format(date).toString()+"\n";	//Replace the date by the current one
					}else{
						linesBis += lines[i]+"\n";				//Else just keeps the line
					}
				}
				FileOutputStream fooStream = new FileOutputStream(new File(filePath), false); 
				byte[] myBytes = linesBis.getBytes();
				fooStream.write(myBytes);						//rewrite the file
				fooStream.close();
			}else{												//Else just appends the frequency and the current date
				FileOutputStream fooStream = new FileOutputStream(new File(filePath), true);
				byte[] myBytes = (frequency+"="+df.format(date)+"\n").getBytes();
				fooStream.write(myBytes);						//Append to the file
				fooStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		DBCursor cursor = coll1.find();							//Get all the elements in the collection
		SolrServer solr=null;
		Core[] core = Core.values();
		while (cursor.hasNext()) {
			DBObject dbo = cursor.next();											//Get the next Object in the collection
			if(frequency.equals(dbo.get("frequency").toString())){	
				for(int i=0; i<core.length;i++){								//Get the right core by comparing all the return of the Enum Type Core to the one in the database
					if(dbo.get("core").toString().toUpperCase().equals(""+core[i].toString().toUpperCase())){
						try {
							solr = SolrServers.getSolrServer(core[i]);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}//Creates an alert with the attributes of the element found in the database.
				Alert alert = new Alert(dbo.get("subject").toString(), dbo.get("address").toString(), solr, dbo.get("keyword").toString(), dbo.get("frequency").toString());
				alert.run();													//Makes the request and send the mail if they are some results
			}
		}

	}
	/**
	 * Closes the database connection
	 * Cancels the scheduled runnables
	 * Resets various variables
	 */
	public void turnOff(){
		if(alertHandleD!=null){
			alertHandleD.cancel(true);
			alertHandleH.cancel(true);
			alertHandleW.cancel(true);
			scheduler.shutdownNow();
			mongoClient.close();
			Hourly.setBool(false);
			Daily.setBool(false);
			Weekly.setBool(false);
		}
		filePath = null;
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

}
