/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.SolrAPI;
import com.francelabs.datafari.utils.SolrConfiguration;

/**
 * Servlet implementation class GetFavorites
 */
@WebServlet("/GetAutocompleteThreshold")
public class GetAutocompleteThreshold extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(GetAutocompleteThreshold.class.getName());

  private final String DEFAULT_SOLR_SERVER = "localhost";
  private final String DEFAULT_SOLR_PORT = "8983";
  private final String DEFAULT_SOLR_PROTOCOL = "http";

  private final String solrserver;
  private final String solrport;
  private final String protocol;

  /**
   * @throws IOException
   * @see HttpServlet#HttpServlet()
   */
  public GetAutocompleteThreshold() throws IOException {
    super();

    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
    solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
    protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    String autocompleteThreshold = "0";
    JSONObject jsonresponse = new JSONObject();

    try {
      jsonresponse = SolrAPI.readConfigOverlay(Core.FILESHARE.toString());
      //autocompleteThreshold = SolrAPI.getAutocompleteThreshold(jsonresponse);
      autocompleteThreshold = (SolrAPI.getUserProp(jsonresponse, "autocomplete.threshold").toString());
      // Write the values to the response object and send
      jsonResponse.put("autoCompleteThreshold", autocompleteThreshold);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
      logger.error("Solr API getAutocompleteThreshold request error", e);
    }
    // Perform retrieve operations to get the actual values

    logger.debug("overlay" + jsonresponse.toJSONString());

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

}
