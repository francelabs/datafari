package com.francelabs.realm;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.catalina.realm.RealmBase;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.*;

public class User {
	final static Logger logger = Logger.getLogger(MongoDBRunning.class.getName());
	public final static String USERNAMECOLUMN = "username";
	public final static String PASSWORDCOLUMN = "password";
	public final static String ROLECOLUMN = "role";
	public final static String IDENTIFIERSCOLLECTION = "users";
	public final static String IDENTIFIERSDB = "db-containing";
	private String algorithmHash = "SHA-256";
	private String userName;
	private String password;
	private boolean isSignedUp = false;
	private boolean isSignedIn = false;
	private String[] role;
	private MongoCollection<Document> coll;
	
	
	
	 
	public User(String username,String password, MongoDatabase db){
		this.userName = username;
		this.password = digest(password);
		if (db!=null){
			 this.coll = db.getCollection(this.IDENTIFIERSCOLLECTION);
		}	
	}
		
	/**
	 * Attempt a sign up with assigning only one role to the user
	 * @param role is the string that will be assigned to the user
	 * @return true if the signup was successful and false if not
	 */
	public boolean signup(String role){
		if (coll!=null){
			String[] roleArray = {role};
			return signup(roleArray);
		}
		return this.isSignedUp = false;
	}
		
	/**
	 * Attempt a sign up with assigning more than one role to the user
	 * @param role is the array containing the roles of the user
	 * @return true if the sign up was successful and false if not
	 */	
	public boolean signup(String[] role){
		if (coll!=null){
			this.role = role;
			Document doc = new Document(this.USERNAMECOLUMN, this.userName)
				.append(this.PASSWORDCOLUMN, this.password);
			ArrayList<Document> roleDBList =  new ArrayList<Document> ();
			for (int i = 0 ; i < role.length ; i++){
				roleDBList.add(new Document("name", role[i]));
			}
			doc.append(this.ROLECOLUMN,roleDBList);
			Document userDoc =  new Document(this.USERNAMECOLUMN,this.userName);
			if (coll.find(userDoc).first()==null){
				coll.insertOne(doc);	
				return this.isSignedUp = true;
			}else{
				return this.isSignedUp = false;
			}
		}else{
			return this.isSignedUp = false;
		}	
	}
		
	/**
	 * method used to attemp a login in the database using the attribute userName and password
	 * @return true if the login was successful and false in fail
	 */
	public boolean signIn(){
		try {
			// to have more informations, we can try to query by using only the user, so we can know 
			// in a failure if the user exist or not, and if it's the password which is incorrect 
			Document myDoc = coll.find(and(eq(this.USERNAMECOLUMN, this.userName),eq(this.PASSWORDCOLUMN, this.password))).first();
			if (myDoc != null)
				return isSignedIn = true;
			else 
				return isSignedIn = false;
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return isSignedIn = false;
		}
	}
	
	public void changePassword(String password){
		Document newDocument = new Document();
		newDocument.append("$set", new Document(this.PASSWORDCOLUMN,digest(password)));		
		Document searchQuery = new Document(this.USERNAMECOLUMN, this.userName).append(this.PASSWORDCOLUMN, this.password);
		coll.updateOne(searchQuery, newDocument);
		
	}
	
	/**
	 * Delete the user
	 * @return true if the delete was performed correctly, false if not.
	 */
	public boolean deleteUser(){
		if (coll!=null){
			Document doc = new Document(this.USERNAMECOLUMN, this.userName)
				.append(this.PASSWORDCOLUMN, this.password);		
			if (coll.deleteOne(doc).getDeletedCount()>0)
				return true;
			else
				return false;
		}else{
			return false;
		}
	}
		
		/**
		 * Returns the roles of a user
		 * @return arrayList containing the roles
		 */
	public ArrayList<String> getRoles(){
		Document doc = new Document(this.USERNAMECOLUMN, this.userName);
		// to have more informations, we can try to query by using only the user, so we can know 
		// in a failure if the user exist or not, and if it's the password which is incorrect 
		Document myDoc = coll.find(doc).first();
		if(myDoc==null)
			return null;
		ArrayList<Document> rolesList = (ArrayList<Document>) myDoc.get(this.ROLECOLUMN);
		ArrayList <String> result = new ArrayList<String>();
		for (int i = 0 ; i < rolesList.size() ; i++)
			result.add( (String)((Document)(rolesList.get(i))).get("name"));
		return result;
	}
		
		public String getPassword(){
			Document doc = new Document(this.USERNAMECOLUMN, this.userName);
			Document myDoc = coll.find(doc).first();
			if (myDoc==null)
				return null;
			String password = (String) myDoc.get(this.PASSWORDCOLUMN);
			this.password = password;
			return password;
		}
		
		public void addRole(String role){
			Document doc = new Document(this.USERNAMECOLUMN, this.userName);
			// to have more informations, we can try to query by using only the user, so we can know 
			// in a failure if the user exist or not, and if it's the password which is incorrect 
			Document myDoc = coll.find(doc).first();
			ArrayList<Object> rolesList = (ArrayList<Object>) myDoc.get(this.ROLECOLUMN);
			rolesList.add(new BasicDBObject("name", role));
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("$set", new BasicDBObject().append(this.ROLECOLUMN, rolesList));
		 
			BasicDBObject searchQuery = new BasicDBObject().append(this.USERNAMECOLUMN, this.userName).append(this.PASSWORDCOLUMN, this.password);
			coll.updateOne(searchQuery, newDocument);
		}
		
		/**
		 * Inform if the user exists already in the database
		 * @return true if is exists and false if not
		 */
		public boolean isInBase(){
			BasicDBObject doc = new BasicDBObject(this.USERNAMECOLUMN, this.userName);
			return coll.find(doc).first()!=null;
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
