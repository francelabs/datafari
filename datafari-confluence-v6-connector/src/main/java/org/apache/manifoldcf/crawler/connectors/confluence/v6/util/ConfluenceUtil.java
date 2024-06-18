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

package org.apache.manifoldcf.crawler.connectors.confluence.v6.util;

/**
 * <p>Utility class for Confluence connectors</p>
 * 
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 */
public class ConfluenceUtil {

  private static final String ATTACHMENT_ID_CHARACTER = "-";
  
  /**
   * <p>Generates a repository document identifier for the specific attachment and page to be used for Repository Documents for attachment pages</p>
   * @param attachmentId
   * @param pageId
   * @return a generated 
   */
  public static String generateRepositoryDocumentIdentifier(String attachmentId, String pageId) {
    StringBuilder sb = new StringBuilder();
    sb.append(attachmentId).append("-").append(pageId);
    return sb.toString();
  }
  
  /**
   * <p>Checks if the given id is an attachment or not</p>
   * @param id
   * @return a {@code Boolean} indicating if the id is related to an attachment or not 
   */
  public static boolean isAttachment(String id) {
    return id.contains(ATTACHMENT_ID_CHARACTER);
  }
  
  /**
   * <p>Gets the attachment id and page id from a repository document id</p>
   * @param id the repository document id
   * @return an Array containing the attachment and page ids where index 0 is the attachment id and index 1 is the page id
   */
  public static String[] getAttachmentAndPageId(String id) {
    return id.split("-");
  }
}
