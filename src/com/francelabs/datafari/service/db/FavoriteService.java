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

import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.realm.MongoDBRunning;

public class FavoriteService {
	public static String FAVORITEDB = DatabaseConstants.FAVORITEDB;
	private static FavoriteService instance;
	private static MongoCollection<Document> coll;
	
	/**
	 * Add a document to the favorites list of the user
	 * @param username of the user
	 * @param idDocument the id that should be add as a favorite
	 * @return true if it was success and false if not
	 */
	public static void addFavorite(String username, String idDocument){
			getInstance();
			BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
			Document myDoc = coll.find(doc).first(); // getting the doc of the username 
			if (myDoc != null){
				// if the doc exists in the collection (had already liked a document or saved a document as a favrorite
				ArrayList<Object> favoriteList =  (ArrayList<Object>) myDoc.get(DatabaseConstants.FAVORITECOLUMN);
				if (favoriteList!=null){
					for (int i=0; i<favoriteList.size(); i++){
						//checking if the document is already saved as a favorite for the username
						if (((Document)favoriteList.get(i)).get(DatabaseConstants.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
							return ;
						}
					}
				}else{
					favoriteList = new BasicDBList();
				}
				// if it wasn't saved yet by the username we added to the favorite List saved in MongoDB
				favoriteList.add(new BasicDBObject(DatabaseConstants.DOCUMENTIDCOLUMN, idDocument));
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.append("$set", new BasicDBObject().append(DatabaseConstants.FAVORITECOLUMN, favoriteList));
			 
				BasicDBObject searchQuery = new BasicDBObject().append(DatabaseConstants.USERNAMECOLUMN, username);
				coll.updateOne(searchQuery, newDocument);
			}else{
				// if the username has not a doc in the database, we create one
				BasicDBList dbList = new BasicDBList();
				dbList.add(new BasicDBObject(DatabaseConstants.DOCUMENTIDCOLUMN,idDocument));
				Document docAdd = new Document(DatabaseConstants.USERNAMECOLUMN, username)
					.append(DatabaseConstants.FAVORITECOLUMN,dbList )
					.append(DatabaseConstants.LIKESCOLUMN, new BasicDBList());
				coll.insertOne(docAdd);	
			}
	}
	
	
	/**
	 * delete a document from the favorites list of the user
	 * @param username of the user
	 * @param idDocument the id that should be deleted from the favorites
	 * @return true if it was success and false if not
	 */
	public static void deleteFavorite(String username, String idDocument){	
			getInstance();
			BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
			Document myDoc = coll.find(doc).first();
			ArrayList<Object> favoriteList = (ArrayList<Object>) myDoc.get(DatabaseConstants.FAVORITECOLUMN);
			for (int i=0; i<favoriteList.size(); i++){
				// searching the document in the favoriteList saved in MongoDB and delete when it found it 
				if (((Document)favoriteList.get(i)).get(DatabaseConstants.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
					favoriteList.remove(i);
					break;
				}
			}
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(DatabaseConstants.FAVORITECOLUMN, favoriteList));
			BasicDBObject searchQuery = new BasicDBObject().append(DatabaseConstants.USERNAMECOLUMN, username);
			coll.updateOne(searchQuery, newDocument);
	}
	
	/**
	 * get all the favorites of a user
	 * @param username of the user
	 * @return an array list of all the favorites document of the user. Return null if there's an error.
	 */
	public static ArrayList<String> getFavorites(String username){
			getInstance();
			BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
			Document myDoc = coll.find(doc).first();
			if (myDoc!=null){
				ArrayList<Object> favoritesListDB = (ArrayList<Object>) myDoc.get(DatabaseConstants.FAVORITECOLUMN);
				if (favoritesListDB != null){
					ArrayList<String> favoritesList = new ArrayList<String>();
					for (int i=0 ; i<favoritesListDB.size(); i++){
						favoritesList.add(((Document)favoritesListDB.get(i)).get(DatabaseConstants.DOCUMENTIDCOLUMN).toString());
					}
					return favoritesList;
				}else{
					return new ArrayList<String>();
				}
			}else{
				return new ArrayList<String>();
			}
	}
	
	/**
	 * Delete all favorites of a user without deleting also his likes
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONMONGODB if the mongoDB isn't running	
	 */
	public static int removeFavorites(String username){
			getInstance();	
			BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
			Document myDoc = coll.find(doc).first();
			if (myDoc != null){
					ArrayList<Object> favoritesList = (ArrayList<Object>) myDoc.get(DatabaseConstants.FAVORITECOLUMN);
					if (favoritesList!=null){
						favoritesList = new BasicDBList();
						BasicDBObject newDocument = new BasicDBObject();					 
						newDocument.append("$set", new BasicDBObject().append(DatabaseConstants.FAVORITECOLUMN, favoritesList));
						coll.updateOne(doc, newDocument);
					}
					return CodesReturned.ALLOK;
			}else{
				return CodesReturned.ALLOK;
			}
	}
	
	/**
	 * change the database of Favorites and Likes
	 * @param db the new database
	 */
	public static void setFavoriteDB(String db){
		FavoriteService.FAVORITEDB = db;
	}

	/**
	 * Remove a user from the collection favorites. This will delete his likes and his favorites
	 * @param username
	 * @return 
	 */
	public static int removeUserFromFavoriteDB(String username){
			BasicDBObject doc = new BasicDBObject(DatabaseConstants.USERNAMECOLUMN, username);
			coll.deleteOne(doc);
			return CodesReturned.ALLOK;
	}

	/**
	 * Get the instance
	 */
	private static FavoriteService getInstance(){
		if (instance==null){
			return instance = new FavoriteService(); 
		}else{
			return instance;
		}
	}
	
	private FavoriteService(){
		coll = MongoDBContextListerner.getInstance().getDatabase(FavoriteService.FAVORITEDB).getCollection(DatabaseConstants.LIKESCOLUMN);
	}
}
