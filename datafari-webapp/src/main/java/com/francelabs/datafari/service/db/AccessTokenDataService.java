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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class AccessTokenDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(AccessTokenDataService.class.getName());

  public static final String ACCESS_TOKENS_COLLECTION = "access_tokens";
  public static final String USERNAME_COLUMN = "username";
  public static final String API_COLUMN = "api";
  public static final String IDENTIFIER_COLUMN = "identifier";
  public static final String TOKEN_COLUMN = "a_token";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  private static AccessTokenDataService instance;

  public static synchronized AccessTokenDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new AccessTokenDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  private AccessTokenDataService() {
    refreshSession();
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  /**
   * Get user's API token
   *
   * @param username
   * @param api
   *          the API the token belongs to
   * @return the user's API token
   */
  public AccessToken getToken(final String username, final String api) {
    AccessToken token = null;
    try {
      final String query = "SELECT " 
          + TOKEN_COLUMN + "," 
          + IDENTIFIER_COLUMN 
          + " FROM " + ACCESS_TOKENS_COLLECTION 
          + " WHERE " + USERNAME_COLUMN + "='" + username + "'" 
          + " AND " + API_COLUMN + "='" + api + "'";
      final ResultSet result = session.execute(query);
      final Row row = result.one();
      if (row != null && !row.isNull(TOKEN_COLUMN) && !row.getString(TOKEN_COLUMN).isEmpty()) {
        token = new AccessToken(api, row.getString(IDENTIFIER_COLUMN), row.getString(TOKEN_COLUMN));
      }
    } catch (final Exception e) {
      logger.warn("Unable to get token for user " + username + " for API " + api, e);
    }
    return token;
  }

  /**
   * Set user's API Token
   *
   * @param username
   * @param api
   *          the API the token belongs to
   * @param identifier
   *          identifier for the token
   * @param token
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int setToken(final String username, final String api, final String identifier, final String token) throws DatafariServerException {
    try {
      String ttlToUse = userDataTTL;
      if (username.contentEquals("admin")) {
        ttlToUse = "0";
      }
      final String query = "INSERT INTO " + ACCESS_TOKENS_COLLECTION 
          + " (" + USERNAME_COLUMN  + "," 
          + API_COLUMN + "," 
          + IDENTIFIER_COLUMN + "," 
          + TOKEN_COLUMN + "," 
          + LASTREFRESHCOLUMN + ")"
          + " values ('" + username + "'," 
          + "$$" + api + "$$," 
          + "$$" + identifier + "$$," 
          + "$$" + token + "$$," 
          + "toTimeStamp(NOW()))"
          + " USING TTL " + ttlToUse;
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to insert Token for user " + username + " for API " + api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Update user's API Token
   *
   * @param username
   * @param api
   * @param identifier
   *          identifier for the token
   * @param token
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int updateToken(final String username, final String api, final String identifier, final String token) throws DatafariServerException {
    try {
      String ttlToUse = userDataTTL;
      final String query = "UPDATE " + ACCESS_TOKENS_COLLECTION 
          + " USING TTL " + ttlToUse 
          + " SET " + TOKEN_COLUMN + " = '" + token + "'," 
          + LASTREFRESHCOLUMN + " = toTimeStamp(NOW())"
          + " WHERE " + USERNAME_COLUMN + " = '" + username 
          + "' AND " + API_COLUMN + " = '" + api 
          + "' AND " + IDENTIFIER_COLUMN + " = $$" + identifier + "$$";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to update Token for user " + username + " for API " + api, e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Delete user's API Token
   *
   * @param username
   * @param api
   * @param identifier
   *          identifier for the token
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int deleteToken(final String username, final String api, final String identifier) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + ACCESS_TOKENS_COLLECTION 
          + " WHERE " + USERNAME_COLUMN + " = '" + username 
          + "' AND " + API_COLUMN + " = '" + api 
          + "' AND " + IDENTIFIER_COLUMN + " = $$" + identifier + "$$"
          + " IF EXISTS";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to delete Token for user " + username + " for API " + api, e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * get all the API tokens of a user
   *
   * @param username
   *          of the user
   * @return an array list of all the API tokens of the user. Return null if there's an error.
   */
  public JSONArray getTokens(final String username) throws Exception {
    final JSONArray tokens = new JSONArray();
    final ResultSet results = session
        .execute("SELECT " + API_COLUMN + "," 
            + IDENTIFIER_COLUMN + "," 
            + TOKEN_COLUMN 
            + " FROM " + ACCESS_TOKENS_COLLECTION 
            + " WHERE " + USERNAME_COLUMN + "='" + username + "'");
    for (final Row row : results) {
      final JSONObject token = new JSONObject();
      token.put(API_COLUMN, row.getString(API_COLUMN));
      token.put(IDENTIFIER_COLUMN, row.getString(IDENTIFIER_COLUMN));
      token.put(TOKEN_COLUMN, row.getString(TOKEN_COLUMN));
      tokens.add(token);
    }
    return tokens;
  }

  /**
   * Delete all API Tokens of a user
   *
   * @param username
   * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONCASSANDRA
   */
  public int removeTokens(final String username) throws DatafariServerException {
    try {
      final JSONArray tokens = getTokens(username);
      for (int i = 0; i < tokens.size(); i++) {
        final JSONObject token = (JSONObject) tokens.get(i);
        final String api = token.get(API_COLUMN).toString();
        final String identifier = token.get(IDENTIFIER_COLUMN).toString();
        final String query = "DELETE FROM " + ACCESS_TOKENS_COLLECTION 
            + " WHERE " + USERNAME_COLUMN + " = '" + username + "'"
            + " AND " + API_COLUMN + "='" + api + "'"
            + " AND " + IDENTIFIER_COLUMN + "=$$" + identifier + "$$" 
            + " IF EXISTS";
        session.execute(query);
      }
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void refreshAccessTokens(final String username) throws DatafariServerException {
    try {
      final JSONArray userTokens = getTokens(username);
      if (userTokens != null && !userTokens.isEmpty()) {
        for (final Object oToken : userTokens) {
          final JSONObject userToken = (JSONObject) oToken;
          final String api = userToken.get(API_COLUMN).toString();
          final String identifier = userToken.get(IDENTIFIER_COLUMN).toString();
          final String token = userToken.get(TOKEN_COLUMN).toString();
          updateToken(username, api, identifier, token);
        }
      }
    } catch (final Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public class AccessToken {
    private final String identifier;
    private final String token;
    private final String api;

    public AccessToken(final String api, final String identifier, final String token) {
      this.api = api;
      this.identifier = identifier;
      this.token = token;
    }

    public String getApi() {
      return api;
    }

    public String getIdentifier() {
      return identifier;
    }

    public String getToken() {
      return token;
    }

  }

}