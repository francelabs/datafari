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

import org.apache.commons.lang.WordUtils;

import java.util.Locale;

/**
 * <p>PageType class</p>
 * <p>Represents the kind of pages we can have in Confluence</p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 */
public enum PageType {

  PAGE, ATTACHMENT, BLOGPOST;
  
  public static PageType fromName(String type) {
    for(PageType pageType: values()) {
      if(pageType.name().equalsIgnoreCase(type)) {
        return pageType;
      }
    }
    
    return PageType.PAGE;
  }
  
  public String toString() {
    return WordUtils.capitalize(name().toLowerCase(Locale.ROOT));
  }
}
