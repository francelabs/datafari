package com.francelabs.datafari.service.db;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class SavedSearchDataService {

  final static Logger logger = LogManager.getLogger(SavedSearchDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String SEARCHCOLLECTION = "search";
  public static final String REQUESTCOLUMN = "request";
  public static final String REQUESTNAMECOLUMN = "name";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  // TTL variable kept for compatibility/documentation
  // Note: PostgreSQL does not natively support TTL.
  private final String userDataTTL = null;

  private static SavedSearchDataService instance;

  private final PostgresService pgService = new PostgresService();

  public static synchronized SavedSearchDataService getInstance() {
    if (instance == null) {
      instance = new SavedSearchDataService();
    }
    return instance;
  }

  /**
   * Add a search to the list of searches saved by the user
   */
  public int saveSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      // Emulation of TTL could be handled by a cleanup job if needed
      String sql = "INSERT INTO " + SEARCHCOLLECTION + " (" +
          USERNAMECOLUMN + ", " +
          REQUESTNAMECOLUMN + ", " +
          REQUESTCOLUMN + ", " +
          LASTREFRESHCOLUMN + ") VALUES (?, ?, ?, now()) " +
          "ON CONFLICT (" + USERNAMECOLUMN + ", " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN + ") DO UPDATE SET " +
          LASTREFRESHCOLUMN + " = now()";
      pgService.executeUpdate(sql, username, requestName, request);
    } catch (final Exception e) {
      logger.error("Unable to save search in database for user: " + username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * delete a search
   */
  public int deleteSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      String sql = "DELETE FROM " + SEARCHCOLLECTION +
          " WHERE " + USERNAMECOLUMN + " = ?" +
          " AND " + REQUESTCOLUMN + " = ?" +
          " AND " + REQUESTNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, username, request, requestName);
    } catch (final Exception e) {
      logger.error("Unable to delete search in database for user: " + username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * get all the saved searches of a user
   */
  public Map<String, String> getSearches(final String username) throws Exception {
    final Map<String, String> searches = new HashMap<>();
    String sql = "SELECT " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN +
        " FROM " + SEARCHCOLLECTION +
        " WHERE " + USERNAMECOLUMN + " = ?";
    try (ResultSet rs = pgService.executeSelect(sql, username)) {
      while (rs.next()) {
        searches.put(rs.getString(REQUESTNAMECOLUMN), rs.getString(REQUESTCOLUMN));
      }
    }
    return searches;
  }

  /**
   * Delete all saved searches of a user
   */
  public int removeSearches(final String username) throws Exception {
    final Map<String, String> searches = getSearches(username);
    for (final String searchName : searches.keySet()) {
      final String search = searches.get(searchName);
      try {
        String sql = "DELETE FROM " + SEARCHCOLLECTION +
            " WHERE " + USERNAMECOLUMN + " = ?" +
            " AND " + REQUESTNAMECOLUMN + " = ?" +
            " AND " + REQUESTCOLUMN + " = ?";
        pgService.executeUpdate(sql, username, searchName, search);
      } catch (final Exception e) {
        logger.error("Unable to delete saved search for user: " + username, e);
        // Continue deleting remaining, but signal error
        return CodesReturned.ALREADYPERFORMED.getValue();
      }
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * refresh a search
   */
  private int refreshSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      String sql = "UPDATE " + SEARCHCOLLECTION +
          " SET " + LASTREFRESHCOLUMN + " = now()" +
          " WHERE " + USERNAMECOLUMN + " = ?" +
          " AND " + REQUESTCOLUMN + " = ?" +
          " AND " + REQUESTNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, username, request, requestName);
    } catch (final Exception e) {
      logger.warn("Unable to refresh search in database for user: " + username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshSavedSearches(final String username) throws DatafariServerException {
    try {
      final Map<String, String> userSearches = getSearches(username);
      if (userSearches != null && !userSearches.isEmpty()) {
        for (final Map.Entry<String, String> entry : userSearches.entrySet()) {
          final String requestName = entry.getKey();
          final String request = entry.getValue();
          refreshSearch(username, requestName, request);
        }
      }
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

}