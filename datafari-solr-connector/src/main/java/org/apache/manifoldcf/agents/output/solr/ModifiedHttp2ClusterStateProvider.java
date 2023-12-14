package org.apache.manifoldcf.agents.output.solr;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BaseHttpClusterStateProvider;

public class ModifiedHttp2ClusterStateProvider extends BaseHttpClusterStateProvider {
  final ModifiedHttp2SolrClient httpClient;
  final boolean closeClient;

  public ModifiedHttp2ClusterStateProvider(final List<String> solrUrls, final ModifiedHttp2SolrClient httpClient) throws Exception {
    this.httpClient = httpClient == null ? new ModifiedHttp2SolrClient.Builder().build() : httpClient;
    this.closeClient = httpClient == null;
    init(solrUrls);
  }

  @Override
  public void close() throws IOException {
    if (this.closeClient && this.httpClient != null) {
      httpClient.close();
    }
  }

  @Override
  protected SolrClient getSolrClient(final String baseUrl) {
    return new ModifiedHttp2SolrClient.Builder(baseUrl).withHttpClient(httpClient).build();
  }
}
