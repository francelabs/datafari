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
package com.francelabs.datafari.servlets.admin;

import java.io.File;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;
import com.francelabs.datafari.utils.SolrAPI;

/**
 * Javadoc
 *
 * This servlet is used to see the various fields or modify the weight of those fields of a Solr core It is called by the FieldWeight.html, IndexField.html and FacetConfig. doGet is used to get the
 * fields and the informations about the fields, also used to clean the semaphore doPost is used to modify the weight of a field The semaphores (one for each type of query) are created in the
 * constructor.
 *
 *
 */
@WebServlet("/admin/FieldWeightAPI")
public class FieldWeightAPI extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private final String env;
  private NodeList childList;
  private Document doc;
  private File config = null;
  /** Custom fields. **/
  private File customFields = null;
  /** Custom requestHandlers. **/
  private File customSearchHandler = null;
  /** Using Custom Search Handler. **/
  private final boolean usingCustom = false;
  /** Search Handler. **/
  private final Node searchHandler = null;
  private final static Logger LOGGER = LogManager.getLogger(FieldWeightAPI.class.getName());
  private static String mainCollection = "FileShare";

  /**
   * @see HttpServlet#HttpServlet() Gets the path Create the semaphore Checks if the required files exist
   */
  public FieldWeightAPI() {

    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + File.separator + "solr" + File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + "conf";

    if (new File(env + File.separator + "solrconfig.xml").exists()) {
      config = new File(env + File.separator + "solrconfig.xml");
    }

    if (new File(env + File.separator + "customs_schema" + File.separator + "custom_fields.incl").exists()) {
      customFields = new File(env + File.separator + "customs_schema" + File.separator + "custom_fields.incl");
    }

    if (new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl").exists()) {
      customSearchHandler = new File(env + File.separator + "customs_solrconfig" + File.separator + "custom_search_handler.incl");
    }

    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION) != null)
      mainCollection = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION);

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response) Used to free a semaphore on an other select without any confirl Checks if the files still exist Gets the list of
   *      the fields Gets the weight of a field in a type of query
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    String queryFields = "";
    String phraseFields = "";
    JSONObject jsonresponse = new JSONObject();

    try {
      jsonresponse = SolrAPI.readConfig(Core.FILESHARE.toString());
      queryFields = SolrAPI.getFieldsWeight(jsonresponse);
      phraseFields = SolrAPI.getPhraseFieldsWeight(jsonresponse);
      LOGGER.debug("fieldWeight" + jsonresponse.toJSONString());
      // Write the values to the response object and send
      jsonResponse.put("qfAPI", queryFields);
      jsonResponse.put("pfAPI", phraseFields);
      jsonResponse.put("code", CodesReturned.ALLOK.getValue());
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
      LOGGER.error("Solr API getFieldsWeight and/or getPhraseFieldsWeight request error", e);
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response) Checks if the files still exist Used to modify the weight of a field
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    try {
      SolrAPI.setFieldsWeight(Core.FILESHARE.toString(),request.getParameter("qfAPI"));
      SolrAPI.setPhraseFieldsWeight(Core.FILESHARE.toString(),request.getParameter("pfAPI"));

      List<String> collectionsList = null;

      final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
      if (!config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).equals("")) {
        collectionsList = Arrays.asList(config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).split(","));
      }

      if (collectionsList != null) {
        for (String object: collectionsList) {
          SolrAPI.setFieldsWeight(object,request.getParameter("qfAPI"));
          SolrAPI.setPhraseFieldsWeight(object,request.getParameter("pfAPI"));

        }
      }
      jsonResponse.put("code", CodesReturned.ALLOK.getValue());
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
      LOGGER.error("Solr API setFieldsWeight and/or setPhraseFieldsWeight request error", e);
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  }

}
