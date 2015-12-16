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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.logs.StatLevel;
import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;

public class StatsPusher {

	private static final List<String> queryParams = Arrays.asList("q", "numFound", "noHits", "QTime");
	private static final List<String> documentParams = Arrays.asList("url", "id");

	private final static Logger LOGGER = Logger.getLogger(StatsPusher.class.getName());

	public static void pushDocument(final ModifiableSolrParams params) {
		try {

			final SolrClient solrServer = SolrServers.getSolrServer(Core.STATISTICS);

			final Map<String, Integer> increment = new HashMap<String, Integer>();
			increment.put("inc", 1);

			final Map<String, Integer> setToOne = new HashMap<String, Integer>();
			setToOne.put("set", 1);

			final Map<String, Integer> incrementPosition = new HashMap<String, Integer>();
			incrementPosition.put("inc", Integer.parseInt(params.get("position")));

			final SolrInputDocument doc = new SolrInputDocument();
			doc.addField("click", setToOne);
			doc.addField("numClicks", increment);
			doc.addField("positionClickTot", incrementPosition);

			final Map<String, String> paramsMap = new HashMap<String, String>();

			for (final String paramName : params.getParameterNames()) {
				for (final String paramValue : params.getParams(paramName)) {
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

			solrServer.add(doc, 10000);

			final SolrDocument insertedSolrDoc = solrServer.getById(doc.getFieldValue("id").toString());

			LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc));

		} catch (final Exception e) {
			LOGGER.error("Cannot add doc for statistic component : " + e.getMessage(), e);
			e.printStackTrace();
		}
	}

	public static void pushQuery(final ModifiableSolrParams params) {
		try {

			final SolrClient solrServer = SolrServers.getSolrServer(Core.STATISTICS);

			final SolrQuery query = new SolrQuery();
			query.setRequestHandler("/get");
			query.add("id", params.get("id"));
			query.add("fl", "id");
			final QueryResponse queryResponse = solrServer.query(query);

			final SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", params.get("id"));

			final Map<String, String> paramsMap = new HashMap<String, String>();

			for (final String paramName : params.getParameterNames()) {
				for (final String paramValue : params.getParams(paramName)) {
					paramsMap.put(paramName, normalizeParameterValue(paramName, paramValue));
				}
			}

			if (queryResponse.getResponse().get("doc") == null) {
				for (final Entry<String, String> entry : paramsMap.entrySet()) {
					if (queryParams.contains(entry.getKey())) {
						doc.addField(entry.getKey(), entry.getValue());
					}
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

			solrServer.add(doc, 10000);

			final SolrDocument insertedSolrDoc = solrServer.getById(doc.getFieldValue("id").toString());

			LOGGER.log(StatLevel.STAT, StatsUtils.createStatLog(insertedSolrDoc));

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
