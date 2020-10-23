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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class SavedSearchDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(SavedSearchDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String SEARCHCOLLECTION = "search";
  public static final String REQUESTCOLUMN = "request";
  public static final String REQUESTNAMECOLUMN = "name";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;
  private static SavedSearchDataService instance;

  public static synchronized SavedSearchDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new SavedSearchDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  private SavedSearchDataService() {
    refreshSession();
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  /**
   * Add a search to the list of searches saved by the user
   *
   * @param username
   *          of the user
   * @param requestName
   *          the request name
   * @param request
   *          the search request
   * @return Search.ALREADYPERFORMED if the search was already saved, CodesUser.ALLOK if all was ok
   */
  public int saveSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      String ttlToUse = userDataTTL;
      if (username.contentEquals("admin")) {
        ttlToUse = "0";
      }
      final String query = "insert into " + SEARCHCOLLECTION + " (" + USERNAMECOLUMN + "," + REQUESTNAMECOLUMN + "," + REQUESTCOLUMN + "," + LASTREFRESHCOLUMN + ")" + " values ('" + username + "',$$"
          + requestName + "$$,$$" + request + "$$,toTimeStamp(NOW())) USING TTL " + ttlToUse;
      session.execute(query);
      // TODO change exception
    } catch (final Exception e) {
      logger.error("Unable to save search in database for user: " + username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }

    return CodesReturned.ALLOK.getValue();
  }

  /**
   * delete a search
   *
   * @param username
   *          of the user
   * @param requestName
   *          the request name
   * @param request
   *          the search request
   * @return Search.ALREADYPERFORMED if the search was already deleted, Search.ALLOK if all was ok and Search.CodesReturned.CASSANDRAN if
   *         there's an error
   */
  public int deleteSearch(final String username, final String requestName, final String request) throws Exception {
    try {
      final String query = "DELETE FROM " + SEARCHCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'" + " AND " + REQUESTCOLUMN + " = $$" + request + "$$ AND " + REQUESTNAMECOLUMN
          + " = $$" + requestName + "$$ IF EXISTS";
      session.execute(query);
    } catch (final Exception e) {
      logger.error("Unable to delete search in database for user: " + username, e);
      return CodesReturned.ALREADYPERFORMED.getValue();
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * get all the saved searches of a user
   *
   * @param username
   *          of the user
   * @return an array list of all the the saved searches of the user. Return null if there's an error.
   */
  public Map<String, String> getSearches(final String username) throws Exception {
    final Map<String, String> searches = new HashMap<>();
    final ResultSet results = session.execute("SELECT " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN + " FROM " + SEARCHCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
    for (final Row row : results) {
      searches.put(row.getString(REQUESTNAMECOLUMN), row.getString(REQUESTCOLUMN));
    }
    return searches;
  }

  /**
   * Delete all saved searches of a user
   *
   * @param username
   * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONCASSANDRA
   */
  public int removeSearches(final String username) throws Exception {
    final Map<String, String> searches = getSearches(username);
    for (final String searchName : searches.keySet()) {
      final String search = searches.get(searchName);
      final String query = "DELETE FROM " + SEARCHCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "' AND " + REQUESTNAMECOLUMN + "=$$" + searchName + "$$ AND " + REQUESTCOLUMN + "=$$"
          + search + "$$ IF EXISTS";
      session.execute(query);
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshSavedSearches(final String username) throws DatafariServerException {
    try {
      final Map<String, String> userSearches = getSearches(username);
      if (userSearches != null && !userSearches.isEmpty()) {
        removeSearches(username);
        for (final Map.Entry<String, String> entry : userSearches.entrySet()) {
          final String requestName = entry.getKey();
          final String request = entry.getValue();
          saveSearch(username, requestName, request);
        }
      }
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

}