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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.service.db.UserDataService;
import com.francelabs.datafari.utils.HexUtils;


public class User {
	final static Logger logger = Logger.getLogger(User.class.getName());
	private String algorithmHash = "SHA-256";
	private String username;
	private String passwordHashed;
	private String[] role;
	private boolean isSignedUp = false;
	private boolean isSignedIn = false;
	
	
	
	 
	public User(String username,String password){
		this.username = username;
		this.passwordHashed = digest(password);
	}
		
	/**
	 * Attempt a sign up with assigning only one role to the user
	 * @param role is the string that will be assigned to the user
	 * @return true if the signup was successful and false if not
	 */
	public int signup(String role){
			return signup(Collections.singletonList(role));
	}
		
	/**
	 * Attempt a sign up with assigning more than one role to the user
	 * @param role is the array containing the roles of the user
	 * @return true if the sign up was successful and false if not
	 */	
	public int signup(List<String> role){
		try {
			if (UserDataService.getInstance().addUser(this.username, this.passwordHashed, role)){
				this.isSignedUp = true;
				return CodesReturned.ALLOK;
			}else{
				this.isSignedUp = false;
				return CodesReturned.USERALREADYINBASE;
			}
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
		
	/**
	 * method used to attemp a login in the database using the attribute userName and password
	 * @return true if the login was successful and false in fail
	 */
	public int signIn(){
		try {
			String passwordDatabaseHashed = UserDataService.getInstance().getPassword(digest(this.username));
			if (passwordHashed.equals(passwordDatabaseHashed)){
				return CodesReturned.ALLOK;
			}else{
				return CodesReturned.FAILTOSIGNIN;
			}
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
	
	public int changePassword(String password){
		try {
			UserDataService.getInstance().changePassword(digest(password),this.username);
			return CodesReturned.ALLOK;
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
	
	/**
	 * Delete the user
	 * @return true if the delete was performed correctly, false if not.
	 */
	public int deleteUser(){
		try {
			UserDataService.getInstance().deleteUser(this.username);
			return CodesReturned.ALLOK;
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
		
	/**
	 * Returns the roles of a user
	 * @return arrayList containing the roles
	 */
	public List<String> getRoles(){
		try {
			return UserDataService.getInstance().getRoles(this.username);
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
		
	/**
	 * Add a role to the user
	 * @param role string representing the role that we want to add
	 */
	public int addRole(String role){
		try {
			UserDataService.getInstance().addRole(role, this.username);
			return CodesReturned.ALLOK;
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
	
	
	/**
	 * Delete a role from the user
	 * @param role string representing the role that we want to delete
	 */
	public int deleteRole(String role){
		try {
			UserDataService.getInstance().deleteRole(role, this.username);
			return CodesReturned.ALLOK;
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
		
	/**
	 * Inform if the user exists already in the database
	 * @return true if is exists and false if not
	 */
	public int isInBase(){
		try {
			boolean bool = UserDataService.getInstance().isInBase(this.username);
			if (bool)
				return CodesReturned.TRUE;
			else
				return CodesReturned.FALSE;
		} catch (Exception e) {
			return CodesReturned.PROBLEMCONNECTIONDATABASE;
		}
	}
		
	/**
	 * get all user with the corresponding roles
	 * @param db instance of the database that contains the identifier collection	
	 * @return array list of array list containing at index 0 the username and the index 1 an array list of the user's roles
	 * and null if there's problem with database
	 */
	public static Map<String, List<String>> getAllUsers(){	
		try {
			return UserDataService.getInstance().getAllUsers();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}
		
	/**
	 * Used to hash a password using the algorithm setted in the attribute algorithmHash
	 * @param password that you want to hash
	 * @return the password hashed 
	 */
	public String digest(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance(this.algorithmHash);
			byte[] digest = md.digest(password.getBytes("UTF-8"));
			return HexUtils.convert(digest);
		} catch (UnsupportedEncodingException ex) {
			logger.error(ex);
			return null;
		} catch (NoSuchAlgorithmException ex) {
			logger.error(ex);
			return null;
		}
	}

		//setters 
		public void setAlgorithmHash(String algo){
			 this.algorithmHash = algo; 
		 }
		
		//getters
		
		public boolean isSignedUp() {
			return isSignedUp;
		}

		public boolean isSignedIn() {
			return isSignedIn;
		}
		
		 
}
