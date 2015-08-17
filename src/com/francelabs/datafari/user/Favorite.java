package com.francelabs.datafari.user;

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
import com.mongodb.realm.MongoDBRunning;
import com.francelabs.datafari.user.StringsUser.*;

public class Favorite {
	public static String FAVORITEDB = StringsUser.FAVORITEDB;
	private static Favorite instance;
	private static MongoCollection<Document> coll;
	
	/**
	 * Add a document to the favorites list of the user
	 * @param username of the user
	 * @param idDocument the id that should be add as a favorite
	 * @return true if it was success and false if not
	 */
	public static boolean addFavorite(String username, String idDocument){
		BasicDBObject doc = new BasicDBObject(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first(); // getting the doc of the username 
			if (myDoc != null){
				// if the doc exists in the collection (had already liked a document or saved a document as a favrorite
				ArrayList<Object> favoriteList =  (ArrayList<Object>) myDoc.get(StringsUser.FAVORITECOLUMN);
				if (favoriteList!=null){
					for (int i=0; i<favoriteList.size(); i++){
						//checking if the document is already saved as a favorite for the username
						if (((Document)favoriteList.get(i)).get(StringsUser.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
							return true;
						}
					}
				}else{
					favoriteList = new BasicDBList();
				}
				// if it wasn't saved yet by the username we added to the favorite List saved in MongoDB
				favoriteList.add(new BasicDBObject(StringsUser.DOCUMENTIDCOLUMN, idDocument));
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.append("$set", new BasicDBObject().append(StringsUser.FAVORITECOLUMN, favoriteList));
			 
				BasicDBObject searchQuery = new BasicDBObject().append(StringsUser.USERNAMECOLUMN, username);
				coll.updateOne(searchQuery, newDocument);
			}else{
				// if the username has not a doc in the database, we create one
				BasicDBList dbList = new BasicDBList();
				dbList.add(new BasicDBObject(StringsUser.DOCUMENTIDCOLUMN,idDocument));
				Document docAdd = new Document(StringsUser.USERNAMECOLUMN, username)
					.append(StringsUser.FAVORITECOLUMN,dbList )
					.append(StringsUser.LIKESCOLUMN, new BasicDBList());
				coll.insertOne(docAdd);	
			}
			return true;
		}
		// MongoDB isn't running
		return false;
	}
	
	
	/**
	 * delete a document from the favorites list of the user
	 * @param username of the user
	 * @param idDocument the id that should be deleted from the favorites
	 * @return true if it was success and false if not
	 */
	public static boolean deleteFavorite(String username, String idDocument){
		BasicDBObject doc = new BasicDBObject(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first();
			ArrayList<Object> favoriteList = (ArrayList<Object>) myDoc.get(StringsUser.FAVORITECOLUMN);
			for (int i=0; i<favoriteList.size(); i++){
				// searching the document in the favoriteList saved in MongoDB and delete when it found it 
				if (((Document)favoriteList.get(i)).get(StringsUser.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
					favoriteList.remove(i);
					break;
				}
			}
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(StringsUser.FAVORITECOLUMN, favoriteList));
			BasicDBObject searchQuery = new BasicDBObject().append(StringsUser.USERNAMECOLUMN, username);
			coll.updateOne(searchQuery, newDocument);
			return true;
		}
		// MongoDB isn't running
		return false;
	}
	
	/**
	 * get all the favorites of a user
	 * @param username of the user
	 * @return an array list of all the favorites document of the user. Return null if there's an error.
	 */
	public static ArrayList<String> getFavorites(String username){
		BasicDBObject doc = new BasicDBObject(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first();
			ArrayList<Object> favoritesListDB = (ArrayList<Object>) myDoc.get(StringsUser.FAVORITECOLUMN);
			ArrayList<String> favoritesList = new ArrayList<String>();
			for (int i=0 ; i<favoritesListDB.size(); i++){
				favoritesList.add(((Document)favoritesListDB.get(i)).get(StringsUser.DOCUMENTIDCOLUMN).toString());
			}
			return favoritesList;
		}
		return null;
	}
	/**
	 * change the database of Favorites and Likes
	 * @param db the new database
	 */
	public static void setFavoriteDB(String db){
		Favorite.FAVORITEDB = db;
	}


	/**
	 * Get the instance
	 */
	private static Favorite getInstance(){
		// we need to take a new instance of MongoDBRuning every time we use this function
		// because we need to check the connexion to MongoDB
		return instance = new Favorite();
	}
	
	private Favorite(){
		MongoDBRunning mongoDBRuning = new MongoDBRunning(FAVORITEDB);
		MongoDatabase db = mongoDBRuning.getDb();
		Favorite.coll = null;
		if (db!=null)
			Favorite.coll = db.getCollection(StringsUser.FAVORTIECOLLECTION );
		}
}
