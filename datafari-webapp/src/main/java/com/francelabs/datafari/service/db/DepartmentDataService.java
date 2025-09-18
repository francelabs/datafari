package com.francelabs.datafari.service.db;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

@Service
public class DepartmentDataService {

  public static final String DEPARTMENTCOLLECTION = "department";
  public static final String DEPARTMENTCOLUMN     = "department";
  public static final String USERNAMECOLUMN       = "username";
  public static final String LASTREFRESHCOLUMN    = "last_refresh";

  private static final Logger logger = LogManager.getLogger(DepartmentDataService.class);

  // Pont de compatibilité avec l’ancien code (getInstance())
  private static volatile DepartmentDataService instance;

  private final SqlService sql; // accès JDBC centralisé via JdbcTemplate

  // ---- compat getInstance() pour le code existant ----
  public static synchronized DepartmentDataService getInstance() {
    return instance;
  }

  // Spring injecte SqlService ; on en profite pour initialiser 'instance'
  public DepartmentDataService(final SqlService sql) {
    this.sql = sql;
    instance = this;
  }

  public String getDepartment(final String username) {
    try {
      final String q = "SELECT " + DEPARTMENTCOLUMN +
                       " FROM " + DEPARTMENTCOLLECTION +
                       " WHERE " + USERNAMECOLUMN + " = ?";
      return sql.getJdbcTemplate().queryForObject(q, String.class, username);
    } catch (EmptyResultDataAccessException e) {
      return null; // pas de ligne pour cet utilisateur
    } catch (Exception e) {
      logger.warn("Unable to get department for user {} : {}", username, e.getMessage());
      return null;
    }
  }

  public int setDepartment(final String username, final String department) throws DatafariServerException {
    try {
      final String q =
          "INSERT INTO " + DEPARTMENTCOLLECTION + " (" +
              USERNAMECOLUMN + ", " + DEPARTMENTCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
          "VALUES (?, ?, ?) " +
          "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " +
              DEPARTMENTCOLUMN + " = EXCLUDED." + DEPARTMENTCOLUMN + ", " +
              LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;

      sql.getJdbcTemplate().update(q, username, department, Timestamp.from(Instant.now()));
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.error("Unable to insert department for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public int updateDepartment(final String username, final String department) throws DatafariServerException {
    try {
      final String q =
          "UPDATE " + DEPARTMENTCOLLECTION + " SET " +
              DEPARTMENTCOLUMN + " = ?, " +
              LASTREFRESHCOLUMN + " = ? " +
          "WHERE " + USERNAMECOLUMN + " = ?";

      sql.getJdbcTemplate().update(q, department, Timestamp.from(Instant.now()), username);
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to update department for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public int deleteDepartment(final String username) throws DatafariServerException {
    try {
      final String q = "DELETE FROM " + DEPARTMENTCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      sql.getJdbcTemplate().update(q, username);
      return CodesReturned.ALLOK.getValue();
    } catch (Exception e) {
      logger.warn("Unable to delete department for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void refreshDepartment(final String username) throws DatafariServerException {
    final String dept = getDepartment(username);
    if (dept != null && !dept.isEmpty()) {
      updateDepartment(username, dept);
    }
  }
}