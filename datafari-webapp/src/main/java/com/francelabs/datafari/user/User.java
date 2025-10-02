/*******************************************************************************
 *  Copyright 2015-2025 France Labs
 *  Licensed under the Apache License, Version 2.0
 *******************************************************************************/
package com.francelabs.datafari.user;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;


public class User {

  private static final Logger logger = LogManager.getLogger(User.class.getName());


  private static final int BCRYPT_STRENGTH = 12;
  private static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

  private final String username;

  private final String passwordPlain;

  private boolean isSignedUp = false;
  private final boolean isSignedIn = false;
  private boolean isImported = false;


  public User(final String username, final String password) {
    this.username = Objects.requireNonNull(username, "username");
    this.passwordPlain = Objects.requireNonNull(password, "password");
  }

  public User(final String username, final String password, final boolean activeDirectoryUser) {
    this(username, password);
    this.isImported = activeDirectoryUser;
  }


  public void signup(final String role) throws DatafariServerException {
    signup(Collections.singletonList(role));
  }

  public void signup(final List<String> roles) throws DatafariServerException {
    final String bcrypt = BCRYPT.encode(passwordPlain);
    UserDataService.getInstance().addUser(this.username, bcrypt, roles, this.isImported);
    this.isSignedUp = true;
  }


  public void signIn() throws DatafariServerException {
    final String storedBcrypt = UserDataService.getInstance().getPassword(this.username); // <-x- lookup par username (fix)
    if (storedBcrypt == null || storedBcrypt.isBlank()) {
      throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Credentials not set");
    }
    if (!BCRYPT.matches(this.passwordPlain, storedBcrypt)) {
      throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Uncorrect password");
    }
   
  }


  public void changePassword(final String newPassword) throws DatafariServerException {
    final String bcrypt = BCRYPT.encode(Objects.requireNonNull(newPassword, "newPassword"));
    UserDataService.getInstance().changePassword(bcrypt, this.username);
  }


  public List<String> getRoles() throws DatafariServerException {
    return UserDataService.getInstance().getRoles(this.username);
  }

  public void addRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().addRole(role, this.username);
  }

  public void deleteRole(final String role) throws DatafariServerException {
    UserDataService.getInstance().deleteRole(role, this.username);
  }

  public boolean isInBase() throws DatafariServerException {
    return UserDataService.getInstance().isInBase(this.username);
  }

  public void deleteUser() throws DatafariServerException {
    UserDataService.getInstance().deleteUser(this.username);
  }

  public static JSONArray getAllUsers() {
    try {
      return UserDataService.getInstance().getAllUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public static JSONArray getAllADUsers() {
    try {
      return UserDataService.getInstance().getAllADUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public static JSONArray getAllDatafariUsers() {
    try {
      return UserDataService.getInstance().getAllDatafariUsers();
    } catch (final Exception e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }


  public boolean isSignedUp() { return isSignedUp; }
  public boolean isSignedIn() { return isSignedIn; }
  public boolean isImported() { return isImported; }
  public String getUsername() { return username; }
}