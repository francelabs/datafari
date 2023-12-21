package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDocuments implements AutoCloseable{
  protected SolrClient solrClient;
  protected String solrCollection;
  protected int maxDocsPerQuery;

  protected JobConfig jobConfig;

  public void setJobConfig(final JobConfig jobConfig) throws IOException {
    this.jobConfig = jobConfig;
    final CollectionPathConfig collectionPath = getCollectionPath();
    if (solrClient != null){
        solrClient.close();
    }
    this.solrClient = getSolrClient(collectionPath.getBaseUrl());

    this.solrCollection = collectionPath.getSolrCollection();
    this.maxDocsPerQuery = jobConfig.getNbDocsPerBatch();

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
  @Override
  public void close() throws Exception {
    solrClient.close();
  }

}
