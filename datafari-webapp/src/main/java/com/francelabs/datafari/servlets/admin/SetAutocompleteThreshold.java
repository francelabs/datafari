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
package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.utils.HighlightConfiguration;
import com.francelabs.datafari.utils.SolrAPI;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/admin/SetAutocompleteThreshold")
public class SetAutocompleteThreshold extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(SetAutocompleteThreshold.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public SetAutocompleteThreshold() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    if (request.getParameter("autocompleteThreshold") != null) {
      logger.debug("threshold"+request.getParameter("autocompleteThreshold"));
      System.out.println(request.getParameter("autocompleteThreshold"));
    }
    
      
   try {
    SolrAPI.setAutocompleteThreshold(Double.parseDouble(request.getParameter("autocompleteThreshold")));
  } catch (NumberFormatException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  } catch (InterruptedException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  } catch (ParseException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
    
   
   
    
 
    
    jsonResponse.put("code", CodesReturned.ALLOK.getValue());
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
    
     
    
   
  }
}
