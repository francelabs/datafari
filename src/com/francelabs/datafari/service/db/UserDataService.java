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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.catalina.realm.RealmBase;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.*;

public class UserDataService {
	final static Logger logger = Logger.getLogger(UserDataService.class.getName());


	private static MongoCollection<Document> coll;
	
	
	

	private static void initiate(){
		if (coll!=null)
			coll = MongoDBContextListerner.getInstance().getDatabase(DatabaseConstants.IDENTIFIERSDB)
				.getCollection(DatabaseConstants.IDENTIFIERSCOLLECTION);
	}
	/**
	 * Inform if the user exists already in the database
	 * @return true if is exists and false if not
	 * @throws Exception if there's a problem with MongoDB (database)
	 */
	public static boolean isInBase(String username) throws Exception{
		initiate();
		BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
		return coll.find(doc).first()!=null;
	}

	/**
	 * Get the password of a user
	 * @param username
	 * @return the password of the user
	 * @throws Exception if there's a problem with MongoDB (database)
	 */
	public static String getPassword(String username) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		if (myDoc==null)
			return null;
		String password = (String) myDoc.get(DatabaseConstants.PASSWORDCOLUMN);
		return password;
	}
	
	/**
	 * Returns the roles of a user containing in the myDoc
	 * @param myDoc the document containing the user with the roles
	 * @return an arrayList of roles of the user
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */
	public static ArrayList<String> getRoles(String username) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		ArrayList<Document> rolesList = (ArrayList<Document>) doc.get(DatabaseConstants.ROLECOLUMN);
		ArrayList <String> result = new ArrayList<String>();
		for (int i = 0 ; i < rolesList.size() ; i++)
			result.add( (String)((Document)(rolesList.get(i))).get(DatabaseConstants.ROLEATTRIBUTE));
		return result;
	}
	

	/**
	 * get all user with the corresponding roles
	 * @param db instance of the database that contains the identifier collection	
	 * @return array list of array list containing at index 0 the username and the index 1 an array list of the user's roles
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */
	public static ArrayList<ArrayList<Object>> getAllUsers() throws Exception{
		initiate();
			FindIterable<Document> iterable = coll.find().sort(new Document("username",1));
			final ArrayList<ArrayList<Object>>  results= new ArrayList<ArrayList<Object>>();
			iterable.forEach(new Block<Document>() {
			    @Override
			    public void apply(final Document document) {
			    	ArrayList<Object> arrayList = new ArrayList<Object>();
			    	if (document.get(DatabaseConstants.USERNAMECOLUMN)!=null && document.get(DatabaseConstants.ROLECOLUMN)!=null){
			    		arrayList.add(document.get(DatabaseConstants.USERNAMECOLUMN).toString());
			    	try {
						arrayList.add(UserDataService.getRoles(document.get(DatabaseConstants.USERNAMECOLUMN).toString()));
					} catch (Exception e) {
						logger.error(e);
					}
				       results.add(arrayList);
			    	}
			    }
			});		

		return results;
	}

	/**
	 * Change a password of username with the "password"
	 * @param password new password hashed
	 * @param username the username that we want to change
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */
	public static void changePassword(String passwordHashed,String username) throws Exception{
		initiate();
		Document newDocument = new Document();
		newDocument.append("$set", new Document(DatabaseConstants.PASSWORDCOLUMN,passwordHashed));		
		Document searchQuery = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		coll.updateOne(searchQuery, newDocument);
	}
	
	/**
	 * Add a role to the user
	 * @param role string representing the role that we want to add
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */
	public static void addRole(String role, String username) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		// to have more informations, we can try to query by using only the user, so we can know 
		// in a failure if the user exist or not, and if it's the password which is incorrect 
		Document myDoc = coll.find(doc).first();
		ArrayList<Object> rolesList = (ArrayList<Object>) myDoc.get(DatabaseConstants.ROLECOLUMN);
		rolesList.add(new BasicDBObject(DatabaseConstants.ROLEATTRIBUTE, role));
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("$set", new BasicDBObject().append(DatabaseConstants.ROLECOLUMN, rolesList));
	 
		BasicDBObject searchQuery = new BasicDBObject().append(DatabaseConstants.USERNAMECOLUMN, username);
		coll.updateOne(searchQuery, newDocument);
	}

	/**
	 * Attempt a sign up with assigning more than one role to the user
	 * @param username 
	 * @param password
	 * @param role is the array containing the roles of the user that we want to add
	 * @return true if the sign up was successful and false if not
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */	
	public static boolean addUser(String username, String password, String[] role) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username)
				.append(DatabaseConstants.PASSWORDCOLUMN, password);
		ArrayList<Document> roleDBList =  new ArrayList<Document> ();
		for (int i = 0 ; i < role.length ; i++){
			roleDBList.add(new Document(DatabaseConstants.ROLEATTRIBUTE, role[i]));
		}
		doc.append(DatabaseConstants.ROLECOLUMN,roleDBList);
		Document userDoc =  new Document(DatabaseConstants.USERNAMECOLUMN,username);
		if (coll.find(userDoc).first()==null){
			coll.insertOne(doc);	
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Delete a user
	 * @param username the user to delete
	 * @throws Exception if there's a problem with MongoDB (database)
	 */
	public static void deleteUser(String username) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		coll.deleteOne(doc);
	}
	 	
	/**
	 * Delete a role from the user
	 * @param role string representing the role that we want to delete
	 * @throws Exception if there's a probleme with MongoDB (database)
	 */
	public static void deleteRole(String role,String username) throws Exception{
		initiate();
		Document doc = new Document(DatabaseConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		if (myDoc!=null){
			ArrayList<Object> rolesList = (ArrayList<Object>) myDoc.get(DatabaseConstants.ROLECOLUMN);
			ArrayList<Object> rolesResult =  new ArrayList<Object>();
			for (int i=0; i<rolesList.size() ; i++){
				if (((Document) rolesList.get(i)).get(DatabaseConstants.ROLEATTRIBUTE).toString().equals(role)){
					continue;
				}
				rolesResult.add(new Document(DatabaseConstants.ROLEATTRIBUTE,((Document) rolesList.get(i)).get(DatabaseConstants.ROLEATTRIBUTE)));
			}
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(DatabaseConstants.ROLECOLUMN, rolesResult));
			BasicDBObject searchQuery = new BasicDBObject().append(DatabaseConstants.USERNAMECOLUMN, username);
			coll.updateOne(searchQuery, newDocument);
		}
	}
	
		 
}
