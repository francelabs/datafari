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

import java.sql.Timestamp;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class LangDataService {

  private static final Logger logger = LogManager.getLogger(LangDataService.class);

  public static final String USERNAMECOLUMN = "username";
  public static final String LANGCOLLECTION = "lang";
  public static final String LANGCOLUMN = "lang";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  private static LangDataService instance;

  private final JdbcTemplate jdbc;

  private LangDataService() {
    // Récupération du JdbcTemplate via SqlService
    this.jdbc = SqlService.get().getJdbcTemplate();
  }

  public static synchronized LangDataService getInstance() {
    if (instance == null) {
      instance = new LangDataService();
    }
    return instance;
  }

  /**
   * Get user preferred language
   */
  public String getLang(final String username) {
    try {
      String sql = "SELECT " + LANGCOLUMN + " FROM " + LANGCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      return jdbc.query(sql, rs -> {
        if (rs.next()) {
          return rs.getString(LANGCOLUMN);
        }
        return null;
      }, username);
    } catch (final Exception e) {
      logger.warn("Unable to get lang for user {} : {}", username, e.getMessage());
      return null;
    }
  }

  /**
   * Insert or update user language
   */
  public int setLang(final String username, final String lang) throws DatafariServerException {
    try {
      String sql = "INSERT INTO " + LANGCOLLECTION + " (" + USERNAMECOLUMN + "," + LANGCOLUMN + "," + LASTREFRESHCOLUMN + ") "
          + "VALUES (?, ?, ?) "
          + "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET "
          + LANGCOLUMN + " = EXCLUDED." + LANGCOLUMN + ", "
          + LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
      jdbc.update(sql, username, lang, new Timestamp(System.currentTimeMillis()));
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.warn("Unable to insert lang for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Update user language
   */
  public int updateLang(final String username, final String lang) throws DatafariServerException {
    try {
      String sql = "UPDATE " + LANGCOLLECTION
          + " SET " + LANGCOLUMN + " = ?, " + LASTREFRESHCOLUMN + " = ? "
          + "WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, lang, new Timestamp(System.currentTimeMillis()), username);
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.warn("Unable to update lang for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Refresh user language by updating last_refresh
   */
  public void refreshLang(final String username) throws DatafariServerException {
    String userLang = getLang(username);
    if (userLang != null) {
      updateLang(username, userLang);
    }
  }

  /**
   * Delete user language
   */
  public int deleteLang(final String username) throws DatafariServerException {
    try {
      String sql = "DELETE FROM " + LANGCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
      return CodesReturned.ALLOK.getValue();
    } catch (final Exception e) {
      logger.warn("Unable to delete lang for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }
}