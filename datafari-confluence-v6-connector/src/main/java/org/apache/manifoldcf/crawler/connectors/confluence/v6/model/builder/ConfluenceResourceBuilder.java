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

package org.apache.manifoldcf.crawler.connectors.confluence.v6.model.builder;

import org.apache.manifoldcf.crawler.connectors.confluence.v6.model.ConfluenceResource;
import org.json.simple.JSONObject;

/**
 * <p>ConfluenceResourceBuilder interface</p>
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 * @param <T> Subtype of ConfluenceResource to be built
 */
public interface ConfluenceResourceBuilder<T extends ConfluenceResource> {

  /**
   * <p>Creates a T instance from a JSON representation 
   * @param jsonDocument
   * @return T instance
   */
  T fromJson(JSONObject jsonDocument);
  
  /**
   * <p>Populates the given T instance from a JSON representation and return it</p>
   * @param jsonDocument
   * @return T instance
   */
  T fromJson(JSONObject jsonDocument, T document);
  
  /**
   * <p>Returns the Class of the resource that can be built</p>
   * @return the type Class of the resource which can be built by this builder
   */
  Class<T> getType();
}
