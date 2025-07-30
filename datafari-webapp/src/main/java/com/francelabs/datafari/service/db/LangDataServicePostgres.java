package com.francelabs.datafari.service.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class LangDataServicePostgres {

  final static Logger logger = LogManager.getLogger(LangDataServicePostgres.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String LANGCOLLECTION = "lang";
  public static final String LANGCOLUMN = "lang";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  private static LangDataServicePostgres instance;

  private final PostgresService pgService = new PostgresService();

  public static synchronized LangDataServicePostgres getInstance() {
    if (instance == null) {
      instance = new LangDataServicePostgres();
    }
    return instance;
  }

  private LangDataServicePostgres() {
    // No TTL in PG, just keep for compat
    this.userDataTTL = null;
  }

  /**
   * Get user preferred lang
   *
   * @param username
   * @return the user preferred lang
   */
  public synchronized String getLang(final String username) {
    String lang = null;
    try {
      String sql = "SELECT " + LANGCOLUMN + " FROM " + LANGCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      try (ResultSet rs = pgService.executeSelect(sql, username)) {
        if (rs.next()) {
          lang = rs.getString(LANGCOLUMN);
        }
      }
    } catch (final Exception e) {
      logger.warn("Unable to get lang for user " + username + " : " + e.getMessage());
    }
    return lang;
  }

  /**
   * Set user lang
   *
   * @param username
   * @param lang
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int setLang(final String username, final String lang) throws DatafariServerException {
    try {
      String sql = "INSERT INTO " + LANGCOLLECTION
          + " (" + USERNAMECOLUMN + "," + LANGCOLUMN + "," + LASTREFRESHCOLUMN + ")"
          + " VALUES (?, ?, ?)"
          + " ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET "
          + LANGCOLUMN + " = EXCLUDED." + LANGCOLUMN + ", "
          + LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
      pgService.executeUpdate(sql, username, lang, new Timestamp(System.currentTimeMillis()));
    } catch (final Exception e) {
      logger.warn("Unable to insert lang for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Update user lang
   *
   * @param username
   * @param lang
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int updateLang(final String username, final String lang) throws DatafariServerException {
    try {
      String sql = "UPDATE " + LANGCOLLECTION
          + " SET " + LANGCOLUMN + " = ?, "
          + LASTREFRESHCOLUMN + " = ?"
          + " WHERE " + USERNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, lang, new Timestamp(System.currentTimeMillis()), username);
    } catch (final Exception e) {
      logger.warn("Unable to update lang for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshLang(final String username) throws DatafariServerException {
    String userLang = getLang(username);
    if (userLang != null) {
      updateLang(username, userLang);
    }
  }

  /**
   *
   * @param username
   * @return CodesReturned.ALLOK value if all was ok
   * @throws DatafariServerException
   */
  public int deleteLang(final String username) throws DatafariServerException {
    try {
      String sql = "DELETE FROM " + LANGCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, username);
    } catch (final Exception e) {
      logger.warn("Unable to delete lang for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }
}