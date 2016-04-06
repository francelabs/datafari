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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
import com.francelabs.datafari.utils.AlertsConfiguration;

/**
 *
 * This servlet is used to configure Alerts It is only called by the
 * alertsAdmin.html doGet is called at the loading of the AlertsAdmin, to get
 * the parameters from datafari.properties. doPost is called when clicking on
 * the on/off button, turns off and on the alerts Or when the save parameters
 * button is clicked, saves the parameters in datafari.properties If you are in
 * development environment, the path towards the datafari.properties is
 * hardcoded
 *
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/alertsAdmin")
public class alertsAdmin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(alertsAdmin.class.getName());

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public alertsAdmin() {
		super();
	}

	/**
	 * Gets the required parameters parameters
	 *
	 * @throws IOException
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		response.setContentType("application/json");
		final JSONObject json = new JSONObject();

		final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");
		final DateTimeFormatter formatterbis = DateTimeFormat.forPattern("dd/MM/yyyy/ HH:mm");

		try {
			json.put("on", AlertsConfiguration.getProperty("ALERTS"));
			json.put("hourlyDate", AlertsConfiguration.getProperty("HOURLYDELAY"));
			json.put("dailyDate", AlertsConfiguration.getProperty("DAILYDELAY"));
			json.put("weeklyDate", AlertsConfiguration.getProperty("WEEKLYDELAY"));
			json.put("host", AlertsConfiguration.getProperty("HOST"));
			json.put("port", AlertsConfiguration.getProperty("PORT"));
			json.put("database", AlertsConfiguration.getProperty("DATABASE"));
			json.put("collection", AlertsConfiguration.getProperty("COLLECTION"));

			// json.put("nextHourly",
			// new
			// DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Hourly"))).plusHours(1).toString(formatterbis));
			json.put("nextHourly", getNextEvent("hourly", AlertsConfiguration.getProperty("HOURLYDELAY")));
			json.put("hourly", new DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Hourly"))).toString(formatterbis));

			// json.put("nextDaily", new
			// DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Daily"))).plusDays(1).toString(formatterbis));
			json.put("nextDaily", getNextEvent("daily", AlertsConfiguration.getProperty("DAILYDELAY")));
			json.put("daily", new DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Daily"))).toString(formatterbis));

			// json.put("nextWeekly",
			// new
			// DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Weekly"))).plusWeeks(1).toString(formatterbis));
			json.put("nextWeekly", getNextEvent("weekly", AlertsConfiguration.getProperty("WEEKLYDELAY")));
			json.put("weekly", new DateTime(formatter.parseDateTime(AlertsConfiguration.getProperty("Weekly"))).toString(formatterbis));

			json.put("smtp", AlertsConfiguration.getProperty("smtp"));
			json.put("from", AlertsConfiguration.getProperty("from"));
			json.put("user", AlertsConfiguration.getProperty("user"));
			json.put("pass", AlertsConfiguration.getProperty("pass"));

			json.put("code", 0);
		} catch (final JSONException e) {
			LOGGER.error(
					"Error while building the JSON answer in the doGet of the alerts administration servlets, make sure the fields are filled correctly and that datafari.properties have the correct encoding charset(UTF_8). Error 69021",
					e);
			json.put("message",
					"Error while getting the parameters, please retry, if the problem persists contact your system administrator. Error code : 69021");
			json.put("code", "69021");
		} catch (final IOException e) {
			LOGGER.error("Error while reading the datafari.properties file in the doGet of the alerts administration Servlet . Error 69020 ", e);
			json.put("message",
					"Error while reading the datafari.properties file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69020");
			json.put("code", "69020");
		}

		final PrintWriter out = response.getWriter();
		out.print(json);

	}

	/**
	 * Two uses : When user clicks on turn on/off button, starts or stops the
	 * alerts When user clicks on the parameter saving button, saves all the
	 * parameters
	 *
	 * @throws IOException
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		response.setContentType("application/json");
		final JSONObject json = new JSONObject();

		try {
			if (request.getParameter("activated") != null) {
				AlertsConfiguration.setProperty("ALERTS", request.getParameter("activated"));
				if (request.getParameter("activated").equals("on")) {
					AlertsManager.getInstance().turnOn();
				} else {
					AlertsManager.getInstance().turnOff();
				}
			} else {

				final DateFormat df = new SimpleDateFormat("dd/MM/yyyy/ HH:mm"); // Create
				// a
				// date
				// format
				// and
				// get
				// current
				// time
				final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");

				AlertsConfiguration.setProperty("HOURLYDELAY", new DateTime(df.parse(request.getParameter("hourlyDelay"))).toString(formatter));
				AlertsConfiguration.setProperty("DAILYDELAY", new DateTime(df.parse(request.getParameter("dailyDelay"))).toString(formatter));
				AlertsConfiguration.setProperty("WEEKLYDELAY", new DateTime(df.parse(request.getParameter("weeklyDelay"))).toString(formatter));
				AlertsConfiguration.setProperty("HOST", request.getParameter("host"));
				AlertsConfiguration.setProperty("PORT", request.getParameter("port"));
				AlertsConfiguration.setProperty("DATABASE", request.getParameter("database"));
				AlertsConfiguration.setProperty("COLLECTION", request.getParameter("collection"));
				AlertsConfiguration.setProperty("smtp", request.getParameter("SMTP"));
				AlertsConfiguration.setProperty("from", request.getParameter("address"));
				AlertsConfiguration.setProperty("user", request.getParameter("user"));
				AlertsConfiguration.setProperty("pass", request.getParameter("pass"));

				// Restart scheduler
				AlertsManager.getInstance().turnOff();
				AlertsManager.getInstance().turnOn();

				json.put("code", 0);

			}
		} catch (final Exception e) {
			LOGGER.error("Error while accessing the datafari.properties file in the doPost of the alerts administration Servlet . Error 69020 ", e);
			json.put("message",
					"Error while accessing the datafari.properties file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69020");
			json.put("code", "69020");
		}

		final PrintWriter out = response.getWriter();
		out.print(json);
	}

	private String getNextEvent(final String frequency, final String initialDate) {
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");
		final DateTime scheduledDate = new DateTime(formatter.parseDateTime(initialDate));
		final Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		final DateTime currentDateTime = new DateTime(cal.getTime());
		DateTime scheduledDateTimeUpdate = new DateTime(cal.getTime());

		switch (frequency.toLowerCase()) {
		case "hourly":
			// Create what would be the current scheduled date
			cal.setTime(new Date());
			cal.set(Calendar.MINUTE, scheduledDate.getMinuteOfHour());
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			scheduledDateTimeUpdate = new DateTime(cal.getTime());

			// Compare the current date with the current scheduled one, if the
			// current date is later than the scheduled one then create the next
			// scheduled date
			if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
				cal.add(Calendar.HOUR_OF_DAY, 1);
				scheduledDateTimeUpdate = new DateTime(cal.getTime());
			}
			break;

		case "daily":
			// Create what would be the current scheduled date
			cal.setTime(new Date());
			cal.set(Calendar.HOUR_OF_DAY, scheduledDate.getHourOfDay());
			cal.set(Calendar.MINUTE, scheduledDate.getMinuteOfHour());
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			scheduledDateTimeUpdate = new DateTime(cal.getTime());

			// Compare the current date with the current scheduled one, if the
			// current date is later than the scheduled one then create the next
			// scheduled date
			if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
				cal.add(Calendar.DAY_OF_YEAR, 1);
				scheduledDateTimeUpdate = new DateTime(cal.getTime());
			}
			break;

		case "weekly":
			// Create what would be the current scheduled date
			cal.setTime(new Date());
			cal.set(Calendar.DAY_OF_WEEK, scheduledDate.getDayOfWeek() + 1); // +1
																				// =
																				// diff
																				// between
																				// Joda
																				// and
																				// Calendar
			cal.set(Calendar.HOUR_OF_DAY, scheduledDate.getHourOfDay());
			cal.set(Calendar.MINUTE, scheduledDate.getMinuteOfHour());
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			scheduledDateTimeUpdate = new DateTime(cal.getTime());

			// Compare the current date with the current scheduled one, if the
			// current date is later than the scheduled one then create the next
			// scheduled date
			if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
				cal.add(Calendar.WEEK_OF_YEAR, 1);
				scheduledDateTimeUpdate = new DateTime(cal.getTime());
			}
			break;

		default:
			break;
		}
		return scheduledDateTimeUpdate.toString(formatter);
	}

}
