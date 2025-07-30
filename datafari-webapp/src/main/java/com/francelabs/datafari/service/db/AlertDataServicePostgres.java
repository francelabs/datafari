package com.francelabs.datafari.service.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class AlertDataServicePostgres {

  public static final String ALERTCOLLECTION = "alerts";

  private static AlertDataServicePostgres instance;

  final static Logger logger = LogManager.getLogger(AlertDataServicePostgres.class);
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
  private final PostgresService pgService = new PostgresService();

  public static synchronized AlertDataServicePostgres getInstance() {
    if (instance == null) {
      instance = new AlertDataServicePostgres();
    }
    return instance;
  }

  private AlertDataServicePostgres() {
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  public String addAlert(final Properties alertProp) throws DatafariServerException {
    try {
      final UUID uuid = UUID.randomUUID();
      String ttlToUse = alertProp.getProperty(USER_COLUMN).contentEquals("admin") ? "0" : userDataTTL;

      String sql = "INSERT INTO " + ALERTCOLLECTION + " (" +
          ID_COLUMN + ", " + KEYWORD_COLUMN + ", " + FILTERS_COLUMN + ", " + CORE_COLUMN + ", " +
          FREQUENCY_COLUMN + ", " + MAIL_COLUMN + ", " + SUBJECT_COLUMN + ", " +
          USER_COLUMN + ", " + LASTREFRESHCOLUMN + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

      pgService.executeUpdate(sql, uuid, alertProp.getProperty(KEYWORD_COLUMN), alertProp.getProperty(FILTERS_COLUMN),
          alertProp.getProperty(CORE_COLUMN), alertProp.getProperty(FREQUENCY_COLUMN),
          alertProp.getProperty(MAIL_COLUMN), alertProp.getProperty(SUBJECT_COLUMN),
          alertProp.getProperty(USER_COLUMN), Timestamp.from(java.time.Instant.now()));

      return uuid.toString();
    } catch (SQLException e) {
      logger.error("Unable to add alert", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getAlerts() throws DatafariServerException {
    final List<Properties> alerts = new ArrayList<>();
    try {
      final String sql = "SELECT * FROM " + ALERTCOLLECTION;
      try (ResultSet rs = pgService.executeSelect(sql)) {
        while (rs.next()) {
          final Properties alertProp = new Properties();
          alertProp.put("_id", rs.getObject(ID_COLUMN).toString());
          alertProp.put(KEYWORD_COLUMN, rs.getString(KEYWORD_COLUMN));
          alertProp.put(FILTERS_COLUMN, rs.getString(FILTERS_COLUMN));
          alertProp.put(CORE_COLUMN, rs.getString(CORE_COLUMN));
          alertProp.put(FREQUENCY_COLUMN, rs.getString(FREQUENCY_COLUMN));
          alertProp.put(MAIL_COLUMN, rs.getString(MAIL_COLUMN));
          alertProp.put(SUBJECT_COLUMN, rs.getString(SUBJECT_COLUMN));
          alertProp.put(USER_COLUMN, rs.getString(USER_COLUMN));
          alerts.add(alertProp);
        }
      }
    } catch (SQLException e) {
      logger.error("Unable to get alerts", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return alerts;
  }

  public List<Properties> getUserAlerts(final String username) throws DatafariServerException {
    final List<Properties> alerts = new ArrayList<>();
    try {
      final String sql = "SELECT * FROM " + ALERTCOLLECTION + " WHERE " + USER_COLUMN + " = ?";
      try (ResultSet rs = pgService.executeSelect(sql, username)) {
        while (rs.next()) {
          final Properties alertProp = new Properties();
          alertProp.put("_id", rs.getObject(ID_COLUMN).toString());
          alertProp.put(KEYWORD_COLUMN, rs.getString(KEYWORD_COLUMN));
          alertProp.put(FILTERS_COLUMN, rs.getString(FILTERS_COLUMN));
          alertProp.put(CORE_COLUMN, rs.getString(CORE_COLUMN));
          alertProp.put(FREQUENCY_COLUMN, rs.getString(FREQUENCY_COLUMN));
          alertProp.put(MAIL_COLUMN, rs.getString(MAIL_COLUMN));
          alertProp.put(SUBJECT_COLUMN, rs.getString(SUBJECT_COLUMN));
          alertProp.put(USER_COLUMN, rs.getString(USER_COLUMN));
          alerts.add(alertProp);
        }
      }
    } catch (SQLException e) {
      logger.error("Unable to get user alerts", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return alerts;
  }

  public void deleteAlert(final String id) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + ALERTCOLLECTION + " WHERE " + ID_COLUMN + " = ?";
      pgService.executeUpdate(sql, UUID.fromString(id));
    } catch (SQLException e) {
      logger.error("Unable to delete alert", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void deleteUserAlerts(final String username) throws DatafariServerException {
    final List<Properties> alerts = getUserAlerts(username);
    for (final Properties alert : alerts) {
      final String alertID = alert.getProperty("_id");
      deleteAlert(alertID);
    }
  }

  public void updateAlert(final Properties alertProp) throws DatafariServerException {
    try {
      String ttlToUse = alertProp.getProperty(USER_COLUMN).contentEquals("admin") ? "0" : userDataTTL;

      String sql = "UPDATE " + ALERTCOLLECTION + " SET " +
          KEYWORD_COLUMN + " = ?, " +
          FILTERS_COLUMN + " = ?, " +
          CORE_COLUMN + " = ?, " +
          FREQUENCY_COLUMN + " = ?, " +
          MAIL_COLUMN + " = ?, " +
          SUBJECT_COLUMN + " = ?, " +
          USER_COLUMN + " = ?, " +
          LASTREFRESHCOLUMN + " = ? WHERE " + ID_COLUMN + " = ?";

      pgService.executeUpdate(sql,
          alertProp.getProperty(KEYWORD_COLUMN),
          alertProp.getProperty(FILTERS_COLUMN),
          alertProp.getProperty(CORE_COLUMN),
          alertProp.getProperty(FREQUENCY_COLUMN),
          alertProp.getProperty(MAIL_COLUMN),
          alertProp.getProperty(SUBJECT_COLUMN),
          alertProp.getProperty(USER_COLUMN),
          Timestamp.from(java.time.Instant.now()),
          UUID.fromString(alertProp.getProperty("_id")));
    } catch (SQLException e) {
      logger.error("Unable to update alert", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
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