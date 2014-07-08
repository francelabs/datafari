package com.francelabs.datafari.statistics;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.francelabs.datafari.solrj.SolrServers;
import com.francelabs.datafari.solrj.SolrServers.Core;

public class StatsPusher {

	private static final List<String> queryParams = Arrays.asList("q",
			"numFound", "noHits", "QTime");
	private static final List<String> documentParams = Arrays.asList("url",
			"id");

	private final static Logger LOGGER = Logger.getLogger(StatsPusher.class
			.getName());

	public static void pushDocument(ModifiableSolrParams params) {
		try {

			SolrServer solrServer = SolrServers
					.getSolrServer(Core.STATISTICS);

			Map increment = new HashMap();
			increment.put("inc", 1);

			Map setToOne = new HashMap();
			setToOne.put("set", 1);

			Map incrementPosition = new HashMap();
			incrementPosition.put("inc", params.get("position"));

			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("click", setToOne);
			doc.addField("numClicks", increment);
			doc.addField("positionClickTot", incrementPosition);

			Map<String, String> paramsMap = new HashMap<String, String>();

			for (String paramName : params.getParameterNames()) {
				for (String paramValue : params.getParams(paramName)) {
					paramsMap.put(paramName,
							normalizeParameterValue(paramName, paramValue));
				}
			}

			for (Entry<String, String> entry : paramsMap.entrySet()) {
				if (documentParams.contains(entry.getKey())) {
					doc.addField(entry.getKey(), entry.getValue());
				}
			}

			// TODO clean this!
			String history = "";
			history += "///";
			history += "///";
			history += "///";
			history += "///";
			history += "///" + paramsMap.get("url");
			history += "///" + paramsMap.get("position");

			Map addValue = new HashMap();
			addValue.put("add", history);

			doc.addField("history", addValue);

			solrServer.add(doc, 10000);


		} catch (Exception e) {
			LOGGER.error(
					"Cannot add doc for statistic component : "
							+ e.getMessage(), e);
			e.printStackTrace();
		}
	}


	public static void pushQuery(ModifiableSolrParams params) {
		try {

			SolrServer solrServer = SolrServers
					.getSolrServer(Core.STATISTICS);

			SolrQuery query = new SolrQuery();
			query.setRequestHandler("/get");
			query.add("id", params.get("id"));
			query.add("fl", "id");
			QueryResponse queryResponse = solrServer.query(query);

			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", params.get("id"));

			Map<String, String> paramsMap = new HashMap<String, String>();

			for (String paramName : params.getParameterNames()) {
				for (String paramValue : params.getParams(paramName)) {
					paramsMap.put(paramName,
							normalizeParameterValue(paramName, paramValue));
				}
			}

			if (queryResponse.getResponse().get("doc") == null) {
				for (Entry<String, String> entry : paramsMap.entrySet()) {
					if (queryParams.contains(entry.getKey())) {
						doc.addField(entry.getKey(), entry.getValue());
					}
				}
			}

			// TODO clean this!
			String history = "" + paramsMap.get("q");
			history += "///";
			if (paramsMap.get("fq") != null) {
				history += paramsMap.get("fq");
			}

			history += "///" + paramsMap.get("numFound");
			history += "///" + paramsMap.get("QTime");
			history += "///";
			if (paramsMap.get("start") != null && paramsMap.get("rows") != null) {
				history += (int)(Double.parseDouble(paramsMap.get("start")) / Double.parseDouble(paramsMap.get("rows"))+1);
			} else {
				history += 1;
			}

			history += "///";
			history += "///";

			Map addValue = new HashMap();
			addValue.put("add", history);

			doc.addField("history", addValue);

			solrServer.add(doc, 10000);
			

		} catch (Exception e) {
			LOGGER.error(
					"Cannot add query for statistic component : "
							+ e.getMessage(), e);
			e.printStackTrace();
		}
		
	}
	


	private static String normalizeParameterValue(String param, String value) {
		value = URLDecoder.decode(value);
		value = value.replaceAll("\\{\\!tag=[^}]*\\}", "");
		return value;
	}

}
