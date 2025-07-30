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

public class LicenceDataServicePostgres {

  final static Logger logger = LogManager.getLogger(LicenceDataServicePostgres.class.getName());

  public static final String IDCOLUMN = "licence_id";
  public static final String LICENCECOLLECTION = "licence";
  public static final String LICENCECOLUMN = "licence";

  private static LicenceDataServicePostgres instance;

  private final PostgresService pgService = new PostgresService();

  public static synchronized LicenceDataServicePostgres getInstance() {
    if (instance == null) {
      instance = new LicenceDataServicePostgres();
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
      String sql = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " WHERE " + IDCOLUMN + " = ?";
      try (ResultSet rs = pgService.executeSelect(sql, licenceID)) {
        if (rs.next()) {
          Blob blob = rs.getBlob(LICENCECOLUMN);
          if (blob != null) {
            try (ObjectInputStream ois = new ObjectInputStream(blob.getBinaryStream())) {
              licence = (Licence) ois.readObject();
            }
          }
        }
      }
    } catch (final Exception e) {
      logger.error("Unable to retrieve licence " + licenceID, e);
    }
    return licence;
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