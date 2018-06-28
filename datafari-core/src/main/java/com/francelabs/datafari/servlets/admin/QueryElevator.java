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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.jaxb.Elevate;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

@WebServlet("/SearchExpert/queryElevator")
public class QueryElevator extends HttpServlet {
  private final String env;
  private final String server = Core.FILESHARE.toString();
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = Logger.getLogger(QueryElevator.class.getName());
  private File elevatorFile;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public QueryElevator() {
    super();

    String environnement = Environment.getEnvironmentVariable("DATAFARI_HOME");

    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/solr/solrcloud/FileShare/conf";

    if (new File(env + "/elevate.xml").exists()) {
      elevatorFile = new File(env + "/elevate.xml");
    }
  }

  /**
   * Try to find a query tag in the provided elevate, corresponding to the
   * provided query text
   *
   * @param elevate
   *          the elevate object (JAXB representation of the elevate.xml file)
   * @param queryText
   *          the query text to search
   * @return the {@link Elevate.Query} object if found in the elevate object,
   *         null otherwise
   */
  private Elevate.Query findQuery(final Elevate elevate, final String queryText) {
    for (final Elevate.Query q : elevate.getQuery()) {
      if (q.getText().equals(queryText)) {
        return q;
      }
    }
    return null;
  }

  /**
   * Try to find the doc associated to the docId in the provided query
   *
   * @param query
   *          {@link Elevate.Query} the query object
   * @param docId
   *          the docId to search
   * @return the {@link Elevate.Query.Doc} object if found, null otherwise
   */
  private Elevate.Query.Doc findDoc(final Elevate.Query query, final String docId) {
    for (final Elevate.Query.Doc doc : query.getDoc()) {
      if (doc.getId().equals(docId)) {
        return doc;
      }
    }
    return null;
  }

  /**
   * @throws IOException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final String getParam = request.getParameter("get");

    try {
      // Use JAXB on the elevate.xml file to create the corresponding
      // Java object
      final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
      final Unmarshaller unmarshal = jxbc.createUnmarshaller();
      final Elevate elevate = (Elevate) unmarshal.unmarshal(elevatorFile);

      if (getParam.equals("queries")) {
        final List<String> queriesList = new ArrayList<>();
        for (final Elevate.Query query : elevate.getQuery()) {
          queriesList.add(query.getText());
        }
        jsonResponse.put("queries", queriesList);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      } else if (getParam.equals("docs")) {
        final String queryParam = request.getParameter("query");
        final List<String> docsList = new ArrayList<>();
        for (final Elevate.Query query : elevate.getQuery()) {
          if (query.getText().equals(queryParam)) {
            for (final Elevate.Query.Doc doc : query.getDoc()) {
              docsList.add(doc.getId());
            }
          }
        }
        jsonResponse.put("docs", docsList);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      }
    } catch (final Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      LOGGER.error("Error on marshal/unmarshal elevate.xml file ", e);
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }

  /**
   * @throws IOException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    final JSONObject jsonResponse = new JSONObject();
    if (request.getParameter("action") != null && !request.getParameter("action").equals("") && request.getParameter("query") != null && !request.getParameter("query").equals("")) {
      try {
        // Retrieve the query used for the search
        final String queryReq = request.getParameter("query");

        // Retrieve the docId and the action to
        // perform (elevate or remove from elevate)
        final String docId = request.getParameter("item");
        final String action = request.getParameter("action");

        // Use JAXB on the elevate.xml file to create the corresponding
        // Java object
        final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
        final Unmarshaller unmarshal = jxbc.createUnmarshaller();
        final Elevate elevate = (Elevate) unmarshal.unmarshal(elevatorFile);

        if (action.equals("up")) { // Elevate the doc

          // Retrieve the query entry if it already exists in the
          // elevate.xml file
          Elevate.Query query = findQuery(elevate, queryReq);

          // If the entry does not exist, create it
          if (query == null) {
            query = new Elevate.Query();
            query.setText(queryReq);
            elevate.getQuery().add(query);
          }

          // Try to find an existing entry for the doc, if it does not
          // exists then create it and add it at the end of the list, otherwise
          // move
          // the doc one step up.
          Elevate.Query.Doc doc = findDoc(query, docId);
          if (doc == null) {
            doc = new Elevate.Query.Doc();
            doc.setId(docId);
            query.getDoc().add(doc);
          } else {
            final int index = query.getDoc().indexOf(doc);
            if (index > 0) {
              query.getDoc().remove(index);
              query.getDoc().add(index - 1, doc);
            }
          }

          // Set the response code
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

        } else if (action.equals("down")) { // Remove the doc

          // Down the doc if it is found, otherwise there is nothing
          // to do
          final Elevate.Query q = findQuery(elevate, queryReq);
          if (q != null) {
            final Elevate.Query.Doc doc = findDoc(q, docId);
            if (doc != null) {
              final int index = q.getDoc().indexOf(doc);
              if (index < q.getDoc().size() - 1) {
                q.getDoc().remove(index);
                q.getDoc().add(index + 1, doc);
              }
            }
          }

          // Set the response code
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        } else if (action.equals("remove")) { // Remove the doc

          // Remove the doc if it is found, otherwise there is nothing
          // to do
          final Elevate.Query q = findQuery(elevate, queryReq);
          if (q != null) {
            final Elevate.Query.Doc doc = findDoc(q, docId);
            if (doc != null) {
              q.getDoc().remove(doc);
            }
          }

          // Set the response code
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
        }

        // Re-transform the Java object into the elevate.xml file thanks
        // to JAXB
        final Marshaller marshal = jxbc.createMarshaller();
        marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
        final OutputStream os = new FileOutputStream(elevatorFile);
        marshal.marshal(elevate, os);

      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        LOGGER.error("Error on marshal/unmarshal elevate.xml file in solr/solrcloud/" + server + "/conf", e);
      }
    } else if (request.getParameter("query") != null && !request.getParameter("query").equals("") && request.getParameter("tool") != null && !request.getParameter("tool").equals("")) {
      try {
        // Retrieve the query used for the search
        final String queryReq = request.getParameter("query");

        // Retrieve the docInfos, containing the docId and the action to
        // perform (elevate or remove from elevate)
        final String[] docs;
        if (request.getParameter("docs[]") != null) {
          docs = request.getParameterValues("docs[]");
        } else {
          docs = new String[0];
        }

        // Use JAXB on the elevate.xml file to create the corresponding
        // Java object
        final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
        final Unmarshaller unmarshal = jxbc.createUnmarshaller();
        final Elevate elevate = (Elevate) unmarshal.unmarshal(elevatorFile);

        // Retrieve the query entry if it already exists in the
        // elevate.xml file
        Elevate.Query query = findQuery(elevate, queryReq);

        if (request.getParameter("tool").equals("delete")) {
          if (query != null) {
            elevate.getQuery().remove(query);
          }
        } else {

          // If the entry does not exist, create it
          if (query == null) {
            query = new Elevate.Query();
            query.setText(queryReq);
            elevate.getQuery().add(query);
          }

          if (request.getParameter("tool").equals("modify")) {
            // Clear the docs because everything must be like the user
            // did
            // in the admin UI
            query.getDoc().clear();
          }

          for (int i = 0; i < docs.length; i++) {
            final String docId = docs[i];

            // Try to find an existing entry for the doc, if it does not
            // exists then create it on top of the list, otherwise move
            // the doc in top of the list.
            Elevate.Query.Doc doc = findDoc(query, docId);
            if (doc == null) {
              doc = new Elevate.Query.Doc();
              doc.setId(docId);
              query.getDoc().add(i, doc);
            } else {
              final int index = query.getDoc().indexOf(doc);
              if (index != i) {
                query.getDoc().remove(index);
                query.getDoc().add(i, doc);
              }
            }
          }

        }

        // Re-transform the Java object into the elevate.xml file thanks
        // to JAXB
        final Marshaller marshal = jxbc.createMarshaller();
        marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
        final OutputStream os = new FileOutputStream(elevatorFile);
        marshal.marshal(elevate, os);

        // Set the response code
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      } catch (final Exception e) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        LOGGER.error("Error on marshal/unmarshal elevate.xml file in solr/solrcloud/" + server + "/conf", e);
      }
    }
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);
  }
}
