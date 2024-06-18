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

import java.util.List;

/**
 * <p>ConfluenceUser class</p>
 * <p>Represents a Confluence user</p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 */
public class ConfluenceUser {
    private final String username;
    private final List<String> authorities;

    public ConfluenceUser(String username, List<String> authorities) {
      this.username = username;
      this.authorities = authorities;
    }

    public String getUsername() {
      return username;
    }

    public List<String> getAuthorities() {
      return authorities;
    }
  }
