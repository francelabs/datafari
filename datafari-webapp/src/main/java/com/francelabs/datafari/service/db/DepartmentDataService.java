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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class DepartmentDataService extends CassandraService {

  final static Logger logger = LogManager.getLogger(DepartmentDataService.class.getName());

  public static final String USERNAMECOLUMN = "username";
  public static final String DEPARTMENTCOLLECTION = "department";
  public static final String DEPARTMENTCOLUMN = "department";
  public final static String LASTREFRESHCOLUMN = "last_refresh";

  private final String userDataTTL;

  private static DepartmentDataService instance;

  public static synchronized DepartmentDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new DepartmentDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to get instance : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public DepartmentDataService() {
    refreshSession();
    userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  /**
   * Get user's department
   *
   * @param username
   * @return the user's department
   */
  public String getDepartment(final String username) {
    String department = null;
    try {
      final String query = "SELECT " + DEPARTMENTCOLUMN + " FROM " + DEPARTMENTCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'";
      final ResultSet result = session.execute(query);
      final Row row = result.one();
      if (row != null && !row.isNull(DEPARTMENTCOLUMN) && !row.getString(DEPARTMENTCOLUMN).isEmpty()) {
        department = row.getString(DEPARTMENTCOLUMN);
      }
    } catch (final Exception e) {
      logger.warn("Unable to get department for user " + username + " : " + e.getMessage());
    }
    return department;
  }

  /**
   * Set user's department
   *
   * @param username
   * @param department
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int setDepartment(final String username, final String department) throws DatafariServerException {
    try {
      String ttlToUse = userDataTTL;
      if (username.contentEquals("admin")) {
        ttlToUse = "0";
      }
      final String query = "INSERT INTO " + DEPARTMENTCOLUMN + " (" + USERNAMECOLUMN + "," + DEPARTMENTCOLUMN + "," + LASTREFRESHCOLUMN + ")" + " values ('" + username + "','" + department
          + "',toTimeStamp(NOW())) USING TTL " + ttlToUse;
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to insert lang for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Update user's department
   *
   * @param username
   * @param department
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int updateDepartment(final String username, final String department) throws DatafariServerException {
    try {
      final String query = "UPDATE " + DEPARTMENTCOLLECTION + " SET " + DEPARTMENTCOLUMN + " = '" + department + "' WHERE " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to update department for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  /**
   * Update user's department
   *
   * @param username
   * @return CodesReturned.ALLOK if all was ok
   * @throws DatafariServerException
   */
  public int deleteDepartment(final String username) throws DatafariServerException {
    try {
      final String query = "DELETE FROM " + DEPARTMENTCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "' IF EXISTS";
      session.execute(query);
    } catch (final Exception e) {
      logger.warn("Unable to delete department for user " + username + " : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshDepartment(final String username) throws DatafariServerException {
    final String userDepartment = getDepartment(username);
    if (userDepartment != null && !userDepartment.isEmpty()) {
      deleteDepartment(username);
      setDepartment(username, userDepartment);
    }
  }

}