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
        

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;


             
public class GenericCassandraRealm extends RealmBase {

	final static Logger logger = Logger.getLogger(GenericCassandraRealm.class.getName());

    public static final String KEYROLENAME = "datafari";
    

    // db credentials
    String defaultDbHost = "localhost";
    static String defaultDbUser = "";
    static String defaultDbPass = "";
    // use this as a default role
    static String defaultRole = "";


    // db constants
	public final static String USERCOLLECTION = "user";
	public final static String ROLECOLLECTION = "role";

	public static final String USERNAMECOLUMN = "username";
	public final static String PASSWORDCOLUMN = "password";
	public final static String ROLECOLUMN = "role";
    


	
	@Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected String getPassword(final String username) {
    	ResultSet results = CassandraDBContextListerner.getSession().execute("SELECT * FROM " + USERCOLLECTION
				+ " where " + USERNAMECOLUMN + "='" + username + "'");
		Row entry = results.one();
		if (entry == null) {
			return null;
		} else {
			return entry.getString(PASSWORDCOLUMN);
		}
    }

    public List<String> getRole(String username) {
    	List<String> roles = new ArrayList<String>();
		ResultSet results = CassandraDBContextListerner.getSession().execute("SELECT " + ROLECOLUMN + " FROM "
				+ ROLECOLLECTION + " where " + USERNAMECOLUMN + " = '" + username
				+ "'");

		for (Row row : results) {
			roles.add(row.getString(ROLECOLUMN));
		}
		return roles;
    }

    @Override
    protected Principal getPrincipal(final String username) {
	    	return (new GenericPrincipal(username,
	                getPassword(username),
	                getRole(username)));

    }

    /**
     * Digest the password using the specified algorithm and convert the result to a corresponding hexadecimal string.
     * If exception, the plain credentials string is returned.
     *
     * @param credentials Password or other credentials to use in authenticating this username
     */
    @Override
    protected String digest(String password) {
    		try {
    			MessageDigest md = MessageDigest.getInstance("SHA-256");
    			byte[] digest = md.digest(password.getBytes("UTF-8"));
    			return HexUtils.convert(digest);
    		} catch (UnsupportedEncodingException ex) {
    			return null;
    			
    		} catch (NoSuchAlgorithmException ex) {
    			return null;
    			
    		}
    	}

    @Override
	public Principal authenticate(String username, String password) {
    	String hashedPassword = digest(password);
		return super.authenticate(username,hashedPassword);
	}
    

    public String getDefaultDbHost() {
        return defaultDbHost;
    }

    public void setDefaultDbHost(String defaultDbHost) {
        this.defaultDbHost = defaultDbHost;
    }

}
