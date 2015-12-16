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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class UserDataService {
	final static Logger logger = Logger
			.getLogger(UserDataService.class.getName());
	private static UserDataService instance;
	

	public final static String SEARCHADMINISTRATOR = "SearchAdministrator";
	public final static String USERCOLLECTION = "user";
	public final static String ROLECOLLECTION = "role";

	public static final String USERNAMECOLUMN = "username";
	public final static String PASSWORDCOLUMN = "password";
	public final static String ROLECOLUMN = "role";
	
	

	private Session session;

	public static synchronized UserDataService getInstance()
			throws IOException {
		if (instance == null) {
			instance = new UserDataService();
		}
		return instance;
	}

	public UserDataService() throws IOException {

		// Gets the name of the collection
		session = DBContextListerner.getSession();

	}

	/**
	 * Inform if the user exists already in the database
	 * 
	 * @return true if is exists and false if not
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public boolean isInBase(String username) throws Exception {
		ResultSet results = session.execute("SELECT * FROM " + USERCOLLECTION
				+ " where " + USERNAMECOLUMN + " = '" + username + "'");
		if (results.one() != null) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Get the password of a user
	 * 
	 * @param username
	 * @return the password of the user
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public String getPassword(String username) throws Exception {
		ResultSet results = session.execute("SELECT * FROM " + USERCOLLECTION
				+ " where " + USERNAMECOLUMN + " = '" + username + "'");
		Row entry = results.one();
		if (entry == null) {
			return null;
		} else {
			return entry.getString(PASSWORDCOLUMN);
		}
	}

	/**
	 * Returns the roles of a user containing in the myDoc
	 * 
	 * @param myDoc
	 *            the document containing the user with the roles
	 * @return an arrayList of roles of the user
	 * @throws Exception
	 *             if there's a probleme with database
	 */
	public List<String> getRoles(String username) throws Exception {
		List<String> roles = new ArrayList<String>();
		ResultSet results = session.execute("SELECT " + ROLECOLUMN + " FROM "
				+ ROLECOLLECTION + " where " + USERNAMECOLUMN + " = '" + username
				+ "'");

		for (Row row : results) {
			roles.add(row.getString(ROLECOLUMN));
		}
		return roles;
	}

	/**
	 * get all user with the corresponding roles
	 * 
	 * @param db
	 *            instance of the database that contains the identifier
	 *            collection
	 * @return Map of <username, list of roles>
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public Map<String, List<String>> getAllUsers() throws Exception {
		Map<String, List<String>> users = new HashMap<String, List<String>>();

		ResultSet results = session.execute("SELECT * FROM " + ROLECOLLECTION);

		for (Row row : results) {
			String user = row.getString(USERNAMECOLUMN);
			if (!users.containsKey(user)) {
				users.put(user, new ArrayList<String>());
			}
			users.get(user).add(row.getString(ROLECOLUMN));
		}
		return users;
	}

	/**
	 * Change a password of username with the "password"
	 * 
	 * @param password
	 *            new password hashed
	 * @param username
	 *            the username that we want to change
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public void changePassword(String passwordHashed, String username)
			throws Exception {

		String query = "update " + USERCOLLECTION + " set " + PASSWORDCOLUMN
				+ " = '" + passwordHashed + "' where " + USERNAMECOLUMN
				+ " = '" + username + "'";
		session.execute(query);
	}

	/**
	 * Add a role to the user
	 * 
	 * @param role
	 *            string representing the role that we want to add
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public void addRole(String role, String username) throws Exception {
		String query = "insert into " + ROLECOLLECTION + " (" + USERNAMECOLUMN
				+ "," + ROLECOLUMN + ")" + " values ('" + username + "','"
				+ role + "')";
		session.execute(query);
	}

	/**
	 * Add a user
	 * 
	 * @param username
	 * @param password
	 * @param role
	 *            is the array containing the roles of the user that we want to
	 *            add
	 * @return 
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public boolean addUser(String username, String password, List<String> roles)
			throws Exception {
		try {
		String query = "insert into " + USERCOLLECTION + " (" + USERNAMECOLUMN
				+ "," + PASSWORDCOLUMN + ")" + " values ('" + username + "','"
				+ password + "')";
		session.execute(query);
		for (String role : roles) {
			this.addRole(role, username);
		}
		//TODO correctly catch already in base database
		} catch (Exception e){
			logger.warn(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Delete a user
	 * 
	 * @param username
	 *            the user to delete
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public void deleteUser(String username) throws Exception {
		String queryUser = "DELETE FROM " + USERCOLLECTION + " WHERE "
				+ USERNAMECOLUMN + " = '" + username+"'";
		session.execute(queryUser);
	
		String queryRole = "DELETE FROM " + ROLECOLLECTION + " WHERE "
				+ USERNAMECOLUMN + " = '" + username+"'";
		session.execute(queryRole);
	}

	/**
	 * Delete a role from the user
	 * 
	 * @param role
	 *            string representing the role that we want to delete
	 * @throws Exception
	 *             if there's a problem with Cassandra
	 */
	public  void deleteRole(String role, String username)
			throws Exception {
		String query = "DELETE FROM " + ROLECOLLECTION + " WHERE "
				+ USERNAMECOLUMN + " = '" + username + "'"
				+ " AND "+ROLECOLUMN + " = '"+role+"'";
		session.execute(query);
	}

}
