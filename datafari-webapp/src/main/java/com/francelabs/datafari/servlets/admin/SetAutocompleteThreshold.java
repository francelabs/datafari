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
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
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
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    if (request.getParameter("autocompleteThreshold") == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "autocompleteThreshold parameter not provided");
    } else {

      try {
        SolrAPI.setUserProp(Core.FILESHARE.toString(), "autocomplete.threshold", request.getParameter("autocompleteThreshold"));
        List<String> collectionsList = null;
        final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
        if (!config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).equals("")) {
          collectionsList = Arrays.asList(config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).split(","));
        }
        if (collectionsList != null) {
          for (String object: collectionsList) {
            //SolrAPI.setAutocompleteThreshold(object,Double.parseDouble(request.getParameter("autocompleteThreshold")));
            SolrAPI.setUserProp(object, "autocomplete.threshold", request.getParameter("autocompleteThreshold"));
          }
        }

        jsonResponse.put("code", CodesReturned.ALLOK.getValue());
      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
        logger.error("Solr API setAutocompleteThreshold request error", e);
      }
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }
}
