/*******************************************************************************
 *  Copyright 2020 France Labs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

@Service
public class AlertDataService {

  public static final String ALERTCOLLECTION   = "alerts";             // DB table name
  public static final String ID_COLUMN         = "id";
  public static final String KEYWORD_COLUMN    = "keyword";
  public static final String CORE_COLUMN       = "core";
  public static final String FILTERS_COLUMN    = "filters";
  public static final String FREQUENCY_COLUMN  = "frequency";
  public static final String MAIL_COLUMN       = "mail";
  public static final String SUBJECT_COLUMN    = "subject";
  public static final String USER_COLUMN       = "username";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  private static final Logger logger = LogManager.getLogger(AlertDataService.class);

  private static volatile AlertDataService instance; // legacy bridge for existing static calls

  private final SqlService sql;

  // RowMapper that converts a row into Properties (kept for backward compatibility with existing API)
  private static final RowMapper<Properties> ALERT_MAPPER = (rs, rowNum) -> rowToProps(rs);

  // --- Legacy bridge: keep getInstance() so older code keeps working --------------------
  public static synchronized AlertDataService getInstance() {
    return instance;
  }

  public AlertDataService(final SqlService sql) {
    this.sql = sql;
    instance = this; // publish legacy singleton reference when Spring creates the bean
  }

  // -------------------------------------------------------------------------------------
  // Create
  // -------------------------------------------------------------------------------------
  public String addAlert(final Properties alertProp) throws DatafariServerException {
    try {
      final UUID uuid = UUID.randomUUID();

      // Default keyword for safety
      final String keyword = valueOrDefault(alertProp.getProperty(KEYWORD_COLUMN), "*:*");

      sql.getJdbcTemplate().update(
          "INSERT INTO public." + ALERTCOLLECTION + " (" +
              ID_COLUMN + ", " + KEYWORD_COLUMN + ", " + FILTERS_COLUMN + ", " + CORE_COLUMN + ", " +
              FREQUENCY_COLUMN + ", " + MAIL_COLUMN + ", " + SUBJECT_COLUMN + ", " +
              USER_COLUMN + ", " + LASTREFRESHCOLUMN + ") " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          uuid,
          keyword,
          alertProp.getProperty(FILTERS_COLUMN),
          alertProp.getProperty(CORE_COLUMN),
          alertProp.getProperty(FREQUENCY_COLUMN),
          alertProp.getProperty(MAIL_COLUMN),
          alertProp.getProperty(SUBJECT_COLUMN),
          alertProp.getProperty(USER_COLUMN),
          Timestamp.from(Instant.now())
      );

      return uuid.toString();
    } catch (Exception e) {
      logger.error("Unable to add alert", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------------------
  public List<Properties> getAlerts() throws DatafariServerException {
    try {
      return sql.getJdbcTemplate().query(
          "SELECT * FROM public." + ALERTCOLLECTION,
          ALERT_MAPPER
      );
    } catch (Exception e) {
      logger.error("Unable to list alerts", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getUserAlerts(final String username) throws DatafariServerException {
    final String uParam = username == null ? "" : username;
    try {
      // Case/space-insensitive match to avoid UI/DB subtle mismatches
      return sql.getJdbcTemplate().query(
          "SELECT * FROM public." + ALERTCOLLECTION + " " +
          "WHERE btrim(lower(" + USER_COLUMN + ")) = btrim(lower(?))",
          ps -> ps.setString(1, uParam),
          ALERT_MAPPER
      );
    } catch (Exception e) {
      logger.error("Unable to list alerts for user {}", username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Update
  // -------------------------------------------------------------------------------------
  public void updateAlert(final Properties alertProp) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "UPDATE public." + ALERTCOLLECTION + " SET " +
              KEYWORD_COLUMN + " = ?, " +
              FILTERS_COLUMN + " = ?, " +
              CORE_COLUMN + " = ?, " +
              FREQUENCY_COLUMN + " = ?, " +
              MAIL_COLUMN + " = ?, " +
              SUBJECT_COLUMN + " = ?, " +
              USER_COLUMN + " = ?, " +
              LASTREFRESHCOLUMN + " = ? " +
              "WHERE " + ID_COLUMN + " = ?",
          alertProp.getProperty(KEYWORD_COLUMN),
          alertProp.getProperty(FILTERS_COLUMN),
          alertProp.getProperty(CORE_COLUMN),
          alertProp.getProperty(FREQUENCY_COLUMN),
          alertProp.getProperty(MAIL_COLUMN),
          alertProp.getProperty(SUBJECT_COLUMN),
          alertProp.getProperty(USER_COLUMN),
          Timestamp.from(Instant.now()),
          UUID.fromString(alertProp.getProperty("_id"))
      );
    } catch (Exception e) {
      logger.error("Unable to update alert", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void refreshUserAlerts(final String username) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "UPDATE public." + ALERTCOLLECTION + " " +
          "SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
          "WHERE " + USER_COLUMN + " = ?",
          username
      );
    } catch (Exception e) {
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Delete
  // -------------------------------------------------------------------------------------
  public void deleteAlert(final String id) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "DELETE FROM public." + ALERTCOLLECTION + " WHERE " + ID_COLUMN + " = ?",
          UUID.fromString(id)
      );
    } catch (Exception e) {
      logger.error("Unable to delete alert {}", id, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void deleteUserAlerts(final String username) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "DELETE FROM public." + ALERTCOLLECTION + " WHERE " + USER_COLUMN + " = ?",
          username
      );
    } catch (Exception e) {
      logger.error("Unable to delete alerts for user {}", username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------
  private static Properties rowToProps(final ResultSet rs) throws SQLException {
    final Properties p = new Properties();
    p.put("_id", rs.getObject(ID_COLUMN).toString());
    p.put(KEYWORD_COLUMN, rs.getString(KEYWORD_COLUMN));
    p.put(FILTERS_COLUMN, rs.getString(FILTERS_COLUMN));
    p.put(CORE_COLUMN, rs.getString(CORE_COLUMN));
    p.put(FREQUENCY_COLUMN, rs.getString(FREQUENCY_COLUMN));
    p.put(MAIL_COLUMN, rs.getString(MAIL_COLUMN));
    p.put(SUBJECT_COLUMN, rs.getString(SUBJECT_COLUMN));
    p.put(USER_COLUMN, rs.getString(USER_COLUMN));
    return p;
  }

  private static String valueOrDefault(final String v, final String def) {
    return (v == null || v.isEmpty()) ? def : v;
  }
}