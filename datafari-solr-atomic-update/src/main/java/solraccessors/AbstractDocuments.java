package solraccessors;

import config.CollectionPathConfig;
import config.JobConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import java.util.concurrent.TimeUnit;

public abstract class AbstractDocuments {
  final protected SolrClient solrClient;
  final protected String solrCollection;
  final protected int maxDocsPerQuery;

  final protected JobConfig jobConfig;

  public AbstractDocuments(final JobConfig jobConfig, final int maxDocsPerQuery) {
    this.jobConfig = jobConfig;
    final CollectionPathConfig collectionPath = getCollectionPath();
    this.solrClient = getSolrClient(collectionPath.getBaseUrl());

    this.solrCollection = collectionPath.getSolrCollection();
    this.maxDocsPerQuery = maxDocsPerQuery;

  }

  protected abstract CollectionPathConfig getCollectionPath();

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
