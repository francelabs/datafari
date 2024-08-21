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
package com.francelabs.datafari.rest.v2_0.search;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.utils.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.servlets.GetUserQueryConf;

@RestController
public class Search2 extends HttpServlet {

  /**
   * Automatically generated serial ID
   */
  private static final long serialVersionUID = -7963279533577712482L;

  private static final Logger logger = LogManager.getLogger(Search2.class.getName());

  /**
   * Check if a search session id is provided in the request, if not then create a random one. Search session id is used by Datafari to save statistics about a search session
   * (documents clicked/not clicked etc.)
   *
   * @param request the original request
   */
  private void setSearchSessionId(final HttpServletRequest request) {
    if (request.getParameter("id") == null) {
      final UUID id = UUID.randomUUID();
      request.setAttribute("id", id.toString());
    }
  }


  /**
   * Apply user's specific query config (specific boosts related to user context) on the request
   *
   * @param request the original request
   */
  private void applyUserQueryConf(final HttpServletRequest request) {
    Timer timer = new Timer(this.getClass().getName(), "applyUserQueryConf");
    final String userConf = GetUserQueryConf.getUserQueryConf(request);
    if (userConf != null && !userConf.isEmpty()) {
      final JSONParser parser = new JSONParser();
      try {
        final JSONObject jsonConf = (JSONObject) parser.parse(userConf);
        final String qf = (String) jsonConf.get("qf");
        final String pf = (String) jsonConf.get("pf");
        if (qf != null && qf.length() > 0) {
          request.setAttribute("qf", qf);
        }

        if (pf != null && pf.length() > 0) {
          request.setAttribute("pf", pf);
        }
      } catch (final ParseException e) {
        logger.warn("An issue has occured while reading user query conf", e);
      }
    }
    timer.stop();
  }

  /**
   * Check if search response contains errors and throw an {@link InternalErrorException} if it is the case
   *
   * @param searchResponse the JSONObject representing the search response to check
   */
  private void checkException(final JSONObject searchResponse) {
    // Check if we get a code, if this is the case, we got an error
    // We will throw an internal error exception with the message if there is one
    if (searchResponse.get("code") != null) {
      final String message = (String) searchResponse.get("message");
      if (message != null) {
        throw new InternalErrorException(message);
      } else {
        throw new InternalErrorException("Error while performing the search request.");
      }
    }
  }

  /**
   * Add a new 'click_url' field to result docs of the provided search response to point to the URL REST API. The URL REST API allows to keep track of clicked documents even if
   * clicked from an external UI
   *
   * @param searchResponse The search response to modify
   * @param request        The {@link HttpServletRequest} that performed the search request
   * @param searchEndpoint The search endpoint from which the provided searchResponse is issued from
   */
  private void switchDocURLToURLAPI(final JSONObject searchResponse, final HttpServletRequest request, final String searchEndpoint) {
    Timer timer = new Timer(this.getClass().getName(), "switchDocURLToURLAPI");

    final JSONObject responseObj = (JSONObject) searchResponse.get("response");
    if (responseObj != null) {
      final JSONArray docsArray = (JSONArray) responseObj.get("docs");
      for (final Object docObj : docsArray) {
        final JSONObject jsonDoc = (JSONObject) docObj;
        final String url = (String) jsonDoc.get("url");
        if (url != null && isUrlSafe(URLDecoder.decode(url, StandardCharsets.UTF_8))) {
          // temper with the URL to point on our URL endpoint
          // Also add a path array giving path information for display purposes
          final StringBuffer currentURL = request.getRequestURL();

          // Get query id if available
          String queryId = request.getParameter("id");
          if (request.getAttribute("id") != null) {
            queryId = (String) request.getAttribute("id");
          }

          String newUrl = currentURL.substring(0, currentURL.indexOf(searchEndpoint));
          newUrl += "/rest/v2.0/url?url=" + URLEncoder.encode(URLDecoder.decode(url, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
          newUrl += "&id=" + queryId;
          jsonDoc.put("click_url", newUrl);

          String newUrlFolder = currentURL.substring(0, currentURL.indexOf(searchEndpoint));
          if (url.contains("/")) {
            newUrlFolder += "/rest/v2.0/url?url=" + URLEncoder.encode(URLDecoder.decode(url.substring(0,url.lastIndexOf('/')), StandardCharsets.UTF_8), StandardCharsets.UTF_8);
          }
          else 
          {
            newUrlFolder += "/rest/v2.0/url?url=" + URLEncoder.encode(URLDecoder.decode(url, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
          }
          //newUrlFolder += "&id=" + queryId;
          jsonDoc.put("folder_url", newUrlFolder);

        } else if (url != null) {
          jsonDoc.put("click_url", url);
          jsonDoc.put("folder_url", url);
        }
      }
    }
    timer.stop();
  }


  /**
   * Check if a String (url) contains characters out of the whitelist :
   * ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=ÀÁÂàáâãäåÃÄÅ?ÈÉÊËèéêëÌÍÎÏìíîïÒÓÔòóôõöÕÖÙÚÛÜùúûüÇçÑñÆæŒœ
   * @param url A String url
   */
  private boolean isUrlSafe(String url) {
    String blacklist = "[^A-Za-z0-9-._~:/?#\\[\\]@!$%&'()*+,;={}^¨€£\"`<>|ÀÁÂàáâãäåÃÄÅÈÉÊËèéêëÌÍÎÏìíîïÒÓÔòóôõöÕÖÙÚÛÜùúûüÇçÑñÆæ ]";
    Pattern p = Pattern.compile(blacklist);
    Matcher m = p.matcher(url);
    return !m.find();
  }

  @GetMapping(value = "/rest/v2.0/search/*", produces = "application/json;charset=UTF-8")
  protected JSONObject performSearch(final HttpServletRequest request, final HttpServletResponse response) {
    Timer timer = new Timer(this.getClass().getName(), "perfomSearch");
    try {
      setSearchSessionId(request);
      applyUserQueryConf(request);

      final JSONObject jsonResponse = SearchAggregator.doGetSearch(request, response);
      checkException(jsonResponse);
      switchDocURLToURLAPI(jsonResponse, request, "/rest/v2.0/search/");
      timer.stop();
      return jsonResponse;
    } catch (ServletException | IOException e) {
      timer.stop();
      throw new InternalErrorException("Error while performing the search request.");
    }
  }

  @GetMapping(value = "/rest/v2.0/search/noaggregator/*", produces = "application/json;charset=UTF-8")
  protected JSONObject performAggregatorlessSearch(final HttpServletRequest request, final HttpServletResponse response) {
    Timer timer = new Timer(this.getClass().getName(), "performAggregatorlessSearch");
    try {
      setSearchSessionId(request);
      applyUserQueryConf(request);

      final JSONObject jsonResponse = SearchAggregator.doGetSearch(request, response, true);
      checkException(jsonResponse);
      switchDocURLToURLAPI(jsonResponse, request, "/rest/v2.0/search/noaggregator/");
      timer.stop();
      return jsonResponse;
    } catch (ServletException | IOException e) {
      timer.stop();
      throw new InternalErrorException("Error while performing the search request.");
    }
  }

  @PostMapping("/rest/v2.0/search/*")
  protected void stopSearch(final HttpServletRequest request, final HttpServletResponse response) {
    try {
      SearchAggregator.doPostSearch(request, response);
    } catch (ServletException | IOException e) {
      throw new InternalErrorException("Error while stopping the search request.");
    }
  }
}
