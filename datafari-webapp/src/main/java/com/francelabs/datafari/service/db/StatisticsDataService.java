package com.francelabs.datafari.service.db;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;
import com.francelabs.datafari.utils.UsageStatisticsConfiguration;

/**
 * PostgreSQL implementation of statistics persistence (user_search_actions).
 */
public class StatisticsDataService {

  private static final Logger logger = LogManager.getLogger(StatisticsDataService.class);

  private static final String STATISTICS_COLLECTION = "user_search_actions";
  private static final String QUERY_ID_COLUMN = "query_id";
  private static final String USER_ID_COLUMN = "user_id";
  private static final String ACTION_COLUMN = "action";
  private static final String TIMESTAMP_COLUMN = "time_stamp";
  private static final String PARAMETERS_COLUMN = "parameters"; // jsonb

  private static StatisticsDataService instance;

  // Kept for documentation (no native TTL in PG)
  @SuppressWarnings("unused")
  private final int userActionsTTL;

  // SQL helpers via the static bridge
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  private static final DateTimeFormatter TS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private StatisticsDataService() {
    this.jdbc  = SqlService.get().getJdbcTemplate();
    this.named = SqlService.get().getNamedJdbcTemplate();
    this.userActionsTTL = Integer.parseInt(
        GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_ACTIONS_TTL, "0"));
  }

  public static synchronized StatisticsDataService getInstance() {
    if (instance == null) {
      instance = new StatisticsDataService();
    }
    return instance;
  }

  /**
   * Returns user history (optionally filtered by a substring on the "query" field inside parameters JSON).
   * The JSON returned keeps the same structure as before.
   */
  public synchronized JSONArray getHistory(final String username, final String query) {
    final JSONArray out = new JSONArray();
    final JSONParser parser = new JSONParser();

    // Si 'query' est fourni, on filtre côté SQL dans parameters->>'query' (jsonb -> text)
    final boolean filter = (query != null && !query.isEmpty());
    final String sql =
        "SELECT " + QUERY_ID_COLUMN + ", " + USER_ID_COLUMN + ", " + ACTION_COLUMN + ", " +
        "       " + TIMESTAMP_COLUMN + ", " + PARAMETERS_COLUMN +
        "  FROM " + STATISTICS_COLLECTION +
        " WHERE " + USER_ID_COLUMN + " = ? " +
        (filter ? " AND " + PARAMETERS_COLUMN + "->>'query' ILIKE ? " : "") +
        " ORDER BY " + TIMESTAMP_COLUMN + " DESC";

    final Object[] args = filter ? new Object[] { username, "%" + query + "%" }
                                 : new Object[] { username };

    jdbc.query(sql, rs -> {
      try {
        final Map<String,Object> stat = new HashMap<>();
        stat.put(QUERY_ID_COLUMN, rs.getString(QUERY_ID_COLUMN));
        stat.put(USER_ID_COLUMN,  rs.getString(USER_ID_COLUMN));
        stat.put(ACTION_COLUMN,   rs.getString(ACTION_COLUMN));

        final Timestamp ts = rs.getTimestamp(TIMESTAMP_COLUMN);
        stat.put(TIMESTAMP_COLUMN, ts != null ? TS_FORMATTER.format(ts.toInstant()) : null);

        // parameters est jsonb -> on récupère en String puis on parse
        final String params = rs.getString(PARAMETERS_COLUMN);
        stat.put(PARAMETERS_COLUMN, params != null ? parser.parse(params) : null);

        out.add(new JSONObject(stat));
      } catch (Exception ex) {
        // On loggue mais on continue (une ligne mal formée n'interrompt pas tout)
        logger.warn("Skipping malformed statistics row", ex);
      }
    }, args);

    return out;
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId,
                                     final int num_hit, final Instant timestamp) throws DatafariServerException {
    return saveQueryStatistics(queryId, query, userId, num_hit, timestamp, null);
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId,
                                     final int num_hit, final Instant timestamp, final String action)
      throws DatafariServerException {
    final boolean enabled = Boolean.parseBoolean(
        UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));

    if (!enabled) return true;

    final JSONObject jsonParams = new JSONObject();
    jsonParams.put("query", query);
    jsonParams.put("num_hit", num_hit);

    return saveStatistic(queryId, userId, UserActions.SEARCH, jsonParams, timestamp);
  }

  public boolean saveClickStatistics(final String queryId, final String query, final String userId,
                                     final String document_id, final int rank, final Instant timestamp)
      throws DatafariServerException {
    return saveClickStatistics(queryId, query, userId, document_id, rank, timestamp, null);
  }

  public boolean saveClickStatistics(final String queryId, final String query, final String userId,
                                     final String document_id, final int rank, final Instant timestamp,
                                     final String action) throws DatafariServerException {
    final boolean enabled = Boolean.parseBoolean(
        UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));

    if (!enabled) return true;

    final JSONObject jsonParams = new JSONObject();
    jsonParams.put("doc_id", document_id);
    jsonParams.put("rank", rank);

    return saveStatistic(queryId, userId, UserActions.OPEN, jsonParams, timestamp);
  }

  public boolean saveStatistic(final String queryId, final String userId, final UserActions action,
                               final JSONObject parameters, final Instant timestamp)
      throws DatafariServerException {
    final boolean enabled = Boolean.parseBoolean(
        UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));

    if (!enabled) return true;

    try {
      final String sql =
          "INSERT INTO " + STATISTICS_COLLECTION + " (" +
              QUERY_ID_COLUMN + ", " + USER_ID_COLUMN + ", " +
              TIMESTAMP_COLUMN + ", " + ACTION_COLUMN + ", " + PARAMETERS_COLUMN + ") " +
          "VALUES (?, ?, ?, ?, ?::jsonb)";

      jdbc.update(sql,
          queryId,
          userId,
          Timestamp.from(timestamp),
          action.toString(),
          parameters != null ? parameters.toJSONString() : "{}"
      );
      return true;
    } catch (Exception e) {
      logger.warn("Unable to register statistics in PostgreSQL: {}", e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Retourne toutes les stats d’un utilisateur (sans filtre).
   */
  @SuppressWarnings("unchecked")
  public JSONArray getUserStatistics(final String username) {
    final JSONArray out = new JSONArray();
    final JSONParser parser = new JSONParser();

    final String sql =
        "SELECT " + QUERY_ID_COLUMN + ", " + USER_ID_COLUMN + ", " + ACTION_COLUMN + ", " +
        "       " + TIMESTAMP_COLUMN + ", " + PARAMETERS_COLUMN +
        "  FROM " + STATISTICS_COLLECTION +
        " WHERE " + USER_ID_COLUMN + " = ? " +
        " ORDER BY " + TIMESTAMP_COLUMN + " DESC";

    jdbc.query(sql, rs -> {
      try {
        final Map<String,Object> stat = new HashMap<>();
        stat.put(QUERY_ID_COLUMN, rs.getString(QUERY_ID_COLUMN));
        stat.put(USER_ID_COLUMN,  rs.getString(USER_ID_COLUMN));
        stat.put(ACTION_COLUMN,   rs.getString(ACTION_COLUMN));

        final Timestamp ts = rs.getTimestamp(TIMESTAMP_COLUMN);
        stat.put(TIMESTAMP_COLUMN, ts != null ? ts.toInstant() : null);

        final String params = rs.getString(PARAMETERS_COLUMN);
        stat.put(PARAMETERS_COLUMN, params != null ? parser.parse(params) : null);

        out.add(new JSONObject(stat));
      } catch (Exception ex) {
        logger.warn("Skipping malformed statistics row", ex);
      }
    }, username);

    return out;
  }

  /**
   * Supprime toutes les stats d’un utilisateur (plus simple et plus performant que
   * de boucler sur un JSONArray).
   */
  public int deleteUserStatistics(final String username) throws DatafariServerException {
    try {
      final String sql =
          "DELETE FROM " + STATISTICS_COLLECTION + " WHERE " + USER_ID_COLUMN + " = ?";
      jdbc.update(sql, username);
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public static enum UserActions {
    SEARCH("SEARCH"),
    OPEN("OPEN"),
    RESULT_PAGE_CHANGE("RESULT_PAGE_CHANGE"),
    FACET_CLICK("FACET_CLICK"),
    OPEN_PREVIEW("OPEN_PREVIEW"),
    PREVIEW_CHANGE_DOC("PREVIEW_CHANGE_DOC"),
    PREVIEW_OPEN_DOC("PREVIEW_OPEN_DOC"),
    OPEN_PREVIEW_SHARED("OPEN_PREVIEW_SHARED");

    private final String name;

    UserActions(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}