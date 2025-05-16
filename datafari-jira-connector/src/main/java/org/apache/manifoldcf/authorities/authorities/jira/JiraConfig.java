/* $Id$ */

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

package org.apache.manifoldcf.authorities.authorities.jira;

/** Parameters for Jira Authority.
 */
public class JiraConfig {

  public static final String CLIENT_ID_PARAM = "clientid";
  public static final String CLIENT_SECRET_PARAM = "clientsecret";
  public static final String JIRA_PROTOCOL_PARAM = "jiraprotocol";
  public static final String JIRA_HOST_PARAM = "jirahost";
  public static final String JIRA_PORT_PARAM = "jiraport";
  public static final String JIRA_PATH_PARAM = "jirapath";
  
  public static final String JIRA_PROXYHOST_PARAM = "jiraproxyhost";
  public static final String JIRA_PROXYPORT_PARAM = "jiraproxyport";
  public static final String JIRA_PROXYDOMAIN_PARAM = "jiraproxydomain";
  public static final String JIRA_PROXYUSERNAME_PARAM = "jiraproxyusername";
  public static final String JIRA_PROXYPASSWORD_PARAM = "jiraproxypassword";
  
  public static final String CLIENT_ID_DEFAULT = "";
  public static final String CLIENT_SECRET_DEFAULT = "";
  public static final String JIRA_PROTOCOL_DEFAULT = "http";
  public static final String JIRA_HOST_DEFAULT = "";
  public static final String JIRA_PORT_DEFAULT = "";
  public static final String JIRA_PATH_DEFAULT = "/rest/api/2/";
    
  public static final String JIRA_PROXYHOST_DEFAULT = "";
  public static final String JIRA_PROXYPORT_DEFAULT = "";
  public static final String JIRA_PROXYDOMAIN_DEFAULT = "";
  public static final String JIRA_PROXYUSERNAME_DEFAULT = "";
  public static final String JIRA_PROXYPASSWORD_DEFAULT = "";

}
