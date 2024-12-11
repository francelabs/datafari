/* $Id: JDBCConstants.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package org.apache.manifoldcf.jdbc;

/** These are the constant strings needed by the Oracle connector.
*/
public class JDBCConstants
{
  public static final String _rcsid = "@(#)$Id: JDBCConstants.java 988245 2010-08-23 18:39:35Z kwright $";
  public static final String PROVIDER_ORACLE = "oracle:thin:@";
  public static final String PROVIDER_POSTGRESQL = "postgresql://";
  public static final String PROVIDER_JTDS_SQLSERVER = "jtds:sqlserver://";
  public static final String PROVIDER_MS_SQLSERVER = "sqlserver://";
  public static final String PROVIDER_SYBASE = "jtds:sybase://";
  public static final String PROVIDER_MYSQL = "mysql://";
  public static final String PROVIDER_MARIADB = "mariadb://";
  public static final String PROVIDER_CSV = "xbib:csv:";

  /** The jdbc provider parameter */
  public static String providerParameter = "JDBC Provider";
  /** The column interrogation method name parameter */
  public static String methodParameter = "JDBC column access method";
  /** The host machine config parameter */
  public static String hostParameter = "Host";
  /** The database name config parameter */
  public static String databaseNameParameter = "Database name";
  /** The raw configuration string */
  public static String driverStringParameter = "Raw driver string";
  /** For new JDBC drivers SSL certificate check is required. Set this parameter to true to trust all certificates */
  public static String sslTrustAll = "sslTrustAll";
  /** The user name config parameter */
  public static String databaseUserName = "User name";
  /** The password config parameter */
  public static String databasePassword = "Password";

  /** The node containing the identifier query */
  public static String idQueryNode = "idquery";
  /** The node containing the version query */
  public static String versionQueryNode = "versionquery";
  /** The node containing the process query */
  public static String dataQueryNode = "dataquery";
  /** The node containing the acl query */
  public static String aclQueryNode = "aclquery";
  /** The node containing an attribute query */
  public static String attributeQueryNode = "attrquery";
  /** The attribute name for an attribute query */
  public static String attributeName = "attributename";

  /** The name of the id return column */
  public static String idReturnColumnName = "lcf__id";
  /** The name of the version return column */
  public static String versionReturnColumnName = "lcf__version";
  /** The name of the url return column */
  public static String urlReturnColumnName = "lcf__url";
  /** The name of the data return column */
  public static String dataReturnColumnName = "lcf__data";
  /** The name of the content type return column */
  public static String contentTypeReturnColumnName = "lcf__contenttype";
  /** The name of the token return column */
  public static String tokenReturnColumnName = "lcf__token";
  
  /** The name of the id return variable */
  public static String idReturnVariable = "IDCOLUMN";
  /** The name of the version return variable */
  public static String versionReturnVariable = "VERSIONCOLUMN";
  /** The name of the url return variable */
  public static String urlReturnVariable = "URLCOLUMN";
  /** The name of the data return variable */
  public static String dataReturnVariable = "DATACOLUMN";
  /** The name of the content type return variable */
  public static String contentTypeReturnVariable = "CONTENTTYPE";
  /** The name of the start time variable */
  public static String startTimeVariable = "STARTTIME";
  /** The name of the end time variable */
  public static String endTimeVariable = "ENDTIME";
  /** The name of the id list */
  public static String idListVariable = "IDLIST";
  /** The name of token return variable */
  public static String tokenReturnVariable = "TOKENCOLUMN";

  /** JDBCAuthority */
  /** Query returning user Id parameter name */
  public static String databaseUserIdQuery = "User Id Query";
  /** Query returning user tokens parameter name */
  public static String databaseTokensQuery = "User Tokens Query";
  /** The name of the user name variable */
  public static String userNameVariable = "USERNAME";
  /** The name of the user id variable */
  public static String userIDVariable = "UID";
}


