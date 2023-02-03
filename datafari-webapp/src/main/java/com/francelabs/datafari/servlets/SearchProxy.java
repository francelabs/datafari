/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.api.SuggesterAPI;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchProxy/*")
public class SearchProxy extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(SearchProxy.class.getName());

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    String searchResponse;
    if (request.getParameter("action") != null) {
      switch (request.getParameter("action")) {
      case "suggest":
        searchResponse = SuggesterAPI.suggest(request).toJSONString();
        break;
      case "search":
      default:
        searchResponse = SearchAPI.search(request).toJSONString();
      }
    } else {
      searchResponse = SearchAPI.search(request).toJSONString();
    }
    final String wrapperFunction = request.getParameter("json.wrf");
    if (wrapperFunction != null) {
      searchResponse = wrapperFunction + "(" + searchResponse + ")";
    }
    response.setStatus(200);
    response.setCharacterEncoding("utf-8");
    response.setContentType("text/json;charset=utf-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
    response.getWriter().write(searchResponse);

  }

}