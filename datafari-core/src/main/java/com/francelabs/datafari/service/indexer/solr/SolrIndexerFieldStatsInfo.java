package com.francelabs.datafari.service.indexer.solr;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerFacetStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerFieldStatsInfo;
import com.francelabs.datafari.service.indexer.IndexerStatsInfo;

public class SolrIndexerFieldStatsInfo implements IndexerFieldStatsInfo {

  private final String field;
  private final IndexerStatsInfo statsInfo;
  private final Map<String, IndexerFacetStatsInfo> listFacets;

  protected SolrIndexerFieldStatsInfo(final String fieldName, final JSONObject statsInfos) {
    field = fieldName;
    listFacets = new HashMap<String, IndexerFacetStatsInfo>();
    JSONObject facets = new JSONObject();
    if (statsInfos.get("facets") != null) {
      facets = (JSONObject) statsInfos.get("facets");
      for (final Object facet : facets.keySet()) {
        final String facetName = facet.toString();
        final JSONObject values = (JSONObject) facets.get(facet);
        listFacets.put(facetName, new SolrIndexerFacetStatsInfo(facetName, values));
      }
    }
    statsInfo = new SolrIndexerStatsInfo(fieldName, statsInfos);
  }

  @Override
  public String getName() {
    return field;
  }

  @Override
  public IndexerStatsInfo getStatsInfo() {
    return statsInfo;
  }

  @Override
  public Map<String, IndexerFacetStatsInfo> getFacets() {
    return listFacets;
  }

}
