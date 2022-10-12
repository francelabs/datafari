package com.francelabs.datafari.servlets.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

@WebServlet("/SearchExpert/Duplicates")
public class Duplicates extends HttpServlet {

  private final static Logger LOGGER = LogManager.getLogger(Duplicates.class.getName());

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public Duplicates() {

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final JSONObject jsonResp = new JSONObject();
    final String action = request.getParameter("action");
    if (action.equals("global")) {
      String page = request.getParameter("page");
      if (page == null) {
        page = "1";
      }
      String limit = request.getParameter("limit");
      if (limit == null) {
        limit = "10";
      }
      String offset = "0";
      if (!page.equals("1")) {
        final int calcOffset = (Integer.parseInt(page) - 1) * Integer.parseInt(limit);
        offset = String.valueOf(calcOffset);
      }
      final JSONArray duplicatesList = new JSONArray();
      try {
        final IndexerServer duplicatesCore = IndexerServerManager.getIndexerServer(Core.DUPLICATES);
        final IndexerQuery query = IndexerServerManager.createQuery();
        query.addFacetField("signature");
        query.setQuery("*:*");
        query.setParam("rows", "0");
        query.setParam("facet.limit", limit);
        query.setParam("facet.offset", offset);
        final IndexerQueryResponse qResp = duplicatesCore.executeQuery(query);
        for (final IndexerFacetFieldCount facetCountValue : qResp.getFacetFields().get("signature").getValues()) {
          final String signature = facetCountValue.getName();
          final String count = String.valueOf(facetCountValue.getCount());
          // Get a random file id matching the signature
          final IndexerQuery idQuery = IndexerServerManager.createQuery();
          idQuery.setQuery("signature:" + signature);
          idQuery.setParam("rows", "1");
          idQuery.setParam("fl", "id");
          final IndexerQueryResponse idResp = duplicatesCore.executeQuery(idQuery);
          final JSONArray results = idResp.getResults();
          final String randomId = ((JSONObject) results.get(0)).get("id").toString();
          final JSONObject dupe = new JSONObject();
          dupe.put("doc", randomId);
          dupe.put("signature", signature);
          dupe.put("duplicates", count);
          duplicatesList.add(dupe);
        }
        jsonResp.put("duplicates", duplicatesList);
      } catch (final Exception e) {
        LOGGER.error(e);
        jsonResp.put("error", e.getMessage());
      }
    } else if (action.equals("details")) {
      final String signature = request.getParameter("signature");
      if (signature == null) {
        jsonResp.put("error", "Missing signature parameter");
      } else {
        String page = request.getParameter("page");
        if (page == null) {
          page = "1";
        }
        String rows = request.getParameter("rows");
        if (rows == null) {
          rows = "10";
        }
        String start = "0";
        if (!page.equals("1")) {
          final int calcOffset = (Integer.parseInt(page) - 1) * Integer.parseInt(rows);
          start = String.valueOf(calcOffset);
        }
        final List<String> listOfDuplicates = new ArrayList<String>();
        try {
          final IndexerServer duplicatesCore = IndexerServerManager.getIndexerServer(Core.DUPLICATES);
          final IndexerQuery query = IndexerServerManager.createQuery();
          query.setQuery("signature:" + signature);
          query.setParam("rows", rows);
          query.setParam("start", start);
          query.setParam("fl", "id");
          final IndexerQueryResponse qResp = duplicatesCore.executeQuery(query);
          jsonResp.put("num_found", qResp.getNumFound());
          final JSONArray results = qResp.getResults();
          for (int i = 0; i < results.size(); i++) {
            listOfDuplicates.add(((JSONObject) results.get(i)).get("id").toString());
          }
          jsonResp.put("duplicates", listOfDuplicates);
        } catch (final Exception e) {
          LOGGER.error(e);
          jsonResp.put("error", e.getMessage());
        }
      }
    }

    response.setContentType("application/json");
    final PrintWriter out = response.getWriter();
    out.print(jsonResp);
  }

}
