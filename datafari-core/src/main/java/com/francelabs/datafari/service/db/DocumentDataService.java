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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class DocumentDataService {

	final static Logger logger = Logger.getLogger(DocumentDataService.class.getName());

	public static final String USERNAMECOLUMN = "username";
	public static final String DOCUMENTIDCOLUMN = "document_id";
	public static final String LIKESCOLUMN = "like";

	public static final String FAVORITECOLLECTION = "favorite";
	public static final String LIKECOLLECTION = "like";

	public static final String SEARCHCOLLECTION = "search";
	public static final String REQUESTCOLUMN = "request";
	public static final String REQUESTNAMECOLUMN = "name";

	public static final String LANGCOLLECTION = "lang";
	public static final String LANGCOLUMN = "lang";

	private static final String RANKINGCOLLECTION = "ranking";
	private static final String RANKINGCOLUMN = "ranking";

	private static DocumentDataService instance;

	private final Session session;

	public static synchronized DocumentDataService getInstance() throws DatafariServerException {
		try {
			if (instance == null) {
				instance = new DocumentDataService();
			}
			return instance;
		} catch (DriverException | IOException e) {
			logger.warn("Unable to get instance : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	public DocumentDataService() throws IOException {
		// Gets the name of the collection
		session = DBContextListerner.getSession();

	}

	/**
	 * Get user preferred lang
	 *
	 * @param username
	 * @return the user preferred lang
	 */
	public String getLang(final String username) {
		String lang = null;
		try {
			final String query = "SELECT " + LANGCOLUMN + " FROM " + LANGCOLLECTION + " where " + USERNAMECOLUMN + "='"
					+ username + "'";
			final ResultSet result = session.execute(query);
			final Row row = result.one();
			if (row != null && !row.isNull(LANGCOLUMN) && !row.getString(LANGCOLUMN).isEmpty()) {
				lang = row.getString(LANGCOLUMN);
			}
		} catch (final Exception e) {
			logger.warn("Unable to get lang for user " + username + " : " + e.getMessage());
		}
		return lang;
	}

	/**
	 * Set user lang
	 *
	 * @param username
	 * @param lang
	 * @return CodesReturned.ALLOK if all was ok
	 * @throws DatafariServerException
	 */
	public int setLang(final String username, final String lang) throws DatafariServerException {
		try {
			final String query = "INSERT INTO " + LANGCOLUMN + " (" + USERNAMECOLUMN + "," + LANGCOLUMN + ")"
					+ " values ('" + username + "','" + lang + "')";
			session.execute(query);
		} catch (final Exception e) {
			logger.warn("Unable to insert lang for user " + username + " : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * Update user lang
	 *
	 * @param username
	 * @param lang
	 * @return CodesReturned.ALLOK if all was ok
	 * @throws DatafariServerException
	 */
	public int updateLang(final String username, final String lang) throws DatafariServerException {
		try {
			final String query = "UPDATE " + LANGCOLUMN + " SET " + LANGCOLUMN + " = '" + lang + "' WHERE "
					+ USERNAMECOLUMN + " = '" + username + "'";
			session.execute(query);
		} catch (final Exception e) {
			logger.warn("Unable to update lang for user " + username + " : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * Add a search to the list of searches saved by the user
	 *
	 * @param username
	 *            of the user
	 * @param requestName
	 *            the request name
	 * @param request
	 *            the search request
	 * @return Search.ALREADYPERFORMED if the search was already saved,
	 *         CodesUser.ALLOK if all was ok
	 */
	public int saveSearch(final String username, final String requestName, final String request) throws Exception {
		try {
			final String query = "insert into " + SEARCHCOLLECTION + " (" + USERNAMECOLUMN + "," + REQUESTNAMECOLUMN
					+ "," + REQUESTCOLUMN + ")" + " values ('" + username + "','" + requestName + "','" + request
					+ "')";
			session.execute(query);
			// TODO change exception
		} catch (final Exception e) {
			logger.warn(e.getMessage());
			return CodesReturned.ALREADYPERFORMED.getValue();
		}

		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * delete a search
	 *
	 * @param username
	 *            of the user
	 * @param requestName
	 *            the request name
	 * @param request
	 *            the search request
	 * @return Search.ALREADYPERFORMED if the search was already deleted,
	 *         Search.ALLOK if all was ok and Search.CodesReturned.CASSANDRAN if
	 *         there's an error
	 */
	public int deleteSearch(final String username, final String requestName, final String request) throws Exception {
		try {
			final String query = "DELETE FROM " + SEARCHCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username
					+ "'" + " AND " + REQUESTCOLUMN + " = '" + request + "'" + " AND " + REQUESTNAMECOLUMN + " = '"
					+ requestName + "'";
			session.execute(query);
		} catch (final Exception e) {
			logger.warn(e.getMessage());
			return CodesReturned.ALREADYPERFORMED.getValue();
		}
		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * get all the saved searches of a user
	 *
	 * @param username
	 *            of the user
	 * @return an array list of all the the saved searches of the user. Return
	 *         null if there's an error.
	 */
	public Map<String, String> getSearches(final String username) throws Exception {
		final Map<String, String> searches = new HashMap<>();
		final ResultSet results = session.execute("SELECT " + REQUESTNAMECOLUMN + ", " + REQUESTCOLUMN + " FROM "
				+ SEARCHCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
		for (final Row row : results) {
			searches.put(row.getString(REQUESTNAMECOLUMN), row.getString(REQUESTCOLUMN));
		}
		return searches;
	}

	/**
	 * Delete all saved searches of a user
	 *
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONCASSANDRA
	 */
	public int removeSearches(final String username) throws Exception {
		final String query = "DELETE FROM " + SEARCHCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'";
		session.execute(query);
		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * Add a document to the list of documents liked by the user
	 *
	 * @param username
	 *            of the user
	 * @param idDocument
	 *            the id that should be liked
	 * @return Like.ALREADYPERFORMED if the like was already done,
	 *         CodesUser.ALLOK if all was ok
	 */
	public void addLike(final String username, final String idDocument) throws DatafariServerException {
		try {
			final String query = "insert into " + LIKECOLLECTION + " (" + USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN + ")"
					+ " values ('" + username + "','" + idDocument + "')";
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
	 *            of the user who unlike a document
	 * @param idDocument
	 *            the id that should be unliked
	 * @return Like.ALREADYPERFORMED if the like was already done, Like.ALLOK if
	 *         all was ok and Like.CodesReturned.CASSANDRAN if there's an error
	 * @throws DatafariServerException
	 */
	public void unlike(final String username, final String idDocument) throws DatafariServerException {
		try {
			final String query = "DELETE FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'"
					+ " AND " + DOCUMENTIDCOLUMN + " = '" + idDocument + "'";
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
	 *            of the user
	 * @param documentIDs
	 * @return an array list of all the the likes of the user. Return null if
	 *         there's an error.
	 */
	public List<String> getLikes(final String username, final String[] documentIDs) throws DatafariServerException {
		try {
			final List<String> likes = new ArrayList<>();
			if (documentIDs == null) {
				final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION
						+ " where " + USERNAMECOLUMN + "='" + username + "'");
				for (final Row row : results) {
					likes.add(row.getString(DOCUMENTIDCOLUMN));
				}

			} else {
				for (final String documentID : documentIDs) {
					final ResultSet results = session.execute(
							"SELECT " + DOCUMENTIDCOLUMN + " FROM " + LIKECOLLECTION + " where " + USERNAMECOLUMN + "='"
									+ username + "' AND " + DOCUMENTIDCOLUMN + "='" + documentID + "'");
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
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONDATABASE
	 */
	public void removeLikes(final String username) throws DatafariServerException {
		try {

			final String query = "DELETE FROM " + LIKESCOLUMN + " WHERE " + USERNAMECOLUMN + " = '" + username + "'";
			session.execute(query);
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
	 *            of the user
	 * @param idDocument
	 *            the id that should be add as a favorite
	 * @return true if it was success and false if not
	 * @throws DatafariServerException
	 */
	public void addFavorite(final String username, final String idDocument) throws DatafariServerException {
		try {

			final String query = "insert into " + FAVORITECOLLECTION + " (" + USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN
					+ ")" + " values ('" + username + "','" + idDocument + "')";
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
	 *            of the user
	 * @param idDocument
	 *            the id that should be deleted from the favorites
	 * @return true if it was success and false if not
	 */
	public void deleteFavorite(final String username, final String idDocument) throws DatafariServerException {
		try {
			final String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE " + DOCUMENTIDCOLUMN + " = '"
					+ idDocument + "' AND " + USERNAMECOLUMN + " = '" + username + "'";
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
	 *            of the user
	 * @param documentIDs
	 *            : list of document id to check (if null, check all)
	 * @return an array list of all the favorites document of the user. Return
	 *         null if there's an error.
	 * @throws DatafariServerException
	 */
	public List<String> getFavorites(final String username, final String[] documentIDs) throws DatafariServerException {
		try {
			final List<String> favorites = new ArrayList<>();
			if (documentIDs == null) {
				final ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN + " FROM " + FAVORITECOLLECTION
						+ " where " + USERNAMECOLUMN + "='" + username + "'");
				for (final Row row : results) {
					favorites.add(row.getString(DOCUMENTIDCOLUMN));
				}
			} else {
				for (final String documentID : documentIDs) {
					final ResultSet results = session.execute(
							"SELECT " + DOCUMENTIDCOLUMN + " FROM " + FAVORITECOLLECTION + " where " + USERNAMECOLUMN
									+ "='" + username + "' AND " + DOCUMENTIDCOLUMN + "='" + documentID + "'");
					for (final Row row : results) {
						favorites.add(row.getString(DOCUMENTIDCOLUMN));
					}
				}

			}
			return favorites;
		} catch (final DriverException e) {
			logger.warn("Unable getFavorites for " + username + " : " + e.getMessage());
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	/**
	 * Delete all favorites of a user without deleting also his likes
	 *
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONMONGODB if the mongoDB isn't
	 *         running
	 * @throws DatafariServerException
	 */
	public void removeFavorites(final String username) throws DatafariServerException {
		try {
			final String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username
					+ "'";
			session.execute(query);
		} catch (final DriverException e) {
			logger.warn("Unable removeFavorites for " + username + " : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	public void removeFavoritesAndLikeDB(final String username) throws DatafariServerException {
		removeFavorites(username);
		removeLikes(username);
	}

	/**
	 * 
	 * Add a rank for a couple query/document
	 * 
	 * 
	 * @param query
	 * @param document
	 * @param rank
	 * @throws DatafariServerException
	 */
	// TODO convert all queries in this class with prepared statement

	public void addRank(String querySolr, String document, int rank) throws DatafariServerException {
		try {
			final String query = "insert into " + RANKINGCOLLECTION + " (" + REQUESTCOLUMN + "," + DOCUMENTIDCOLUMN
					+ "," + RANKINGCOLUMN + ")" + " values ('" + querySolr + "','" + document + "'," + rank + ")";
			session.execute(query);
		} catch (final DriverException e) {
			logger.warn(
					"Cannot add rank for query  " + querySolr + " and document " + document + " : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	/**
	 * 
	 * Delete a rank for a couple query/document
	 * 
	 * @param querySolr
	 * @param document
	 * @throws DatafariServerException
	 */
	// TODO convert all queries in this class with prepared statement

	public void deleteRank(String querySolr, String document) throws DatafariServerException {

		try {
			final String query = "DELETE FROM " + RANKINGCOLLECTION + " WHERE " + REQUESTCOLUMN + " = '" + querySolr
					+ "' AND " + DOCUMENTIDCOLUMN + " = '" + document + "'";
			session.execute(query);
		} catch (final DriverException e) {
			logger.warn("Delete rank for query  " + querySolr + " and document " + document + " : " + e.getMessage(),
					e);
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	/**
	 * 
	 * get the ranking for a specific query and a list of doc ids
	 * 
	 * @param querySolr
	 * @param docIDs
	 * @return
	 * @throws DatafariServerException
	 */
	// TODO convert all queries in this class with prepared statement
	public Map<String, Integer> getRank(String querySolr, String[] docIDs) throws DatafariServerException {
		try {
			Map<String, Integer> documentRanking = new HashMap<String, Integer>();
			if (docIDs.length == 0) {
				return documentRanking;
			}
			PreparedStatement statement = session.prepare("select * from " + RANKINGCOLLECTION + " WHERE "
					+ REQUESTCOLUMN + " = '" + querySolr + "' AND " + DOCUMENTIDCOLUMN + " = ?");
			List<ResultSetFuture> futures = new ArrayList<>();
			for (String docID : docIDs) {
				ResultSetFuture resultSetFuture = session.executeAsync(statement.bind(docID));
				futures.add(resultSetFuture);
			}
			for (ResultSetFuture future : futures) {
				ResultSet rows = future.getUninterruptibly();
				Row row = rows.one();
				if (row != null){
					documentRanking.put(row.getString(DOCUMENTIDCOLUMN), row.getInt(RANKINGCOLUMN));
				}
			}
			return documentRanking;
		} catch (final DriverException e) {
			logger.warn("Cannot get rank for queries  " + querySolr + " and documenst " + docIDs + e.getMessage(), e);
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

}