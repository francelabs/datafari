/* $Id: SolrConfig.java 991374 2010-08-31 22:32:08Z kwright $ */

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
package org.apache.manifoldcf.agents.output.solr;

/**
 * Parameters and output data for SOLR output connector.
 */
public class SolrConfig {
  public static final String _rcsid = "@(#)$Id: SolrConfig.java 991374 2010-08-31 22:32:08Z kwright $";

  // Configuration parameters

  /** Solr type */
  public static final String PARAM_SOLR_TYPE = "Solr type";
  /** Type: Standard */
  public static final String SOLR_TYPE_STANDARD = "standard";
  /** Type: Solr Cloud */
  public static final String SOLR_TYPE_SOLRCLOUD = "solrcloud";

  // SolrCloud zookeeper parameters

  // Zookeeper hosts, as nodes
  /** Zookeeper node */
  public static final String NODE_ZOOKEEPER = "zookeeper";
  /** Zookeeper hostname */
  public static final String ATTR_HOST = "host";
  /** Zookeeper port */
  public static final String ATTR_PORT = "port";

  /** Zookeeper znode path */
  public static final String PARAM_ZOOKEEPER_ZNODE_PATH = "ZooKeeper znode path";

  /** ZooKeeper client timeout */
  public static final String PARAM_ZOOKEEPER_SOCKET_TIMEOUT = "ZooKeeper socket timeout";
  /** ZooKeeper connect timeout */
  public static final String PARAM_ZOOKEEPER_CONNECTION_TIMEOUT = "ZooKeeper connection timeout";
  /** Collection name */
  public static final String PARAM_COLLECTION = "Collection";

  // General indexing parameters

  /** Protocol */
  public static final String PARAM_PROTOCOL = "Server protocol";
  /** Protocol: http */
  public static final String PROTOCOL_TYPE_HTTP = "http";
  /** Protocol: https */
  public static final String PROTOCOL_TYPE_HTTPS = "https";

  /** Server name */
  public static final String PARAM_SERVER = "Server name";
  /** Port */
  public static final String PARAM_PORT = "Server port";
  /** Connection timeout */
  public static final String PARAM_CONNECTION_TIMEOUT = "Connection timeout";
  /** Socket timeout */
  public static final String PARAM_SOCKET_TIMEOUT = "Socket timeout";
  /** Webapp */
  public static final String PARAM_WEBAPPNAME = "Server web application";
  /** Core */
  public static final String PARAM_CORE = "Solr core name";
  /** Update path */
  public static final String PARAM_UPDATEPATH = "Server update handler";
  /** Remove path */
  public static final String PARAM_REMOVEPATH = "Server remove handler";
  /** Status path */
  public static final String PARAM_STATUSPATH = "Server status handler";
  /** Id field */
  public static final String PARAM_IDFIELD = "Solr id field name";
  /** Optional original size field */
  public static final String PARAM_ORIGINALSIZEFIELD = "Solr original size field name";
  /** Optional modified date field */
  public static final String PARAM_MODIFIEDDATEFIELD = "Solr modified date field name";
  /** Optional created date field */
  public static final String PARAM_CREATEDDATEFIELD = "Solr created date field name";
  /** Optional indexed date field */
  public static final String PARAM_INDEXEDDATEFIELD = "Solr indexed date field name";
  /** Optional file name field */
  public static final String PARAM_FILENAMEFIELD = "Solr filename field name";
  /** Optional mime type field */
  public static final String PARAM_MIMETYPEFIELD = "Solr mime type field name";
  /** Optional basic auth realm */
  public static final String PARAM_REALM = "Realm";
  /** Optional user ID */
  public static final String PARAM_USERID = "User ID";
  /** Optional user password */
  public static final String PARAM_PASSWORD = "Password";
  /** Enable commits */
  public static final String PARAM_COMMITS = "Commits";
  /** Commit within time */
  public static final String PARAM_COMMITWITHIN = "Commit within";
  /** Keystore */
  public static final String PARAM_KEYSTORE = "Keystore";
  /** Maximum document length */
  public static final String PARAM_MAXLENGTH = "Maximum document length";
  /** Included mime types */
  public static final String PARAM_INCLUDEDMIMETYPES = "Included mime types";
  /** Excluded mime types */
  public static final String PARAM_EXCLUDEDMIMETYPES = "Excluded mime types";
  /** Parameter describing the use of Extract Update handler */
  public static final String PARAM_EXTRACTUPDATE = "Use extract update handler";
  /** Optional content field (if not using extract update handler) */
  public static final String PARAM_CONTENTFIELD = "Solr content field name";
  /** Node describing an argument */
  public static final String NODE_ARGUMENT = "argument";
  /** Attribute with the argument name */
  public static final String ATTRIBUTE_NAME = "name";
  /** Attribute with the argument value */
  public static final String ATTRIBUTE_VALUE = "value";

  // Output specification

}
