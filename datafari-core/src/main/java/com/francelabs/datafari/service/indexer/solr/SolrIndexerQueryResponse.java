package com.francelabs.datafari.service.indexer.solr;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerFieldStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;

public class SolrIndexerQueryResponse implements IndexerQueryResponse {

  private JSONObject rawResponse;
  private JSONArray resultsObj;
  private long numFound;
  private int qTime;
  private Map<String, IndexerFacetField> listFacetFields;
  private Map<String, IndexerFieldStatsInfo> listFieldStatsInfo;
  private final Logger LOGGER = LogManager.getLogger(SolrIndexerQueryResponse.class);

  protected SolrIndexerQueryResponse(final JSONObject response) {
    build(response);
  }

  private void build(final JSONObject response) {
    this.rawResponse = response;
    final JSONObject responseObj = (JSONObject) rawResponse.get("response");
    final JSONObject responseHeader = (JSONObject) rawResponse.get("responseHeader");
    if (responseObj != null && responseObj.get("numFound") != null) {
      numFound = Long.parseLong(responseObj.get("numFound").toString());
    } else {
      numFound = 0;
    }
    if (responseHeader != null && responseHeader.get("QTime") != null) {
      qTime = Integer.parseInt(responseHeader.get("QTime").toString());
    } else {
      qTime = -1;
    }

    // Get docs
    if (responseObj == null || responseObj.get("docs") == null) {
      resultsObj = new JSONArray();
    } else {
      resultsObj = (JSONArray) responseObj.get("docs");
    }

    // Get facetFields
    listFacetFields = new HashMap<>();
    if (rawResponse.get("facet_counts") != null) {
      final JSONObject facetCounts = (JSONObject) rawResponse.get("facet_counts");
      final JSONObject facetFields = (JSONObject) facetCounts.get("facet_fields");
      facetFields.forEach((k, v) -> {
        final String fieldName = k.toString();
        final JSONArray values = (JSONArray) v;
        listFacetFields.put(fieldName, new SolrIndexerFacetField(fieldName, values));
      });
    }

    // Get stats infos
    listFieldStatsInfo = new HashMap<>();
    if (rawResponse.get("stats") != null) {
      final JSONObject stats = (JSONObject) rawResponse.get("stats");
      final JSONObject statsFields = (JSONObject) stats.get("stats_fields");
      statsFields.forEach((k, v) -> {
        final String fieldName = k.toString();
        final JSONObject statsInfos = (JSONObject) v;
        listFieldStatsInfo.put(fieldName, new SolrIndexerFieldStatsInfo(fieldName, statsInfos));
      });
    }
  }

  protected JSONObject getQueryResponse() {
    return rawResponse;
  }

  @Override
  public long getNumFound() {
    return numFound;
  }

  @Override
  public int getQTime() {
    return qTime;
  }

  @Override
  public String getStrJSONResponse() {
    return rawResponse.toJSONString();
  }

  @Override
  public JSONArray getResults() {
    return resultsObj;
  }

  @Override
  public Map<String, IndexerFacetField> getFacetFields() {
    return listFacetFields;
  }

  @Override
  public Map<String, IndexerFieldStatsInfo> getFieldStatsInfo() {
    return listFieldStatsInfo;
  }

  public void setResponse(final JSONObject rawResponse) {
    build(rawResponse);
  }

}
