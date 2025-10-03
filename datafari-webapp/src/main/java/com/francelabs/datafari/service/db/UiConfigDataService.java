package com.francelabs.datafari.service.db;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class UiConfigDataService {

  private static final Logger logger = LogManager.getLogger(UiConfigDataService.class);

  public static final String USERNAMECOLUMN      = "username";
  public static final String UICONFIGCOLLECTION  = "ui_config";
  public static final String UICONFIGCOLUMN      = "ui_config";
  public static final String LASTREFRESHCOLUMN   = "last_refresh";

  private static UiConfigDataService instance;

  // Helpers SQL via le pont statique
  private final JdbcTemplate jdbc;
  @SuppressWarnings("unused")
  private final NamedParameterJdbcTemplate named;

  // Conservé pour compat (pas de TTL natif en PG)
  @SuppressWarnings("unused")
  private final String userDataTTL;

  private UiConfigDataService() {
    this.jdbc  = SqlService.get().getJdbcTemplate();
    this.named = SqlService.get().getNamedJdbcTemplate();
    this.userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  public static synchronized UiConfigDataService getInstance() {
    if (instance == null) {
      instance = new UiConfigDataService();
    }
    return instance;
  }

  /**
   * Get user specific UI configuration.
   */
  public synchronized String getUiConfig(final String username) {
    try {
      final String sql = "SELECT " + UICONFIGCOLUMN +
                         "  FROM " + UICONFIGCOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = ?";
      return jdbc.query(sql, rs -> rs.next() ? rs.getString(UICONFIGCOLUMN) : null, username);
    } catch (Exception e) {
      logger.warn("Unable to get ui config for user {} : {}", username, e.getMessage());
      return null;
    }
  }

  /**
   * Set (upsert) user UI config.
   */
  public int setUiConfig(final String username, final String uiConfig) throws DatafariServerException {
    try {
      final String sql =
          "INSERT INTO " + UICONFIGCOLLECTION + " (" +
              USERNAMECOLUMN + ", " + UICONFIGCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
          "VALUES (?, ?, ?) " +
          "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " +
              UICONFIGCOLUMN + " = EXCLUDED." + UICONFIGCOLUMN + ", " +
              LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
      jdbc.update(sql, username, uiConfig, new Timestamp(System.currentTimeMillis()));
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to insert ui config for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Update user UI config.
   */
  public int updateUiConfig(final String username, final String uiConfig) throws DatafariServerException {
    try {
      final String sql =
          "UPDATE " + UICONFIGCOLLECTION + " SET " +
              UICONFIGCOLUMN + " = ?, " +
              LASTREFRESHCOLUMN + " = ? " +
          "WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, uiConfig, new Timestamp(System.currentTimeMillis()), username);
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to update ui config for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Refresh (touch) user UI config.
   */
  public void refreshUiConfig(final String username) throws DatafariServerException {
    final String current = getUiConfig(username);
    if (current != null) {
      updateUiConfig(username, current);
    }
  }

  /**
   * Delete user UI config.
   */
  public int deleteUiConfig(final String username) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + UICONFIGCOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to delete ui config for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }
}