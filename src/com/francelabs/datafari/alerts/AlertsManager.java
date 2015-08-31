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
 * getParameter reads the file and take what it needs then if necessary make the connection with the db
 * startScheduled creates the runnable calculates the delays or the first launch and starts the schedules the tasks
 * alerts is used to run all the alerts of a given frequency
 * If you are in development environment the path to the datafari.properties is hardcoded
 * @author Alexis Karassev
 *
 */
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.francelabs.datafari.service.db.AlertDataService;
import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.utils.ScriptConfiguration;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;


public class AlertsManager {
	private static AlertsManager INSTANCE = new AlertsManager();

	private boolean onOff = false;
	private DateTime delayH, delayD, delayW;
	private DateFormat df = new SimpleDateFormat("dd/MM/yyyy/HH:mm");
	private AlertFrequencyFirstExecution Hourly = new AlertFrequencyFirstExecution(
			AlertFrequencyFirstExecution.AlertFrequency.HOURLY, false);
	private AlertFrequencyFirstExecution Daily = new AlertFrequencyFirstExecution(
			AlertFrequencyFirstExecution.AlertFrequency.DAILY, false);
	private AlertFrequencyFirstExecution Weekly = new AlertFrequencyFirstExecution(
			AlertFrequencyFirstExecution.AlertFrequency.WEEKLY, false);

	private String HourlyHour = "", DailyHour = "", WeeklyHour = "",
			filePath = "";
	private List<AlertFrequencyFirstExecution> HDW = new ArrayList<AlertFrequencyFirstExecution>();
	private ScheduledFuture<?> alertHandleH, alertHandleD, alertHandleW;
	private ScheduledExecutorService scheduler;
	private Mail mail;
	private Alert alert;
	private final static Logger LOGGER = Logger.getLogger(AlertsManager.class
			.getName());

	private AlertsManager() { // Booleans to know if there has been a previous
								// execution for a given frequency
		HDW.add(Hourly);
		HDW.add(Daily);
		HDW.add(Weekly);
	}

	public static AlertsManager getInstance() { // Singleton
		return INSTANCE;
	}

	/**
	 * Gets the path of datafari.properties file Reads the file to fill the
	 * variables if the ALERTS line was set to true, then it establishes the
	 * connection with the  database
	 * 
	 * @throws ParseException
	 */
	public boolean getParameter() throws IOException, ParseException {
		try {
			if (ScriptConfiguration.getProperty("ALERTS").equals("on")) {
				onOff = true;
			}

			// Gets the delay for the hourly alerts
			try {
				delayH = new DateTime(df.parse(ScriptConfiguration
						.getProperty("HOURLYDELAY")));
			} catch (ParseException e) {
				LOGGER.warn(
						"Error parsing the Hourly Date, default value will be used, AlertsManager getParameter()",
						e);
				delayH = new DateTime(df.parse("01/01/0001/00:00"));
			}

			// Gets the delay for the daily alerts
			try {
				delayD = new DateTime(df.parse(ScriptConfiguration
						.getProperty("DAILYDELAY")));
			} catch (ParseException e) {
				LOGGER.warn(
						"Error parsing the Daily Date, default value will be used, AlertsManager getParameter()",
						e);
				delayD = new DateTime(df.parse("01/01/0001/00:00"));
			}

			// Gets the delay for the weekly alerts
			try {
				delayW = new DateTime(df.parse(ScriptConfiguration
						.getProperty("WEEKLYDELAY")));
			} catch (ParseException e) {
				LOGGER.warn(
						"Error parsing the Weekly Date, default value will be used, AlertsManager getParameter()",
						e);
				delayW = new DateTime(df.parse("01/01/0001/00:00"));
			}


			// Checks if there has been a previous execution for alerts
			String hourlyStr = ScriptConfiguration.getProperty("Hourly");
			if (hourlyStr != null) {
				Hourly.setHasBeenExecuted(true);
				HourlyHour = hourlyStr;
			}
			String dailyStr = ScriptConfiguration.getProperty("Daily");
			if (dailyStr != null) {
				Daily.setHasBeenExecuted(true);
				DailyHour = dailyStr;
			}
			String weeklyStr = ScriptConfiguration.getProperty("Weekly");
			if (weeklyStr != null) {
				Weekly.setHasBeenExecuted(true);
				WeeklyHour = weeklyStr;
			}
			LOGGER.info("Alert config file successfully read");
			return onOff;
		} catch (Exception e) {
			LOGGER.error(
					"Unindentified error in the AlertsManager getParameter(). Error 69516 ",
					e);
			return false;
		}
	}

	/**
	 * Creates the runnables Calculates the delays if necessary Schedules the
	 * runnables
	 * 
	 * @throws IOException
	 */
	private void startScheduled() throws IOException {
		try {
			scheduler = Executors.newScheduledThreadPool(1);
			final Runnable alertHourly = new Runnable() { // Runnable that runs
															// every hour
				public void run() {
					try {
						alerts("Hourly");
					} catch (Exception e) {
						LOGGER.error(
								"Unindentified error while running the hourly alerts in startScheduled(), AlertsManager. Error 69518",
								e);
					}
				}
			};
			final Runnable alertDaily = new Runnable() { // Runnable that runs
															// every Day
				public void run() {
					try {
						alerts("Daily");
					} catch (Exception e) {
						LOGGER.error(
								"Unindentified error while running the daily alerts in startScheduled(), AlertsManager. Error 69519",
								e);
					}
				}
			};
			final Runnable alertWeekly = new Runnable() { // Runnable that runs
															// every Week
				public void run() {
					try {
						alerts("Weekly");
					} catch (Exception e) {
						LOGGER.error(
								"Unindentified error while running the weekly alerts in startScheduled(), AlertsManager. Error 69520",
								e);
					}
				}
			};
			mail = new Mail();

			DateTime currentDate = new DateTime();
			alertHandleH = launch(Hourly, delayH, alertHandleH, currentDate,
					"Hourly", HourlyHour, alertHourly, 60); // Launches the
															// alerts according
															// to their previous
															// execution and the
															// date typed by the
															// user
			alertHandleD = launch(Daily, delayD, alertHandleD, currentDate,
					"Daily", DailyHour, alertDaily, 1440);
			alertHandleW = launch(Weekly, delayW, alertHandleW, currentDate,
					"Weekly", WeeklyHour, alertWeekly, 10080);
		} catch (Exception e) {
			LOGGER.error(
					"Unindentified error in the AlertsManager startScheduled(). Error 69517",
					e);
			return;
		}
	}

	/**
	 * Launches the alerts
	 * 
	 * @param custom
	 *            the customBool to know if alerts of this frequency have been
	 *            launched
	 * @param delay
	 *            The Date typed by the user
	 * @param Handle
	 *            the Scheduler corresponding to the frequency
	 * @param current
	 *            The current time
	 * @param frequency
	 *            The frequency of the alerts you launch
	 * @param Hour
	 *            The time of the previous execution
	 * @param run
	 *            The runnable that run the alerts
	 * @param loop
	 *            The number of minutes between each execution
	 */
	public ScheduledFuture<?> launch(AlertFrequencyFirstExecution custom, DateTime delay,
			ScheduledFuture<?> Handle, DateTime current, String frequency,
			String Hour, Runnable run, long loop) {
		try {
			if ((custom.hasBeenExecuted()) && (delay.plusMinutes(5).isBefore(current))) // If
																				// there
																				// has
																				// been
																				// a
																				// previous
																				// execution
																				// and
																				// the
																				// date
																				// typed
																				// in
																				// the
																				// UI
																				// was
																				// more
																				// than
																				// 5
																				// minutes
																				// before
																				// the
																				// current
																				// date
				Handle = scheduler.scheduleAtFixedRate(run,
						calculateDelays(frequency, loop, Hour), loop,
						TimeUnit.MINUTES); // Launches alerts() every hour with
											// the "Hourly" parameter and as an
											// initial delay, the difference
											// calculated previously
			else if (custom.hasBeenExecuted()
					&& (delay.minusMinutes(10).isAfter(current)))
				Handle = scheduler.scheduleAtFixedRate(run,
						calculateDelays(frequency, delay), loop,
						TimeUnit.MINUTES);// Launches alerts() every hour with
											// the "Hourly" parameter and as an
											// initial delay, the difference
											// between the current date and the
											// date set in the UI
			else
				Handle = scheduler.scheduleAtFixedRate(run, 0, loop,
						TimeUnit.MINUTES); // Launches alerts() every hour with
											// the "Hourly" parameter instantly
			return Handle;
		} catch (Exception e) {
			LOGGER.error(
					"Unindentified error while calculating the delay to launch the "
							+ frequency
							+ " alerts in the AlertsManager launch(). Error 69038",
					e);
			return null;
		}
	}

	/**
	 * Calculates the initial delays according to the frequency, and the
	 * previous execution Only called when one of the hours typed in the UI (or
	 * more) was prior to the current date or invalid and if there has been a
	 * previous execution
	 * 
	 * @param frequency
	 * @param minutes
	 *            the number of minutes corresponding to an hour, a day, a week,
	 *            according to the frequency
	 * @param hour
	 *            the last execution of the alert according to the frequency
	 * @return the initial delay
	 */
	private long calculateDelays(String frequency, long minutes, String hour) {
		try {
			long diff = 0;
			try {
				DateTime dt1 = new DateTime(df.parse(hour)); // Parses the
																// previous
																// execution
																// date
				String now = df.format(new Date());
				DateTime dt2 = new DateTime(df.parse(now)); // Gets the current
															// date
				Interval interval = new Interval(dt1, dt2); // Gets the interval
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();
			} catch (ParseException e) {
				LOGGER.error(
						"Error while parsing the dates to schedule the "
								+ frequency
								+ " alerts in calculateDelays(), AlertsManager. Error 69039",
						e);
				throw new RuntimeException();
			}
			diff = minutes - diff; // Calculates the number of minutes that has
									// still to go before launch
			if (diff < 0) { // if it's under 0 the it is set to 0
				diff = 0;
			}
			return diff;
		} catch (Exception e) {
			LOGGER.error(
					"Error while calculating the delay to launch the "
							+ frequency
							+ " alerts in the AlertsManager calculateDelays(). Error 69518",
					e);
			return 0;
		}
	}

	/**
	 * Calculates the initial delay according to a given time and the current
	 * date Called when dates are regular and it must also not be the first time
	 * that the alerts (of a specific frequency) are runned.
	 * 
	 * @param frequency
	 * @param Hour
	 *            the parameter typed in the UI
	 * @return the initial delay
	 */
	private long calculateDelays(String frequency, DateTime hour) {
		try {
			try {
				long diff = 0;
				String now = df.format(new Date());
				DateTime dt2 = new DateTime(df.parse(now)); // Gets the current
															// date
				Interval interval = new Interval(dt2, hour); // Gets the
																// interval
				Duration duration = interval.toDuration();
				diff = duration.getStandardMinutes();
				return diff;
			} catch (ParseException e) {
				LOGGER.error(
						"Error while parsing the dates to schedule the "
								+ frequency
								+ " alerts in calculateDelays(), AlertsManager. Error 69040",
						e);
				throw new RuntimeException();
			}
		} catch (Exception e) {
			LOGGER.error(
					"Error while calculating the delay to launch the "
							+ frequency
							+ " alerts in the AlertsManager calculateDelays(). Error 69519",
					e);
			return 0;
		}
	}

	/**
	 * Updates the datafari.properties file Run the alerts with the correct
	 * frequency
	 * 
	 * @param frequency
	 *            : Hourly/Daily/Weekly
	 */
	private void alerts(String frequency) {
		try {
			for (AlertFrequencyFirstExecution c : HDW) { // Checks if alerts with the correct
										// frequency have already run at least
										// once
				if (c.getFrequency().equals(frequency) && c.hasBeenExecuted()) {
					ScriptConfiguration.setProperty(frequency, df.format(new Date()).toString());
				}
			}
			List<Properties> alertList = AlertDataService.getInstance().getAlerts(); // Get all the
															// elements in the
															// collection
			Core[] core = Core.values();
			for (Properties alertProp : alertList) { // Get the next Object in the collection
				if (frequency.equals(alertProp.get("frequency").toString())) {
					SolrClient solr = null;
					for (int i = 0; i < core.length; i++) { // Get the right
															// core by comparing
															// all the return of
															// the Enum Type
															// Core to the one
															// in the database
						if (alertProp.get("core").toString().toUpperCase()
								.equals("" + core[i].toString().toUpperCase())) {
							try {
								solr = SolrServers.getSolrServer(core[i]);
							} catch (IOException e) {
								LOGGER.error(
										"Error while getting the Solr core in alerts(), AlertsManager. Error 69042 ",
										e);
								return;
							}
						}
					}// Creates an alert with the attributes of the element
						// found in the database.
					alert = new Alert(alertProp.get("subject").toString(), alertProp
							.get("mail").toString(), solr, alertProp.get("keyword")
							.toString(), alertProp.get("frequency").toString(), mail, alertProp
							.get("user").toString());
					alert.run(); // Makes the request and send the mail if they
									// are some results
				}
			}
		} catch (Exception e) {
			LOGGER.error("Unindentified error while running  the " + frequency
					+ " alerts in the AlertsManager alerts(). Error 69520", e);
			return;
		}
	}

	/**
	 * Gets the parameters Establishes the connection to the database Starts the
	 * alerts
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public void turnOn() throws IOException {
		try {
			if (this.getParameter()) {
				this.startScheduled(); // Starts the scheduled task
				LOGGER.info("Alert scheduler started");

			}
		} catch (IOException e) {
			LOGGER.error(
					"Error while turning on the alerts during instantiation, AlertsManager turnOn(). Error 69043",
					e);
			throw e;
		} catch (Exception e) {
			LOGGER.error(
					"Error while turning on the alerts during instantiation, AlertsManager turnOn(). Error 69044",
					e);
		}
	}

	/**
	 * Closes the database connection Cancels the scheduled runnables Resets
	 * various variables
	 */
	public void turnOff() {
		if (alertHandleD != null) {
			alertHandleD.cancel(true);
			alertHandleH.cancel(true);
			alertHandleW.cancel(true);
			scheduler.shutdownNow();
			Hourly.setHasBeenExecuted(false);
			Daily.setHasBeenExecuted(false);
			Weekly.setHasBeenExecuted(false);
		}
		filePath = null;
		mail = null;
		LOGGER.info("Alert scheduler shutdown");
	}

	static String readFile(String path, Charset encoding) // Read the file
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}