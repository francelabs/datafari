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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class LangDataService {

  final static Logger logger = LogManager.getLogger(LangDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String LANGCOLLECTION = "lang";
  public static final String LANGCOLUMN = "lang";

  private static LangDataService instance;

  private final Session session;

  public static synchronized LangDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new LangDataService();
      }
      return instance;
    } catch (DriverException | IOException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public LangDataService() throws IOException {
    // Gets the name of the collection
    session = CassandraManager.getInstance().getSession();

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
      final String query = "SELECT " + LANGCOLUMN + " FROM " + LANGCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'";
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
      final String query = "INSERT INTO " + LANGCOLUMN + " (" + USERNAMECOLUMN + "," + LANGCOLUMN + ")" + " values ('" + username + "','" + lang
          + "')";
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
      final String query = "UPDATE " + LANGCOLUMN + " SET " + LANGCOLUMN + " = '" + lang + "' WHERE " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to update lang for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

}