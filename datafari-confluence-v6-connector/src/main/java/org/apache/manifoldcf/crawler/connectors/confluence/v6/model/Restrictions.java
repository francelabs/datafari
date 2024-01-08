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

package org.apache.manifoldcf.crawler.connectors.confluence.v6.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.builder.ConfluenceResourceBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Restrictions extends ConfluenceResource {

  protected static final String KEY_READ = "read";
  protected static final String KEY_OPERATION = "operation";
  protected static final String KEY_RESTRICTIONS = "restrictions";
  protected static final String KEY_USER = "user";
  protected static final String KEY_RESULTS = "results";
  protected static final String KEY_USERNAME = "username";
  protected static final String KEY_USER_KEY = "userKey";
  protected static final String KEY_DISPLAY_NAME = "displayName";
  protected static final String KEY_GROUP = "group";
  protected static final String KEY_NAME = "name";
  protected static final String KEY_TYPE = "type";
  protected static final String KEY_UPDATE = "update";
  
  protected ReadRestrictions readRestrictions;
  
  public ReadRestrictions getReadRestrictions() {
    return readRestrictions;
  }

  public Restrictions() {
    
  }
  
  public static ConfluenceResourceBuilder<? extends Restrictions> builder() {
    return new RestrictionsBuilder();
  }
  
  public class ReadRestrictions {
    protected final List<User> users = new ArrayList<User>();
    protected final List<Group> groups = new ArrayList<Group>();
    
    public List<User> getUsers() {
      return users;
    }

    public List<Group> getGroups() {
      return groups;
    }

    protected ReadRestrictions() {
      
    }
    
    protected ReadRestrictions(final JSONObject readRestrictionsJson) {
      // Get users
      if(readRestrictionsJson.get(KEY_USER) != null) {
        JSONObject jsonUsers = (JSONObject) readRestrictionsJson.get(KEY_USER);
        JSONArray usersResults = (JSONArray) jsonUsers.get(KEY_RESULTS);
        for(int i=0; i<usersResults.size(); i++) {
          JSONObject jsonUser = (JSONObject) usersResults.get(i);
          User user = User.builder().fromJson(jsonUser);
          users.add(user);
        }
      }
      
      //Get Groups
      if(readRestrictionsJson.get(KEY_GROUP) != null) {
        JSONObject jsonGroups = (JSONObject) readRestrictionsJson.get(KEY_GROUP);
        JSONArray groupsResults = (JSONArray) jsonGroups.get(KEY_RESULTS);
        for(int i=0; i<groupsResults.size(); i++) {
          JSONObject jsonGroup = (JSONObject) groupsResults.get(i);
          Group group = Group.builder().fromJson(jsonGroup);
          groups.add(group);
        }
      }
    }
  }

  public static class RestrictionsBuilder implements ConfluenceResourceBuilder<Restrictions>{

    @Override
    public Restrictions fromJson(JSONObject restrictionsJson) {
      return fromJson(restrictionsJson, new Restrictions());
    }

    @Override
    public Restrictions fromJson(JSONObject restrictionsJson, Restrictions restrictions) {
      restrictions.readRestrictions = restrictions.new ReadRestrictions(restrictionsJson);
      return restrictions;
    }

    @Override
    public Class<Restrictions> getType() {
      return Restrictions.class;
    }
    
  }
  
}
