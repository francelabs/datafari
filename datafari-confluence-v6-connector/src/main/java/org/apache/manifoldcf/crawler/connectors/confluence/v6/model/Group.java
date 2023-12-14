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

public class Group extends ConfluenceResource {

  protected static final String KEY_NAME = "name";
  protected static final String KEY_TYPE = "type";
  
  protected String type;
  protected String name;
  
  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }
  
  public static ConfluenceResourceBuilder<? extends Group> builder() {
    return new GroupBuilder();
  }

  public static class GroupBuilder implements ConfluenceResourceBuilder<Group>{

    @Override
    public Group fromJson(JSONObject groupJson) {
      return fromJson(groupJson, new Group());
    }

    @Override
    public Group fromJson(JSONObject groupJson, Group group) {
      group.type = (groupJson.get(KEY_TYPE)==null)?"":groupJson.get(KEY_TYPE).toString();
      group.name = (groupJson.get(KEY_NAME)==null)?"":groupJson.get(KEY_NAME).toString();
      return group;
    }

    @Override
    public Class<Group> getType() {
      return Group.class;
    }
    
  }
  
}
