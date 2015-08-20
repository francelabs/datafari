package com.francelabs.datafari.service.db;

import java.io.IOException;

import org.bson.Document;

import com.francelabs.datafari.utils.ScriptConfiguration;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DBService {
	
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> coll1;
	private String IP;
	private int port;
	private String database;
	private String collectionName;
	

	public static DBService getInstance() throws IOException{
		return new DBService();
	}
	
	public DBService() throws IOException{
		// Gets the address of the host
		IP = ScriptConfiguration.getProperty("HOST");

		// Gets the port
		port = Integer.parseInt(ScriptConfiguration.getProperty("PORT"));

		// Gets the name of the database
		database = ScriptConfiguration.getProperty("DATABASE");

		// Gets the name of the collection
		collectionName = ScriptConfiguration.getProperty("COLLECTION");
		mongoClient = new MongoClient(IP, port);						//Connect to the mongoDB database
		db = mongoClient.getDatabase(database);							//Switch to the right Database
		coll1 = db.getCollection(collectionName);							//Switch to the right Collection
	}

	public MongoCollection<Document> getCollection() {
		return coll1;
	}

}
