/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class DocumentDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(DocumentDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String DOCUMENTIDCOLUMN = "document_id";
  public static final String DOCUMENTTITLECOLUMN = "document_title";
  public static final String LIKESCOLUMN = "like";

  public static final String FAVORITECOLLECTION = "favorite";
  public static final String LIKECOLLECTION = "like";

  private static DocumentDataService instance;

  public static synchronized DocumentDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new DocumentDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Add a document to the list of documents liked by the user
   *
   * @param username
   *          of the user
   * @param idDocument
   *          the id that should be liked
   * @return Like.ALREADYPERFORMED if the like was already done, CodesUser.ALLOK if all was ok
   */
  public void addLike(final String username, final String idDocument) throws DatafariServerException {
    try {
      final String query = "insert into " + LIKECOLLECTION + " (" + USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN + ")" + " values ('" + username + "',$$" + idDocument + "$$)";
      session.execute(query);
    } catch (final DriverException e) {
      logger.warn("Unable to add like : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }

  }

  /**
   * unlike a document
   *
   * @param username
   *          of the user who unlike a document
   * @param idDocument
   *          the id that should be unliked
   * @return Like.ALREADYPERFORMED if the like was already done, Like.ALLOK if all was ok and Like.CodesReturned.CASSANDRAN if there's an error
   * @throws DatafariServerException
   */
  public void unlike(final String username, final String idDocument) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'" + " AND " + DOCUMENTIDCOLUMN + " = $$" + idDocument + "$$";
      session.execute(query);
    } catch (final DriverException e) {
      logger.warn("Unable to unlike : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }
  }

  /**
   * get all the likes of a user
   *
   * @param username
   *          of the user
   * @param documentIDs
   * @return an array list of all the the likes of the user. Return null if there's an error.
   */
  public List<String> getLikes(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      final List<String> likes = new ArrayList<>();
      if (documentIDs == null) {
        final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
        for (final Row row : results) {
          likes.add(row.getString(DOCUMENTIDCOLUMN));
        }

      } else {
        for (final String documentID : documentIDs) {
          final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "' AND " + DOCUMENTIDCOLUMN + "=$$" + documentID + "$$");
          for (final Row row : results) {
            likes.add(row.getString(DOCUMENTIDCOLUMN));
          }
        }

      }
      return likes;
    } catch (final DriverException e) {
      logger.warn("Unable to getLikes : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete all likes of a user without deleting also his favorites
   *
   * @param username
   * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONDATABASE
   */
  public void removeLikes(final String username) throws DatafariServerException {
    try {
      final List<String> likes = getFavorites(username, null);
      for (final String like : likes) {
        final String query = "DELETE FROM " + LIKESCOLUMN + " WHERE " + USERNAMECOLUMN + " = '" + username + "' AND " + DOCUMENTIDCOLUMN + "=$$" + like + "$$ IF EXISTS";
        session.execute(query);
      }
    } catch (final DriverException e) {
      logger.warn("Unable to remove likes for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Add a document to the favorites list of the user
   *
   * @param username
   *          of the user
   * @param idDocument
   *          the id that should be add as a favorite
   * @param titleDocument
   *          the title associated to the id
   * @return true if it was success and false if not
   * @throws DatafariServerException
   */
  public void addFavorite(final String username, final String idDocument, final String titleDocument) throws DatafariServerException {
    try {

      final String query = "insert into " + FAVORITECOLLECTION + " (" + USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN + "," + DOCUMENTTITLECOLUMN + ")" + " values ('" + username + "',$$" + idDocument + "$$, '" + titleDocument + "')";
      session.execute(query);
    } catch (final DriverException e) {
      logger.warn("Unable add favorite " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * delete a document from the favorites list of the user
   *
   * @param username
   *          of the user
   * @param idDocument
   *          the id that should be deleted from the favorites
   * @return true if it was success and false if not
   */
  public void deleteFavorite(final String username, final String idDocument) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE " + DOCUMENTIDCOLUMN + " = $$" + idDocument + "$$ AND " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(query);
    } catch (final DriverException e) {
      logger.warn("Unable delete favorite " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * get all the favorites of a user
   *
   * @param username
   *          of the user
   * @param documentIDs
   *          : list of document id to check (if null, check all)
   * @return an array list of all the favorites document of the user. Return null if there's an error.
   * @throws DatafariServerException
   */
  public List<String> getFavorites(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      final List<String> favorites = new ArrayList<>();
      if (documentIDs == null) {
        final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN + " FROM " + FAVORITECOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
        for (final Row row : results) {
          final JSONObject fav = new JSONObject();
          fav.put("id", row.getString(DOCUMENTIDCOLUMN));
          fav.put("title", row.getString(DOCUMENTTITLECOLUMN));
          favorites.add(fav.toJSONString());
        }
      } else {
        for (final String documentID : documentIDs) {
          final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN + " FROM " + FAVORITECOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "' AND " + DOCUMENTIDCOLUMN + "=$$" + documentID + "$$");
          for (final Row row : results) {
            final JSONObject fav = new JSONObject();
            fav.put("id", row.getString(DOCUMENTIDCOLUMN));
            fav.put("title", row.getString(DOCUMENTTITLECOLUMN));
            favorites.add(fav.toJSONString());
          }
        }

      }
      return favorites;
    } catch (final DriverException e) {
      logger.error("Unable getFavorites for " + username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete all favorites of a user without deleting also his likes
   *
   * @param username
   * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONMONGODB if the mongoDB isn't running
   * @throws DatafariServerException
   */
  public void removeFavorites(final String username) throws DatafariServerException {
    try {
      final List<String> favorites = getFavorites(username, null);
      for (final String jsonFavorite : favorites) {
        final JSONParser parser = new JSONParser();
        final JSONObject favorite = (JSONObject) parser.parse(jsonFavorite);
        final String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "' AND " + DOCUMENTIDCOLUMN + "=$$" + favorite.get("id") + "$$ IF EXISTS";
        session.execute(query);
      }
    } catch (final DriverException | ParseException e) {
      logger.error("Unable removeFavorites for " + username, e);
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void removeFavoritesAndLikeDB(final String username) throws DatafariServerException {
    removeFavorites(username);
    removeLikes(username);
  }

}