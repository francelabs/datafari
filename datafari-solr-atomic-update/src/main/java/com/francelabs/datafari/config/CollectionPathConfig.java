package com.francelabs.datafari.config;

public class CollectionPathConfig {
  private String baseUrl;
  private String solrCollection;

  public String getBaseUrl() {
    return baseUrl;
  }
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getSolrCollection() {
    return solrCollection;
  }

  public void setSolrCollection(String solrCollection) {
    this.solrCollection = solrCollection;
  }
}
