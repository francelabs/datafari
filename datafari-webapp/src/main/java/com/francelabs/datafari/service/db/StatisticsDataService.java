package com.francelabs.datafari.service.db;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;
import com.francelabs.datafari.utils.UsageStatisticsConfiguration;

/**
 * PostgreSQL version of StatisticsDataService.
 * 100% API compatible with Cassandra version.
 */
public class StatisticsDataService {

  private final static Logger logger = LogManager.getLogger(StatisticsDataService.class.getName());
  private static final String STATISTICS_COLLECTION = "user_search_actions";
  private static final String QUERY_ID_COLUMN = "query_id";
  private static final String USER_ID_COLUMN = "user_id";
  private static final String ACTION_COLUMN = "action";
  private static final String TIMESTAMP_COLUMN = "time_stamp";
  private static final String PARAMETERS_COLUMN = "parameters";

  private static StatisticsDataService instance;

  // Not native in PGSQL, left for doc
  private final int userActionsTTL;

  private final PostgresService pgService = new PostgresService();

  private StatisticsDataService() {
    userActionsTTL = Integer.parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_ACTIONS_TTL, "0"));
  }

  public static synchronized StatisticsDataService getInstance() {
    if (instance == null) {
      instance = new StatisticsDataService();
    }
    return instance;
  }

  /**
   * Get user history from user_search_actions table
   */
  public synchronized JSONArray getHistory(String username, String query) {
    final JSONParser parser = new JSONParser();
    final JSONArray userStatisticsObj = new JSONArray();

    final String sql = "SELECT * FROM " + STATISTICS_COLLECTION + " WHERE " + USER_ID_COLUMN + " = ?";
    final String PATTERN_FORMAT = "yyyy-MM-dd HH:mm:ss";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
            .withZone(ZoneId.systemDefault());

    try (ResultSet results = pgService.executeSelect(sql, username)) {
      while (results.next()) {
        try {
          String paramStr = results.getString(PARAMETERS_COLUMN);
          if (paramStr != null && query != null &&
              !java.util.regex.Pattern.compile(".*query.*" + query + ".*").matcher(paramStr).matches()) {
            continue;
          }
          final HashMap<String, Object> stat = new HashMap<>();
          stat.put(QUERY_ID_COLUMN, results.getString(QUERY_ID_COLUMN));
          stat.put(USER_ID_COLUMN, results.getString(USER_ID_COLUMN));
          stat.put(ACTION_COLUMN, results.getString(ACTION_COLUMN));
          Timestamp ts = results.getTimestamp(TIMESTAMP_COLUMN);
          stat.put(TIMESTAMP_COLUMN, ts != null ? formatter.format(ts.toInstant()) : null);
          stat.put(PARAMETERS_COLUMN, parser.parse(paramStr));
          userStatisticsObj.add(new JSONObject(stat));
        } catch (ParseException e) {
          logger.warn("Skipping a stat entry because the parameter column is not a valid json.", e);
        }
      }
    } catch (Exception e) {
      logger.warn("Unable to get history", e);
    }
    return userStatisticsObj;
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId, final int num_hit, final Instant timestamp) throws DatafariServerException {
    return saveQueryStatistics(queryId, query, userId, num_hit, timestamp, null);
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId, final int num_hit, final Instant timestamp, final String action) throws DatafariServerException {
    boolean enabled = false;
    enabled = Boolean.parseBoolean(UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));
    if (enabled) {
      final HashMap<String, Object> parameters = new HashMap<>();
      parameters.put("query", query);
      parameters.put("num_hit", num_hit);
      final JSONObject jsonParams = new JSONObject(parameters);
      return this.saveStatistic(queryId, userId, UserActions.SEARCH, jsonParams, timestamp);
    }
    return true;
  }

  public boolean saveClickStatistics(final String queryId, final String query, final String userId, final String document_id, final int rank, final Instant timestamp) throws DatafariServerException {
    return saveClickStatistics(queryId, query, userId, document_id, rank, timestamp, null);
  }

  public boolean saveClickStatistics(final String queryId, final String query, final String userId, final String document_id, final int rank, final Instant timestamp, final String action)
      throws DatafariServerException {
    boolean enabled = false;
    enabled = Boolean.parseBoolean(UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));
    if (enabled) {
      final HashMap<String, Object> parameters = new HashMap<>();
      parameters.put("doc_id", document_id);
      parameters.put("rank", rank);
      final JSONObject jsonParams = new JSONObject(parameters);
      return this.saveStatistic(queryId, userId, UserActions.OPEN, jsonParams, timestamp);
    }
    return true;
  }

  public boolean saveStatistic(final String queryId, final String userId, final UserActions action, final JSONObject parameters, final Instant timestamp) throws DatafariServerException {
    boolean enabled = false;
    enabled = Boolean.parseBoolean(UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));
    if (enabled) {
      try {
        String sql = "INSERT INTO " + STATISTICS_COLLECTION +
            " (" + QUERY_ID_COLUMN + ", " + USER_ID_COLUMN + ", " + TIMESTAMP_COLUMN + ", " + ACTION_COLUMN + ", " + PARAMETERS_COLUMN + ") " +
            "VALUES (?, ?, ?, ?, ?::jsonb)";
        pgService.executeUpdate(sql, queryId, userId, Timestamp.from(timestamp), action.toString(), parameters.toJSONString());
      } catch (final Exception e) {
        logger.warn("Unable to register statistics in PostgreSQL for the query : " + e.getMessage());
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  public JSONArray getUserStatistics(final String username) {
    final JSONParser parser = new JSONParser();
    final JSONArray userStatisticsObj = new JSONArray();
    final String sql = "SELECT * FROM " + STATISTICS_COLLECTION + " WHERE " + USER_ID_COLUMN + " = ?";
    try (ResultSet results = pgService.executeSelect(sql, username)) {
      while (results.next()) {
        try {
          final HashMap<String, Object> stat = new HashMap<>();
          stat.put(QUERY_ID_COLUMN, results.getString(QUERY_ID_COLUMN));
          stat.put(USER_ID_COLUMN, results.getString(USER_ID_COLUMN));
          stat.put(ACTION_COLUMN, results.getString(ACTION_COLUMN));
          Timestamp ts = results.getTimestamp(TIMESTAMP_COLUMN);
          stat.put(TIMESTAMP_COLUMN, ts != null ? ts.toInstant() : null);
          stat.put(PARAMETERS_COLUMN, parser.parse(results.getString(PARAMETERS_COLUMN)));
          userStatisticsObj.add(new JSONObject(stat));
        } catch (ParseException e) {
          logger.warn("Skipping a stat entry because the parameter column is not a valid json.", e);
        }
      }
    } catch (Exception e) {
      logger.warn("Unable to getUserStatistics", e);
    }
    return userStatisticsObj;
  }

  public int deleteUserStatistics(final String username) throws DatafariServerException {
    try {
      final JSONArray userStats = getUserStatistics(username);
      for (int i = 0; i < userStats.size(); i++) {
        final JSONObject stat = (JSONObject) userStats.get(i);
        final String queryId = stat.get(QUERY_ID_COLUMN).toString();
        final Instant timestamp = ((java.sql.Timestamp)stat.get(TIMESTAMP_COLUMN)).toInstant();
        String sql = "DELETE FROM " + STATISTICS_COLLECTION +
            " WHERE " + QUERY_ID_COLUMN + " = ? AND " + TIMESTAMP_COLUMN + " = ?";
        pgService.executeUpdate(sql, queryId, Timestamp.from(timestamp));
      }
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
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

    private String name;

    UserActions(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}