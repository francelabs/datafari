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
import com.francelabs.datafari.utils.ScriptConfiguration;

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
			json.put("on", ScriptConfiguration.getProperty("ALERTS"));
			json.put("hourlyDate", ScriptConfiguration.getProperty("HOURLYDELAY"));
			json.put("dailyDate", ScriptConfiguration.getProperty("DAILYDELAY"));
			json.put("weeklyDate", ScriptConfiguration.getProperty("WEEKLYDELAY"));
			json.put("host", ScriptConfiguration.getProperty("HOST"));
			json.put("port", ScriptConfiguration.getProperty("PORT"));
			json.put("database", ScriptConfiguration.getProperty("DATABASE"));
			json.put("collection", ScriptConfiguration.getProperty("COLLECTION"));

			json.put("nextHourly",
					new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Hourly"))).plusHours(1).toString(formatterbis));
			json.put("hourly", new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Hourly"))).toString(formatterbis));

			json.put("nextDaily", new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Daily"))).plusDays(1).toString(formatterbis));
			json.put("daily", new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Daily"))).toString(formatterbis));

			json.put("nextWeekly",
					new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Weekly"))).plusWeeks(1).toString(formatterbis));
			json.put("weekly", new DateTime(formatter.parseDateTime(ScriptConfiguration.getProperty("Weekly"))).toString(formatterbis));

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
				ScriptConfiguration.setProperty("ALERTS", request.getParameter("activated"));
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

				ScriptConfiguration.setProperty("HOURLYDELAY", new DateTime(df.parse(request.getParameter("hourlyDelay"))).toString(formatter));
				ScriptConfiguration.setProperty("DAILYDELAY", new DateTime(df.parse(request.getParameter("dailyDelay"))).toString(formatter));
				ScriptConfiguration.setProperty("WEEKLYDELAY", new DateTime(df.parse(request.getParameter("weeklyDelay"))).toString(formatter));
				ScriptConfiguration.setProperty("HOST", request.getParameter("host"));
				ScriptConfiguration.setProperty("PORT", request.getParameter("port"));
				ScriptConfiguration.setProperty("DATABASE", request.getParameter("database"));
				ScriptConfiguration.setProperty("COLLECTION", request.getParameter("collection"));

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

}
