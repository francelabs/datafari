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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.francelabs.datafari.logs.StatLevel;
import com.francelabs.datafari.service.indexer.IndexerInputDocument;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

public class StatsPusher {

  private static final List<String> queryParams = Arrays.asList("q", "numFound", "noHits", "QTime");
  private static final List<String> documentParams = Arrays.asList("url", "id");

  private final static Logger LOGGER = Logger.getLogger(StatsPusher.class.getName());

  public static void pushDocument(final IndexerQuery query, final String protocol) {
    try {

      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.STATISTICS);

      final Map<String, Integer> increment = new HashMap<String, Integer>();
      increment.put("inc", 1);

      final Map<String, Integer> setToOne = new HashMap<String, Integer>();
      setToOne.put("set", 1);

      final Map<String, Integer> incrementPosition = new HashMap<String, Integer>();
      incrementPosition.put("inc", Integer.parseInt(query.getParamValue("position")));

      final IndexerInputDocument doc = IndexerServerManager.createDocument();
      doc.addField("click", setToOne);
      doc.addField("numClicks", increment);
      doc.addField("positionClickTot", incrementPosition);

      final Map<String, String> paramsMap = new HashMap<String, String>();

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

      String history = "";
      history += "///";
      history += "///";
      history += "///";
      history += "///";
      history += "///" + paramsMap.get("url");
      history += "///" + paramsMap.get("position");

      final Map<String, String> addValue = new HashMap<String, String>();
      addValue.put("add", history);

      doc.addField("history", addValue);

      server.pushDoc(doc, 10000);

      final IndexerResponseDocument insertedSolrDoc = server.getDocById(doc.getFieldValue("id").toString());

      String username = "";
      if (query.getParamValue("AuthenticatedUserName") != null) {
        username = query.getParamValue("AuthenticatedUserName");
      }

      LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc, username));

    } catch (final Exception e) {
      LOGGER.error("Cannot add doc for statistic component : " + e.getMessage(), e);
      e.printStackTrace();
    }
  }

  public static void pushQuery(final IndexerQuery inputQuery, final String protocol) {
    try {

      final IndexerServer server = IndexerServerManager.getIndexerServer(Core.STATISTICS);

      final IndexerQuery query = IndexerServerManager.createQuery();
      query.setRequestHandler("/get");
      query.setParam("id", inputQuery.getParamValue("id"));
      query.setParam("fl", "id");
      server.executeQuery(query);

      final IndexerInputDocument doc = IndexerServerManager.createDocument();
      doc.addField("id", inputQuery.getParamValue("id"));

      final Map<String, String> paramsMap = new HashMap<String, String>();

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

      String history = "" + paramsMap.get("q");
      history += "///";
      if (paramsMap.get("fq") != null) {
        history += paramsMap.get("fq");
      }

      history += "///" + paramsMap.get("numFound");
      history += "///" + paramsMap.get("QTime");
      history += "///";
      if (paramsMap.get("start") != null && paramsMap.get("rows") != null) {
        history += (int) (Double.parseDouble(paramsMap.get("start")) / Double.parseDouble(paramsMap.get("rows")) + 1);
      } else {
        history += 1;
      }

      history += "///";
      history += "///";

      final Map<String, String> addValue = new HashMap<String, String>();
      addValue.put("add", history);

      doc.addField("history", addValue);

      server.pushDoc(doc, 10000);

      final IndexerResponseDocument insertedSolrDoc = server.getDocById(doc.getFieldValue("id").toString());

      String username = "";
      if (inputQuery.getParamValue("AuthenticatedUserName") != null) {
        username = inputQuery.getParamValue("AuthenticatedUserName");
      }

      LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc, username));

    } catch (final Exception e) {
      LOGGER.error("Cannot add query for statistic component : " + e.getMessage(), e);
      e.printStackTrace();
    }

  }

  private static String normalizeParameterValue(final String param, String value) throws UnsupportedEncodingException {
    value = URLDecoder.decode(value, "UTF-8");
    value = value.replaceAll("\\{\\!tag=[^}]*\\}", "");
    return value;
  }

}
