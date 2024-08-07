/*******************************************************************************
 *  * Copyright 2015 France Labs
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
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;

/**
 * Servlet implementation class GetExactFields
 *
 * returns the list of fields that has a specific Solr field clone for exact match queries. This
 * list is found in the advanced-search.properties file
 */
@WebServlet("/GetExactFields")
public class GetExactFields extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetExactFields.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public GetExactFields() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String strExactFields = AdvancedSearchConfiguration.getInstance().getProperty(AdvancedSearchConfiguration.EXACT_FIELDS, "");
    final List<String> exactFieldsList = Arrays.asList(strExactFields.split(","));
    jsonResponse.put("code", CodesReturned.ALLOK.getValue());
    jsonResponse.put("exactFieldsList", exactFieldsList);
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
