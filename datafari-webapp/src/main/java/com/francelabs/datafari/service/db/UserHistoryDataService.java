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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class UserHistoryDataService extends CassandraService {
  final static Logger logger = LogManager.getLogger(UserHistoryDataService.class.getName());

  public static final int MAX_HISTORY_LENGTH = 10;

  public static final String HISTORYCOLLECTION = "user_history";
  public static final String USERNAMECOLUMN = "username";
  public static final String HISTORYCOLUMN = "history";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  private static UserHistoryDataService instance;

  private UserHistoryDataService() {
    refreshSession();
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  public static synchronized UserHistoryDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new UserHistoryDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public boolean isHistoryEnabled() {
    return Boolean.valueOf(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_HISTORY_ENABLED));
  }

  /**
   * Get user history
   *
   * @param username
   * @return the user history
   */
  public synchronized List<String> getHistory(final String username) {
    List<String> history = new ArrayList<>();
    if (isHistoryEnabled()) {
      try {
        final String query = "SELECT " + HISTORYCOLUMN + " FROM " + HISTORYCOLLECTION + " WHERE " + USERNAMECOLUMN + "='" + username + "'";
        final ResultSet result = session.execute(query);
        final Row row = result.one();
        if (row != null && !row.isNull(HISTORYCOLUMN) && !row.getList(HISTORYCOLUMN, String.class).isEmpty()) {
          history = row.getList(HISTORYCOLUMN, String.class);
        }
      } catch (final Exception e) {
        logger.warn("Unable to get history of user " + username + " : " + e.getMessage());
      }
    }
    return history;
  }

  /**
   * Set user history
   *
   * @param username
   * @param history
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int setHistory(final String username, final List<String> history) throws DatafariServerException {
    if (isHistoryEnabled()) {
      try {
        String ttlToUse = userDataTTL;
        if (username.contentEquals("admin")) {
          ttlToUse = "0";
        }
        final List<String> savedHistory = new ArrayList<>(history);
        while (savedHistory.size() > MAX_HISTORY_LENGTH) {
          savedHistory.remove(savedHistory.size() - 1);
        }
        final String query = "INSERT INTO " + HISTORYCOLLECTION + " (" + USERNAMECOLUMN + "," + HISTORYCOLUMN + "," + LASTREFRESHCOLUMN + ")" + " values ('" + username + "',"
            + savedHistory.stream().map(s -> "'" + s.replace("'", "''") + "'").collect(Collectors.joining(",", "[", "]")) + "," + "toTimeStamp(NOW()))" + " USING TTL " + ttlToUse;
        session.execute(query);
      } catch (final Exception e) {
        logger.warn("Unable to insert ui config for user " + username + " : " + e.getMessage());
        // TODO catch specific exception
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Update user history
   *
   * @param username
   * @param history
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int updateHistory(final String username, final List<String> history) throws DatafariServerException {
    if (isHistoryEnabled()) {
      try {
        String ttlToUse = userDataTTL;
        if (username.contentEquals("admin")) {
          ttlToUse = "0";
        }
        final List<String> savedHistory = new ArrayList<>(history);
        while (savedHistory.size() > MAX_HISTORY_LENGTH) {
          savedHistory.remove(savedHistory.size() - 1);
        }
        final String query = "UPDATE " + HISTORYCOLLECTION + " USING TTL " + ttlToUse + " SET " + HISTORYCOLUMN + " = "
            + savedHistory.stream().map(s -> "'" + s.replace("'", "''") + "'").collect(Collectors.joining(",", "[", "]")) + "," + LASTREFRESHCOLUMN + " = toTimeStamp(NOW())" + " WHERE "
            + USERNAMECOLUMN + " = '" + username + "'";
        session.execute(query);
      } catch (final Exception e) {
        logger.warn("Unable to update ui config for user " + username + " : " + e.getMessage());
        // TODO catch specific exception
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshHistory(final String username) throws DatafariServerException {
    if (isHistoryEnabled()) {
      final List<String> userHistory = getHistory(username);
      if (userHistory != null) {
        updateHistory(username, userHistory);
      }
    }
  }

  /**
   *
   * @param username
   * @return CodesReturned.ALLOK value if all was ok
   * @throws DatafariServerException
   */
  public int deleteHistory(final String username) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + HISTORYCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'" + " IF EXISTS";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to update ui config for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }
}
