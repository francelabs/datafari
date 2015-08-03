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
import java.nio.file.NoSuchFileException;
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

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.bson.Document;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
public class AlertsManager {
	private static AlertsManager INSTANCE = new AlertsManager();
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> coll1;
	private boolean onOff = false;
	private int port;
	private DateTime delayH, delayD, delayW;
	private DateFormat df = new SimpleDateFormat("dd/MM/yyyy/HH:mm");
	private customBool Hourly = new customBool("Hourly", false), Daily = new customBool("Daily", false), Weekly = new customBool("Weekly", false);
	private String HourlyHour = "", DailyHour = "", WeeklyHour = "", IP="", filePath="", database="", collection="" ;
	private List<customBool> HDW = new ArrayList<customBool>(); 
	private ScheduledFuture<?> alertHandleH, alertHandleD, alertHandleW;
	private ScheduledExecutorService scheduler;
	private Mail mail;
	private Alert alert;
	private final static Logger LOGGER = Logger.getLogger(AlertsManager.class
			.getName());
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
	 * @throws ParseException 
	 */
	public boolean getParameter() throws IOException, ParseException{
		try{
			onOff=false;
			filePath = System.getenv("DATAFARI_HOME");		//Gets the installation directory if in standard environment 
			if(filePath==null)								//If development environment
				filePath = "/home/youp/workspaceTest/Servers/Datafari-config/datafari.properties";	//hardcoded path
			else
				filePath += "/tomcat/conf/datafari.properties";	//completing the path 
			String content;
			String[] lines = new String[0];
			try {
				content = readFile(filePath, StandardCharsets.UTF_8);
			} catch (NoSuchFileException e1) {
				LOGGER.error("Error while reading the datafari.properties in the AlertsManager getParameter(), make sure the file exists at : "+filePath+" . Error 69037 ", e1);
				return false;
			}
			lines = content.split(System.getProperty("line.separator"));	//Read the file line by line
			for(int i = 0; i < lines.length ; i++){							//For each line
				if(lines[i].startsWith("ALERTS") && lines[i].substring(lines[i].indexOf("=")+1).equals("on"))
					onOff = true;
				else if(lines[i].startsWith("HOURLYDELAY"))	{				//Gets the delay for the hourly alerts
					try{	
						delayH = new DateTime(df.parse(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1)));
					}catch(ParseException e){
						LOGGER.warn("Error parsing the Hourly Date, default value will be used, AlertsManage getParameter()r",e);
						delayH = new DateTime(df.parse("01/01/0001/00:00"));
					}
				}
				else if(lines[i].startsWith("DAILYDELAY"))	{					//Gets the delay for the daily alerts
					try{	
						delayD = new DateTime(df.parse(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1)));
					}catch(ParseException e){
						LOGGER.warn("Error parsing the Daily Date, default value will be used, AlertsManager getParameter()",e);
						delayD = new DateTime(df.parse("01/01/0001/00:00"));
					}				
				}
				else if(lines[i].startsWith("WEEKLYDELAY"))	{					//Gets the delay for the weekly alerts
					try{	
						delayW = new DateTime(df.parse(lines[i].replaceAll("(\\r|\\n)", "").substring(lines[i].indexOf("=")+1)));
					}catch(ParseException e){
						LOGGER.warn("Error parsing the Weekly Date, default value will be used, AlertsManager getParameter()",e);
						delayW = new DateTime(df.parse("01/01/0001/00:00"));
					}				
				}
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
			}
			return onOff;
		}catch(Exception e){
			LOGGER.error("Unindentified error in the AlertsManager getParameter(). Error 69516 ", e);
			return false;
		}
	}
	/**
	 * Creates the runnables
	 * Calculates the delays if necessary
	 * Schedules the runnables
	 * @throws IOException 
	 */
	private void startScheduled() throws IOException {
		try{
			scheduler = Executors.newScheduledThreadPool(1);
			final Runnable alertHourly = new Runnable() {					//Runnable that runs every hour
				public void run() {
					try {
						alerts("Hourly");
					} catch (Exception e) {
						LOGGER.error("Unindentified error while running the hourly alerts in startScheduled(), AlertsManager. Error 69518", e);
					} }
			};
			final Runnable alertDaily = new Runnable() {					//Runnable that runs every Day
				public void run() {
					try {
						alerts("Daily");
					} catch (Exception e) {
						LOGGER.error("Unindentified error while running the daily alerts in startScheduled(), AlertsManager. Error 69519", e);
					} }
			};
			final Runnable alertWeekly = new Runnable() {					//Runnable that runs every Week
				public void run() {
					try {
						alerts("Weekly");
					} catch (Exception e) {
						LOGGER.error("Unindentified error while running the weekly alerts in startScheduled(), AlertsManager. Error 69520", e);
					} }
			};
			mail = new Mail();
			DateTime currentDate = new DateTime();	
			alertHandleH = launch(Hourly, delayH, alertHandleH, currentDate,"Hourly", HourlyHour, alertHourly, 60);		//Launches the alerts according to their previous execution and the date typed by the user
			alertHandleD = launch(Daily, delayD, alertHandleD, currentDate,"Daily", DailyHour, alertDaily, 1440);
			alertHandleW = launch(Weekly, delayW, alertHandleW, currentDate,"Weekly", WeeklyHour, alertWeekly, 10080);
		}catch(Exception e){
			LOGGER.error("Unindentified error in the AlertsManager startScheduled(). Error 69517", e);
			return;
		}
	}
	/**
	 * Launches the alerts
	 * @param custom	the customBool to know if alerts of this frequency have been launched
	 * @param delay		The Date typed by the user
	 * @param Handle	the Scheduler corresponding to the frequency
	 * @param current	The current time
	 * @param frequency	The frequency of the alerts you launch
	 * @param Hour		The time of the previous execution
	 * @param run		The runnable that run the alerts
	 * @param loop 		The number of minutes between each execution
	 */
	public ScheduledFuture<?> launch(customBool custom, DateTime delay, ScheduledFuture<?> Handle, DateTime current, String frequency, String Hour, Runnable run, long loop){
		try{
			if((custom.isBool()) && (delay.plusMinutes(5).isBefore(current)))								//If there has been a previous execution and the date typed in the UI was more than 5 minutes before the current date			
				Handle = scheduler.scheduleAtFixedRate(run, calculateDelays(frequency, loop, Hour), loop, TimeUnit.MINUTES);	//Launches alerts() every hour with the "Hourly" parameter and as an initial delay, the difference calculated previously 
			else if(custom.isBool() && (delay.minusMinutes(10).isAfter(current)))
				Handle = scheduler.scheduleAtFixedRate(run, calculateDelays(frequency, delay), loop, TimeUnit.MINUTES);//Launches alerts() every hour with the "Hourly" parameter and as an initial delay, the difference between the current date and the date set in the UI 
			else
				Handle = scheduler.scheduleAtFixedRate(run, 0, loop, TimeUnit.MINUTES);		//Launches alerts() every hour with the "Hourly" parameter instantly
			return Handle;
		}catch (Exception e){
			LOGGER.error("Error while calculating the delay to launch the "+frequency+" alerts in the AlertsManager launch(). Error 69038", e);
			return null;
		}
	}
	/**
	 * Calculates the initial delays according to the frequency, and the previous execution
	 * Only called when one of the hours typed in the UI (or more) was prior to the current date or invalid and if there has been a previous execution
	 * @param frequency 
	 * @param minutes the number of minutes corresponding to an hour, a day, a week, according to the frequency
	 * @param hour the last execution of the alert according to the frequency
	 * @return the initial delay
	 */
	private long calculateDelays(String frequency, long minutes, String hour){
		try {
			long diff = 0;
			try{
				DateTime dt1 = new DateTime(df.parse(hour));				//Parses the previous execution date
				String now = df.format(new Date());		
				DateTime dt2 = new DateTime(df.parse(now));					//Gets the current date
				Interval interval = new Interval(dt1, dt2);					//Gets the interval
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();	
			}catch(ParseException e){
				LOGGER.error("Error while parsing the dates to schedule the "+frequency+" alerts in calculateDelays(), AlertsManager. Error 69039", e);
				throw new RuntimeException();
			}
			diff = minutes - diff;										//Calculates the number of minutes that has still to go before launch
			if(diff<0){													//if it's under 0 the it is set to 0
				diff = 0;
			}
			return diff;
		}catch (Exception e){
			LOGGER.error("Error while calculating the delay to launch the "+frequency+" alerts in the AlertsManager calculateDelays(). Error 69518", e);
			return 0;
		}
	}
	/**
	 * Calculates the initial delay according to a given time and the current date
	 * Called when dates are regular and it must also not be the first time that the alerts (of a specific frequency) are runned.
	 * @param frequency
	 * @param Hour the parameter typed in the UI 
	 * @return the initial delay
	 */
	private long calculateDelays(String frequency, DateTime hour){
		try {
			try{
				long diff = 0;		
				String now = df.format(new Date());		
				DateTime dt2 = new DateTime(df.parse(now));					//Gets the current date
				Interval interval = new Interval(dt2, hour);					//Gets the interval
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();						
				return diff;
			}
			catch(ParseException e){
				LOGGER.error("Error while parsing the dates to schedule the "+frequency+" alerts in calculateDelays(), AlertsManager. Error 69040", e);
				throw new RuntimeException();
			}
		}catch (Exception e){
			LOGGER.error("Unindentified error while calculating the delay to launch the "+frequency+" alerts in the AlertsManager calculateDelays(). Error 69519", e);
			return 0;
		}
	}
	/**
	 * Updates the datafari.properties file
	 * Run the alerts with the correct frequency
	 * @param frequency : Hourly/Daily/Weekly
	 */
	private void alerts(String frequency){
		try{
			boolean bool = false;
			for(customBool c : HDW){								//Checks if alerts with the correct frequency have already run at least once
				if(c.getFrequency().equals(frequency) && c.isBool()){
					bool = true;
				}
			}
			Date date = new Date();
			try {													//Append the current d
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
				LOGGER.error("Error while appending the moment of execution to the datafari.properties file in alerts(), AlertsManager. Error 69041 ", e);
				return;
			}
			FindIterable<Document> cursor = coll1.find();							//Get all the elements in the collection
			Core[] core = Core.values();
			for (Document d : cursor) {												//Get the next Object in the collection
				if(frequency.equals(d.get("frequency").toString())){	
					SolrServer solr=null;
					for(int i=0; i<core.length;i++){								//Get the right core by comparing all the return of the Enum Type Core to the one in the database
						if(d.get("core").toString().toUpperCase().equals(""+core[i].toString().toUpperCase())){
							try {
								solr = SolrServers.getSolrServer(core[i]);
							} catch (IOException e) {
								LOGGER.error("Error while getting the Solr core in alerts(), AlertsManager. Error 69042 ", e);
								return;
							}
						}
					}//Creates an alert with the attributes of the element found in the database.
					alert = new Alert(d.get("subject").toString(), d.get("mail").toString(), solr, d.get("keyword").toString(), d.get("frequency").toString(), mail, d.get("user").toString());
					alert.run();													//Makes the request and send the mail if they are some results
				}
			}
		}catch(Exception e){
			LOGGER.error("Unindentified error while running  the "+frequency+" alerts in the AlertsManager alerts(). Error 69520", e);
			return;
		}
	}
	/**
	 * Gets the parameters
	 * Establishes the connection to the database
	 * Starts the alerts
	 * @throws IOException
	 * @throws ParseException 
	 */
	public void turnOn() throws IOException{
		try {
			if(this.getParameter()){
				mongoClient = new MongoClient(IP, port);						//Connect to the mongoDB database
				db = mongoClient.getDatabase(database);							//Switch to the right Database
				coll1 = db.getCollection(collection);							//Switch to the right Collection
				this.startScheduled();											//Starts the scheduled task
			}
		} catch (ParseException e) {
			LOGGER.error("Error while turning on the alerts during instantiation, AlertsManager turnOn(). Error 69043", e);
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
		mail = null;
	}
	static String readFile(String path, Charset encoding) 					//Read the file
			throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}