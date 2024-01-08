package org.apache.manifoldcf.agents.output.solr;

import java.io.Closeable;

import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;

public interface ModifiedHttpClientBuilderFactory extends Closeable {

  /**
   * This method configures the {@linkplain SolrHttpClientBuilder} by overriding the configuration of passed SolrHttpClientBuilder or as a new instance.
   *
   * @param builder The instance of the {@linkplain SolrHttpClientBuilder} which should by configured (optional).
   * @return the {@linkplain SolrHttpClientBuilder}
   */
  public SolrHttpClientBuilder getHttpClientBuilder(SolrHttpClientBuilder builder);

  public default void setup(final ModifiedHttp2SolrClient client) {
  }
}
