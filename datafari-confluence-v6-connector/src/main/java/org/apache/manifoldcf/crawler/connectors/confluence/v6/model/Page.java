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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.manifoldcf.core.common.DateParser;
import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.builder.ConfluenceResourceBuilder;
import org.json.simple.JSONObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * <p>
 * Page class
 * </p>
 * <p>
 * Represents a Confluence Page
 * </p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 */
public class Page extends ConfluenceResource{

  protected static final String KEY_LINKS = "_links";
  protected static final String KEY_ID = "id";
  protected static final String KEY_SELF = "self";
  protected static final String KEY_WEBUI = "webui";
  protected static final String KEY_BASE = "base";
  protected static final String KEY_CONTEXT = "context";
  protected static final String KEY_KEY = "key";
  protected static final String KEY_TITLE = "title";
  protected static final String KEY_BODY = "body";
  protected static final String KEY_VIEW = "view";
  protected static final String KEY_VALUE = "value";
  protected static final String KEY_SPACE = "space";
  protected static final String KEY_HISTORY = "history";
  protected static final String KEY_CREATED_DATE = "createdDate";
  protected static final String KEY_CREATED_BY = "createdBy";
  protected static final String KEY_BY = "by";
  protected static final String KEY_TYPE = "type";
  protected static final String KEY_DISPLAY_NAME = "displayName";
  protected static final String KEY_USER_NAME = "username";
  protected static final String KEY_VERSION = "version";
  protected static final String KEY_WHEN = "when";
  protected static final String KEY_MEDIATYPE = "mediaType";

  private static final String PAGE_ID = "confluenceId";
  private static final String PAGE_URL = "url";
  private static final String PAGE_WEBURL = "webUrl";
  private static final String PAGE_LAST_MODIFIED = "lastModified";
  private static final String PAGE_CREATOR = "creator";
  private static final String PAGE_CREATOR_USERNAME = "creatorUsername";
  private static final String PAGE_LAST_MODIFIER = "lastModifier";
  private static final String PAGE_LAST_MODIFIER_USERNAME = "lastModifierUsername";
  private static final String PAGE_SIZE = "size";
  private static final String PAGE_LABEL = "label";

  protected String id;
  protected String space;
  protected String baseUrl;
  protected String urlContext;
  protected String url;
  protected String webUrl;
  protected Date createdDate;
  protected Date lastModified;
  protected PageType type;
  protected String title;
  protected int version;
  protected String creator;
  protected String creatorUsername;
  protected String lastModifier;
  protected String lastModifierUsername;
  protected String mediaType = "text/html; charset=utf-8";
  protected long length;
  protected String content;
  protected List<Label> labels = Lists.newArrayList();

  @SuppressWarnings("unused")
  private JSONObject delegated;

  public Page() {

  }

  public String getContent() {
    return this.content;
  }

  public String getId() {
    return this.id;
  }

  public PageType getType() {
    return this.type;
  }

  public String getMediaType() {
    return this.mediaType;
  }

  public int getVersion() {
    return this.version;
  }

  public String getTitle() {
    return this.title;
  }

  public String getBaseUrl() {
    return this.baseUrl;
  }

  public String getUrlContext() {
    return this.urlContext;
  }

  public String getWebUrl() {
    return this.webUrl;
  }

  public String getUrl() {
    return this.url;
  }

  public String getSpace() {
    return this.space;
  }

  public String getCreator() {
    return this.creator;
  }

  public String getCreatorUsername() {
    return this.creatorUsername;
  }

  public String getLastModifier() {
    return this.lastModifier;
  }

  public String getLastModifierUsername() {
    return this.lastModifierUsername;
  }

  public Date getCreatedDate() {
    return this.createdDate;
  }

  public Date getLastModifiedDate() {
    return this.lastModified;
  }

  public long getLength() {
    return this.length;
  }

  public boolean hasContent() {
    return this.length > 0 && this.content != null;
  }
  
  public InputStream getContentStream() {
    String contentStream = content != null ? content : "";
    return new ByteArrayInputStream(
        contentStream.getBytes(StandardCharsets.UTF_8));
  }

  public List<Label> getLabels() {
    return this.labels;
  }
  
  public Map<String, Object> getMetadataAsMap() {
    Map<String, Object> pageMetadata = Maps.newHashMap();
    pageMetadata.put(KEY_ID,  this.id);
    pageMetadata.put(PAGE_ID, this.id);
    pageMetadata.put(KEY_TYPE, this.type.toString());
    pageMetadata.put(KEY_TITLE, this.title);
    pageMetadata.put(KEY_SPACE, this.space);
    pageMetadata.put(PAGE_URL, this.url);
    pageMetadata.put(PAGE_WEBURL, this.webUrl);
    pageMetadata.put(KEY_CREATED_DATE,
        DateParser.formatISO8601Date(this.createdDate));
    pageMetadata.put(PAGE_LAST_MODIFIED,
        DateParser.formatISO8601Date(this.lastModified));
    pageMetadata.put(KEY_MEDIATYPE, this.mediaType);
    pageMetadata.put(KEY_VERSION, String.valueOf(this.version));
    pageMetadata.put(PAGE_CREATOR, this.creator);
    pageMetadata.put(PAGE_CREATOR_USERNAME, this.creatorUsername);
    pageMetadata.put(PAGE_LAST_MODIFIER, this.lastModifier);
    pageMetadata
        .put(PAGE_LAST_MODIFIER_USERNAME, this.lastModifierUsername);
    pageMetadata.put(PAGE_SIZE, String.valueOf(this.length));
    
    putLabelsOnMetadataMap(pageMetadata);
    refineMetadata(pageMetadata);
    return pageMetadata;
  }

  /**
   * <p>Put the page labels on the metadata map</p>
   * @param pageMetadata
   */
  private void putLabelsOnMetadataMap(Map<String, Object> pageMetadata) {
    if(this.labels == null || this.labels.isEmpty()) {
      return;
    }
    
    Iterable<String> labelsString = Iterables.transform(this.labels, new Function<Label, String>() {
      @Override
      public String apply(Label input) {
        return input.getName();
      }
    });
    
    pageMetadata.put(PAGE_LABEL, Lists.newArrayList(labelsString));
    
  }

  /**
   * <p>
   * Used to be overwritten by child classes to add more metadata to the map
   * </p>
   * 
   * @param metadata
   */
  protected void refineMetadata(Map<String, Object> metadata) {
  }

  public static ConfluenceResourceBuilder<? extends Page> builder() {
    return new PageBuilder();
  }

  /**
   * <p>PageBuilder internal class</p>
   * <p>Used to build pages</p>
   * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
   *
   */
  public static class PageBuilder implements ConfluenceResourceBuilder<Page>{
    
    public Page fromJson(JSONObject jsonPage) {
      return fromJson(jsonPage, new Page());
    }
    
    public Page fromJson(JSONObject jsonPage, Page page) {

      String id = jsonPage.get(KEY_ID).toString();
      String type = jsonPage.get(KEY_TYPE).toString();
      String title = jsonPage.get(KEY_TITLE).toString();

      page.delegated = jsonPage;

      /* Init Page fields */
      page.id = id;
      page.type = PageType.fromName(type);
      page.title = title;

      page.space = processSpace(jsonPage);

      /*
        * Url & WebUrl
        */
      JSONObject links = (JSONObject) jsonPage.get(KEY_LINKS);
      if (links != null) {
        page.url = (links.get(KEY_SELF)==null)?"":links.get(KEY_SELF).toString();
        String webUrl = (links.get(KEY_WEBUI)==null)?"":links.get(KEY_WEBUI).toString();
        page.urlContext = (links.get(KEY_CONTEXT)==null)?"":links.get(KEY_CONTEXT).toString();
        page.baseUrl = (links.get(KEY_BASE)==null)?"":links.get(KEY_BASE).toString();
        page.webUrl = page.baseUrl + webUrl;

      }

      /*
        * Created By and created Date
        */
      JSONObject history = (JSONObject) jsonPage.get(KEY_HISTORY);
      if (history != null) {

        page.createdDate = DateParser.parseISO8601Date((history.get(KEY_CREATED_DATE)==null)?"":history.get(KEY_CREATED_DATE).toString());
        JSONObject createdBy = (JSONObject) history.get(KEY_CREATED_BY);
        if (createdBy != null) {
          page.creator = (createdBy.get(KEY_DISPLAY_NAME)==null)?"":createdBy.get(KEY_DISPLAY_NAME).toString();
          page.creatorUsername = (createdBy.get(KEY_USER_NAME)==null)?"":createdBy.get(KEY_USER_NAME).toString();
        }

      }

      /*
        * Last modifier and Last modified date
        */
      JSONObject version = (JSONObject) jsonPage.get(KEY_VERSION);
      if (version != null) {
        JSONObject by = (JSONObject) version.get(KEY_BY);
        if (by != null) {
          page.lastModifier = (by.get(KEY_DISPLAY_NAME)==null)?"":by.get(KEY_DISPLAY_NAME).toString();
          page.lastModifierUsername = (by.get(KEY_USER_NAME)==null)?"":by.get(KEY_USER_NAME).toString();
        }

        page.lastModified = DateParser.parseISO8601Date((version.get(KEY_WHEN)==null)?"":version.get(KEY_WHEN).toString());
      }

      /*
        * Page Content
        */
      JSONObject body = (JSONObject) jsonPage.get(KEY_BODY);
      if (body != null) {
        JSONObject view = (JSONObject) body.get(KEY_VIEW);
        if (view != null) {
          page.content = (view.get(KEY_VALUE)==null)?"":view.get(KEY_VALUE).toString();
          page.length = page.content.getBytes(StandardCharsets.UTF_8).length;
        }
      }

      return page;
    }

    private static String processSpace(JSONObject page) {
      /* Page */
      JSONObject space = (JSONObject) page.get(KEY_SPACE);
      if (space != null)
        return (space.get(KEY_KEY)==null)?"":space.get(KEY_KEY).toString();
      return "";
    }

    @Override
    public Class<Page> getType() {
      return Page.class;
    }
  }
}
