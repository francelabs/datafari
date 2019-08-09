package com.francelabs.datafari.service.indexer.solr;

import org.json.simple.JSONObject;

import com.francelabs.datafari.service.indexer.IndexerStatsInfo;

public class SolrIndexerStatsInfo implements IndexerStatsInfo {

  private final String name;
  private double min;
  private double max;
  private double mean;
  private int count;
  private int missing;
  private double sum;

  protected SolrIndexerStatsInfo(final String name, final JSONObject statsInfos) {
    this.name = name;
    min = 0.0;
    max = 0.0;
    mean = 0.0;
    count = 0;
    missing = 0;
    sum = 0.0;
    for (final Object key : statsInfos.keySet()) {
      final String statName = key.toString();
      if (statsInfos.get(key) != null && !statsInfos.get(key).toString().equals("NaN")) {
        switch (statName) {
          case "min":
            min = Double.parseDouble(statsInfos.get(key).toString());
            break;
          case "max":
            max = Double.parseDouble(statsInfos.get(key).toString());
            break;
          case "mean":
            mean = Double.parseDouble(statsInfos.get(key).toString());
            break;
          case "count":
            count = Integer.parseInt(statsInfos.get(key).toString());
            break;
          case "missing":
            missing = Integer.parseInt(statsInfos.get(key).toString());
            break;
          case "sum":
            sum = Double.parseDouble(statsInfos.get(key).toString());
            break;
          default:
            break;
        }
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public double getMin() {
    return min;
  }

  @Override
  public double getMax() {
    return max;
  }

  @Override
  public double getMean() {
    return mean;
  }

  @Override
  public int getCount() {
    return count;
  }

  @Override
  public int getMissing() {
    return missing;
  }

  @Override
  public double getSum() {
    return sum;
  }

}
