package com.francelabs.datafari.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class AlertDataService extends CassandraService {

  public static final String ALERTCOLLECTION = "alerts";

  private static AlertDataService instance;

  final static Logger logger = LogManager.getLogger(AlertDataService.class.getName());
  public static final String ID_COLUMN = "id";
  public static final String KEYWORD_COLUMN = "keyword";
  public static final String CORE_COLUMN = "core";
  public static final String FILTERS_COLUMN = "filters";
  public static final String FREQUENCY_COLUMN = "frequency";
  public static final String MAIL_COLUMN = "mail";
  public static final String SUBJECT_COLUMN = "subject";
  public static final String USER_COLUMN = "user";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  public static synchronized AlertDataService getInstance() {
    if (instance == null) {
      instance = new AlertDataService();
    }
    instance.refreshSession();
    return instance;
  }

  private AlertDataService() {
    refreshSession();
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  public void deleteAlert(final String id) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + ALERTCOLLECTION + " WHERE id =" + id + ";";
      session.execute(query);
    } catch (final DriverException e) {
      logger.error("Unable to delete alert", e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }

  }

  /**
   * Delete all the alerts of the provided user
   *
   * @param username
   * @throws DatafariServerException
   */
  public void deleteUserAlerts(final String username) throws DatafariServerException {
    try {
      final List<Properties> alerts = getUserAlerts(username);
      for (final Properties alert : alerts) {
        final String alertID = alert.getProperty("_id");
        final String query = "DELETE FROM " + ALERTCOLLECTION 
            + " WHERE " + ID_COLUMN + "=" + alertID 
            + " IF EXISTS;";
        session.execute(query);
      }
    } catch (final DriverException e) {
      logger.error("Unable to delete alert", e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }

  }

  public String addAlert(final Properties alertProp) throws DatafariServerException {
    try {

      final UUID uuid = Uuids.random();
      String ttlToUse = userDataTTL;
      if (alertProp.getProperty(USER_COLUMN).contentEquals("admin")) {
        ttlToUse = "0";
      }
      final String query = "insert into " + ALERTCOLLECTION
          + " (" + ID_COLUMN + ", "
          + KEYWORD_COLUMN + ", "
          + FILTERS_COLUMN + ", "
          + CORE_COLUMN + ", "
          + FREQUENCY_COLUMN + ", "
          + MAIL_COLUMN + ", "
          + SUBJECT_COLUMN + ", "
          + USER_COLUMN + ","
          + LASTREFRESHCOLUMN + ") "
          + "values (" + "uuid(),"
          + "$$" + alertProp.getProperty(KEYWORD_COLUMN) + "$$,"
          + "$$" + alertProp.getProperty(FILTERS_COLUMN) + "$$,"
          + "'" + alertProp.getProperty(CORE_COLUMN) + "',"
          + "'" + alertProp.getProperty(FREQUENCY_COLUMN) + "',"
          + "'" + alertProp.getProperty(MAIL_COLUMN) + "',"
          + "'" + alertProp.getProperty(SUBJECT_COLUMN) + "',"
          + "'" + alertProp.getProperty(USER_COLUMN) + "',"
          + "toTimeStamp(NOW())) "
          + "USING TTL " + ttlToUse;
      session.execute(query);
      return uuid.toString();
    } catch (final DriverException e) {
      logger.error("Unable to add alert", e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getAlerts() throws DatafariServerException {
    try {
      final List<Properties> alerts = new ArrayList<>();
      final ResultSet results = session.execute("SELECT * FROM " + ALERTCOLLECTION);
      for (final Row row : results) {
        final Properties alertProp = new Properties();
        final UUID id = row.getUuid("id");
        alertProp.put("_id", id.toString());
        alertProp.put("keyword", row.getString(KEYWORD_COLUMN));
        alertProp.put("filters", row.getString(FILTERS_COLUMN));
        alertProp.put("core", row.getString(CORE_COLUMN));
        alertProp.put("frequency", row.getString(FREQUENCY_COLUMN));
        alertProp.put("mail", row.getString(MAIL_COLUMN));
        alertProp.put("subject", row.getString(SUBJECT_COLUMN));
        alertProp.put("user", row.getString(USER_COLUMN));
        alerts.add(alertProp);
      }
      return alerts;
    } catch (final DriverException e) {
      logger.error("Unable to get Alerts", e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getUserAlerts(final String username) throws DatafariServerException {
    try {
      final List<Properties> alerts = new ArrayList<>();
      final ResultSet results = session.execute(
          "SELECT * FROM " + ALERTCOLLECTION + " WHERE " + USER_COLUMN + "='" + username + "' ALLOW FILTERING");
      for (final Row row : results) {
        final Properties alertProp = new Properties();
        final UUID id = row.getUuid("id");
        alertProp.put("_id", id.toString());
        alertProp.put("keyword", row.getString(KEYWORD_COLUMN));
        alertProp.put("filters", row.getString(FILTERS_COLUMN));
        alertProp.put("core", row.getString(CORE_COLUMN));
        alertProp.put("frequency", row.getString(FREQUENCY_COLUMN));
        alertProp.put("mail", row.getString(MAIL_COLUMN));
        alertProp.put("subject", row.getString(SUBJECT_COLUMN));
        alertProp.put("user", row.getString(USER_COLUMN));
        alerts.add(alertProp);
      }
      return alerts;
    } catch (final DriverException e) {
      logger.error("Unable to get Alerts", e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void updateAlert(final Properties alertProp) {
    String ttlToUse = userDataTTL;
    if (alertProp.getProperty(USER_COLUMN).contentEquals("admin")) {
      ttlToUse = "0";
    }
    final String query = "UPDATE " + ALERTCOLLECTION
        + " USING TTL " + ttlToUse
        + " SET " + KEYWORD_COLUMN + " = $$" + alertProp.getProperty(KEYWORD_COLUMN) + "$$, "
        + FILTERS_COLUMN + " = $$" + alertProp.getProperty(FILTERS_COLUMN) + "$$, "
        + CORE_COLUMN + " = $$" + alertProp.getProperty(CORE_COLUMN) + "$$, "
        + FREQUENCY_COLUMN + " = $$" + alertProp.getProperty(FREQUENCY_COLUMN) + "$$, "
        + MAIL_COLUMN + " = $$" + alertProp.getProperty(MAIL_COLUMN) + "$$, "
        + SUBJECT_COLUMN + " = $$" + alertProp.getProperty(SUBJECT_COLUMN) + "$$, "
        + USER_COLUMN + " = $$" + alertProp.getProperty(USER_COLUMN) + "$$, "
        + LASTREFRESHCOLUMN + " = toTimeStamp(NOW()) "
        + "WHERE " + ID_COLUMN + " = " + alertProp.getProperty("_id");
    session.execute(query);
  }

  public void refreshUserAlerts(final String username) throws DatafariServerException {
    final List<Properties> userAlerts = getUserAlerts(username);
    if (userAlerts != null && !userAlerts.isEmpty()) {
      for (final Properties userAlert : userAlerts) {
        updateAlert(userAlert);
      }
    }
  }

}
