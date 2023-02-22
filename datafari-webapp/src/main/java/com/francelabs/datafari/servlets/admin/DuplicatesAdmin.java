package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.SolrAPI;

/**
 * Servlet implementation class getAllUsersAndRoles
 */
@WebServlet("/SearchExpert/DuplicatesAdmin")
public class DuplicatesAdmin extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(DuplicatesAdmin.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DuplicatesAdmin() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");

    try {
      final JSONObject duplicatesOverlay = SolrAPI.readConfigOverlay(Core.DUPLICATES.toString());
      final String hashFields = SolrAPI.getUserProp(duplicatesOverlay, "duplicates.hash.fields");
      final String quantRate = SolrAPI.getUserProp(duplicatesOverlay, "duplicates.quant.rate");
      jsonResponse.put("fields", hashFields);
      jsonResponse.put("quant", quantRate);
    } catch (final Exception e) {
      logger.error(e);
      jsonResponse.put("error", e.getMessage());
    }

    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    req.setCharacterEncoding("utf8");
    resp.setContentType("application/json");

    final String config = req.getParameter("config");
    if (config != null) {

      if (config.equals("algorithm")) {
        final String hashFields = req.getParameter("fields");
        final String quantRate = req.getParameter("quant");

        if (hashFields != null && quantRate != null) {
          final Map<String, String> properties = new HashMap<String, String>();
          properties.put("duplicates.hash.fields", hashFields);
          properties.put("duplicates.quant.rate", quantRate);
          try {
            SolrAPI.setUserProp(Core.DUPLICATES.toString(), properties);
          } catch (InterruptedException | ParseException e) {
            logger.error(e);
            jsonResponse.put("error", e.getMessage());
          }
        }
      }
    }

    final PrintWriter out = resp.getWriter();
    out.print(jsonResponse);
  }

}