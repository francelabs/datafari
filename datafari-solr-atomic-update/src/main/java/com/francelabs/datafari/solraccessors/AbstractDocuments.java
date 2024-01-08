package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

  protected SolrClient getSolrClient(final String solrBaseUrls){
    String[] tabBaseSolUrls = solrBaseUrls.split(",");
    final List<String> cloudServers = new ArrayList<>();
    for (String baseSolrUrl : tabBaseSolUrls){
      cloudServers.add(baseSolrUrl.trim());
    }
    SolrClient client;
    if (tabBaseSolUrls[0].startsWith("http")){ // Solr server(s)
      client = new CloudSolrClient.Builder(cloudServers).build();
    } else { // Zookeeper server(s)
      client = new CloudSolrClient.Builder(cloudServers, Optional.empty()).build();
    }

    return client;
  }
  @Override
  public void close() throws Exception {
    solrClient.close();
  }

}
