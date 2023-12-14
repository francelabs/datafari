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

import java.util.Date;
import java.util.List;

/**
 * <p>
 * MutablePage class
 * </p>
 * <p>
 * Represents a Confluence Page which is mutable unlike {@code Page} class which can be also initialized using the PageBuilder obtained from
 * <code>Page.builder()</code> method
 * </p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 */
public class MutablePage extends Page {

  public MutablePage() {

  }
  
  public void setId(String id) {
    this.id = id;
  }

  public void setSpace(String space) {
    this.space = space;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void setUrlContext(String urlContext) {
    this.urlContext = urlContext;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setWebUrl(String webUrl) {
    this.webUrl = webUrl;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public void setType(PageType type) {
    this.type = type;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public void setCreatorUsername(String creatorUsername) {
    this.creatorUsername = creatorUsername;
  }

  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  public void setLastModifierUsername(String lastModifierUsername) {
    this.lastModifierUsername = lastModifierUsername;
  }

  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  public void setLength(long length) {
    this.length = length;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setLabels(List<Label> labels) {
    this.labels = labels;
  }

}
