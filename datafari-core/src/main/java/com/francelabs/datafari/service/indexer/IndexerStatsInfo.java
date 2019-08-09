package com.francelabs.datafari.service.indexer;

public interface IndexerStatsInfo {

  public double getSum();

  public int getMissing();

  public int getCount();

  public double getMean();

  public double getMax();

  public double getMin();

  public String getName();

}
