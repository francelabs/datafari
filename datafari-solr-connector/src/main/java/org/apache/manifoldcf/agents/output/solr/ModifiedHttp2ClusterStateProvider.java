package org.apache.manifoldcf.agents.output.solr;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BaseHttpClusterStateProvider;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

public class ModifiedHttp2ClusterStateProvider extends BaseHttpClusterStateProvider {
  final Http2SolrClient httpClient;
  final boolean closeClient;

  public ModifiedHttp2ClusterStateProvider(final List<String> solrUrls, final ModifiedHttp2SolrClient mc) throws Exception {
    this.httpClient = (mc == null) ? new Http2SolrClient.Builder().build()
                                   : new Http2SolrClient.Builder().build(); // on ne peut pas réutiliser l’instance mc comme Http2SolrClient
    this.closeClient = true;
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
    return new Http2SolrClient.Builder(baseUrl).build();
  }
}
