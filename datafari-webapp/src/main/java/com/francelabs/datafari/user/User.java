/*******************************************************************************
 *  Copyright 2015-2025 France Labs
 *  Licensed under the Apache License, Version 2.0
 *******************************************************************************/

package com.francelabs.datafari.user;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.simple.JSONArray;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;

public class User {

  private static final Logger logger = LogManager.getLogger(User.class.getName());

  // BCrypt encoder
  private static final int BCRYPT_COST = 12;
  private static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder(BCRYPT_COST);

  private String algorithmHash = "SHA-256"; // still used for SHA-256 digest
  private final String username;
  private final String passwordPlain; // plain password (only kept in memory during operations)

  private boolean isSignedUp = false;
  private final boolean isSignedIn = false;
  private boolean isImported = false;

  public User(final String username, final String password) {
    this.username = Objects.requireNonNull(username, "username");
    this.passwordPlain = Objects.requireNonNull(password, "password");
    logger.debug("username: {} - passwordPlain: {}", username, passwordPlain);
  }

  public User(final String username, final String password, final boolean activeDirectoryUser) {
    this(username, password);
    this.isImported = activeDirectoryUser;
  }

  /** Signup with one role */
  public void signup(final String role) throws DatafariServerException {
    signup(Collections.singletonList(role));
  }

  /** Signup with multiple roles → stores SHA-256 and bcrypt */
  public void signup(final List<String> roles) throws DatafariServerException {
    final String sha256 = digest(passwordPlain);
    final String bcrypt = BCRYPT.encode(passwordPlain);
    try {
      UserDataService.getInstance().addUserDualHash(this.username, sha256, bcrypt, roles, this.isImported);
      this.isSignedUp = true;
    } catch (final DatafariServerException e) {
      this.isSignedUp = false;
      throw e;
    }
  }

  /** Login using SHA-256 (legacy mode, unchanged) */
  public void signIn() throws DatafariServerException {
    final String passwordDatabaseHashed = UserDataService.getInstance().getPassword(this.username);
    if (passwordDatabaseHashed == null) {
      throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Credentials not set");
    }
    final String candidateSha = digest(this.passwordPlain);
    if (!candidateSha.equalsIgnoreCase(passwordDatabaseHashed)) {
      throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Incorrect password");
    }
  }

  /** Change password → update SHA-256 and bcrypt columns */
  public void changePassword(final String newPassword) throws DatafariServerException {
    final String sha256 = digest(Objects.requireNonNull(newPassword, "newPassword"));
    final String bcrypt = BCRYPT.encode(newPassword);
    UserDataService.getInstance().changePasswordDualHash(this.username, sha256, bcrypt);
  }

  /** Delete user */
  public void deleteUser() throws DatafariServerException {
    UserDataService.getInstance().deleteUser(this.username);
  }

  /** Roles */
  public List<String> getRoles() throws DatafariServerException {
    return UserDataService.getInstance().getRoles(this.username);
  }

  public void addRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().addRole(role, this.username);
  }

  public void deleteRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().deleteRole(role, this.username);
  }

  /** Check if user exists in DB */
  public boolean isInBase() throws DatafariServerException {
    return UserDataService.getInstance().isInBase(this.username);
  }

  /** Compute SHA-256 hex digest */
  public String digest(final String password) {
    try {
      final MessageDigest md = MessageDigest.getInstance(this.algorithmHash);
      final byte[] digest = md.digest(password.getBytes("UTF-8"));
      return HexUtils.toHexString(digest);
    } catch (final UnsupportedEncodingException | NoSuchAlgorithmException ex) {
      logger.error(ex);
      return null;
    }
  }

  // setters
  public void setAlgorithmHash(final String algo) { this.algorithmHash = algo; }

  // getters
  public boolean isSignedUp() { return isSignedUp; }
  public boolean isSignedIn() { return isSignedIn; }
  public boolean isImported() { return isImported; }
  public String getUsername() { return username; }

  // Static helper methods
  public static JSONArray getAllUsers() {
    try { return UserDataService.getInstance().getAllUsers(); }
    catch (final Exception e) { logger.error(e.getMessage()); return null; }
  }
  public static JSONArray getAllADUsers() {
    try { return UserDataService.getInstance().getAllADUsers(); }
    catch (final Exception e) { logger.error(e.getMessage()); return null; }
  }
  public static JSONArray getAllDatafariUsers() {
    try { return UserDataService.getInstance().getAllDatafariUsers(); }
    catch (final Exception e) { logger.error(e.getMessage()); return null; }
  }
}