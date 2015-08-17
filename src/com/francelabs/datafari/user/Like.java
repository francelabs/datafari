package com.francelabs.datafari.user;

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
import com.francelabs.datafari.user.CodesUser.*;

public class Like {

	private static Like instance;
	private static MongoCollection<Document> coll;
	
	/**
	 * Add a document to the list of documents liked by the user
	 * @param username of the user
	 * @param idDocument the id that should be liked
	 * @return Like.ALREADYPERFORMED if the like was already done, CodesUser.ALLOK 
	 * if all was ok and CodesUser.PROBLEMCONNECTIONMONGODB if there's an error
	 */
	public static int addLike(String username, String idDocument){
		BasicDBObject doc = new BasicDBObject(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first();
			if (myDoc != null){
				ArrayList<Object> likeList = (ArrayList<Object>) myDoc.get(StringsUser.LIKESCOLUMN);
				if (likeList!=null)
					for (int i=0; i<likeList.size(); i++){
						if (((Document)likeList.get(i)).get(StringsUser.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
							// the user has already liked the document
							return CodesUser.ALREADYPERFORMED;
						}
					}
				else
					likeList = new BasicDBList();
				// adding the document to the Like list 
				likeList.add(new BasicDBObject(StringsUser.DOCUMENTIDCOLUMN, idDocument));
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.append("$set", new BasicDBObject().append(StringsUser.LIKESCOLUMN, likeList));
			 
				BasicDBObject searchQuery = new BasicDBObject().append(StringsUser.USERNAMECOLUMN, username);
				coll.updateOne(searchQuery, newDocument);
			}else{
				// if the username isn't in the collection StringsUser.FAVORITEDB yet 
				BasicDBList dbList = new BasicDBList();
				dbList.add(new BasicDBObject(StringsUser.DOCUMENTIDCOLUMN,idDocument));
				Document docAdd = new Document(StringsUser.USERNAMECOLUMN, username)
				.append(StringsUser.LIKESCOLUMN,new BasicDBList())
				.append(StringsUser.LIKESCOLUMN, dbList );
				coll.insertOne(docAdd);	
			}
			return CodesUser.ALLOK;
		}
		return CodesUser.PROBLEMCONNECTIONMONGODB;
	}
	
	
	/**
	 * unlike a document 
	 * @param username of the user who unlike a document
	 * @param idDocument the id that should be unliked
	 * @return Like.ALREADYPERFORMED if the like was already done, Like.ALLOK 
	 * if all was ok and Like.CodesUser.PROBLEMCONNECTIONMONGODB if there's an error
	 */
	public static int unlike(String username, String idDocument){
		BasicDBObject doc = new BasicDBObject(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first();
			//getting the document liked by the username
			ArrayList<Object> likeList = (ArrayList<Object>) myDoc.get(StringsUser.LIKESCOLUMN);
			boolean deleted = false;
			for (int i=0; i<likeList.size(); i++){
				if (((Document)likeList.get(i)).get(StringsUser.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
					likeList.remove(i);
					deleted = true;
					break;
				}
			}
			if (deleted){
				// if we found the document in the list and we removed it from the list
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.append("$set", new BasicDBObject().append(StringsUser.LIKESCOLUMN, likeList));
				BasicDBObject searchQuery = new BasicDBObject().append(StringsUser.USERNAMECOLUMN, username);
				coll.updateOne(searchQuery, newDocument);
				return CodesUser.ALLOK;
			}else{
				// if we didn't found it in the like List
				return CodesUser.ALREADYPERFORMED;
			}
		}
		return CodesUser.PROBLEMCONNECTIONMONGODB;
	}
	
	/**
	 * get all the likes of a user
	 * @param username of the user
	 * @return an array list of all the the likes of the user. Return null if there's an error.
	 */
	public static ArrayList<String> getLikes(String username){
		Document doc = new Document(StringsUser.USERNAMECOLUMN, username);
		if (getInstance().coll!=null){
			Document myDoc = coll.find(doc).first();
			if (myDoc!=null){
				ArrayList<Document> likesListDB = (ArrayList<Document>) myDoc.get(StringsUser.LIKESCOLUMN);
				if (likesListDB != null){
					ArrayList<String> likesList = new ArrayList<String>();
					for (int i=0 ; i<likesListDB.size(); i++){
						//constructing the likesList
						Document tmp =((Document)likesListDB.get(i));
						likesList.add(tmp.get(StringsUser.DOCUMENTIDCOLUMN).toString());
					}
					return likesList;
				}
			}
		}
		return null;
	}


	/**
	 * Get the instance
	 */
	private static Like getInstance(){
		// we need to take a new instance of MongoDBRuning every time we use this function
		// because we need to check the connexion to MongoDB
		return instance = new Like();
	}
	
	private Like(){
		MongoDBRunning mongoDBRuning = new MongoDBRunning(Favorite.FAVORITEDB);
		MongoDatabase db = mongoDBRuning.getDb();
		Like.coll = null;
		if (db!=null)
			Like.coll = db.getCollection(StringsUser.FAVORTIECOLLECTION);
		}
}
