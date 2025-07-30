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
package com.francelabs.datafari.user;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.db.SavedSearchDataServicePostgres;

public class SavedSearch {

	private final static Logger logger = LogManager.getLogger(SavedSearch.class);

	/**
	 * Add a search to the list of searches saved by the user
	 *
	 * @param username
	 *            of the user
	 * @param requestName
	 *            the request name given by the user
	 * @param request
	 *            the search request
	 * @return Search.ALREADYPERFORMED if the search was already saved,
	 *         CodesUser.ALLOK if all was ok and
	 *         CodesUser.PROBLEMCONNECTIONDATABASE if there's an error
	 */
	public static int saveSearch(final String username, final String requestName, final String request) {
		try {
			return SavedSearchDataServicePostgres.getInstance().saveSearch(username, requestName, request);
		} catch (final Exception e) {
			logger.error(e);
			e.printStackTrace();
			return CodesReturned.PROBLEMCONNECTIONDATABASE.getValue();
		}
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
	 *         Search.ALLOK if all was ok and
	 *         Search.CodesReturned.PROBLEMCONNECTIONDATABASE if there's an
	 *         error
	 */
	public static int deleteSearch(final String username, final String requestName, final String request) {
		try {
			return SavedSearchDataServicePostgres.getInstance().deleteSearch(username, requestName, request);
		} catch (final Exception e) {
			logger.error(e);
			e.printStackTrace();
			return CodesReturned.PROBLEMCONNECTIONDATABASE.getValue();
		}
	}

	/**
	 * get all the searches of a user
	 *
	 * @param username
	 *            of the user
	 * @return an array list of all the saved searches of the user. Return null
	 *         if there's an error.
	 */
	public static Map<String, String> getSearches(final String username) {
		try {
			return SavedSearchDataServicePostgres.getInstance().getSearches(username);
		} catch (final Exception e) {
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Delete all saved searches of a user
	 *
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and
	 *         CodesReturned.PROBLEMCONNECTIONDATABASE if the database isn't
	 *         running
	 */
	public static int removeSearches(final String username) {
		try {
			return SavedSearchDataServicePostgres.getInstance().removeSearches(username);
		} catch (final Exception e) {
			logger.error(e);
			return CodesReturned.PROBLEMCONNECTIONDATABASE.getValue();
		}
	}

}
