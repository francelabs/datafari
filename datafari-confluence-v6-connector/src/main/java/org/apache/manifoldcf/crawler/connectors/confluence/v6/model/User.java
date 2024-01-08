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

import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.builder.ConfluenceResourceBuilder;
import org.json.simple.JSONObject;

public class User extends ConfluenceResource {

  protected static final String KEY_USERNAME = "username";
  protected static final String KEY_USER_KEY = "userKey";
  protected static final String KEY_DISPLAY_NAME = "displayName";
  protected static final String KEY_NAME = "name";
  protected static final String KEY_TYPE = "type";;
  
  protected String type;
  protected String username;
  protected String userKey;
  protected String displayName;
  
  public String getType() {
    return type;
  }

  public String getUsername() {
    return username;
  }

  public String getUserKey() {
    return userKey;
  }

  public String getDisplayName() {
    return displayName;
  }
  
  public static ConfluenceResourceBuilder<? extends User> builder() {
    return new UserBuilder();
  }

  public static class UserBuilder implements ConfluenceResourceBuilder<User>{

    @Override
    public User fromJson(JSONObject userJson) {
      return fromJson(userJson, new User());
    }

    @Override
    public User fromJson(JSONObject userJson, User user) {
      user.type = (userJson.get(KEY_TYPE)==null)?"":userJson.get(KEY_TYPE).toString();
      user.username = (userJson.get(KEY_USERNAME)==null)?"":userJson.get(KEY_USERNAME).toString();
      user.userKey = (userJson.get(KEY_USER_KEY)==null)?"":userJson.get(KEY_USER_KEY).toString();
      user.displayName = (userJson.get(KEY_DISPLAY_NAME)==null)?"":userJson.get(KEY_DISPLAY_NAME).toString();
      return user;
    }

    @Override
    public Class<User> getType() {
      return User.class;
    }
    
  }
  
}
