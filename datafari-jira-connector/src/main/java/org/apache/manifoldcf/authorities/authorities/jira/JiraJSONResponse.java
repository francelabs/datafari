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

import org.json.simple.JSONObject;

/** An instance of this class represents a Jira JSON object, and the parser hooks
* needed to understand it.
*
* If we needed streaming anywhere, this would implement org.json.simple.parser.ContentHandler,
* where we would extract the data from a JSON event stream.  But since we don't need that
* functionality, instead we're just going to accept an already-parsed JSONObject.
*
* This class is meant to be overridden (selectively) by derived classes.
*/
public class JiraJSONResponse {

  protected Object object = null;

  public JiraJSONResponse() {
  }
  
  /** Receive a parsed JSON object.
  */
  public void acceptJSONObject(Object object) {
    this.object = object;
  }
  
}
