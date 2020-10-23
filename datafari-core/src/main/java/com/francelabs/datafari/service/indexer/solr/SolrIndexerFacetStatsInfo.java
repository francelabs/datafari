package com.francelabs.datafari.service.indexer.solr;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerFacetStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerStatsInfo;

public class SolrIndexerFacetStatsInfo implements IndexerFacetStatsInfo {

  private final String name;
  private final List<IndexerStatsInfo> valuesStatsInfo;

  protected SolrIndexerFacetStatsInfo(final String facetName, final JSONObject values) {
    name = facetName;
    valuesStatsInfo = new ArrayList<IndexerStatsInfo>();
    values.forEach((k, v) -> {
      final String value = k.toString();
      final JSONObject statsInfos = (JSONObject) v;
      valuesStatsInfo.add(new SolrIndexerStatsInfo(value, statsInfos));
    });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<IndexerStatsInfo> getValuesStatsInfo() {
    return valuesStatsInfo;
  }

}
