/*******************************************************************************
 *  * Copyright 2020 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.rest.v1_0.search;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.utils.userqueryconf.UserQueryAllConf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
public class Search extends HttpServlet {

  /**
   * Automatically generated serial ID
   */
  private static final long serialVersionUID = -7963279533577712482L;

  private static final Logger logger = LogManager.getLogger(Search.class.getName());

  @GetMapping(value = "/rest/v1.0/search/*", produces = "application/json;charset=UTF-8")
  protected String performSearch(final HttpServletRequest request, final HttpServletResponse response) {
    try {
      if (request.getParameter("id") == null) {
        final UUID id = UUID.randomUUID();
        request.setAttribute("id", id.toString());
      }

      UserQueryAllConf.apply(request);

      final JSONObject jsonResponse = SearchAggregator.doGetSearch(request, response);
      // Check if we get a code, if this is the case, we got an error
      // We will throw an internal error exception with the message if there is one
      final Long code = (Long) jsonResponse.get("code");
      if (code != null) {
        final String message = (String) jsonResponse.get("message");
        if (message != null) {
          throw new InternalErrorException(message);
        } else {
          throw new InternalErrorException("Error while performing the search request.");
        }
      }
      return jsonResponse.toJSONString();
    } catch (ServletException | IOException e) {
      throw new InternalErrorException("Error while performing the search request.");
    }
  }

  @PostMapping("/rest/v1.0/search/*")
  protected void stopSearch(final HttpServletRequest request, final HttpServletResponse response) {
    try {
      SearchAggregator.doPostSearch(request, response);
    } catch (ServletException | IOException e) {
      throw new InternalErrorException("Error while stopping the search request.");
    }
  }
}