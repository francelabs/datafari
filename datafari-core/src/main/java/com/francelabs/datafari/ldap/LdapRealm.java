package com.francelabs.datafari.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.ObfuscationTool;

public class LdapRealm {

  private String connectionName;
  private String connectionPassword;
  private String connectionURL;;
  private final List<String> userBases;
  private String userFilter;

  private static final Logger logger = LogManager.getLogger(LdapRealm.class);

  public LdapRealm(final JSONObject jsonConf) {
    connectionName = jsonConf.get("connectionName").toString();
    connectionPassword = jsonConf.get("connectionPassword").toString();
    connectionURL = jsonConf.get("connectionURL").toString();
    final JSONArray juserBases = (JSONArray) jsonConf.get("userBases");
    final String[] ub = (String[]) juserBases.toArray(new String[0]);
    userBases = new ArrayList<>();
    userBases.addAll(Arrays.asList(ub));
    if (jsonConf.containsKey("userFilter")) {
      userFilter = jsonConf.get("userFilter").toString();
    } else {
      userFilter = LdapUtils.baseFilter;
    }
  }

  /**
   * ActiveDirectoryRealmConf constructor
   *
   * @param connectionName         the username that will be used to connect to the Active Directory
   * @param connectionPassword     the password of the provided connectionName
   * @param isClearPassword        set to true if the connectionPassword has not already been obfuscated by the ManifoldCF obfuscation method, false otherwise
   * @param connectionURL          the connection URL to reach the Active Directory (ldap://host:port)
   * @param domainSuffix           the domain suffix to use with the Active Directory
   * @param authenticationProtocol the authentication protocol to use with the Active Directory
   * @param userSubtree            set to true if the users may have to be found deeply in the provided userBase(s)
   * @param userBases              the list of userBases (separated by char return)
   * @param userFilter             the LDAP filter to use in order to find users in the user bases
   */
  public LdapRealm(final String connectionName, final String connectionPassword, final boolean isClearPassword, final String connectionURL, final List<String> userBases, final String userFilter) {
    this.connectionName = connectionName;
    if (isClearPassword) {
      try {
        this.connectionPassword = ObfuscationTool.obfuscate(connectionPassword);

      } catch (final Exception e) {
        logger.error("MCF obfuscation error for password: " + connectionPassword);
        this.connectionPassword = connectionPassword;
      }
    } else {
      this.connectionPassword = connectionPassword;
    }
    this.connectionURL = connectionURL;
    this.userBases = userBases;
    this.userFilter = userFilter;
  }

  /**
   * ActiveDirectoryRealmConf constructor
   *
   * @param connectionName         the username that will be used to connect to the Active Directory
   * @param connectionPassword     the password of the provided connectionName
   * @param isClearPassword        set to true if the connectionPassword has not already been obfuscated by the ManifoldCF obfuscation method, false otherwise
   * @param connectionURL          the connection URL to reach the Active Directory (ldap://host:port)
   * @param domainSuffix           the domain suffix to use with the Active Directory
   * @param authenticationProtocol the authentication protocol to use with the Active Directory
   * @param userSubtree            set to true if the users may have to be found deeply in the provided userBase(s)
   * @param userFilter             the LDAP filter to use in order to find users in the user bases
   */
  public LdapRealm(final String connectionName, final String connectionPassword, final boolean isClearPassword, final String connectionURL, final String domainSuffix,
      final String authenticationProtocol, final String userSubtree, final String userFilter) {
    this.connectionName = connectionName;
    if (isClearPassword) {
      try {
        this.connectionPassword = ObfuscationTool.obfuscate(connectionPassword);

      } catch (final Exception e) {
        logger.error("MCF obfuscation error for password: " + connectionPassword);
        this.connectionPassword = connectionPassword;

      }
    } else {
      this.connectionPassword = connectionPassword;

    }
    this.connectionURL = connectionURL;
    this.userBases = new ArrayList<>();
    this.userFilter = userFilter;
  }

  public JSONObject toJson() {
    final JSONObject json = new JSONObject();
    json.put("connectionURL", connectionURL);
    json.put("connectionName", connectionName);
    json.put("connectionPassword", connectionPassword);
    json.put("userBases", userBases);
    return json;
  }

  public void addUserBase(final String userBase) {
    this.userBases.add(userBase);
  }

  public void clearUserBases() {
    this.userBases.clear();
  }

  public void addAllUserBases(final List<String> userBases) {
    this.userBases.addAll(userBases);
  }

  public String getConnectionName() {
    return connectionName;
  }

  public String getDeobfuscatedConnectionPassword() {
    try {
      return ObfuscationTool.deobfuscate(connectionPassword);
    } catch (final Exception e) {
      logger.error("MCF deobfuscation error for password");
      return connectionPassword;
    }

  }

  public String getObfuscatedConnectionPassword() {
    return connectionPassword;
  }

  public String getConnectionURL() {
    return connectionURL;
  }

  public List<String> getUserBases() {
    return userBases;
  }

  public void setConnectionName(final String connectionName) {
    this.connectionName = connectionName;
  }

  public void setConnectionPassword(final String connectionPassword) {
    try {
      this.connectionPassword = ObfuscationTool.obfuscate(connectionPassword);
    } catch (final Exception e) {
      logger.error("Unable to obfuscate the password", e);
      this.connectionPassword = "";
    }
  }

  public void setConnectionURL(final String connectionURL) {
    this.connectionURL = connectionURL;
  }

  public String getUserFilter() {
    return userFilter;
  }

  public void setUserFilter(final String userFilter) {
    this.userFilter = userFilter;
  }

}
