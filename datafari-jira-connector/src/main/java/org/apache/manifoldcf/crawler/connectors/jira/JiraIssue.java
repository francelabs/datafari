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

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/** An instance of this class represents a Jira issue, and the parser hooks
* needed to extract the data from the JSON event stream we use to parse it.
*/
public class JiraIssue extends JiraJSONResponse {

  // Specific keys we care about
  private final static String KEY_FIELDS = "fields";
  private final static String KEY_KEY = "key";
  private final static String KEY_SELF = "self";
  private final static String KEY_CREATED = "created";
  private final static String KEY_UPDATED = "updated";
  private final static String KEY_DESCRIPTION = "description";
  private final static String KEY_SUMMARY = "summary";

  public JiraIssue() {
    super();
  }

  public String getKey() {
    Object key = ((JSONObject)object).get(KEY_KEY);
    if (key == null)
      return null;
    return key.toString();
  }
  
  public String getSelf() {
    Object key = ((JSONObject)object).get(KEY_SELF);
    if (key == null)
      return null;
    return key.toString();
  }
  
  public Date getCreatedDate() {
    JSONObject fields = (JSONObject)((JSONObject)object).get(KEY_FIELDS);
    if (fields == null)
      return null;
    Object createdDate = fields.get(KEY_CREATED);
    if (createdDate == null)
      return null;
    return DateParser.parseISO8601Date(createdDate.toString());
  }
  
  public Date getUpdatedDate() {
    JSONObject fields = (JSONObject)((JSONObject)object).get(KEY_FIELDS);
    if (fields == null)
      return null;
    Object updatedDate = fields.get(KEY_UPDATED);
    if (updatedDate == null)
      return null;
    return DateParser.parseISO8601Date(updatedDate.toString());
  }
  
  public String getDescription() {
    JSONObject fields = (JSONObject)((JSONObject)object).get(KEY_FIELDS);
    if (fields == null)
      return null;
    Object description = fields.get(KEY_DESCRIPTION);
    if (description == null)
      return null;
    return description.toString();
  }
  
  public String getSummary() {
    JSONObject fields = (JSONObject)((JSONObject)object).get(KEY_FIELDS);
    if (fields == null)
      return null;
    Object summary = fields.get(KEY_SUMMARY);
    if (summary == null)
      return null;
    return summary.toString();
  }
  
  public Map<String,String[]> getMetadata() {
    Map<String,List<String>> map = new HashMap<String,List<String>>();
    JSONObject fields = (JSONObject)((JSONObject)object).get(KEY_FIELDS);
    if (fields != null)
      addMetadataToMap("", fields, map);
    
    // Now convert to a form more suited for RepositoryDocument
    Map<String,String[]> rmap = new HashMap<String,String[]>();
    for (String key : map.keySet()) {
      List<String> values = map.get(key);
      String[] valueArray = values.toArray(new String[0]);
      rmap.put(key,valueArray);
    }
    return rmap;
  }

  protected static void addMetadataToMap(String parent, Object cval, Map<String,List<String>> currentMap) {

    if (cval == null)
      return;

    // See if it is a basic type
    if (cval instanceof String || cval instanceof Number || cval instanceof Boolean) {
      List<String> current = currentMap.get(parent);
      if (current == null) {
        current = new ArrayList<String>();
        currentMap.put(parent,current);
      }
      current.add(cval.toString());
      return;
    }

    // See if it is an array
    if (cval instanceof JSONArray) {
      JSONArray ja = (JSONArray)cval;
      for (Object subpiece : ja) {
        addMetadataToMap(parent, subpiece, currentMap);
      }
      return;
    }
    
    // See if it is a JSONObject
    if (cval instanceof JSONObject) {
      JSONObject jo = (JSONObject)cval;
      String append="";
      if (parent.length() > 0) {
        append=parent+"_";
      }
      for (Object key : jo.keySet()) {
        Object value = jo.get(key);
        if (value == null) {
          continue;
        }
        String newKey = append + key;
        addMetadataToMap(newKey, value, currentMap);
      }
      return;
    }
    

    throw new IllegalArgumentException("Unknown object to addMetadataToMap: "+cval.getClass().getName());
  }

}
