/* $Id: Messages.java 1295926 2012-03-01 21:56:27Z kwright $ */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
* http://www.apache.org/licenses/LICENSE-2.0
 * 
* Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.manifoldcf.crawler.connectors.solr;

public class SolrIngesterConfig {

  // Repository connection Configuration parameters
  public static final String PARAM_SOLRADDRESS = "solrAddress";
  public static final String PARAM_SOLRUSERNAME = "solrUsername";
  public static final String PARAM_SOLRPASSWORD = "solrPassword";
//  public static final String PARAM_SOLRPORT = "solrPort";
  //public static final String SOLRPORTPORT_DEFAULT = "8983";
  public static final String PARAM_CONNECTIONTIMEOUT = "connectionTimeout";
  public static final String SOLRUSERNAME_DEFAULT = "";
  public static final String SOLRPASSWORD_DEFAULT = "";
  public static final String PARAM_SOCKETTIMEOUT = "socketTimeout";
  public static final String SOLRADDRESS_DEFAULT = "http://localhost:8983/solr";
  public static final String CONNECTIONTIMEOUT_DEFAULT = "60000";
  public static final String SOCKETTIMEOUT_DEFAULT = "180000";
 
  // Specification nodes and values
  public static final String NODE_FIELDMAP = "fieldmap";
  public static final String ATTRIBUTE_SOURCE = "source";
  public static final String ATTRIBUTE_TARGET = "target";
  public static final String ATTRIBUTE_VALUE = "value";
  public static final String SECURITY_ACTIVATED = "securityactivated";
  public static final String SECURITY_FIELD = "securityfield";
  public static final String SECURITY_FIELD2 = "securityfield2";
  public static final String ID_FIELD = "fieldid";
  public static final String ROWS_NUMBER = "rowsnumber";
  public static final String DATE_FIELD = "fielddate";
  public static final String CONTENT_FIELD = "fieldcontent";
  public static final String FILTER_CONDITION = "filter";
  public static final String COLLECTION_NAME = "collection";

}
