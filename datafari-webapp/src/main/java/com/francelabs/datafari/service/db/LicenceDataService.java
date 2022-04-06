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

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.server.ByteBufferInputStream;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.licence.Licence;

public class LicenceDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(LicenceDataService.class.getName());

  public static final String IDCOLUMN = "licence_id";
  public static final String LICENCECOLLECTION = "licence";
  public static final String LICENCECOLUMN = "licence";

  private static LicenceDataService instance;

  public static synchronized LicenceDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new LicenceDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
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
      final String query = "SELECT " + LICENCECOLUMN + " FROM " + LICENCECOLLECTION + " where " + IDCOLUMN + "='" + licenceID + "'";
      final ResultSet result = session.execute(query);
      final Row row = result.one();
      if (row != null && !row.isNull(LICENCECOLUMN)) {
        final ByteBuffer bb = row.getByteBuffer(LICENCECOLUMN);
        final ByteBufferInputStream bbi = new ByteBufferInputStream(bb);
        final ObjectInputStream ois = new ObjectInputStream(bbi);
        licence = (Licence) ois.readObject();
      }
    } catch (final Exception e) {
      logger.error("Unable to retrieve licence " + licenceID, e);
    }
    return licence;
  }

  /**
   * Save the licence
   *
   * @param licenceId
   *          the licenceId
   * @param licence
   *          the licence
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int saveLicence(final String licenceId, final Licence licence) {
    try {
      final String existingLicence = getLicence();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(licence);
      baos.flush();
      final ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());
      final PreparedStatement ps = session.prepare("INSERT INTO " + LICENCECOLUMN 
          + " (" + IDCOLUMN + "," 
          + LICENCECOLUMN + ")" 
          + " values (?,?)");
      final BoundStatement bs = ps.bind(licenceId, bb);
      session.execute(bs);

      if (existingLicence != null && !existingLicence.isEmpty()) {
        deleteLicence(existingLicence);
      }
    } catch (final Exception e) {
      logger.warn("Unable to save the licence", e);
    }
    return CodesReturned.ALLOK.getValue();
  }

  private String getLicence() {
    try {
      final String query = "SELECT " + IDCOLUMN + " FROM " + LICENCECOLLECTION + ";";
      final ResultSet result = session.execute(query);
      final Row row = result.one();
      if (row != null && !row.isNull(IDCOLUMN)) {
        final String licenceId = row.getString(IDCOLUMN);
        return licenceId;
      }
    } catch (final Exception e) {
      logger.warn("Unable to get the licence", e);
    }
    return null;
  }

  private void deleteLicence(final String licenceId) {
    try {
      final String query = "DELETE FROM " + LICENCECOLLECTION 
          + " WHERE " + IDCOLUMN + " = '" + licenceId + "'"
          + " IF EXISTS";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to delete licence: " + licenceId, e);
    }
  }

  private void emptyLicence() {
    try {
      final String query = "TRUNCATE " + LICENCECOLLECTION + ";";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to empty licence table", e);
    }
  }

}