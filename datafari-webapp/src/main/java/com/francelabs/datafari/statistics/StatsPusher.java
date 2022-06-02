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
package com.francelabs.datafari.statistics;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.logs.StatLevel;
import com.francelabs.datafari.service.db.StatisticsDataService;
import com.francelabs.datafari.service.db.StatisticsDataService.UserActions;
import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

public class StatsPusher {

  private static final List<String> queryParams = Arrays.asList("q", "numFound", "noHits", "QTime");
  private static final List<String> documentParams = Arrays.asList("url", "id");

  private final static Logger LOGGER = LogManager.getLogger(StatsPusher.class.getName());

  public static void pushDocument(final IndexerQuery query, final String protocol) {
    try {

      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.STATISTICS);

      final Map<String, Integer> increment = new HashMap<>();
      increment.put("inc", 1);

      final Map<String, Integer> incrementPosition = new HashMap<>();
      incrementPosition.put("inc", Integer.parseInt(query.getParamValue("position")));

      final IndexerInputDocument doc = IndexerServerManager.createDocument();
      doc.addField("click", "Clicked");
      doc.addField("numClicks", increment);
      doc.addField("positionClickTot", incrementPosition);

      final Map<String, String> paramsMap = new HashMap<>();

      for (final String paramName : query.getParamNames()) {
        for (final String paramValue : query.getParamValues(paramName)) {
          paramsMap.put(paramName, normalizeParameterValue(paramName, paramValue));
        }
      }

      for (final Entry<String, String> entry : paramsMap.entrySet()) {
        if (documentParams.contains(entry.getKey())) {
          doc.addField(entry.getKey(), entry.getValue());
        }
      }

      //////////
      final JSONArray jsonHistories = new JSONArray();
      final JSONObject jHistory = new JSONObject();
      jHistory.put("query", "");
      jHistory.put("fq", "");
      jHistory.put("numFound", 0);
      jHistory.put("QTime", 0);
      jHistory.put("page", 0);
      jHistory.put("url", paramsMap.get("url"));
      jHistory.put("position", Integer.parseInt(paramsMap.get("position")));

      final Map<String, String> addValue = new HashMap<>();
      addValue.put("add", jHistory.toString());

      doc.addField("history", addValue);

      server.pushDoc(doc, 10000);

      final IndexerResponseDocument insertedSolrDoc = server.getDocById(doc.getFieldValue("id").toString());
      for (final Object historyValue : insertedSolrDoc.getFieldValues("history")) {
        final JSONParser parser = new JSONParser();
        final JSONObject jHistoryValue = (JSONObject) parser.parse(historyValue.toString());
        jsonHistories.add(jHistoryValue);
      }
      insertedSolrDoc.removeField("history");
      insertedSolrDoc.addField("history", jsonHistories);

      String username = "";
      if (query.getParamValue("AuthenticatedUserName") != null) {
        username = query.getParamValue("AuthenticatedUserName");
      }

      LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc, username));

      // Pushing to the cassandra user_search_actions collection
      // TODO: Consider using url params or cookie to get the timestamp from the client side. (Involves changing the behavior of pushDocument
      // too).
      // TODO: Consider retrieving the user ID to store it in the logs
      StatisticsDataService.getInstance().saveClickStatistics(query.getParamValue("id"), query.getParamValue("q"), username, query.getParamValue("url"),
          Integer.parseInt(query.getParamValue("position")), new Date().toInstant());

    } catch (final Exception e) {
      LOGGER.error("Cannot add doc for statistic component : " + e.getMessage(), e);
      e.printStackTrace();
    }
  }

  public static void pushQuery(final IndexerQuery inputQuery, final String protocol) {
    // Pushing to the Statistics core in solr
    try {

      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.STATISTICS);

      final IndexerInputDocument doc = IndexerServerManager.createDocument();
      doc.addField("id", inputQuery.getParamValue("id"));

      final Map<String, String> paramsMap = new HashMap<>();

      for (final String paramName : inputQuery.getParamNames()) {
        for (final String paramValue : inputQuery.getParamValues(paramName)) {
          paramsMap.put(paramName, normalizeParameterValue(paramName, paramValue));
        }
      }

      for (final Entry<String, String> entry : paramsMap.entrySet()) {
        if (queryParams.contains(entry.getKey())) {
          doc.addField(entry.getKey(), entry.getValue());
        }
      }

      /////////////
      final JSONArray jsonHistories = new JSONArray();
      final JSONObject jHistory = new JSONObject();
      jHistory.put("query", paramsMap.get("q"));
      if (paramsMap.get("fq") != null) {
        jHistory.put("fq", paramsMap.get("fq"));
      } else {
        jHistory.put("fq", "");
      }
      jHistory.put("numFound", Integer.parseInt(paramsMap.get("numFound")));
      jHistory.put("QTime", Integer.parseInt(paramsMap.get("QTime")));
      if (paramsMap.get("start") != null && paramsMap.get("rows") != null) {
        jHistory.put("page", (int) (Double.parseDouble(paramsMap.get("start")) / Double.parseDouble(paramsMap.get("rows")) + 1));
      } else {
        jHistory.put("page", 1);
      }
      jHistory.put("url", "");
      jHistory.put("position", 0);

      final Map<String, String> addValue = new HashMap<>();
      addValue.put("add", jHistory.toString());

      doc.addField("history", addValue);

      server.pushDoc(doc, 10000);

      final IndexerResponseDocument insertedSolrDoc = server.getDocById(doc.getFieldValue("id").toString());
      for (final Object historyValue : insertedSolrDoc.getFieldValues("history")) {
        final JSONParser parser = new JSONParser();
        final JSONObject jHistoryValue = (JSONObject) parser.parse(historyValue.toString());
        jsonHistories.add(jHistoryValue);
      }
      insertedSolrDoc.removeField("history");
      insertedSolrDoc.addField("history", jsonHistories);

      String username = "";
      if (inputQuery.getParamValue("AuthenticatedUserName") != null) {
        username = inputQuery.getParamValue("AuthenticatedUserName");
      }

      LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc, username));

      // Pushing to the cassandra user_search_actions collection
      // TODO: Consider using url params or cookie to get the timestamp from the client side. (Involves changing the behavior of pushDocument
      // too).
      // TODO: COnsider retrieving the user ID to store it in the log.
      StatisticsDataService.getInstance().saveQueryStatistics(inputQuery.getParamValue("id"), inputQuery.getParamValue("q"), username, Integer.parseInt(inputQuery.getParamValue("numFound")),
          new Date().toInstant());

    } catch (final Exception e) {
      LOGGER.error("Cannot add query for statistic component : " + e.getMessage(), e);
      e.printStackTrace();
    }

  }

  public static void pushUserAction(final String queryId, final String userId, final UserActions action, final JSONObject parameters, final Instant timestamp) {
    try {
      StatisticsDataService.getInstance().saveStatistic(queryId, userId, action, parameters, timestamp);
    } catch (final Exception e) {
      LOGGER.error("Cannot add query for statistic component : " + e.getMessage(), e);
    }
  }

  private static String normalizeParameterValue(final String param, String value) throws UnsupportedEncodingException {
    value = URLDecoder.decode(value, "UTF-8");
    value = value.replaceAll("\\{\\!tag=[^}]*\\}", "");
    return value;
  }

}
