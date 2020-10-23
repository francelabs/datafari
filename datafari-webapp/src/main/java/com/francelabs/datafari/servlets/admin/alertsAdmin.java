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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;

import com.francelabs.datafari.alerts.AlertsManager;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.AlertsConfiguration;
import com.francelabs.datafari.utils.PasswordMapper;

/**
 *
 * This servlet is used to configure Alerts It is only called by the alertsAdmin.html doGet is called at the loading of the AlertsAdmin, to get the parameters from datafari.properties. doPost is
 * called when clicking on the on/off button, turns off and on the alerts Or when the save parameters button is clicked, saves the parameters in datafari.properties If you are in development
 * environment, the path towards the datafari.properties is hardcoded
 *
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/alertsAdmin")
public class alertsAdmin extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = LogManager.getLogger(alertsAdmin.class.getName());

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
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

    response.setContentType("application/json");
    final JSONObject json = new JSONObject();

    final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy/HH:mm");
    final DateTimeFormatter formatterbis = DateTimeFormat.forPattern("dd/MM/yyyy/ HH:mm");

    final AlertsConfiguration alertsConf = AlertsConfiguration.getInstance();
    json.put("on", alertsConf.getProperty(AlertsConfiguration.ALERTS_ON_OFF));
    json.put("hourlyDate", alertsConf.getProperty(AlertsConfiguration.HOURLY_DELAY));
    json.put("dailyDate", alertsConf.getProperty(AlertsConfiguration.DAILY_DELAY));
    json.put("weeklyDate", alertsConf.getProperty(AlertsConfiguration.WEEKLY_DELAY));
    json.put("host", alertsConf.getProperty(AlertsConfiguration.DATABASE_HOST));
    json.put("port", alertsConf.getProperty(AlertsConfiguration.DATABASE_PORT));
    json.put("database", alertsConf.getProperty(AlertsConfiguration.DATABASE_NAME));
    json.put("collection", alertsConf.getProperty(AlertsConfiguration.DATABASE_COLLECTION));

    json.put("nextHourly", getNextEvent("hourly", alertsConf.getProperty(AlertsConfiguration.HOURLY_DELAY)));
    json.put("hourly", new DateTime(formatter.parseDateTime(alertsConf.getProperty(AlertsConfiguration.LAST_HOURLY_EXEC))).toString(formatterbis));

    json.put("nextDaily", getNextEvent("daily", alertsConf.getProperty(AlertsConfiguration.DAILY_DELAY)));
    json.put("daily", new DateTime(formatter.parseDateTime(alertsConf.getProperty(AlertsConfiguration.LAST_DAILY_EXEC))).toString(formatterbis));

    json.put("nextWeekly", getNextEvent("weekly", alertsConf.getProperty(AlertsConfiguration.WEEKLY_DELAY)));
    json.put("weekly", new DateTime(formatter.parseDateTime(alertsConf.getProperty(AlertsConfiguration.LAST_WEEKLY_EXEC))).toString(formatterbis));

    json.put("smtp", alertsConf.getProperty(AlertsConfiguration.SMTP_ADDRESS));
    json.put("from", alertsConf.getProperty(AlertsConfiguration.SMTP_FROM));
    json.put("user", alertsConf.getProperty(AlertsConfiguration.SMTP_USER));
    json.put("pass", PasswordMapper.getInstance().mapPasswordToKey(alertsConf.getProperty(AlertsConfiguration.SMTP_PASSWORD)));

    json.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

    final PrintWriter out = response.getWriter();
    out.print(json);

  }

  /**
   * Two uses : When user clicks on turn on/off button, starts or stops the alerts When user clicks on the parameter saving button, saves all the parameters
   *
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

    response.setContentType("application/json");
    final JSONObject json = new JSONObject();
    final AlertsConfiguration alertsConf = AlertsConfiguration.getInstance();

    try {
      if (request.getParameter("activated") != null) {
        alertsConf.setProperty(AlertsConfiguration.ALERTS_ON_OFF, request.getParameter("activated"));
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

        alertsConf.setProperty(AlertsConfiguration.HOURLY_DELAY, new DateTime(df.parse(request.getParameter(AlertsConfiguration.HOURLY_DELAY))).toString(formatter));
        alertsConf.setProperty(AlertsConfiguration.DAILY_DELAY, new DateTime(df.parse(request.getParameter(AlertsConfiguration.DAILY_DELAY))).toString(formatter));
        alertsConf.setProperty(AlertsConfiguration.WEEKLY_DELAY, new DateTime(df.parse(request.getParameter(AlertsConfiguration.WEEKLY_DELAY))).toString(formatter));
        alertsConf.setProperty(AlertsConfiguration.DATABASE_HOST, request.getParameter(AlertsConfiguration.DATABASE_HOST));
        alertsConf.setProperty(AlertsConfiguration.DATABASE_PORT, request.getParameter(AlertsConfiguration.DATABASE_PORT));
        alertsConf.setProperty(AlertsConfiguration.DATABASE_NAME, request.getParameter(AlertsConfiguration.DATABASE_NAME));
        alertsConf.setProperty(AlertsConfiguration.DATABASE_COLLECTION, request.getParameter(AlertsConfiguration.DATABASE_COLLECTION));
        alertsConf.setProperty(AlertsConfiguration.SMTP_ADDRESS, request.getParameter(AlertsConfiguration.SMTP_ADDRESS));
        alertsConf.setProperty(AlertsConfiguration.SMTP_FROM, request.getParameter(AlertsConfiguration.SMTP_FROM));
        alertsConf.setProperty(AlertsConfiguration.SMTP_USER, request.getParameter(AlertsConfiguration.SMTP_USER));
        alertsConf.setProperty(AlertsConfiguration.SMTP_PASSWORD, PasswordMapper.getInstance().mapKeyToPassword(request.getParameter(AlertsConfiguration.SMTP_PASSWORD)));

        if (request.getParameter("restart") == null || request.getParameter("restart").equals("") || request.getParameter("restart").equals("true")) { // restart
          // param
          // used
          // for
          // tests,
          // DO
          // NOT
          // REMOVE
          // Restart scheduler
          AlertsManager.getInstance().turnOff();
          AlertsManager.getInstance().turnOn();
        }

        json.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      }
      // Save the properties
      alertsConf.saveProperties();
    } catch (final Exception e) {
      LOGGER.error("Error while accessing the alerts.properties file in the doPost of the alerts administration Servlet . Error 69020 ", e);
      json.put("message", "Error while accessing the alerts.properties file, please make sure the file exists and retry, if the problem persists contact your system administrator. Error code : 69020");
      json.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
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
