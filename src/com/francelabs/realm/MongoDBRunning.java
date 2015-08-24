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
package com.francelabs.realm;

import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
//import org.apache.catalina.core.ApplicationContext;

public class MongoDBRunning extends GenericMongoRealm{
	
		final static Logger logger = Logger.getLogger(MongoDBRunning.class.getName());
		private static MongoClient mongoClient;
		private static String address = "localhost";
		private static int port  = 27017;
		private MongoDatabase db=null;
		private static boolean isConnected = false;
		private String authDB;
		public static String TESTCOLLECTION = "testlol1459872Running";

		/**
		 * this constructor should be used when we to test the connection to a database with the default port and the host set in the environment 
		 * @param authDB is the name of the database 
		 */
		public MongoDBRunning(String authDB){
			this.authDB = authDB;
			address = getEnvVar(envHost, defaultDbHost);
			connect();
		}

		/**
		 * this constructor should be used when we want to specify a host different from the one that we chose in the environment
		 * @param url the name of the host 
		 * @param port of the host
		 * @param authDB is the name of the database 
		 */
		public MongoDBRunning(String url,int port, String authDB){
			this.address = url;
			this.port = port;
			this.authDB = authDB;
			connect();
		}
		/**
		 * the purpose of this function is to try to connect to the database. If it success, it will set isConnected to true
		 */
		private boolean connect(){
			BasicConfigurator.configure();
			String user = getEnvVar(envUser, defaultDbUser);
		    String pass = getEnvVar(envPass, defaultDbPass);
			try{
				MongoClientOptions.Builder options= new MongoClientOptions.Builder();
				options = options.connectTimeout(1000).maxWaitTime(1).serverSelectionTimeout(3000);
				logger.debug(options.build().getConnectTimeout());
				
				if (pass!=null && user!=null && !pass.equals("") && !user.equals("") && !user.isEmpty()){
					MongoCredential credential = MongoCredential.createCredential(
							user,
	            			authDB,
	            			pass.toCharArray());
					if (mongoClient !=null){
						mongoClient.close();
						mongoClient = null;
					}
					mongoClient = new MongoClient(new ServerAddress(address,port),Arrays.asList(credential),options.build());
				}else{
					mongoClient = new MongoClient(new ServerAddress(address,port),options.build());
				}
				
				this.db = mongoClient.getDatabase(authDB);
				if (this.db.getCollection(TESTCOLLECTION)!=null)
					this.db.getCollection(TESTCOLLECTION).drop();
				else{
					this.db.createCollection(TESTCOLLECTION);
					this.db.getCollection(TESTCOLLECTION).drop();
				}
				return isConnected = true;
		        
		    }catch(com.mongodb.MongoSocketOpenException  e){
		    	db = null;
		    	this.db=null;
		    	return isConnected = false;
		    }catch(Exception e){
		    	db = null;
		    	this.db=null;
		    	e.printStackTrace();  
		    	return isConnected = false;
		    }
		}
		
		public boolean isConnected(){
			return isConnected;
		}
		
		public void setTestCollection(String testCollection){
			this.TESTCOLLECTION = testCollection;
		}
		
		public MongoDatabase getDb(){
			return db;
		}
		
		public void stopConnection(){
			mongoClient.close();
		}
}
