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
package com.francelabs.datafari.realm;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

public class GenericCassandraRealm extends RealmBase {

  final static Logger logger = LogManager.getLogger(GenericCassandraRealm.class.getName());

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
  protected String getPassword(final String username) {
    final ResultSet results = CassandraDBContextListerner.getSession()
        .execute("SELECT * FROM " + USERCOLLECTION + " where " + USERNAMECOLUMN + "='" + username + "'");
    final Row entry = results.one();
    if (entry == null) {
      return null;
    } else {
      return entry.getString(PASSWORDCOLUMN);
    }
  }

  public List<String> getRole(final String username) {
    final List<String> roles = new ArrayList<String>();
    final ResultSet results = CassandraDBContextListerner.getSession().execute(
        "SELECT " + ROLECOLUMN + " FROM " + ROLECOLLECTION + " where " + USERNAMECOLUMN + " = '" + username + "'");

    for (final Row row : results) {
      roles.add(row.getString(ROLECOLUMN));
    }
    return roles;
  }

  @Override
  protected Principal getPrincipal(final String username) {
    return (new GenericPrincipal(username, getPassword(username), getRole(username)));

  }

  /**
   * Digest the password using the specified algorithm and convert the result to a
   * corresponding hexadecimal string. If exception, the plain credentials string
   * is returned.
   *
   * @param credentials Password or other credentials to use in authenticating
   *                    this username
   */
  protected String digest(final String password) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] digest = md.digest(password.getBytes("UTF-8"));
      return HexUtils.convert(digest);
    } catch (final UnsupportedEncodingException ex) {
      return null;

    } catch (final NoSuchAlgorithmException ex) {
      return null;

    }
  }

  @Override
  public Principal authenticate(final String username, final String password) {
    final String hashedPassword = digest(password);
    return super.authenticate(username, hashedPassword);
  }

  public String getDefaultDbHost() {
    return defaultDbHost;
  }

  public void setDefaultDbHost(final String defaultDbHost) {
    this.defaultDbHost = defaultDbHost;
  }

}
