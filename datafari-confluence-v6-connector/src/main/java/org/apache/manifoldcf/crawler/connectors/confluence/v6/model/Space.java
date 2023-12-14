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

public class Space extends ConfluenceResource {

  protected static final String KEY_LINKS = "_links";
  protected static final String KEY_ID = "id";
  protected static final String KEY_SELF = "self";
  protected static final String KEY_WEBUI = "webui";
  protected static final String KEY_NAME = "name";
  protected static final String KEY_KEY = "key";
  protected static final String KEY_TYPE = "type";
  protected static final String KEY_EXPANDABLE = "_expandable";
  protected static final String KEY_METADATA = "metadata";
  protected static final String KEY_ICON = "icon";
  protected static final String KEY_DESCRIPTION = "description";
  protected static final String KEY_HOMEPAGE = "homepage";

  protected String key;
  protected String name;
  protected String type;
  protected String id;
  protected String webUrl;
  protected String metadata;
  protected String icon;
  protected String description;
  protected String homepage;
  protected String url;

  public Space() {

  }

  public String getKey() {
    return key;
  }

  public void setKey(final String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getWebUrl() {
    return webUrl;
  }

  public void setWebUrl(final String webUrl) {
    this.webUrl = webUrl;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(final String metadata) {
    this.metadata = metadata;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(final String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(final String homepage) {
    this.homepage = homepage;
  }

  public static ConfluenceResourceBuilder<? extends Space> builder() {
    return new SpaceBuilder();
  }

  public static class SpaceBuilder implements ConfluenceResourceBuilder<Space> {

    @Override
    public Space fromJson(final JSONObject spaceJson) {
      return fromJson(spaceJson, new Space());
    }

    @Override
    public Space fromJson(final JSONObject spaceJson, final Space space) {
      space.id = spaceJson.get(KEY_ID) == null ? "" : spaceJson.get(KEY_ID).toString();
      space.key = spaceJson.get(KEY_KEY) == null ? "" : spaceJson.get(KEY_KEY).toString();
      space.name = spaceJson.get(KEY_NAME) == null ? "" : spaceJson.get(KEY_NAME).toString();
      space.type = spaceJson.get(KEY_TYPE) == null ? "" : spaceJson.get(KEY_TYPE).toString();

      /*
       * metadata, icon, description & homepage
       */
      final JSONObject expandable = (JSONObject) spaceJson.get(KEY_EXPANDABLE);
      if (expandable != null) {
        space.metadata = expandable.get(KEY_METADATA) == null ? "" : expandable.get(KEY_METADATA).toString();
        space.icon = expandable.get(KEY_ICON) == null ? "" : expandable.get(KEY_ICON).toString();
        space.description = expandable.get(KEY_DESCRIPTION) == null ? "" : expandable.get(KEY_DESCRIPTION).toString();
        space.homepage = expandable.get(KEY_HOMEPAGE) == null ? "" : expandable.get(KEY_HOMEPAGE).toString();
      }

      /*
       * Url & WebUrl
       */
      final JSONObject links = (JSONObject) spaceJson.get(KEY_LINKS);
      if (links != null) {
        space.url = links.get(KEY_SELF) == null ? "" : links.get(KEY_SELF).toString();
        space.webUrl = links.get(KEY_WEBUI) == null ? "" : links.get(KEY_WEBUI).toString();
      }

      return space;
    }

    @Override
    public Class<Space> getType() {
      return Space.class;
    }

  }

}
