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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.licence.Licence;

public class LicenceDataService {

  private static final Logger logger = LogManager.getLogger(LicenceDataService.class);

  public static final String IDCOLUMN = "licence_id";
  public static final String LICENCECOLLECTION = "licence";
  public static final String LICENCECOLUMN = "licence";

  private static LicenceDataService instance;

  // Helpers JDBC via le pont statique
  private final JdbcTemplate jdbc;

  private LicenceDataService() {
    this.jdbc = SqlService.get().getJdbcTemplate();
  }

  public static synchronized LicenceDataService getInstance() {
    if (instance == null) {
      instance = new LicenceDataService();
    }
    return instance;
  }

  /**
   * Retrieve a licence by ID, or fallback to the first row if not found/empty.
   */
  public Licence getLicence(final String licenceID) {
    Licence licence = null;
    try {
      // 1) Tentative par ID
      final String byIdSql = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " WHERE " + IDCOLUMN + " = ?";
      byte[] data = jdbc.query(byIdSql, rs -> rs.next() ? rs.getBytes(LICENCECOLUMN) : null, licenceID);

      if (data != null && data.length > 0) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
          return (Licence) ois.readObject();
        }
      } else {
        logger.warn("Licence not found or empty for id='{}'. Trying fallback.", licenceID);
      }

      // 2) Fallback : première ligne de la table
      final String anySql = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " LIMIT 1";
      byte[] anyData = jdbc.query(anySql, rs -> rs.next() ? rs.getBytes(LICENCECOLUMN) : null);

      if (anyData != null && anyData.length > 0) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(anyData))) {
          licence = (Licence) ois.readObject();
          logger.info("Licence retrieved via fallback (first row).");
        }
      } else {
        logger.warn("Fallback: licence table '{}' is empty or column is empty.", LICENCECOLLECTION);
      }
    } catch (final Exception e) {
      logger.error("Error while retrieving/deserializing licence (id='{}')", licenceID, e);
    }
    return licence; // peut être null
  }

  /**
   * Save (upsert) a licence (BYTEA) under a given id. Deletes previous id if it differs.
   */
  public int saveLicence(final String licenceId, final Licence licence) {
    try {
      final String existingLicence = getLicenceId();

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(licence);
        oos.flush();
      }
      byte[] licenceBytes = baos.toByteArray();

      final String sql =
          "INSERT INTO " + LICENCECOLLECTION + " (" + IDCOLUMN + ", " + LICENCECOLUMN + ") VALUES (?, ?) " +
          "ON CONFLICT (" + IDCOLUMN + ") DO UPDATE SET " + LICENCECOLUMN + " = EXCLUDED." + LICENCECOLUMN;

      jdbc.update(sql, licenceId, licenceBytes);

      if (existingLicence != null && !existingLicence.isEmpty() && !existingLicence.equals(licenceId)) {
        deleteLicence(existingLicence);
      }
    } catch (final Exception e) {
      logger.warn("Unable to save the licence", e);
    }
    return CodesReturned.ALLOK.getValue();
  }

  /** Return any existing licence_id (first row) or null. */
  private String getLicenceId() {
    try {
      final String sql = "SELECT " + IDCOLUMN + " FROM " + LICENCECOLLECTION + " LIMIT 1";
      return jdbc.query(sql, rs -> rs.next() ? rs.getString(IDCOLUMN) : null);
    } catch (final Exception e) {
      logger.warn("Unable to get the licence id", e);
      return null;
    }
  }

  private void deleteLicence(final String licenceId) {
    try {
      final String sql = "DELETE FROM " + LICENCECOLLECTION + " WHERE " + IDCOLUMN + " = ?";
      jdbc.update(sql, licenceId);
    } catch (final Exception e) {
      logger.warn("Unable to delete licence: {}", licenceId, e);
    }
  }

  @SuppressWarnings("unused")
  private void emptyLicence() {
    try {
      final String sql = "TRUNCATE TABLE " + LICENCECOLLECTION;
      jdbc.update(sql);
    } catch (final Exception e) {
      logger.warn("Unable to empty licence table", e);
    }
  }
}