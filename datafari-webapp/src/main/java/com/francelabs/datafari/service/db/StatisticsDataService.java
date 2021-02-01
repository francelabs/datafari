/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.db;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;
import com.francelabs.datafari.utils.UsageStatisticsConfiguration;

/**
 * Helper class to save user actions statistics to a cassandra table dedicated to them. Please refer to https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/432242693/Usage+Statistics+Storage for
 * a description of the storage schema.
 */
public class StatisticsDataService extends CassandraService {
  private final static Logger logger = LogManager.getLogger(StatisticsDataService.class.getName());
  private static final String STATISTICS_COLLECTION = "user_search_actions";
  // A unique id for a search session
  private static final String QUERY_ID_COLUMN = "query_id";
  // The id of the user performing the action
  private static final String USER_ID_COLUMN = "user_id";
  // An action extracted from the UserActions enum
  private static final String ACTION_COLUMN = "action";
  // A timestamp at which the action happened, better if computed on the client side since network
  // might screw up the order of actions in case of congestion
  // (Click action that happen in a close succsession may not arrive in order on the server for
  // example) Client side timestamp may be tempered with but this is
  // not very likely in our context.
  private static final String TIMESTAMP_COLUMN = "time_stamp";
  // A JSON Object holding complimentary parameters about the action.
  private static final String PARAMETERS_COLUMN = "parameters";

  private static StatisticsDataService instance;

  private final PreparedStatement saveStatistics;
  private final PreparedStatement getUserStatistics;
  private final PreparedStatement deleteUserStatistics;

  private final String userActionsTTL;

  private StatisticsDataService() {
    refreshSession();
    saveStatistics = session.prepare("insert into " + STATISTICS_COLLECTION + " (" + QUERY_ID_COLUMN + "," + USER_ID_COLUMN + "," + TIMESTAMP_COLUMN + "," + ACTION_COLUMN + "," + PARAMETERS_COLUMN
        + ")" + " values (?, ?, ?, ?, ?) USING TTL ?");
    getUserStatistics = session.prepare("SELECT * FROM " + STATISTICS_COLLECTION + " WHERE " + USER_ID_COLUMN + " = ?");
    deleteUserStatistics = session.prepare("DELETE FROM " + STATISTICS_COLLECTION + " WHERE " + QUERY_ID_COLUMN + " = ? AND " + TIMESTAMP_COLUMN + " = ?");

    userActionsTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_ACTIONS_TTL);
  }

  public static synchronized StatisticsDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new StatisticsDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to connect to database : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId, final int num_hit, final Instant timestamp) throws DatafariServerException {
    return saveQueryStatistics(queryId, query, userId, num_hit, timestamp, null);
  }

  public boolean saveQueryStatistics(final String queryId, final String query, final String userId, final int num_hit, final Instant timestamp, final String action) throws DatafariServerException {
    boolean enabled = false;
    enabled = Boolean.parseBoolean(UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));
    if (enabled) {
      final JSONObject parameters = new JSONObject();
      parameters.put("query", query);
      parameters.put("num_hit", num_hit);
      return this.saveStatistic(queryId, userId, UserActions.SEARCH, parameters, timestamp);
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
      final JSONObject parameters = new JSONObject();
      parameters.put("doc_id", document_id);
      parameters.put("rank", rank);
      return this.saveStatistic(queryId, userId, UserActions.OPEN, parameters, timestamp);
    }
    return true;
  }

  public boolean saveStatistic(final String queryId, final String userId, final UserActions action, final JSONObject parameters, final Instant timestamp) throws DatafariServerException {
    boolean enabled = false;
    enabled = Boolean.parseBoolean(UsageStatisticsConfiguration.getInstance().getProperty(UsageStatisticsConfiguration.ENABLED, "false"));
    if (enabled) {
      try {
        final BoundStatement bs = saveStatistics.bind(queryId, userId, timestamp, action.toString(), parameters.toJSONString(), userActionsTTL);
        session.execute(bs);
      } catch (final DriverException e) {
        logger.warn("Unable to register statistics in cassandra for the query : " + e.getMessage());
        // TODO catch specific exception
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
    }
    return true;
  }

  public JSONArray getUserStatistics(final String username) {
    final JSONArray userStatisticsObj = new JSONArray();
    final BoundStatement bs = getUserStatistics.bind(username);
    final ResultSet results = session.execute(bs);
    for (final Row row : results) {
      final JSONObject stat = new JSONObject();
      stat.put(QUERY_ID_COLUMN, row.getString(QUERY_ID_COLUMN));
      stat.put(USER_ID_COLUMN, row.getString(USER_ID_COLUMN));
      stat.put(ACTION_COLUMN, row.getString(ACTION_COLUMN));
      stat.put(TIMESTAMP_COLUMN, row.getInstant(TIMESTAMP_COLUMN));
      stat.put(PARAMETERS_COLUMN, row.getString(PARAMETERS_COLUMN));
      userStatisticsObj.add(stat);
    }
    return userStatisticsObj;
  }

  public int deleteUserStatistics(final String username) throws DatafariServerException {
    try {
      final JSONArray userStats = getUserStatistics(username);
      for (int i = 0; i < userStats.size(); i++) {
        final JSONObject stat = (JSONObject) userStats.get(i);
        final String queryId = stat.get(QUERY_ID_COLUMN).toString();
        final Instant timestamp = (Instant) stat.get(TIMESTAMP_COLUMN);
        final BoundStatement bs = deleteUserStatistics.bind(queryId, timestamp);
        session.execute(bs);
      }
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public static enum UserActions {
    SEARCH("SEARCH"), OPEN("OPEN"), RESULT_PAGE_CHANGE("RESULT_PAGE_CHANGE"), FACET_CLICK("FACET_CLICK"), OPEN_PREVIEW("OPEN_PREVIEW"), PREVIEW_CHANGE_DOC("PREVIEW_CHANGE_DOC"),
    PREVIEW_OPEN_DOC("PREVIEW_OPEN_DOC"), OPEN_PREVIEW_SHARED("OPEN_PREVIEW_SHARED");

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
