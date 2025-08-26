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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class DocumentDataService {

  final static Logger logger = LogManager.getLogger(DocumentDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String DOCUMENTIDCOLUMN = "document_id";
  public static final String DOCUMENTTITLECOLUMN = "document_title";
  public static final String LIKESCOLUMN = "like";
  public static final String FAVORITECOLLECTION = "favorite";
  public static final String LIKECOLLECTION = "liked";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  private static DocumentDataService instance;

  private final PostgresService pgService = new PostgresService();

  public static synchronized DocumentDataService getInstance() throws DatafariServerException {
    if (instance == null) {
      instance = new DocumentDataService();
    }
    return instance;
  }

  public DocumentDataService() {
    this.userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }
  /**
   * Add a document to the list of documents liked by the user
   */
  public void addLike(final String username, final String idDocument) throws DatafariServerException {
    try {
      String sql = "INSERT INTO " + LIKECOLLECTION + " (" + USERNAMECOLUMN + ", " + DOCUMENTIDCOLUMN + ", " + LASTREFRESHCOLUMN + ") VALUES (?, ?, now())";
      pgService.executeUpdate(sql, username, idDocument);
    } catch (final Exception e) {
      logger.warn("Unable to add like : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }
  }

  /**
   * Unlike a document
   */
  public void unlike(final String username, final String idDocument) throws DatafariServerException {
    try {
      String sql = "DELETE FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
      pgService.executeUpdate(sql, username, idDocument);
    } catch (final Exception e) {
      logger.warn("Unable to unlike : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }
  }

  /**
   * Get all the likes of a user
   */
  public List<String> getLikes(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      final List<String> likes = new ArrayList<>();
      if (documentIDs == null) {
        String sql = "SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
        try (ResultSet results = pgService.executeSelect(sql, username)) {
          while (results.next()) {
            likes.add(results.getString(DOCUMENTIDCOLUMN));
          }
        }
      } else {
        for (final String documentID : documentIDs) {
          String sql = "SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
          try (ResultSet results = pgService.executeSelect(sql, username, documentID)) {
            while (results.next()) {
              likes.add(results.getString(DOCUMENTIDCOLUMN));
            }
          }
        }
      }
      return likes;
    } catch (final Exception e) {
      logger.warn("Unable to getLikes : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete all likes of a user without deleting also his favorites
   */
  public void removeLikes(final String username) throws DatafariServerException {
    try {
      final List<String> likes = getLikes(username, null);
      for (final String like : likes) {
        String sql = "DELETE FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
        pgService.executeUpdate(sql, username, like);
      }
    } catch (final Exception e) {
      logger.warn("Unable to remove likes for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void refreshLikes(final String username) throws DatafariServerException {
    final List<String> userLikes = getLikes(username, null);
    final String userLikesAsString = userLikes.stream()
        .map(s -> "?")
        .collect(Collectors.joining(",", "(", ")"));
    try {
      // Notion de TTL à gérer côté application : on ne peut pas faire "USING TTL" en SQL.
      if (!userLikes.isEmpty()) {
        String sql = "UPDATE " + LIKECOLLECTION
            + " SET " + LASTREFRESHCOLUMN + " = now()"
            + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " IN " + userLikesAsString;
        List<Object> params = new ArrayList<>();
        params.add(username);
        params.addAll(userLikes);
        pgService.executeUpdate(sql, params.toArray());
      }
    } catch (final Exception e) {
      logger.warn("Unable to refresh likes for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }
  /**
   * Add a document to the favorites list of the user
   */
  public void addFavorite(final String username, final String idDocument, final String titleDocument)
      throws DatafariServerException {
    try {
      String sql = "INSERT INTO " + FAVORITECOLLECTION
          + " (" + USERNAMECOLUMN + ", " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN + ", " + LASTREFRESHCOLUMN + ") VALUES (?, ?, ?, now())";
      pgService.executeUpdate(sql, username, idDocument, titleDocument);
    } catch (final Exception e) {
      logger.warn("Unable add favorite " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete a document from the favorites list of the user
   */
  public void deleteFavorite(final String username, final String idDocument) throws DatafariServerException {
    try {
      String sql = "DELETE FROM " + FAVORITECOLLECTION
          + " WHERE " + DOCUMENTIDCOLUMN + " = ? AND " + USERNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, idDocument, username);
    } catch (final Exception e) {
      logger.warn("Unable delete favorite " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Get all the favorites of a user
   */
  public List<String> getFavorites(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      final List<String> favorites = new ArrayList<>();
      if (documentIDs == null) {
        String sql = "SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN
            + " FROM " + FAVORITECOLLECTION
            + " WHERE " + USERNAMECOLUMN + " = ?";
        try (ResultSet results = pgService.executeSelect(sql, username)) {
          while (results.next()) {
            final HashMap<String, String> fav = new HashMap<>();
            fav.put("id", results.getString(DOCUMENTIDCOLUMN));
            fav.put("title", results.getString(DOCUMENTTITLECOLUMN));
            favorites.add(new JSONObject(fav).toJSONString());
          }
        }
      } else {
        for (final String documentID : documentIDs) {
          String sql = "SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN
              + " FROM " + FAVORITECOLLECTION
              + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
          try (ResultSet results = pgService.executeSelect(sql, username, documentID)) {
            while (results.next()) {
              final HashMap<String, String> fav = new HashMap<>();
              fav.put("id", results.getString(DOCUMENTIDCOLUMN));
              fav.put("title", results.getString(DOCUMENTTITLECOLUMN));
              favorites.add(new JSONObject(fav).toJSONString());
            }
          }
        }
      }
      return favorites;
    } catch (final Exception e) {
      logger.error("Unable getFavorites for " + username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete all favorites of a user without deleting also his likes
   */
  public void removeFavorites(final String username) throws DatafariServerException {
    try {
      final List<String> favorites = getFavorites(username, null);
      final JSONParser parser = new JSONParser();
      for (final String jsonFavorite : favorites) {
        final JSONObject favorite = (JSONObject) parser.parse(jsonFavorite);
        String sql = "DELETE FROM " + FAVORITECOLLECTION
            + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
        pgService.executeUpdate(sql, username, favorite.get("id").toString());
      }
    } catch (final Exception e) {
      logger.error("Unable removeFavorites for " + username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete all favorites and likes of a user
   */
  public void removeFavoritesAndLikeDB(final String username) throws DatafariServerException {
    removeFavorites(username);
    removeLikes(username);
  }

  /**
   * Refresh favorites (set last_refresh for all favorites)
   */
  public void refreshFavorites(final String username) throws DatafariServerException {
    final List<String> userFavorites = getFavorites(username, null);
    final JSONParser parser = new JSONParser();
    final List<String> ids = userFavorites.stream()
        .map(s -> {
          try {
            JSONObject fav = (JSONObject) parser.parse(s);
            return fav.get("id").toString();
          } catch (Exception e) {
            return null;
          }
        })
        .filter(s -> s != null)
        .collect(Collectors.toList());
    if (!ids.isEmpty()) {
      try {
        String inSql = ids.stream().map(s -> "?").collect(Collectors.joining(",", "(", ")"));
        String sql = "UPDATE " + FAVORITECOLLECTION
            + " SET " + LASTREFRESHCOLUMN + " = now()"
            + " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " IN " + inSql;
        List<Object> params = new ArrayList<>();
        params.add(username);
        params.addAll(ids);
        pgService.executeUpdate(sql, params.toArray());
      } catch (final Exception e) {
        logger.warn("Unable to refresh favorites for user " + username + " : " + e.getMessage());
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
    }
  }
}