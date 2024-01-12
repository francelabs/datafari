package com.francelabs.datafari.config;

/**
 * A representation of Solr Collection location with the base URL of stored collection and the name the collection.
 * <p>
 *   The Solr Collection ({@link CollectionPathConfig#solrCollection}) is a simple name like FileShare, Spacy...
 * </p>
 * <p>
 *   The base URL ({@link CollectionPathConfig#baseUrl}) can be:a Solr url or a Zookeeper url.
 *   See {@link com.francelabs.datafari.solraccessors.AbstractDocuments#getSolrClient(String) AbstractDocuments.getSolrClient(String)}
 *   for more about URL syntax.
 * </p>
 */
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
