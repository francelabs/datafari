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
        

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.QueryBuilder;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;

             
public class GenericMongoRealm extends RealmBase {

	//protected final Logger logger = Logger.getLogger(GenericMongoRealm.class.getName());
	final static Logger logger = Logger.getLogger(GenericMongoRealm.class.getName());

    // environment variables to take db credentials from
    public static final String envHost = "OPENSHIFT_MONGODB_DB_HOST";
    public static final String envUser = "OPENSHIFT_MONGODB_DB_USERNAME";
    public static final String envPass = "OPENSHIFT_MONGODB_DB_PASSWORD";
    public static final String KEYROLENAME = "name";

    // db credentials
    static String defaultDbHost = "localhost";
    static String defaultDbUser = "";
    static String defaultDbPass = "";
    // use this as a default role
    static String defaultRole = "";

    // db connection 
    
    private MongoDatabase db;

    // set from outside - connect to this db/connection/fields
    private String authDB = "";
    private String authCollection = "";
    private String authUserField = "";
    private String authPasswordField = "";
    private String authRoleField = "";
    
    private MongoDBRunning mongoDBRunning;

    @Override
    protected void startInternal() throws LifecycleException {
        initConnection();
        super.startInternal();
    }

    void initConnection() {
    	 BasicConfigurator.configure();
     
            logger.info("starting MongoRealm");
            String host = getEnvVar(envHost, defaultDbHost);
            String user = getEnvVar(envUser, defaultDbUser);
            String pass = getEnvVar(envPass, defaultDbPass);
            logger.info("connect to host: "+host+"/ user: "+user+" / pass set: "+!pass.isEmpty());
            /*if (!user.isEmpty()){
            	MongoCredential credential = MongoCredential.createCredential(user,
            			authDB,
            			 pass.toCharArray());
            	isMongoDBConnected = new MongoDBRunning(authDB).isConnected();
            	mongoClient = new MongoClient(new ServerAddress(host),Arrays.asList(credential));
            	if (isMongoDBConnected){
            		logger.info("realm authentication succeded");
                }
            }else{
            	mongoClient = new MongoClient(host);
            	logger.info("realm authentication ommitted as no username is given");
            }
            
            db = mongoClient.getDatabase(authDB);
            */
            mongoDBRunning = new MongoDBRunning(authDB);
        	if (mongoDBRunning.isConnected()){
        		logger.info("MongoDBRealm is connected to MongoDB");
            }else{
            	logger.error("MongoDBRealm failed to connect to MongoDB");
            }
        	db = mongoDBRunning.getDb();
    }
    @Override
    protected void stopInternal() throws LifecycleException {
        logger.info("stopping MongoRealm");
        mongoDBRunning.stopConnection();
        super.stopInternal();
    }

    protected String getEnvVar(String cfg, String def) {
        String v = System.getenv(cfg);
        return (v == null) ? def : v;
    }

	@Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected String getPassword(final String username) {
    	mongoDBRunning = new MongoDBRunning(authDB);
    	if ( mongoDBRunning.isConnected()){
	    	logger.info("getting password for {0}"+username);
	        MongoCollection<Document> collection = db.getCollection(authCollection);
	        Document result = collection.find(eq(authUserField,username)).first();
	        String password = null;
	        if (result != null) {
	            password = result.get(authPasswordField).toString();
	        }
	        return password;
        }else{
        	return null;
        }
    }

    List<String> getRole(String username) {
    	mongoDBRunning = new MongoDBRunning(authDB);
    	if ( mongoDBRunning.isConnected()){
	    	logger.info("getting role for {0}"+ username);
	        List<String> roles = new ArrayList<>();
	        if (authRoleField != null && !authRoleField.isEmpty()) {
	            MongoCollection<Document> collection = db.getCollection(authCollection);
	
	            //DBObject where = QueryBuilder.start(authUserField).in(username).get();
	            BasicDBObject where = new  BasicDBObject(authUserField,username);
	            DBObject field = QueryBuilder.start(authRoleField).is(1).get();
	            Document result = collection.find(eq(authUserField,username)).first();
	            
	            ArrayList<Object> basicDBList = (ArrayList<Object>) result.get(authRoleField);    
	           
	            if (basicDBList==null){
	            	roles.add(defaultRole);
	            }else{
		            for (int i=0; i<basicDBList.size();i++){
		            	String name = ((Document)basicDBList.get(i)).get(KEYROLENAME).toString();
		            	roles.add(name);
		            }
	            }
	            
	        } else {
	        	roles.add(defaultRole);
	        }
	        return roles;
    	}else{
    		return null;
    	}
    }

    @Override
    protected Principal getPrincipal(final String username) {
    	mongoDBRunning = new MongoDBRunning(authDB);
        if ( new MongoDBRunning(authDB).isConnected()){
	    	return (new GenericPrincipal(username,
	                getPassword(username),
	                getRole(username)));
        }else{
        	return null;
        }
    }

    /**
     * Digest the password using the specified algorithm and convert the result to a corresponding hexadecimal string.
     * If exception, the plain credentials string is returned.
     *
     * @param credentials Password or other credentials to use in authenticating this username
     */
    @Override
    protected String digest(String credentials) {

    	logger.info("--- Password:"+credentials);
        // If no MessageDigest instance is specified, return unchanged
        if (hasMessageDigest() == false) {
            return (credentials);
        }

        // Digest the user credentials and return as hexadecimal
        synchronized (this) {
            try {
                md.reset();

                byte[] bytes = null;
                try {
                    bytes = credentials.getBytes(getDigestCharset());
                } catch (UnsupportedEncodingException uee) {
                    logger.fatal("Illegal digestEncoding: " + getDigestEncoding(), uee);
                    throw new IllegalArgumentException(uee.getMessage());
                }
                md.update(bytes);

                return (HexUtils.toHexString(md.digest()));
            } catch (Exception e) {
                logger.fatal(sm.getString("realmBase.digest"), e);
                return (credentials);
            }
        }

    }
    public void closeConnexion(){
    	if (mongoDBRunning != null)
    	mongoDBRunning.closeConnexion();
    }
    
    /**
     * This function allow the admin to add a user
     *
     */

    //<editor-fold defaultstate="collapsed" desc="getter/setter">
    // #################################################################################################################

    public boolean isMongoDBConnected() {
		return mongoDBRunning.isConnected();
	}

    public String getDefaultDbHost() {
        return defaultDbHost;
    }

    public void setDefaultDbHost(String defaultDbHost) {
        this.defaultDbHost = defaultDbHost;
    }

    public String getDefaultDbUser() {
        return defaultDbUser;
    }

    public void setDefaultDbUser(String defaultDbUser) {
        this.defaultDbUser = defaultDbUser;
    }

    public String getDefaultDbPass() {
        return defaultDbPass;
    }

    public void setDefaultDbPass(String defaultDbPass) {
        this.defaultDbPass = defaultDbPass;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getAuthDB() {
        return authDB;
    }

    public void setAuthDB(String authDB) {
        this.authDB = authDB;
    }

    public String getAuthCollection() {
        return authCollection;
    }

    public void setAuthCollection(String authCollection) {
        this.authCollection = authCollection;
    }

    public String getAuthUserField() {
        return authUserField;
    }

    public void setAuthUserField(String authUserField) {
        this.authUserField = authUserField;
    }

    public String getAuthPasswordField() {
        return authPasswordField;
    }

    public void setAuthPasswordField(String authPasswordField) {
        this.authPasswordField = authPasswordField;
    }

    public String getAuthRoleField() {
        return authRoleField;
    }

    public void setAuthRoleField(String authRoleField) {
        this.authRoleField = authRoleField;
    }

//</editor-fold>
}
