package com.francelabs.datafari.service.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.licence.Licence;

public class LicenceDataService {

  final static Logger logger = LogManager.getLogger(LicenceDataService.class.getName());

  public static final String IDCOLUMN = "licence_id";
  public static final String LICENCECOLLECTION = "licence";
  public static final String LICENCECOLUMN = "licence";

  private static LicenceDataService instance;

  private final PostgresService pgService = new PostgresService();

  public static synchronized LicenceDataService getInstance() {
    if (instance == null) {
      instance = new LicenceDataService();
    }
    return instance;
  }

  /**
   * Get the licence
   *
   * @param licenceID
   * @return the licence corresponding to the licence id or null if not found
   */
  public Licence getLicence(final String licenceID) {
    Licence licence = null;
    try {
      // 1) First attempt: search for the licence using the given licenceID
      final String byIdSql = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " WHERE " + IDCOLUMN + " = ?";
      try (ResultSet rs = pgService.executeSelect(byIdSql, licenceID)) {
        if (rs.next()) {
          byte[] data = rs.getBytes(LICENCECOLUMN); // Read BYTEA column as raw bytes
          if (data != null && data.length > 0) {
            // Deserialize the byte array back into a Licence object
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
              licence = (Licence) ois.readObject();
              return licence; // Return immediately if found and deserialization succeeded
            }
          } else {
            logger.warn("Licence found for id='{}' but column is empty (0 bytes).", licenceID);
          }
        } else {
          logger.warn("No licence found for id='{}'.", licenceID);
        }
      }

      // 2) Fallback: if nothing found by ID, take the first licence available in the table
      final String anySql = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " LIMIT 1";
      try (ResultSet rs2 = pgService.executeSelect(anySql)) {
        if (rs2.next()) {
          byte[] data = rs2.getBytes(LICENCECOLLECTION);
          if (data != null && data.length > 0) {
            // Deserialize the fallback licence
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
              licence = (Licence) ois.readObject();
              logger.info("Licence successfully retrieved via fallback (first row of the table).");
            }
          } else {
            logger.warn("Fallback found a row but licence column is empty (0 bytes).");
          }
        } else {
          logger.warn("Fallback: licence table {} is empty.", LICENCECOLLECTION);
        }
      }
    } catch (final Exception e) {
      // Log any SQL or deserialization issue
      logger.error("Error while retrieving/deserializing licence (id='{}')", licenceID, e);
    }
    return licence; // May return null if licence not found or deserialization failed
  }
  /**
   * Save the licence
   *
   * @param licenceId the licenceId
   * @param licence   the licence
   * @return CodesReturned.ALLOK if all was ok
   */
  public int saveLicence(final String licenceId, final Licence licence) {
    try {
      final String existingLicence = getLicenceId();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(licence);
      oos.flush();
      byte[] licenceBytes = baos.toByteArray();

      // Upsert
      String sql = "INSERT INTO " + LICENCECOLLECTION + " (" + IDCOLUMN + ", " + LICENCECOLUMN + ") VALUES (?, ?) "
                 + "ON CONFLICT (" + IDCOLUMN + ") DO UPDATE SET " + LICENCECOLUMN + " = EXCLUDED." + LICENCECOLUMN;
      pgService.executeUpdate(sql, licenceId, licenceBytes);

      if (existingLicence != null && !existingLicence.isEmpty() && !existingLicence.equals(licenceId)) {
        deleteLicence(existingLicence);
      }
    } catch (final Exception e) {
      logger.warn("Unable to save the licence", e);
    }
    return CodesReturned.ALLOK.getValue();
  }

  // Helper to get the current licence id (if exists)
  private String getLicenceId() {
    try {
      String sql = "SELECT " + IDCOLUMN + " FROM " + LICENCECOLLECTION + " LIMIT 1";
      try (ResultSet rs = pgService.executeSelect(sql)) {
        if (rs.next()) {
          return rs.getString(IDCOLUMN);
        }
      }
    } catch (final Exception e) {
      logger.warn("Unable to get the licence", e);
    }
    return null;
  }

  private void deleteLicence(final String licenceId) {
    try {
      String sql = "DELETE FROM " + LICENCECOLLECTION + " WHERE " + IDCOLUMN + " = ?";
      pgService.executeUpdate(sql, licenceId);
    } catch (final Exception e) {
      logger.warn("Unable to delete licence: " + licenceId, e);
    }
  }

  private void emptyLicence() {
    try {
      String sql = "TRUNCATE TABLE " + LICENCECOLLECTION;
      pgService.executeUpdate(sql);
    } catch (final Exception e) {
      logger.warn("Unable to empty licence table", e);
    }
  }
}