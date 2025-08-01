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

import java.io.IOException;
import java.util.List;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.DocumentDataService;

public class Favorite {

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
   * @throws IOException
   * @throws DatafariServerException
   */
  public static void addFavorite(final String username, final String idDocument, final String titleDocument) throws DatafariServerException {
    DocumentDataService.getInstance().addFavorite(username, idDocument, titleDocument);
  }

  /**
   * delete a document from the favorites list of the user
   *
   * @param username
   *          of the user
   * @param idDocument
   *          the id that should be deleted from the favorites
   * @return true if it was success and false if not
   * @throws DatafariServerException
   */
  public static void deleteFavorite(final String username, final String idDocument) throws DatafariServerException {
    DocumentDataService.getInstance().deleteFavorite(username, idDocument);
  }

  /**
   * get all the favorites of a user
   *
   * @param username
   *          of the user
   * @param documentIDs
   *          : list of document id to check (if null, check all)
   * @return an array list of all the favorites document of the user. Return
   *         null if there's an error.
   * @throws DatafariServerException
   */
  public static List<String> getFavorites(final String username, final String[] documentIDs) throws DatafariServerException {
    return DocumentDataService.getInstance().getFavorites(username, documentIDs);

  }

  /**
   * Delete all favorites of a user without deleting also his likes
   *
   * @param username
   * @return CodesReturned.ALLOK if the operation was success and
   *         CodesReturned.PROBLEMCONNECTIONDATABASE if the db isn't running
   */
  public static void removeFavorites(final String username) throws DatafariServerException {
    DocumentDataService.getInstance().removeFavorites(username);

  }

  /**
   * Remove a user from the collection favorites. This will delete his likes and
   * his favorites
   *
   * @param username
   * @return
   */
  public static void removeFavoritesAndLikesDB(final String username) throws DatafariServerException {
    DocumentDataService.getInstance().removeFavoritesAndLikeDB(username);

  }
}
