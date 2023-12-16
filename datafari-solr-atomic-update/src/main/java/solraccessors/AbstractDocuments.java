package solraccessors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import java.util.concurrent.TimeUnit;

public abstract class AbstractDocuments {
  final protected SolrClient solrClient;
  final protected String solrCollection;
  final protected int maxDocsPerQuery;

  public AbstractDocuments(final String baseSolrUrl, final String solrCollection, final int maxDocsPerQuery) {
    this.solrClient = getSolrClient(baseSolrUrl);

    this.solrCollection = solrCollection;
    this.maxDocsPerQuery = maxDocsPerQuery;

  }

  protected SolrClient getSolrClient(final String baseSolrUrl){
    //TODO best for SolrCloud mode
    /*final List<String> zkServers = new ArrayList<>();
    //zkServers.add("zookeeper1:2181");
    //zkServers.add("zookeeper2:2181");
    zkServers.add("https://dev.datafari.com:2181"); // ?
    solrClient = new CloudSolrClient.Builder(zkServers, Optional.of("/solr")).build();*/
    return new Http2SolrClient.Builder(baseSolrUrl)
        .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
        .withIdleTimeout(60000, TimeUnit.MILLISECONDS)
        .build();

  }
}
