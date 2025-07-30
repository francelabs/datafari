package com.francelabs.datafari.service.db;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class DepartmentDataServicePostgres {

  public static final String DEPARTMENTCOLLECTION = "department";
  public static final String DEPARTMENTCOLUMN = "department";
  public static final String USERNAMECOLUMN = "username";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  private static DepartmentDataServicePostgres instance = null;
  private final PostgresService pgService = new PostgresService();
  private static final Logger logger = LogManager.getLogger(DepartmentDataServicePostgres.class.getName());

  private final String userDataTTL; // non utilisé mais conservé pour compatibilité

  private DepartmentDataServicePostgres() {
    this.userDataTTL = null;
  }

  public static synchronized DepartmentDataServicePostgres getInstance() {
    if (instance == null) {
      instance = new DepartmentDataServicePostgres();
    }
    return instance;
  }

  public String getDepartment(final String username) {
    try {
      String sql = "SELECT " + DEPARTMENTCOLUMN + " FROM " + DEPARTMENTCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      try (ResultSet rs = pgService.executeSelect(sql, username)) {
        if (rs.next()) {
          return rs.getString(DEPARTMENTCOLUMN);
        }
      }
    } catch (final Exception e) {
      logger.warn("Unable to get department for user " + username + " : " + e.getMessage());
    }
    return null;
  }

  public int setDepartment(final String username, final String department) throws DatafariServerException {
    try {
      String sql = "INSERT INTO " + DEPARTMENTCOLLECTION + " (" + USERNAMECOLUMN + ", " + DEPARTMENTCOLUMN + ", " + LASTREFRESHCOLUMN + ") VALUES (?, ?, ?) "
          + "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " + DEPARTMENTCOLUMN + " = EXCLUDED." + DEPARTMENTCOLUMN + ", "
          + LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
      pgService.executeUpdate(sql, username, department, Timestamp.from(Instant.now()));
    } catch (final Exception e) {
      logger.error("Unable to insert department for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public int updateDepartment(final String username, final String department) throws DatafariServerException {
    try {
      String sql = "UPDATE " + DEPARTMENTCOLLECTION + " SET " + DEPARTMENTCOLUMN + " = ?, " + LASTREFRESHCOLUMN + " = ? WHERE " + USERNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, department, Timestamp.from(Instant.now()), username);
    } catch (final Exception e) {
      logger.warn("Unable to update department for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public int deleteDepartment(final String username) throws DatafariServerException {
    try {
      String sql = "DELETE FROM " + DEPARTMENTCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      pgService.executeUpdate(sql, username);
    } catch (final Exception e) {
      logger.warn("Unable to delete department for user " + username + " : " + e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return CodesReturned.ALLOK.getValue();
  }

  public void refreshDepartment(final String username) throws DatafariServerException {
    final String department = getDepartment(username);
    if (department != null && !department.isEmpty()) {
      updateDepartment(username, department);
    }
  }

}