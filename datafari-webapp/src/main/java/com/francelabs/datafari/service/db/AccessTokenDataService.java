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

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class AccessTokenDataService {

  private static final Logger logger = LogManager.getLogger(AccessTokenDataService.class.getName());

  public static final String ACCESS_TOKENS_COLLECTION = "access_tokens";
  public static final String USERNAME_COLUMN = "username";
  public static final String API_COLUMN = "api";
  public static final String IDENTIFIER_COLUMN = "identifier";
  public static final String TOKEN_COLUMN = "a_token";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  private static AccessTokenDataService instance;
  private final PostgresService pgService = new PostgresService();
  private final String userDataTTL;

  private AccessTokenDataService() {
    // Valeur TTL uniquement à titre indicatif/documentaire.
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  public static synchronized AccessTokenDataService getInstance() throws DatafariServerException {
    if (instance == null) {
      instance = new AccessTokenDataService();
    }
    return instance;
  }

  public AccessToken getToken(final String username, final String api) {
    AccessToken token = null;
    try {
      final String query = "SELECT " + TOKEN_COLUMN + "," + IDENTIFIER_COLUMN + " FROM " + ACCESS_TOKENS_COLLECTION
          + " WHERE " + USERNAME_COLUMN + "=? AND " + API_COLUMN + "=?";
      try (ResultSet rs = pgService.executeSelect(query, username, api)) {
        if (rs.next()) {
          token = new AccessToken(api, rs.getString(IDENTIFIER_COLUMN), rs.getString(TOKEN_COLUMN));
        }
      }
    } catch (final Exception e) {
      logger.warn("Unable to get token for user " + username + " for API " + api, e);
    }
    return token;
  }

  public int setToken(final String username, final String api, final String identifier, final String token) throws DatafariServerException {
    try {
      String ttlToUse = userDataTTL;
      if ("admin".equals(username)) {
        ttlToUse = "0";
      }
      final String sql = "INSERT INTO " + ACCESS_TOKENS_COLLECTION + " (" + USERNAME_COLUMN + "," + API_COLUMN + ","
          + IDENTIFIER_COLUMN + "," + TOKEN_COLUMN + "," + LASTREFRESHCOLUMN + ") VALUES (?, ?, ?, ?, ?) "
          + "ON CONFLICT (" + USERNAME_COLUMN + "," + API_COLUMN + "," + IDENTIFIER_COLUMN + ") "
          + "DO UPDATE SET " + TOKEN_COLUMN + "=EXCLUDED." + TOKEN_COLUMN + "," + LASTREFRESHCOLUMN + "=EXCLUDED." + LASTREFRESHCOLUMN;
      pgService.executeUpdate(sql, username, api, identifier, token, Timestamp.from(Instant.now()));
    } catch (final Exception e) {
      logger.warn("Unable to insert Token for user " + username + " for API " + api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public int updateToken(final String username, final String api, final String identifier, final String token) throws DatafariServerException {
    try {
      final String sql = "UPDATE " + ACCESS_TOKENS_COLLECTION + " SET " + TOKEN_COLUMN + "=?," + LASTREFRESHCOLUMN + "=? "
          + "WHERE " + USERNAME_COLUMN + "=? AND " + API_COLUMN + "=? AND " + IDENTIFIER_COLUMN + "=?";
      pgService.executeUpdate(sql, token, Timestamp.from(Instant.now()), username, api, identifier);
    } catch (final Exception e) {
      logger.warn("Unable to update Token for user " + username + " for API " + api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public int deleteToken(final String username, final String api, final String identifier) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + ACCESS_TOKENS_COLLECTION + " WHERE " + USERNAME_COLUMN + "=? AND "
          + API_COLUMN + "=? AND " + IDENTIFIER_COLUMN + "=?";
      pgService.executeUpdate(sql, username, api, identifier);
    } catch (final Exception e) {
      logger.warn("Unable to delete Token for user " + username + " for API " + api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public JSONArray getTokens(final String username) throws Exception {
    final JSONArray tokens = new JSONArray();
    final String sql = "SELECT " + API_COLUMN + "," + IDENTIFIER_COLUMN + "," + TOKEN_COLUMN + " FROM " + ACCESS_TOKENS_COLLECTION
        + " WHERE " + USERNAME_COLUMN + "=?";
    try (ResultSet rs = pgService.executeSelect(sql, username)) {
      while (rs.next()) {
        final JSONObject token = new JSONObject();
        token.put(API_COLUMN, rs.getString(API_COLUMN));
        token.put(IDENTIFIER_COLUMN, rs.getString(IDENTIFIER_COLUMN));
        token.put(TOKEN_COLUMN, rs.getString(TOKEN_COLUMN));
        tokens.add(token);
      }
    }
    return tokens;
  }

  public int removeTokens(final String username) throws DatafariServerException {
    try {
      final JSONArray tokens = getTokens(username);
      for (int i = 0; i < tokens.size(); i++) {
        final JSONObject token = (JSONObject) tokens.get(i);
        final String api = token.get(API_COLUMN).toString();
        final String identifier = token.get(IDENTIFIER_COLUMN).toString();
        final String sql = "DELETE FROM " + ACCESS_TOKENS_COLLECTION + " WHERE " + USERNAME_COLUMN + "=? AND " + API_COLUMN + "=? AND "
            + IDENTIFIER_COLUMN + "=?";
        pgService.executeUpdate(sql, username, api, identifier);
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