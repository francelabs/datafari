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

package org.apache.manifoldcf.crawler.connectors.jira;

import org.apache.manifoldcf.core.common.*;
import org.apache.manifoldcf.connectorcommon.common.*;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/** An instance of this class represents the results of a Jira query, and
* the ability to parse the corresponding JSON response.
*/
public class JiraQueryResults extends JiraJSONResponse {

  // Specific keys we care about
  private final static String KEY_TOTAL = "total";
  private final static String KEY_ISSUES = "issues";
  private final static String KEY_KEY = "key";

  public JiraQueryResults() {
    super();
  }

  public Long getTotal() {
    return (Long)((JSONObject)object).get(KEY_TOTAL);
  }
  
  public void pushIds(XThreadStringBuffer seedBuffer)
    throws IOException, InterruptedException {
    JSONArray issues = (JSONArray)((JSONObject)object).get(KEY_ISSUES);
    for (Object issue : issues) {
      if (issue instanceof JSONObject) {
        JSONObject jo = (JSONObject)issue;
        seedBuffer.add(jo.get(KEY_KEY).toString());
      }
    }
  }

}
