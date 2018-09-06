package com.francelabs.datafari.service.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.utils.UUIDs;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class AlertDataService {

  public static final String ALERTCOLLECTION = "alerts";

  private final Session session;

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

  public static synchronized AlertDataService getInstance() throws IOException {
    if (instance == null) {
      instance = new AlertDataService();
    }
    return instance;
  }

  public AlertDataService() throws IOException {

    // Gets the name of the collection
    session = CassandraManager.getInstance().getSession();
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

  public String addAlert(final Properties alertProp) throws DatafariServerException {
    try {

      final UUID uuid = UUIDs.random();
      final String query = "insert into " + ALERTCOLLECTION + " (" + ID_COLUMN + ", " + KEYWORD_COLUMN + ", " + FILTERS_COLUMN + ", " + CORE_COLUMN + ", " + FREQUENCY_COLUMN + ", " + MAIL_COLUMN + ", " + SUBJECT_COLUMN + ", " + USER_COLUMN
          + ") values (" + "uuid()," + "$$" + alertProp.getProperty(KEYWORD_COLUMN) + "$$," + "$$" + alertProp.getProperty(FILTERS_COLUMN) + "$$," + "'" + alertProp.getProperty(CORE_COLUMN) + "'," + "'" + alertProp.getProperty(FREQUENCY_COLUMN)
          + "'," + "'" + alertProp.getProperty(MAIL_COLUMN) + "'," + "'" + alertProp.getProperty(SUBJECT_COLUMN) + "'," + "'" + alertProp.getProperty(USER_COLUMN) + "');";
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
        final UUID id = row.getUUID("id");
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

}
