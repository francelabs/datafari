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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.DriverException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class UserDataService extends CassandraService {
  final static Logger logger = LogManager.getLogger(UserDataService.class.getName());
  private static UserDataService instance;

  public final static String SEARCHADMINISTRATOR = "SearchAdministrator";
  public final static String USERCOLLECTION = "user";
  public final static String ROLECOLLECTION = "role";

  public static final String USERNAMECOLUMN = "username";
  public final static String PASSWORDCOLUMN = "password";
  public final static String LDAPCOLUMN = "ldap";
  public final static String ROLECOLUMN = "role";

  public static synchronized UserDataService getInstance() throws DatafariServerException {
    try {
      if (instance == null) {
        instance = new UserDataService();
      }
      instance.refreshSession();
      return instance;
    } catch (final DriverException e) {
      logger.warn("Unable to connect to database : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Inform if the user exists already in the database
   *
   * @return true if is exists and false if not
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public boolean isInBase(final String username) throws DatafariServerException {
    try {
      final ResultSet results = session.execute("SELECT * FROM " + USERCOLLECTION + " where " + USERNAMECOLUMN + " = '" + username + "'");
      if (results.one() != null) {
        return true;
      } else {
        return false;
      }
    } catch (final DriverException e) {
      logger.warn("Unable to check if user in base : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }

  }

  /**
   * Get the password of a user
   *
   * @param username
   * @return the password of the user
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public String getPassword(final String username) throws DatafariServerException {
    try {

      final ResultSet results = session.execute("SELECT * FROM " + USERCOLLECTION + " where " + USERNAMECOLUMN + " = '" + username + "'");
      final Row entry = results.one();
      if (entry == null) {
        return null;
      } else {
        return entry.getString(PASSWORDCOLUMN);
      }

    } catch (final DriverException e) {
      logger.warn("Unable to get password : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Returns the roles of a user containing in the myDoc
   *
   * @param myDoc
   *          the document containing the user with the roles
   * @return an arrayList of roles of the user
   * @throws Exception
   *           if there's a probleme with database
   */
  public List<String> getRoles(final String username) throws DatafariServerException {
    try {

      final List<String> roles = new ArrayList<>();
      final ResultSet results = session.execute("SELECT " + ROLECOLUMN + " FROM " + ROLECOLLECTION + " where " + USERNAMECOLUMN + " = '" + username + "'");

      for (final Row row : results) {
        roles.add(row.getString(ROLECOLUMN));
      }
      return roles;
    } catch (final DriverException e) {
      logger.warn("Unable to get roles : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * get all user with the corresponding roles
   *
   * @param db
   *          instance of the database that contains the identifier collection
   * @return Map of <username, list of roles>
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public Map<String, List<String>> getAllUsers() throws DatafariServerException {
    try {
      final Map<String, List<String>> users = new HashMap<>();

      final ResultSet userResults = session.execute("SELECT * FROM " + USERCOLLECTION);
      for (final Row row : userResults) {
        final String user = row.getString(USERNAMECOLUMN);
        if (!users.containsKey(user)) {
          users.put(user, new ArrayList<String>());
        }
      }

      final ResultSet roleResults = session.execute("SELECT * FROM " + ROLECOLLECTION);

      for (final Row row : roleResults) {
        final String user = row.getString(USERNAMECOLUMN);
        if (!users.containsKey(user)) {
          users.put(user, new ArrayList<String>());
        }
        users.get(user).add(row.getString(ROLECOLUMN));
      }

      return users;

    } catch (final DriverException e) {
      logger.warn("Unable to get all users : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Change a password of username with the "password"
   *
   * @param password
   *          new password hashed
   * @param username
   *          the username that we want to change
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public void changePassword(final String passwordHashed, final String username) throws DatafariServerException {
    try {

      final String query = "update " + USERCOLLECTION + " set " + PASSWORDCOLUMN + " = '" + passwordHashed + "' where " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(query);
    } catch (final DriverException e) {

      logger.warn("Unable to change password : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Add a role to the user
   *
   * @param role
   *          string representing the role that we want to add
   * @throws DatafariServerException
   *           if there's a problem with Cassandra
   */
  public void addRole(final String role, final String username) throws DatafariServerException {
    try {
      final String query = "insert into " + ROLECOLLECTION + " (" + USERNAMECOLUMN + "," + ROLECOLUMN + ")" + " values ('" + username + "','" + role + "')";
      session.execute(query);
    } catch (final DriverException e) {

      logger.warn("Unable to add role : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Add a user
   *
   * @param username
   * @param password
   * @param role
   *          is the array containing the roles of the user that we want to add
   * @return
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public boolean addUser(final String username, final String password, final List<String> roles) throws DatafariServerException {
    try {
      final String query = "insert into " + USERCOLLECTION + " (" + USERNAMECOLUMN + "," + PASSWORDCOLUMN + ")" + " values ('" + username + "','" + password + "')";
      session.execute(query);
      for (final String role : roles) {
        this.addRole(role, username);
      }
      // TODO correctly catch already in base database
    } catch (final DriverException e) {
      logger.warn("Unable to add user : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
    return true;
  }

  /**
   * Delete a user
   *
   * @param username
   *          the user to delete
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public void deleteUser(final String username) throws DatafariServerException {
    try {
      final String queryUser = "DELETE FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(queryUser);

      final String queryRole = "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'";
      session.execute(queryRole);
    } catch (final DriverException e) {
      logger.warn("Unable to remove user : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Delete a role from the user
   *
   * @param role
   *          string representing the role that we want to delete
   * @throws Exception
   *           if there's a problem with Cassandra
   */
  public void deleteRole(final String role, final String username) throws DatafariServerException {
    try {

      final String query = "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = '" + username + "'" + " AND " + ROLECOLUMN + " = '" + role + "'";
      session.execute(query);
    } catch (final DriverException e) {
      logger.warn("Unable to remove roles : " + e.getMessage());
      // TODO catch specific exception
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

}