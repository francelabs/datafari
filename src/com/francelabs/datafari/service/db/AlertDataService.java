package com.francelabs.datafari.service.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.francelabs.datafari.utils.ScriptConfiguration;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class AlertDataService {
	private MongoDatabase db;
	private MongoCollection<Document> alertCollection;
	private String database;


	private static AlertDataService instance;
	
	public static synchronized AlertDataService getInstance() throws IOException
	{
		if (instance == null){
			instance = new AlertDataService();
		}
		return instance;
	}
	
	public AlertDataService() throws IOException{
			// Gets the name of the database
		database = ScriptConfiguration.getProperty("DATABASE");
		// Gets the name of the collection
		db = MongoDBContextListerner.getInstance().getDatabase(database);							//Switch to the right Database
		alertCollection = db.getCollection(ScriptConfiguration.getProperty("COLLECTION"));							//Switch to the right Collection
	}

	
	public void deleteAlert(String id){
		BasicDBObject query = new BasicDBObject();						
		query.put("_id", new ObjectId(id));	//Create a query where we put the id of the alerts that must be deleted
		alertCollection.findOneAndDelete(query);	
	}

	public void addAlert(Properties properties) {
		Document obj = new Document();
		for(Entry<Object, Object> entry : properties.entrySet()){
				obj.put((String) entry.getKey(), (String) entry.getValue());				//otherwise there will be an exception at the 2nd modification or at a removal after a modification.
			}	
		alertCollection.insertOne(obj);
	}

	public List<Properties> getAlerts() {
		List<Properties> prop = new ArrayList<Properties>();
		FindIterable<Document> cursor = alertCollection.find();								//Get all the existing Alerts
		for (Document alert : cursor) {
			Properties p = new Properties();
			for(Entry<String, Object> entry : alert.entrySet()){
					p.put(entry.getKey(), (String)entry.getValue().toString()) ;
			}
			prop.add(p);
		}
		return prop;
	}

}
