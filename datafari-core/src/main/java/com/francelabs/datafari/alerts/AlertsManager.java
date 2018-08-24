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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.AlertsConfiguration;

public class AlertsManager {
  private static AlertsManager INSTANCE = new AlertsManager();

  private boolean onOff = false;
  private DateTime delayH, delayD, delayW;
  private final DateFormat df = new SimpleDateFormat("dd/MM/yyyy/HH:mm");
  private final AlertFrequencyFirstExecution Hourly = new AlertFrequencyFirstExecution(AlertFrequencyFirstExecution.AlertFrequency.HOURLY, false);
  private final AlertFrequencyFirstExecution Daily = new AlertFrequencyFirstExecution(AlertFrequencyFirstExecution.AlertFrequency.DAILY, false);
  private final AlertFrequencyFirstExecution Weekly = new AlertFrequencyFirstExecution(AlertFrequencyFirstExecution.AlertFrequency.WEEKLY, false);

  private String HourlyHour = "";
  private String DailyHour = "";
  private String WeeklyHour = "";
  private final List<AlertFrequencyFirstExecution> HDW = new ArrayList<>();
  private ScheduledFuture<?> alertHandleH, alertHandleD, alertHandleW;
  private ScheduledExecutorService scheduler;
  private Mail mail;
  private Alert alert;
  private final static Logger LOGGER = LogManager.getLogger(AlertsManager.class.getName());

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
   * connection with the database
   *
   * @throws ParseException
   */
  public boolean getParameter() throws IOException, ParseException {
    try {
      if (AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.ALERTS_ON_OFF).equals("on")) {
        onOff = true;
      }

      // Gets the delay for the hourly alerts
      try {
        delayH = new DateTime(df.parse(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.HOURLY_DELAY).replace("\\", "")));
      } catch (final ParseException e) {
        LOGGER.warn("Error parsing the Hourly Date, default value will be used, AlertsManager getParameter()", e);
        delayH = new DateTime(df.parse("01/01/0001/00:00"));
      }

      // Gets the delay for the daily alerts
      try {
        delayD = new DateTime(df.parse(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.DAILY_DELAY).replace("\\", "")));
      } catch (final ParseException e) {
        LOGGER.warn("Error parsing the Daily Date, default value will be used, AlertsManager getParameter()", e);
        delayD = new DateTime(df.parse("01/01/0001/00:00"));
      }

      // Gets the delay for the weekly alerts
      try {
        delayW = new DateTime(df.parse(AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.WEEKLY_DELAY).replace("\\", "")));
      } catch (final ParseException e) {
        LOGGER.warn("Error parsing the Weekly Date, default value will be used, AlertsManager getParameter()", e);
        delayW = new DateTime(df.parse("01/01/0001/00:00"));
      }

      // Checks if there has been a previous execution for alerts
      final String hourlyStr = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.LAST_HOURLY_EXEC).replace("\\", "");
      if (hourlyStr != null) {
        Hourly.setHasBeenExecuted(true);
        HourlyHour = hourlyStr;
      }
      final String dailyStr = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.LAST_DAILY_EXEC).replace("\\", "");
      if (dailyStr != null) {
        Daily.setHasBeenExecuted(true);
        DailyHour = dailyStr;
      }
      final String weeklyStr = AlertsConfiguration.getInstance().getProperty(AlertsConfiguration.LAST_WEEKLY_EXEC).replace("\\", "");
      if (weeklyStr != null) {
        Weekly.setHasBeenExecuted(true);
        WeeklyHour = weeklyStr;
      }
      LOGGER.info("Alert config file successfully read");
      return onOff;
    } catch (final Exception e) {
      LOGGER.error("Unindentified error in the AlertsManager getParameter(). Error 69516 ", e);
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
        @Override
        public void run() {
          try {
            alerts("Hourly");
          } catch (final Exception e) {
            LOGGER.error("Unindentified error while running the hourly alerts in startScheduled(), AlertsManager. Error 69518", e);
          }
        }
      };
      final Runnable alertDaily = new Runnable() { // Runnable that runs
        // every Day
        @Override
        public void run() {
          try {
            alerts("Daily");
          } catch (final Exception e) {
            LOGGER.error("Unindentified error while running the daily alerts in startScheduled(), AlertsManager. Error 69519", e);
          }
        }
      };
      final Runnable alertWeekly = new Runnable() { // Runnable that runs
        // every Week
        @Override
        public void run() {
          try {
            alerts("Weekly");
          } catch (final Exception e) {
            LOGGER.error("Unindentified error while running the weekly alerts in startScheduled(), AlertsManager. Error 69520", e);
          }
        }
      };
      mail = new Mail();

      final DateTime currentDate = new DateTime();
      // Launches the alerts according to their previous execution and the
      // date typed by the user
      alertHandleH = launch(Hourly, delayH, alertHandleH, currentDate, "Hourly", HourlyHour, alertHourly, 60);
      alertHandleD = launch(Daily, delayD, alertHandleD, currentDate, "Daily", DailyHour, alertDaily, 1440);
      alertHandleW = launch(Weekly, delayW, alertHandleW, currentDate, "Weekly", WeeklyHour, alertWeekly, 10080);
    } catch (final Exception e) {
      LOGGER.error("Unindentified error in the AlertsManager startScheduled(). Error 69517", e);
      return;
    }
  }

  /**
   * Launches the alerts
   *
   * @param custom
   *          the customBool to know if alerts of this frequency have been
   *          launched
   * @param delay
   *          The Date typed by the user
   * @param Handle
   *          the Scheduler corresponding to the frequency
   * @param current
   *          The current time
   * @param frequency
   *          The frequency of the alerts you launch
   * @param Hour
   *          The time of the previous execution
   * @param run
   *          The runnable that run the alerts
   * @param loop
   *          The number of minutes between each execution
   */
  public ScheduledFuture<?> launch(final AlertFrequencyFirstExecution custom, final DateTime delay, ScheduledFuture<?> Handle, final DateTime current, final String frequency, final String Hour, final Runnable run, final long loop) {
    try {

      return Handle = scheduler.scheduleAtFixedRate(run, calculateDelays(frequency, delay), loop, TimeUnit.MINUTES);

    } catch (final Exception e) {
      LOGGER.error("Unindentified error while calculating the delay to launch the " + frequency + " alerts in the AlertsManager launch(). Error 69038", e);
      return null;
    }
  }

  /**
   * Updates the datafari.properties file Run the alerts with the correct
   * frequency
   *
   * @param frequency
   *          : Hourly/Daily/Weekly
   */
  private void alerts(final String frequency) {
    try {
      for (final AlertFrequencyFirstExecution c : HDW) { // Checks if
        // alerts with
        // the correct
        // frequency have already run at least
        // once
        if (c.getFrequency().toString().toLowerCase().equals(frequency.toLowerCase()) && c.hasBeenExecuted()) {
          AlertsConfiguration.getInstance().setProperty(frequency, df.format(new Date()).toString());
        }
      }
      final List<Properties> alertList = com.francelabs.datafari.user.Alert.getAlerts(); // Get
      // all
      // the
      // elements in the
      // collection
      final Core[] core = Core.values();
      for (final Properties alertProp : alertList) { // Get the next
        // Object in the
        // collection
        if (frequency.toLowerCase().equals(alertProp.get("frequency").toString().toLowerCase())) {
          IndexerServer server = null;
          for (int i = 0; i < core.length; i++) { // Get the right
            // core by comparing
            // all the return of
            // the Enum Type
            // Core to the one
            // in the database
            if (alertProp.get("core").toString().toUpperCase().equals("" + core[i].toString().toUpperCase())) {
              try {
                server = IndexerServerManager.getIndexerServer(core[i]);
              } catch (final IOException e) {
                LOGGER.error("Error while getting the Solr core in alerts(), AlertsManager. Error 69042 ", e);
                return;
              }
            }
          } // Creates an alert with the attributes of the element
            // found in the database.
          alert = new Alert(alertProp.get("subject").toString(), alertProp.get("mail").toString(), server, alertProp.get("keyword").toString(), alertProp.get("frequency").toString(), mail, alertProp.get("user").toString());
          alert.run(); // Makes the request and send the mail if they
          // are some results
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Unindentified error while running  the " + frequency + " alerts in the AlertsManager alerts(). Error 69520", e);
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
    } catch (final IOException e) {
      LOGGER.error("Error while turning on the alerts during instantiation, AlertsManager turnOn(). Error 69043", e);
      throw e;
    } catch (final Exception e) {
      LOGGER.error("Error while turning on the alerts during instantiation, AlertsManager turnOn(). Error 69044", e);
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
    mail = null;
    LOGGER.info("Alert scheduler shutdown");
  }

  static String readFile(final String path, final Charset encoding) // Read
      // the
      // file
      throws IOException {
    final byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  /**
   * Calculate the difference in minutes between the current date time and the
   * provided scheduled one, according to the frequency
   *
   * @param frequency
   *          the frequency of the scheduled date time
   * @param scheduledDate
   *          the initial scheduled date time that the user has typed in the
   *          admin UI
   * @return the difference in minutes between the current date time and the
   *         next scheduled one
   */
  private long calculateDelays(final String frequency, final DateTime scheduledDate) {
    long diff = 0L;
    final Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    final DateTime currentDateTime = new DateTime(cal.getTime());
    DateTime scheduledDateTimeUpdate;

    switch (frequency.toLowerCase()) {
    case "hourly":
      // Create what would be the current scheduled date
      cal.setTime(new Date());
      cal.set(Calendar.MINUTE, scheduledDate.getMinuteOfHour());
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      scheduledDateTimeUpdate = new DateTime(cal.getTime());

      // Compare the current date with the current scheduled one, if the
      // current date is earlier than the scheduled one, simply calculate
      // the difference in minutes, otherwise create the next scheduled
      // date and calculate the difference
      if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
        cal.add(Calendar.HOUR_OF_DAY, 1);
        scheduledDateTimeUpdate = new DateTime(cal.getTime());
      }
      diff = Minutes.minutesBetween(currentDateTime, scheduledDateTimeUpdate).getMinutes();
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
      // current date is earlier than the scheduled one, simply calculate
      // the difference in minutes, otherwise create the next scheduled
      // date and calculate the difference
      if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
        cal.add(Calendar.DAY_OF_YEAR, 1);
        scheduledDateTimeUpdate = new DateTime(cal.getTime());
      }
      diff = Minutes.minutesBetween(currentDateTime, scheduledDateTimeUpdate).getMinutes();
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
      // current date is earlier than the scheduled one, simply calculate
      // the difference in minutes, otherwise create the next scheduled
      // date and calculate the difference
      if (!currentDateTime.isBefore(scheduledDateTimeUpdate)) {
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        scheduledDateTimeUpdate = new DateTime(cal.getTime());
      }
      diff = Minutes.minutesBetween(currentDateTime, scheduledDateTimeUpdate).getMinutes();
      break;

    default:
      break;
    }

    return diff;
  }
}