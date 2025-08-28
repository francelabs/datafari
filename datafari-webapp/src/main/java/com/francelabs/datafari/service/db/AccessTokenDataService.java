package com.francelabs.datafari.service.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

@Service
public class AccessTokenDataService {

  private static final Logger logger = LogManager.getLogger(AccessTokenDataService.class.getName());

  public static final String ACCESS_TOKENS_COLLECTION = "access_tokens";
  public static final String USERNAME_COLUMN   = "username";
  public static final String API_COLUMN        = "api";
  public static final String IDENTIFIER_COLUMN = "identifier";
  public static final String TOKEN_COLUMN      = "a_token";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  // Pont de compatibilité pour l'ancien code qui appelle getInstance()
  private static volatile AccessTokenDataService instance;

  private final SqlService sql;
  private final String userDataTTL;

  public static synchronized AccessTokenDataService getInstance() throws DatafariServerException {
    return instance;
  }

  public AccessTokenDataService(SqlService sql) {
    this.sql = sql;
    this.userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
    instance = this;
  }

  // ========= API =========

  public AccessToken getToken(final String username, final String api) {
    try {
      List<AccessToken> list = sql.getJdbcTemplate().query(
          "SELECT " + TOKEN_COLUMN + "," + IDENTIFIER_COLUMN +
          " FROM " + ACCESS_TOKENS_COLLECTION +
          " WHERE " + USERNAME_COLUMN + " = ? AND " + API_COLUMN + " = ?",
          ps -> {
            ps.setString(1, username);
            ps.setString(2, api);
          },
          (rs, rn) -> new AccessToken(api, rs.getString(IDENTIFIER_COLUMN), rs.getString(TOKEN_COLUMN))
      );
      return list.isEmpty() ? null : list.get(0);
    } catch (Exception e) {
      logger.warn("Unable to get token for user {} for API {}", username, api, e);
      return null;
    }
  }

  public int setToken(final String username, final String api, final String identifier, final String token)
      throws DatafariServerException {
    try {
      // (TTL géré applicativement via last_refresh)
      sql.getJdbcTemplate().update(
          "INSERT INTO " + ACCESS_TOKENS_COLLECTION + " (" +
              USERNAME_COLUMN + "," + API_COLUMN + "," + IDENTIFIER_COLUMN + "," + TOKEN_COLUMN + "," + LASTREFRESHCOLUMN + ") " +
              "VALUES (?, ?, ?, ?, ?) " +
              "ON CONFLICT (" + USERNAME_COLUMN + "," + API_COLUMN + "," + IDENTIFIER_COLUMN + ") " +
              "DO UPDATE SET " + TOKEN_COLUMN + " = EXCLUDED." + TOKEN_COLUMN + ", " +
                                LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN,
          username, api, identifier, token, Timestamp.from(Instant.now())
      );
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to insert Token for user {} for API {}", username, api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public int updateToken(final String username, final String api, final String identifier, final String token)
      throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "UPDATE " + ACCESS_TOKENS_COLLECTION +
          " SET " + TOKEN_COLUMN + " = ?, " + LASTREFRESHCOLUMN + " = ? " +
          "WHERE " + USERNAME_COLUMN + " = ? AND " + API_COLUMN + " = ? AND " + IDENTIFIER_COLUMN + " = ?",
          token, Timestamp.from(Instant.now()), username, api, identifier
      );
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to update Token for user {} for API {}", username, api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public int deleteToken(final String username, final String api, final String identifier)
      throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "DELETE FROM " + ACCESS_TOKENS_COLLECTION +
          " WHERE " + USERNAME_COLUMN + " = ? AND " + API_COLUMN + " = ? AND " + IDENTIFIER_COLUMN + " = ?",
          username, api, identifier
      );
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to delete Token for user {} for API {}", username, api, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public JSONArray getTokens(final String username) throws Exception {
    JSONArray tokens = new JSONArray();
    sql.getJdbcTemplate().query(
        "SELECT " + API_COLUMN + "," + IDENTIFIER_COLUMN + "," + TOKEN_COLUMN +
        " FROM " + ACCESS_TOKENS_COLLECTION +
        " WHERE " + USERNAME_COLUMN + " = ?",
        ps -> ps.setString(1, username),
        rs -> {
          JSONObject token = new JSONObject();
          token.put(API_COLUMN, rs.getString(API_COLUMN));
          token.put(IDENTIFIER_COLUMN, rs.getString(IDENTIFIER_COLUMN));
          token.put(TOKEN_COLUMN, rs.getString(TOKEN_COLUMN));
          tokens.add(token);
        }
    );
    return tokens;
  }

  public int removeTokens(final String username) throws DatafariServerException {
    try {
      // suppression en masse
      sql.getJdbcTemplate().update(
          "DELETE FROM " + ACCESS_TOKENS_COLLECTION + " WHERE " + USERNAME_COLUMN + " = ?",
          username
      );
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void refreshAccessTokens(final String username) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "UPDATE " + ACCESS_TOKENS_COLLECTION +
          " SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
          "WHERE " + USERNAME_COLUMN + " = ?",
          username
      );
    } catch (Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // ========= DTO =========
  public class AccessToken {
    private final String identifier;
    private final String token;
    private final String api;

    public AccessToken(final String api, final String identifier, final String token) {
      this.api = api;
      this.identifier = identifier;
      this.token = token;
    }
    public String getApi() { return api; }
    public String getIdentifier() { return identifier; }
    public String getToken() { return token; }
  }
}