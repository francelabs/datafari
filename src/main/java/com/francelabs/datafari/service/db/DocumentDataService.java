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
import java.util.List;

import org.apache.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.NbLikes;

public class DocumentDataService {

	final static Logger logger = Logger
			.getLogger(DocumentDataService.class.getName());

	public static final String USERNAMECOLUMN = "username";
	public static final String DOCUMENTIDCOLUMN = "document_id";
	public static final String LIKESCOLUMN = "like";

	public static final String FAVORITECOLLECTION = "favorite";
	public static final String LIKECOLLECTION = "like";

	private static DocumentDataService instance;

	private Session session;

	public static synchronized DocumentDataService getInstance()
			throws IOException {
		if (instance == null) {
			instance = new DocumentDataService();
		}
		return instance;
	}
	

	public DocumentDataService() throws IOException {

		// Gets the name of the collection
		session = DBContextListerner.getSession();

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
	public int addLike(String username, String idDocument) throws Exception {
		try {
			String query = "insert into " + LIKECOLLECTION + " ("
					+ USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN + ")"
					+ " values ('" + username + "','" + idDocument + "')";
			session.execute(query);
			// TODO change exception
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return CodesReturned.ALREADYPERFORMED;
		}

		return CodesReturned.ALLOK;
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
	 */
	public int unlike(String username, String idDocument) throws Exception {
		try {
			String query = "DELETE FROM " + LIKECOLLECTION + " WHERE "
					+ USERNAMECOLUMN + " = '" + username + "'" + " AND "
					+ DOCUMENTIDCOLUMN + " = '" + idDocument + "'";
			session.execute(query);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return CodesReturned.ALREADYPERFORMED;
		}
		return CodesReturned.ALLOK;
	}

	/**
	 * get all the likes of a user
	 * 
	 * @param username
	 *            of the user
	 * @return an array list of all the the likes of the user. Return null if
	 *         there's an error.
	 */
	public List<String> getLikes(String username) throws Exception {
		List<String> likes = new ArrayList<String>();
		ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN
				+ " FROM " + LIKECOLLECTION + " where " + USERNAMECOLUMN + "='"
				+ username + "'");
		for (Row row : results) {
			likes.add(row.getString(DOCUMENTIDCOLUMN));
		}
		return likes;
	}


	/**
	 * Delete all likes of a user without deleting also his favorites
	 * 
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONCASSANDRA
	 */
	public int removeLikes(String username) throws Exception {
		String query = "DELETE FROM " + LIKESCOLUMN + " WHERE "
				+ USERNAMECOLUMN + " = " + username;
		session.execute(query);
		return CodesReturned.ALLOK;
	}

	/**
	 * Add a document to the favorites list of the user
	 * 
	 * @param username
	 *            of the user
	 * @param idDocument
	 *            the id that should be add as a favorite
	 * @return true if it was success and false if not
	 */
	public void addFavorite(String username, String idDocument) {
		String query = "insert into " + FAVORITECOLLECTION + " (" + USERNAMECOLUMN
				+ "," + DOCUMENTIDCOLUMN + ")" + " values ('" + username + "','"
				+ idDocument + "')";
		session.execute(query);
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
	public int deleteFavorite(String username, String idDocument) {
		String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE "
				+ DOCUMENTIDCOLUMN + " = '" + idDocument +"' AND "+USERNAMECOLUMN+ " = '"+username + "'";
		session.execute(query);
		return CodesReturned.ALLOK;
	}

	/**
	 * get all the favorites of a user
	 * 
	 * @param username
	 *            of the user
	 * @return an array list of all the favorites document of the user. Return
	 *         null if there's an error.
	 */
	public List<String> getFavorites(String username) {
		List<String> favorites = new ArrayList<String>();
		ResultSet results = session.execute("SELECT " + DOCUMENTIDCOLUMN
				+ " FROM " + FAVORITECOLLECTION + " where " + USERNAMECOLUMN
				+ "='" + username + "'");
		for (Row row : results) {
			favorites.add(row.getString(DOCUMENTIDCOLUMN));
		}
		return favorites;
	}

	/**
	 * Delete all favorites of a user without deleting also his likes
	 * 
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONMONGODB if the mongoDB isn't
	 *         running
	 */
	public  int removeFavorites(String username) {
		String query = "DELETE FROM " + FAVORITECOLLECTION + " WHERE "
				+ USERNAMECOLUMN + " = " + username;
		session.execute(query);
		return CodesReturned.ALLOK;
	}

	public void removeFavoritesAndLikeDB(String username) throws Exception {
		removeFavorites(username);
		removeLikes(username);
	}



}
