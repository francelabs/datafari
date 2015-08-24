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

import java.util.ArrayList;

import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static java.util.Arrays.asList;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.user.NbLikes;
import com.francelabs.datafari.user.UserConstants;
import com.francelabs.realm.MongoDBRunning;

public class LikeService {

	private static LikeService instance;
	private static MongoCollection<Document> coll;
	

	
	/**
	 * Add a document to the list of documents liked by the user
	 * @param username of the user
	 * @param idDocument the id that should be liked
	 * @return Like.ALREADYPERFORMED if the like was already done, CodesUser.ALLOK 
	 * if all was ok and CodesUser.PROBLEMCONNECTIONMONGODB if there's an error
	 */
	public static int addLike(String username, String idDocument) throws Exception{
		getInstance();
		BasicDBObject doc = new BasicDBObject(UserConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		if (myDoc != null){
			ArrayList<Object> likeList = (ArrayList<Object>) myDoc.get(UserConstants.LIKESCOLUMN);
			if (likeList!=null)
				for (int i=0; i<likeList.size(); i++){
					if (((Document)likeList.get(i)).get(UserConstants.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
						// the user has already liked the document
						return CodesReturned.ALREADYPERFORMED;
					}
				}
			else
				likeList = new BasicDBList();
			// adding the document to the Like list 
			likeList.add(new BasicDBObject(UserConstants.DOCUMENTIDCOLUMN, idDocument));
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(UserConstants.LIKESCOLUMN, likeList));
			
			BasicDBObject searchQuery = new BasicDBObject().append(UserConstants.USERNAMECOLUMN, username);
			coll.updateOne(searchQuery, newDocument);
		}else{
			// if the username isn't in the collection StringsUser.FAVORITEDB yet 
			BasicDBList dbList = new BasicDBList();
			dbList.add(new BasicDBObject(UserConstants.DOCUMENTIDCOLUMN,idDocument));
			Document docAdd = new Document(UserConstants.USERNAMECOLUMN, username)
			.append(UserConstants.LIKESCOLUMN,new BasicDBList())
			.append(UserConstants.LIKESCOLUMN, dbList );
			coll.insertOne(docAdd);	
		}
		return CodesReturned.ALLOK;
	}
		
	
	
	
	/**
	 * unlike a document 
	 * @param username of the user who unlike a document
	 * @param idDocument the id that should be unliked
	 * @return Like.ALREADYPERFORMED if the like was already done, Like.ALLOK 
	 * if all was ok and Like.CodesReturned.PROBLEMCONNECTIONMONGODB if there's an error
	 */
	public static int unlike(String username, String idDocument) throws Exception{
		getInstance();
		BasicDBObject doc = new BasicDBObject(UserConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		//getting the document liked by the username
		ArrayList<Object> likeList = (ArrayList<Object>) myDoc.get(UserConstants.LIKESCOLUMN);
		boolean deleted = false;
		for (int i=0; i<likeList.size(); i++){
			if (((Document)likeList.get(i)).get(UserConstants.DOCUMENTIDCOLUMN).toString().equals(idDocument)){
				likeList.remove(i);
				deleted = true;
				break;
			}
		}
		if (deleted){
			// if we found the document in the list and we removed it from the list
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(UserConstants.LIKESCOLUMN, likeList));
			BasicDBObject searchQuery = new BasicDBObject().append(UserConstants.USERNAMECOLUMN, username);
			coll.updateOne(searchQuery, newDocument);
			return CodesReturned.ALLOK;
		}else{
			// if we didn't found it in the like List
			return CodesReturned.ALREADYPERFORMED;
		}
	}
	
	/**
	 * get all the likes of a user
	 * @param username of the user
	 * @return an array list of all the the likes of the user. Return null if there's an error.
	 */
	public static ArrayList<String> getLikes(String username) throws Exception{
		getInstance();
		Document doc = new Document(UserConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		if (myDoc!=null){
			ArrayList<Document> likesListDB = (ArrayList<Document>) myDoc.get(UserConstants.LIKESCOLUMN);
			ArrayList<String> likesList = new ArrayList<String>();
			for (int i=0 ; i<likesListDB.size(); i++){
				//constructing the likesList
				Document tmp =((Document)likesListDB.get(i));
				likesList.add(tmp.get(UserConstants.DOCUMENTIDCOLUMN).toString());
			}
			return likesList;
		}else{
			return new ArrayList<String>();
		}
	}

	/**
	 * get the number of Likes of each document from MongoDB. If a document is not returned, it would mean that he hasn't a like.
	 * @return array containing the id of document and the corresponding likes
	 */
	public static ArrayList<NbLikes> getNbLikes() throws Exception{
		getInstance();
		AggregateIterable<Document> iterable = coll.aggregate(asList(
			    	new Document("$project", (new Document("username", 1)).append("likes", 1)),
			        new Document("$unwind","$likes"),
			        new Document("$group",new Document("_id","$likes.document_id").append(UserConstants.NBLIKESCOLUMN,new Document("$sum",1))),
			        new Document("$project",new Document("_id",0).append(UserConstants.DOCUMENTIDCOLUMN,"$_id").append(UserConstants.NBLIKESCOLUMN,1))));
		final ArrayList<NbLikes> fetchNbLikes = new ArrayList<NbLikes> ();   
		iterable.forEach(new Block<Document>() {
		    @Override
		    public void apply(final Document document) {
		    	fetchNbLikes.add(new NbLikes(document.get(UserConstants.DOCUMENTIDCOLUMN).toString(),document.get(UserConstants.NBLIKESCOLUMN).toString())); 
		    }
		});
		return fetchNbLikes;
	}
	
	/**
	 * Delete all likes of a user without deleting also his favorites
	 * @param username
	 * @return CodesReturned.ALLOK if the operation was success and CodesReturned.PROBLEMCONNECTIONMONGODB if the mongoDB isn't running	
	 */
	public static int removeLikes(String username) throws Exception{
		getInstance();
		BasicDBObject doc = new BasicDBObject(UserConstants.USERNAMECOLUMN, username);
		Document myDoc = coll.find(doc).first();
		if (myDoc != null){
			ArrayList<Object> likeList = (ArrayList<Object>) myDoc.get(UserConstants.LIKESCOLUMN);
			if (likeList!=null){
				likeList = new BasicDBList();
				BasicDBObject newDocument = new BasicDBObject();					 
				newDocument.append("$set", new BasicDBObject().append(UserConstants.LIKESCOLUMN, likeList));
				coll.updateOne(doc, newDocument);
			}
		}
		return CodesReturned.ALLOK;
	}

	/**
	 * Get the instance
	 */
	private static LikeService getInstance(){
		// we need to take a new instance of MongoDBRuning every time we use this function
		// because we need to check the connexion to MongoDB
		if (instance == null)
			return instance = new LikeService();
		else return instance;
	}
	
	private LikeService(){
		coll = MongoDBContextListerner.getInstance().getDatabase(FavoriteService.FAVORITEDB).getCollection(DatabaseConstants.FAVORITECOLUMN);
	}
}
