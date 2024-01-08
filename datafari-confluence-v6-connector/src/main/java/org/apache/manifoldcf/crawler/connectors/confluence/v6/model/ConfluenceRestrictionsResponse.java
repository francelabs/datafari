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

public class ConfluenceRestrictionsResponse<T extends ConfluenceResource> {

  private final T result;
  private final int start;
  private final int limit;
  private final Boolean isLast;

  public ConfluenceRestrictionsResponse(final T result, final int start, final int limit, final Boolean isLast) {
    this.result = result;
    this.start = start;
    this.limit = limit;
    this.isLast = isLast;
  }

  public T getResult() {
    return this.result;
  }

  public int getStart() {
    return this.start;
  }

  public int getLimit() {
    return this.limit;
  }

  public Boolean isLast() {
    return isLast;
  }

  public static <T extends ConfluenceResource> ConfluenceRestrictionsResponse<T> fromJson(final JSONObject response, final ConfluenceResourceBuilder<T> builder) {
    final JSONObject restrictions = (JSONObject) response.get("restrictions");
    JSONObject restrictionsUser = new JSONObject();
    JSONObject restrictionsGroup = new JSONObject();
    if (restrictions.get("user") != null) {
      restrictionsUser = (JSONObject) restrictions.get("user");
    }
    if (restrictions.get("group") != null) {
      restrictionsGroup = (JSONObject) restrictions.get("group");
    }

    final T resource = builder.fromJson(restrictions);

    Boolean isLast = false;
    Boolean isLastUser = false;
    Boolean isLastGroup = false;
    int userLimit = -1;
    int groupLimit = -1;
    int userStart = -1;
    int groupStart = -1;
    if (restrictionsUser.get("limit") != null) {
      userLimit = ((Long) restrictionsUser.get("limit")).intValue();
      userStart = ((Long) restrictionsUser.get("start")).intValue();
      final int userSize = ((Long) restrictionsUser.get("size")).intValue();
      if (userSize < userLimit) {
        isLastUser = true;
      }
    } else {
      isLastUser = true;
    }

    if (restrictionsGroup.get("limit") != null) {
      groupLimit = ((Long) restrictionsGroup.get("limit")).intValue();
      groupStart = ((Long) restrictionsGroup.get("start")).intValue();
      final int groupSize = ((Long) restrictionsGroup.get("size")).intValue();
      if (groupSize < groupLimit) {
        isLastGroup = true;
      }
    } else {
      isLastGroup = true;
    }

    isLast = isLastUser && isLastGroup;

    final int limit = userLimit > -1 ? userLimit : groupLimit;
    final int start = userStart > -1 ? userStart : groupStart;

    return new ConfluenceRestrictionsResponse<T>(resource, start, limit, isLast);
  }
}
