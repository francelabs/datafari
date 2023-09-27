/* $Id: ActiveDirectoryConfig.java 988245 2010-08-23 18:39:35Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.authorities.authorities.activedirectory;


/** Parameters and output data for Active Directory authority.
*/
public class ActiveDirectoryConfig
{
  public static final String _rcsid = "@(#)$Id: ActiveDirectoryConfig.java 988245 2010-08-23 18:39:35Z kwright $";

  // Configuration parameters

  /** ACLs Groups Name */
  public static final String PARAM_ACLSGROUPSNAME = "ACLs Groups name";
  /** Domain controller */
  public static final String PARAM_DOMAINCONTROLLER = "Domain controller";
  /** Administrative user name */
  public static final String PARAM_USERNAME = "User name";
  /** Administrative password */
  public static final String PARAM_PASSWORD = "Password";
  /** Authentication */
  public static final String PARAM_AUTHENTICATION = "Authentication";
  /** UserACLs username attribute */
  public static final String PARAM_USERACLsUSERNAME = "UserACLs username attribute";
  /** Cache lifetime */
  public static final String PARAM_CACHELIFETIME = "Cache lifetime";
  /** Cache LRU size */
  public static final String PARAM_CACHELRUSIZE = "Cache LRU size";
  /** LDAP connection timeout*/
  public static final String PARAM_LDAPCONNECTIONTIMEOUT = "LDAP connection timeout";

  /** Domain controller node */
  public static final String NODE_DOMAINCONTROLLER = "domaincontroller";

  // Attributes

  /** Domain suffix */
  public static final String ATTR_SUFFIX = "suffix";
  /** DC server name */
  public static final String ATTR_DOMAINCONTROLLER = "domaincontroller";
  /** DC user name */
  public static final String ATTR_USERNAME = "username";
  /** DC password */
  public static final String ATTR_PASSWORD = "password";
  /** DC authentication method */
  public static final String ATTR_AUTHENTICATION = "authentication";
  /** DC user acls username attribute name */
  public static final String ATTR_USERACLsUSERNAME = "useraclsusername";

}
