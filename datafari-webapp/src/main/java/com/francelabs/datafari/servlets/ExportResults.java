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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.jxls.template.SimpleExporter;

import com.francelabs.datafari.beans.DatafariFile;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.AuthenticatedUserName;

/**
 * Servlet implementation class ExportResults
 *
 */
@WebServlet("/ExportResults")
public class ExportResults extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(ExportResults.class.getName());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ExportResults() {

  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    performExport(request, response);
  }

  public static void performExport(final HttpServletRequest request, final HttpServletResponse response) throws UnsupportedEncodingException {
    request.setCharacterEncoding("utf8");
    final String type = request.getParameter("type");

    try {
      final IndexerServer solr = IndexerServerManager.getIndexerServer(Core.FILESHARE);
      final IndexerQuery query = IndexerServerManager.createQuery();
      IndexerQueryResponse queryResponse = null;

      // Add authentication
      final String authenticatedUserName = AuthenticatedUserName.getName(request);
      if (authenticatedUserName != null) {
        query.setParam("AuthenticatedUserName", authenticatedUserName);
      }

      // Set the parameters to the Solr query
      // Set the default query to *:* if not provided
      final String q = request.getParameter("query") != null ? request.getParameter("query") : "*:*";
      query.setParam("q", q);
      if (request.getParameter("fl") != null) {
        query.setParam("fl", request.getParameter("fl"));
      }
      query.setParam("q.op", "AND");
      final String sort = request.getParameter("sort") != null ? request.getParameter("sort") : "score desc";
      query.setParam("sort", sort);
      // Set the default nummber of results to 10 if not provided
      final String nbResults = request.getParameter("nbResults") != null ? request.getParameter("nbResults") : "10";
      query.setParam("rows", nbResults);
      query.setRequestHandler("/select");
      final String[] fq = request.getParameterValues("fq[]");
      if (fq != null) {
        query.addFilterQuery(fq);
      }
      final String[] facetField = request.getParameterValues("facetField[]");
      if (facetField != null) {
        query.addFacetField(facetField);
      }
      final String[] facetQuery = request.getParameterValues("facetQuery[]");
      if (facetQuery != null) {
        for (int i = 0; i < facetQuery.length; i++) {
          query.addFacetQuery(facetQuery[i]);
        }
      }
      queryResponse = solr.executeQuery(query);

      if (queryResponse != null && queryResponse.getResults() != null) {
        final JSONArray jsonDocs = queryResponse.getResults();

        // TODO adapt your desired export format
        if (type.equalsIgnoreCase("excel")) {
          final List<DatafariFile> docsList = new ArrayList<>();
          final List<String> headers = Arrays.asList("Filename", "Last modified", "URI");

          for (int i = 0; i < jsonDocs.size(); i++) {
            final JSONObject jsonDoc = (JSONObject) jsonDocs.get(i);
            docsList.add(new DatafariFile(jsonDoc));
          }

          final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
          final String strDate = sdf.format(new Date());
          final File extractFile = File.createTempFile("export_" + strDate + "-", ".xls");
          final OutputStream os = new FileOutputStream(extractFile);

          final SimpleExporter exporter = new SimpleExporter();
          exporter.gridExport(headers, docsList, "title, last_modified, url", os);

          final String mimeType = "application/xls";
          final FileInputStream inStream = new FileInputStream(extractFile);
          // modifies response
          response.setContentType(mimeType);
          response.setContentLength((int) extractFile.length());

          // forces download
          final String headerKey = "Content-Disposition";
          final String headerValue = String.format("attachment; filename=\"%s\"", extractFile.getName());
          response.setHeader(headerKey, headerValue);

          // obtains response's output stream
          final OutputStream outStream = response.getOutputStream();

          final byte[] buffer = new byte[4096];
          int bytesRead = -1;

          while ((bytesRead = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
          }

          inStream.close();
          outStream.close();

          extractFile.delete();

        }
      }
    } catch (final Exception e) {
      logger.error("Unable to export results", e);
    }
  }

}
