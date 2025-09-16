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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.jaxb.Elevate;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.ExecutionEnvironment;

@WebServlet("/SearchExpert/queryElevator")
public class QueryElevator extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LogManager.getLogger(QueryElevator.class.getName());

  private final String server = Core.FILESHARE.toString();
  private volatile File elevatorFile; 
  private volatile String envBase;    

  public QueryElevator() {
    super();
    // "Best effort" initialization; final resolution is lazy in resolveElevatorFile()
    String dfHome = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (dfHome == null) {
      dfHome = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    envBase = dfHome + "/solr/solrcloud/FileShare/conf";
    File f = new File(envBase, "elevate.xml");
    if (f.exists() && f.isFile()) {
      elevatorFile = f;
    } else {
      elevatorFile = null; // let resolveElevatorFile() retry later
    }
  }

  /** Re-resolve the file path if necessary, null if not found. */
  private File resolveElevatorFile() {
    File f = elevatorFile;
    if (f != null && f.exists() && f.isFile()) return f;

    // (re)compute envBase if needed
    if (envBase == null) {
      String dfHome = Environment.getEnvironmentVariable("DATAFARI_HOME");
      if (dfHome == null) dfHome = ExecutionEnvironment.getDevExecutionEnvironment();
      envBase = dfHome + "/solr/solrcloud/FileShare/conf";
    }

    File candidate = new File(envBase, "elevate.xml");
    if (candidate.exists() && candidate.isFile()) {
      elevatorFile = candidate;
      return candidate;
    }
    LOGGER.warn("elevate.xml not found at {}", candidate.getAbsolutePath());
    elevatorFile = null;
    return null;
  }

  private Elevate.Query findQuery(final Elevate elevate, final String queryText) {
    for (final Elevate.Query q : elevate.getQuery()) {
      if (q.getText() != null && q.getText().equals(queryText)) {
        return q;
      }
    }
    return null;
  }

  private Elevate.Query.Doc findDoc(final Elevate.Query query, final String docId) {
    for (final Elevate.Query.Doc doc : query.getDoc()) {
      if (doc.getId() != null && doc.getId().equals(docId)) {
        return doc;
      }
    }
    return null;
  }

  private void writeJson(HttpServletResponse response, JSONObject json) throws IOException {
    try (PrintWriter out = response.getWriter()) {
      out.print(json.toJSONString());
    }
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/json");
    final JSONObject jsonResponse = new JSONObject();

    final String getParam = request.getParameter("get");
    if (getParam == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put("message", "Missing 'get' parameter");
      writeJson(response, jsonResponse);
      return;
    }

    final File file = resolveElevatorFile();
    if (file == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put("message", "elevate.xml not found");
      writeJson(response, jsonResponse);
      return;
    }

    try {
      final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
      final Unmarshaller unmarshal = jxbc.createUnmarshaller();
      final Elevate elevate = (Elevate) unmarshal.unmarshal(file);

      if ("queries".equals(getParam)) {
        final List<String> queriesList = new ArrayList<>();
        for (final Elevate.Query query : elevate.getQuery()) {
          if (query.getText() != null) queriesList.add(query.getText());
        }
        Collections.sort(queriesList);
        jsonResponse.put("queries", queriesList);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      } else if ("docs".equals(getParam)) {
        final String queryParam = request.getParameter("query");
        final List<String> docsList = new ArrayList<>();
        if (queryParam != null) {
          for (final Elevate.Query query : elevate.getQuery()) {
            if (queryParam.equals(query.getText())) {
              for (final Elevate.Query.Doc doc : query.getDoc()) {
                if (doc.getId() != null) docsList.add(doc.getId());
              }
            }
          }
        }
        jsonResponse.put("docs", docsList);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

      } else {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put("message", "Unsupported 'get' value");
      }
    } catch (final Exception e) {
      LOGGER.error("Error on marshal/unmarshal elevate.xml file ", e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put("message", "Error parsing elevate.xml");
    }

    writeJson(response, jsonResponse);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/json");
    final JSONObject jsonResponse = new JSONObject();

    final File file = resolveElevatorFile();
    if (file == null) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put("message", "elevate.xml not found");
      writeJson(response, jsonResponse);
      return;
    }

    IndexerServer indexServer = null;
    try {
      indexServer = IndexerServerManager.getIndexerServer(Core.FILESHARE);
    } catch (final Exception e) {
      LOGGER.error("Error while getting the Solr core", e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put("message", "Error while getting the Solr core (code 69000)");
      writeJson(response, jsonResponse);
      return;
    }

    final String action = request.getParameter("action");
    final String queryReq = request.getParameter("query");

    // Case 1: single actions up/down/remove
    if (action != null && !action.isEmpty() && queryReq != null && !queryReq.isEmpty()) {
      final String docId = request.getParameter("item");

      try {
        final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
        final Unmarshaller unmarshal = jxbc.createUnmarshaller();
        final Elevate elevate = (Elevate) unmarshal.unmarshal(file);

        if ("up".equals(action)) {
          Elevate.Query query = findQuery(elevate, queryReq);
          if (query == null) {
            query = new Elevate.Query();
            query.setText(queryReq);
            elevate.getQuery().add(query);
          }
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
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

        } else if ("down".equals(action)) {
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
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

        } else if ("remove".equals(action)) {
          final Elevate.Query q = findQuery(elevate, queryReq);
          if (q != null) {
            final Elevate.Query.Doc doc = findDoc(q, docId);
            if (doc != null) {
              q.getDoc().remove(doc);
            }
          }
          jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());

        } else {
          jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
          jsonResponse.put("message", "Unsupported action");
          writeJson(response, jsonResponse);
          return;
        }

        // Marshal + upload + reload (synchronized on the file)
        synchronized (QueryElevator.class) {
          final Marshaller marshal = jxbc.createMarshaller();
          marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
          try (OutputStream os = new FileOutputStream(file, false)) {
            marshal.marshal(elevate, os);
          }
        }
        indexServer.uploadFile(envBase, "elevate.xml", Core.FILESHARE.toString(), "");
        indexServer.reloadCollection(Core.FILESHARE.toString());

        // Optional propagation to secondary collections
        List<String> collectionsList = null;
        final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
        final String secondary = config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS);
        if (secondary != null && !secondary.isEmpty()) {
          collectionsList = Arrays.asList(secondary.split(","));
        }
        if (collectionsList != null) {
          for (final String object : collectionsList) {
            indexServer.uploadFile(envBase, "elevate.xml", object.trim(), "");
            indexServer.reloadCollection(object.trim());
          }
        }

      } catch (final Exception e) {
        LOGGER.error("Error on marshal/unmarshal elevate.xml file in solr/solrcloud/" + server + "/conf", e);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put("message", "Error updating elevate.xml");
      }

      writeJson(response, jsonResponse);
      return;
    }

    // Case 2: "tool" operations (modify/delete with docs[] list)
    final String tool = request.getParameter("tool");
    if (queryReq != null && !queryReq.isEmpty() && tool != null && !tool.isEmpty()) {
      try {
        final JAXBContext jxbc = JAXBContext.newInstance(Elevate.class);
        final Unmarshaller unmarshal = jxbc.createUnmarshaller();
        final Elevate elevate = (Elevate) unmarshal.unmarshal(file);

        Elevate.Query query = findQuery(elevate, queryReq);

        if ("delete".equals(tool)) {
          if (query != null) {
            elevate.getQuery().remove(query);
          }
        } else {
          if (query == null) {
            query = new Elevate.Query();
            query.setText(queryReq);
            elevate.getQuery().add(query);
          }
          if ("modify".equals(tool)) {
            query.getDoc().clear();
          }

          final String[] docs = request.getParameterValues("docs[]");
          final String[] safeDocs = (docs != null) ? docs : new String[0];

          for (int i = 0; i < safeDocs.length; i++) {
            final String docId = safeDocs[i];
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

        synchronized (QueryElevator.class) {
          final Marshaller marshal = jxbc.createMarshaller();
          marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
          try (OutputStream os = new FileOutputStream(file, false)) {
            marshal.marshal(elevate, os);
          }
        }
        indexServer.uploadFile(envBase, "elevate.xml", Core.FILESHARE.toString(), "");
        indexServer.reloadCollection(Core.FILESHARE.toString());

        List<String> collectionsList = null;
        final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
        final String secondary = config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS);
        if (secondary != null && !secondary.isEmpty()) {
          collectionsList = Arrays.asList(secondary.split(","));
        }
        if (collectionsList != null) {
          for (final String object : collectionsList) {
            indexServer.uploadFile(envBase, "elevate.xml", object.trim(), "");
            indexServer.reloadCollection(object.trim());
          }
        }

        jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      } catch (final Exception e) {
        LOGGER.error("Error on marshal/unmarshal elevate.xml file in solr/solrcloud/" + server + "/conf", e);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put("message", "Error updating elevate.xml");
      }

      writeJson(response, jsonResponse);
      return;
    }

    // Invalid parameters
    jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    jsonResponse.put("message", "Invalid parameters");
    writeJson(response, jsonResponse);
  }
}