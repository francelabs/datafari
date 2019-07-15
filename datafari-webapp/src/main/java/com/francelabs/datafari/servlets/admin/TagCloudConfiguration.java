package com.francelabs.datafari.servlets.admin;

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

@WebServlet("/SearchAdministrator/tagCloudConfiguration")
public class TagCloudConfiguration extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LogManager.getLogger(SimpleEntityExtractorConfiguration.class.getName());

  private final String TAG_CLOUD_PROPERTY = "clustering.enabled";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public TagCloudConfiguration() {
    super();
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    boolean isActivated = false;
    // Perform retrieve operations to get the actual values


    JSONObject jsonresponseAPI = new JSONObject();

    try {
      jsonresponseAPI = SolrAPI.readConfigOverlay(Core.FILESHARE.toString());

      isActivated = Boolean.parseBoolean(SolrAPI.getUserProp(jsonresponseAPI, TAG_CLOUD_PROPERTY));

      LOGGER.debug("Tag cloud "+isActivated);

    } catch (final Exception e) {
      LOGGER.error("Error while querying Solr config API to get tag cloud parameters.", e);
      response.setStatus(500);
      return;
    }

    // Write the values to the response object and send
    jsonResponse.put("isActivated", isActivated);
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    try {

      SolrAPI.setUserProp(Core.FILESHARE.toString(), TAG_CLOUD_PROPERTY, request.getParameter("isActivated"));

      jsonResponse.put("code", CodesReturned.ALLOK.getValue());

    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error with SolrAPI: " + e.getMessage());
      LOGGER.error("Tag cloud configuration error ", e);
      response.setStatus(500);
      return;
    }

    // Dummy always OK response
    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    jsonResponse.put(OutputConstants.STATUS, "Configuration successfully updated.");

    // Send the response
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }


}
