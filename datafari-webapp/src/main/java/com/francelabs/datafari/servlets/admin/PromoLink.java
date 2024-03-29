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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;

import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

/**
 *
 * This servlet is used to print/add/edit/delete promolinks directly from the
 * promolink core of Solr It is only called by the promolink.html doGet is used
 * to print all the promolinks of the core doPost is used to edit/add/delete a
 * promolink
 *
 * @author Alexis Karassev
 *
 */
@WebServlet("/admin/PromoLink")
public class PromoLink extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private IndexerInputDocument doc;
  private final static Logger LOGGER = LogManager.getLogger(PromoLink.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public PromoLink() {
    super();
  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response) Used to print the existing promolinks, and to check when you
   *      add a promolink if an other exist with this keyword. Makes a Solr
   *      request and put the results into a JSON file.
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    try {
      final IndexerQuery query = IndexerServerManager.createQuery();
      IndexerQueryResponse queryResponse = null;
      doc = IndexerServerManager.createDocument();
      IndexerServer server = null;
      try {
        server = IndexerServerManager.getIndexerServer(Core.PROMOLINK);
      } catch (final IOException e1) {
        final PrintWriter out = response.getWriter();
        out.append("Error while getting the Solr core, please make sure the core dedicated to PromoLinks has booted up. Error code : 69000");
        out.close();
        LOGGER.error("Error while getting the Solr core in doGet, admin servlet, make sure the core dedicated to Promolink has booted up and is still called promolink or that the code has been changed to match the changes. Error 69000 ", e1);
        return;

      }

      if (request.getParameter("title") != null) { // If the servlet has
        // been called to
        // check if there
        // was an existing
        // promolink with
        // this keyword
        query.setParam("q", request.getParameter("keyword").toString() + " \"" + request.getParameter("keyword").toString() + "\""); // set
        // the
        // keyword
        // to
        // what
        // was
        // sent

      } else { // the servlet has been called to print the existing
        // promolinks
        if (request.getParameter("keyword").equals("")) { // If nothing
          // was typed
          // into the
          // search
          // field
          query.setParam("q", "*:*"); // the query will return all the
          // promolinks
        } else {
          query.setParam("q", request.getParameter("keyword").toString() + " \"" + request.getParameter("keyword").toString() + "\""); // else
          // set
          // the
          // a
          // research
          // query
          // with
          // the
          // keyword
          // typed
          // in
          // the
          // search
          // field
        }
      }
      query.setRequestHandler("/select");
      try {
        queryResponse = server.executeQuery(query); // send the query
      } catch (SolrServerException | SolrException e) {
        final PrintWriter out = response.getWriter();
        out.append("Error getting the existing promolinks, please retry and look for special characters you could have entered in the search bar, if the problem persists contact your system administrator. Error code : 69001");
        out.close();
        LOGGER.error("Error while getting the results of the Solr Request in doGet, admin servlet. Error 69001 ", e);
        return;
      }
      response.getWriter().write(queryResponse.getStrJSONResponse());
      response.setStatus(200);
      response.setContentType("text/json;charset=UTF-8");

    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69500");
      out.close();
      LOGGER.error("Unindentified error in Admin doGet. Error 69500", e);
    }

  }

  public String formatDate(final String date, final String time) { // format
                                                                   // date to
                                                                   // the
    // format of the
    // datepicker
    if (date.equals("")) {
      return time;
    }
    return date.substring(6, 10) + "-" + date.substring(0, 2) + "-" + date.substring(3, 5) + time;
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response) Used to delete/add/edit an promolink Send request to Solr
   *      and returns nothing
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    try {

      final Enumeration<String> params = request.getParameterNames();

      IndexerServer server = null;
      try {
        server = IndexerServerManager.getIndexerServer(Core.PROMOLINK);
      } catch (final IOException e1) {
        final PrintWriter out = response.getWriter();
        out.append("Error while getting the Solr core, please make sure the core dedicated to PromoLinks has booted up. Error code : 69003");
        out.close();
        LOGGER.error("Error while getting the Solr core in doPost, admin servlet, make sure the core dedicated to Promolink has booted up and is still called promolink or that the code has been changed to match the changes. Error 69003", e1);
        return;
      }
      if (request.getParameter("title") != null && request.getParameter("keyword") != null && request.getParameter("content") != null) { // If
                                                                                                                                         // it's
                                                                                                                                         // an
        // edit or
        // an add
        final String dateB = formatDate(request.getParameter("dateB").toString(), "T00:00:00Z"), dateE = formatDate(request.getParameter("dateE").toString(), "T23:59:59Z"); // Get
        // all
        // the
        // parameters
        // &
        // format
        // the
        // Date
        doc = IndexerServerManager.createDocument();
        try {
          final Enumeration<String> parametersName = request.getParameterNames();
          while (parametersName.hasMoreElements()) {
            final String key = parametersName.nextElement();
            if (!key.equals("dateB") && !key.equals("dateE") && !key.equals("oldKey")) {
              final String value = request.getParameter(key);
              if (!value.equals("")) {
                doc.addField(key, value);
              }
            }
          }
          if (!dateB.equals("T00:00:00Z")) {
            doc.addField("dateBeginning", dateB); // add the
          }
          // Starting Date
          // (if there is
          // one) to the
          // Solrdoc
          if (!dateE.equals("T23:59:59Z")) {
            doc.addField("dateEnd", dateE); // add the ending Date
          }
          // (if there is one) to
          // the Solrdoc
          if (request.getParameter("oldKey") != null) { // If it's an
            // edit and
            // the
            // keyword
            // has been
            // changed
            if (request.getParameter("oldKey") != request.getParameter("keyword")) {
              server.deleteById(request.getParameter("oldKey").toString()); // Delete
              // the
              // previous
              // promolink
              // on
              // the
              // keyword
            }
          }
          server.deleteById(doc.getFieldValue("keyword").toString());// delete a
          // promolink
          // with
          // the
          // same
          // keyword
          // (either
          // it's
          // an
          // edit
          // with
          // the
          // same
          // keyword,
          // either
          // it's
          // an
          // add
          // with
          // a
          // keyword
          // already
          // existing
          // that
          // has
          // been
          // confirmed)
          server.pushDoc(doc); // Insert the new promolink
          server.commit();
        } catch (SolrServerException | IOException e) {
          final PrintWriter out = response.getWriter();
          out.append("Error while adding/editing a promolink, please retry, if the problem persists contact your system administrator. Error code : 69004");
          out.close();
          LOGGER.error("Error while adding/editing a promolink in the Admin Servlet doPost, check if the parameters passed are correct and if the fields described in the schema.xml is matching the Document created. Error 69004  ", e);
          return;
        }
      } else { // delete a promolink
        final String key = request.getParameter("keyword").toString();
        try {
          server.deleteById(key.toString());
          server.commit();
        } catch (final SolrServerException e) {
          final PrintWriter out = response.getWriter();
          out.append("Error while deleting a promolink, please retry, if the problem persists contact your system administrator. Error code : 69005");
          out.close();
          LOGGER.error("Error while deleting a promolink in the Admin Servlet doPost, the promolink might habe already been deleted by an other user since the opening of the promolink.html. Error 69005 ", e);
          return;
        }
      }
    } catch (final Exception e) {
      final PrintWriter out = response.getWriter();
      out.append("Something bad happened, please retry, if the problem persists contact your system administrator. Error code : 69501");
      out.close();
      LOGGER.error("Unindentified error in Admin doPost. Error 69501", e);
    }
  }
}