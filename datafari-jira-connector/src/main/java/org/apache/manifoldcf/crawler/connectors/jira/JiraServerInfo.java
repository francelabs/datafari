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

package org.apache.manifoldcf.crawler.connectors.jira;

import org.json.simple.JSONObject;

/** An instance of this class represents the ServerInfo, but is used only to get the baseUrl.
 * Other fields that are available: versionNumbers, buildDate, serverTime, scmInfo, serverTitle
*/
public class JiraServerInfo extends JiraJSONResponse {
	
  // Specific keys we care about
  private final static String BASEURL = "baseUrl";
  private final static String BUILDNUMBER = "buildNumber";
  private final static String VERSION = "version";

  public JiraServerInfo() {
    super();
  }

  public String getBaseUrl() {
    return (String)((JSONObject)object).get(BASEURL);
  }

  public Long getBuildNumber() {
    return (Long)((JSONObject)object).get(BUILDNUMBER);
  }
  
  public String getVersion() {
    return (String)((JSONObject)object).get(VERSION);
  }
  
}
