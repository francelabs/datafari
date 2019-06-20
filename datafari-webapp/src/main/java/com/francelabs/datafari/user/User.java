/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.tomcat.util.buf.HexUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.UserDataService;

public class User {
	final static Logger logger = LogManager.getLogger(User.class.getName());
	private String algorithmHash = "SHA-256";
	private String username;
	private String passwordHashed;
	private boolean isSignedUp = false;
	private boolean isSignedIn = false;

	public User(String username, String password) {
		this.username = username;
		this.passwordHashed = digest(password);
	}

	/**
	 * Attempt a sign up with assigning only one role to the user
	 * 
	 * @param role
	 *            is the string that will be assigned to the user
	 * @return true if the signup was successful and false if not
	 * @throws DatafariServerException
	 * @throws IOException
	 */
	public void signup(String role) throws DatafariServerException {
		signup(Collections.singletonList(role));
	}

	/**
	 * Attempt a sign up with assigning more than one role to the user
	 * 
	 * @param role
	 *            is the array containing the roles of the user
	 * @return true if the sign up was successful and false if not
	 * @throws IOException
	 * @throws DatafariServerException
	 */
	public void signup(List<String> role) throws DatafariServerException {
		try {
			UserDataService.getInstance().addUser(this.username, this.passwordHashed, role);
			this.isSignedUp = true;
		} catch (DatafariServerException e) {
			this.isSignedUp = false;
			throw e;
		}
	}

	/**
	 * method used to attemp a login in the database using the attribute
	 * userName and password
	 * 
	 * @return true if the login was successful and false in fail
	 */
	public void signIn() throws DatafariServerException {

		String passwordDatabaseHashed = UserDataService.getInstance().getPassword(digest(this.username));
		if (!passwordHashed.equals(passwordDatabaseHashed)) {
			throw new DatafariServerException(CodesReturned.FAILTOSIGNIN, "Uncorrect password");
		}
	}

	public void changePassword(String password) throws DatafariServerException {
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
	 *            string representing the role that we want to add
	 * @throws DatafariServerException
	 */
	public void addRole(String role) throws DatafariServerException {
		UserDataService.getInstance().addRole(role, this.username);
	}

	/**
	 * Delete a role from the user
	 * 
	 * @param role
	 *            string representing the role that we want to delete
	 * @throws DatafariServerException
	 */
	public void deleteRole(String role) throws DatafariServerException {
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
	 *            instance of the database that contains the identifier
	 *            collection
	 * @return array list of array list containing at index 0 the username and
	 *         the index 1 an array list of the user's roles and null if there's
	 *         problem with database
	 */
	public static Map<String, List<String>> getAllUsers() {
		try {
			return UserDataService.getInstance().getAllUsers();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Used to hash a password using the algorithm setted in the attribute
	 * algorithmHash
	 * 
	 * @param password
	 *            that you want to hash
	 * @return the password hashed
	 */
	public String digest(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance(this.algorithmHash);
			byte[] digest = md.digest(password.getBytes("UTF-8"));
			return HexUtils.toHexString((digest));
		} catch (UnsupportedEncodingException ex) {
			logger.error(ex);
			return null;
		} catch (NoSuchAlgorithmException ex) {
			logger.error(ex);
			return null;
		}
	}

	// setters
	public void setAlgorithmHash(String algo) {
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
