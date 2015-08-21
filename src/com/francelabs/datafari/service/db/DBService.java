package com.francelabs.datafari.service.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.francelabs.datafari.utils.ScriptConfiguration;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
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

	
	public void deleteObj(String id){
		BasicDBObject query = new BasicDBObject();						
		query.put("_id", new ObjectId(id));	//Create a query where we put the id of the alerts that must be deleted
		coll1.findOneAndDelete(query);	
	}

	public void addObject(Properties properties) {
		Document obj = new Document();
		for(Entry<Object, Object> entry : properties.entrySet()){
				obj.put((String) entry.getKey(), (String) entry.getValue());				//otherwise there will be an exception at the 2nd modification or at a removal after a modification.
			}	
		coll1.insertOne(obj);
	}

	public List<Properties> getAlerts() {
		List<Properties> prop = new ArrayList<Properties>();
		FindIterable<Document> cursor = coll1.find();								//Get all the existing Alerts
		for (Document alert : cursor) {
			Properties p = new Properties();
			for(Entry<String, Object> entry : alert.entrySet()){
				/*
				if (entry.getValue() instanceof ObjectId){
					p.put(entry.getKey(), ((ObjectId)entry.getValue()).) ;
					
				} else {*/
					p.put(entry.getKey(), (String)entry.getValue().toString()) ;
				//}
			}
			prop.add(p);
		}
		return prop;
	}

}
