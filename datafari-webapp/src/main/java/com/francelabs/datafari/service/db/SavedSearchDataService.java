package com.francelabs.datafari.service.db;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class SavedSearchDataService {

  private static final Logger logger = LogManager.getLogger(SavedSearchDataService.class);

  public static final String USERNAMECOLUMN = "username";
  public static final String SEARCHCOLLECTION = "search";
  public static final String REQUESTCOLUMN = "request";
  public static final String REQUESTNAMECOLUMN = "name";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  // TTL non supporté nativement par PG ; conservé à titre documentaire.
  @SuppressWarnings("unused")
  private final String userDataTTL = null;

  private static SavedSearchDataService instance;

  // Helpers SQL via le pont statique
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  private SavedSearchDataService() {
    this.jdbc  = SqlService.get().getJdbcTemplate();
    this.named = SqlService.get().getNamedJdbcTemplate();
  }

  public static synchronized SavedSearchDataService getInstance() {
    if (instance == null) {
      instance = new SavedSearchDataService();
    }
    return instance;
  }

  /**
   * Ajoute (ou rafraîchit) une recherche sauvegardée pour l’utilisateur.
   */
  public int saveSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      final String sql =
          "INSERT INTO " + SEARCHCOLLECTION + " (" +
              USERNAMECOLUMN + ", " +
              REQUESTNAMECOLUMN + ", " +
              REQUESTCOLUMN + ", " +
              LASTREFRESHCOLUMN + ") " +
          "VALUES (?, ?, ?, now()) " +
          "ON CONFLICT (" + USERNAMECOLUMN + ", " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN + ") " +
          "DO UPDATE SET " + LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;

      jdbc.update(sql, username, requestName, request);
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.error("Unable to save search in database for user: {}", username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
  }

  /**
   * Supprime une recherche sauvegardée.
   */
  public int deleteSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      final String sql =
          "DELETE FROM " + SEARCHCOLLECTION +
          " WHERE " + USERNAMECOLUMN + " = ? " +
          " AND " + REQUESTCOLUMN + " = ? " +
          " AND " + REQUESTNAMECOLUMN + " = ?";

      jdbc.update(sql, username, request, requestName);
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.error("Unable to delete search in database for user: {}", username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
  }

  /**
   * Récupère toutes les recherches sauvegardées d’un utilisateur.
   * Map<name, request>
   */
  public Map<String, String> getSearches(final String username) throws Exception {
    final String sql =
        "SELECT " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN +
        " FROM " + SEARCHCOLLECTION +
        " WHERE " + USERNAMECOLUMN + " = ?";

    return jdbc.query(sql, rs -> {
      final Map<String, String> out = new HashMap<>();
      while (rs.next()) {
        out.put(rs.getString(REQUESTNAMECOLUMN), rs.getString(REQUESTCOLUMN));
      }
      return out;
    }, username);
  }

  /**
   * Supprime toutes les recherches sauvegardées d’un utilisateur.
   */
  public int removeSearches(final String username) throws Exception {
    try {
      final String sql = "DELETE FROM " + SEARCHCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.error("Unable to delete saved searches for user: {}", username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
  }

  /**
   * Met à jour le last_refresh de toutes les recherches d’un utilisateur.
   */
  public void refreshSavedSearches(final String username) throws DatafariServerException {
    try {
      final String sql =
          "UPDATE " + SEARCHCOLLECTION +
          " SET " + LASTREFRESHCOLUMN + " = now()" +
          " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }
}