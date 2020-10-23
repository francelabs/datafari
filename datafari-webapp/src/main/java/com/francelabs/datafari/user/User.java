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
package com.francelabs.datafari.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.simple.JSONArray;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;

public class User {
  final static Logger logger = LogManager.getLogger(User.class.getName());
  private String algorithmHash = "SHA-256";
  private final String username;
  private final String passwordHashed;
  private boolean isSignedUp = false;
  private final boolean isSignedIn = false;
  private boolean isImported = false;

  public User(final String username, final String password) {
    this.username = username;
    this.passwordHashed = digest(password);
  }

  public User(final String username, final String password, final boolean activeDirectoryUser) {
    this.username = username;
    this.passwordHashed = digest(password);
    this.isImported = activeDirectoryUser;
  }

  /**
   * Attempt a sign up with assigning only one role to the user
   *
   * @param role
   *          is the string that will be assigned to the user
   * @return true if the signup was successful and false if not
   * @throws DatafariServerException
   * @throws IOException
   */
  public void signup(final String role) throws DatafariServerException {
    signup(Collections.singletonList(role));
  }

  /**
   * Attempt a sign up with assigning more than one role to the user
   *
   * @param role
   *          is the array containing the roles of the user
   * @return true if the sign up was successful and false if not
   * @throws IOException
   * @throws DatafariServerException
   */
  public void signup(final List<String> role) throws DatafariServerException {
    try {
      UserDataService.getInstance().addUser(this.username, this.passwordHashed, role, this.isImported);
      this.isSignedUp = true;
    } catch (final DatafariServerException e) {
      this.isSignedUp = false;
      throw e;
    }
  }

  /**
   * method used to attemp a login in the database using the attribute userName and password
   *
   * @return true if the login was successful and false in fail
   */
  public void signIn() throws DatafariServerException {

    final String passwordDatabaseHashed = UserDataService.getInstance().getPassword(digest(this.username));
    if (!passwordHashed.equals(passwordDatabaseHashed)) {
      throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Uncorrect password");
    }
  }

  public void changePassword(final String password) throws DatafariServerException {
    UserDataService.getInstance().changePassword(digest(password), this.username);
  }

  /**
   * Delete the user
   *
   * @return true if the delete was performed correctly, false if not.
   * @throws DatafariServerException
   */
  public void deleteUser() throws DatafariServerException {
    UserDataService.getInstance().deleteUser(this.username);
  }

  /**
   * Returns the roles of a user
   *
   * @return arrayList containing the roles
   * @throws DatafariServerException
   */
  public List<String> getRoles() throws DatafariServerException {
    return UserDataService.getInstance().getRoles(this.username);
  }

  /**
   * Add a role to the user
   *
   * @param role
   *          string representing the role that we want to add
   * @throws DatafariServerException
   */
  public void addRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().addRole(role, this.username);
  }

  /**
   * Delete a role from the user
   *
   * @param role
   *          string representing the role that we want to delete
   * @throws DatafariServerException
   */
  public void deleteRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().deleteRole(role, this.username);

  }

  /**
   * Inform if the user exists already in the database
   *
   * @return true if is exists and false if not
   * @throws DatafariServerException
   */
  public boolean isInBase() throws DatafariServerException {
    return UserDataService.getInstance().isInBase(this.username);

  }

  /**
   * get all user with the corresponding roles
   *
   * @param db
   *          instance of the database that contains the identifier collection
   * @return array list of array list containing at index 0 the username and the index 1 an array list of the user's roles and null if there's
   *         problem with database
   */
  public static JSONArray getAllUsers() {
    try {
      return UserDataService.getInstance().getAllUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage());
      return null;
    }
  }

  /**
   * get all Active Directory users with their corresponding roles
   *
   * @param db
   *          instance of the database that contains the identifier collection
   * @return array list of array list containing at index 0 the username and the index 1 an array list of the user's roles and null if there's
   *         problem with database
   */
  public static JSONArray getAllADUsers() {
    try {
      return UserDataService.getInstance().getAllADUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage());
      return null;
    }
  }

  /**
   * get all Datafari users with their corresponding roles
   *
   * @param db
   *          instance of the database that contains the identifier collection
   * @return array list of array list containing at index 0 the username and the index 1 an array list of the user's roles and null if there's
   *         problem with database
   */
  public static JSONArray getAllDatafariUsers() {
    try {
      return UserDataService.getInstance().getAllDatafariUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage());
      return null;
    }
  }

  /**
   * Used to hash a password using the algorithm setted in the attribute algorithmHash
   *
   * @param password
   *          that you want to hash
   * @return the password hashed
   */
  public String digest(final String password) {
    try {
      final MessageDigest md = MessageDigest.getInstance(this.algorithmHash);
      final byte[] digest = md.digest(password.getBytes("UTF-8"));
      return HexUtils.toHexString(digest);
    } catch (final UnsupportedEncodingException ex) {
      logger.error(ex);
      return null;
    } catch (final NoSuchAlgorithmException ex) {
      logger.error(ex);
      return null;
    }
  }

  // setters
  public void setAlgorithmHash(final String algo) {
    this.algorithmHash = algo;
  }

  // getters

  public boolean isSignedUp() {
    return isSignedUp;
  }

  public boolean isSignedIn() {
    return isSignedIn;
  }

}
