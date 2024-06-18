package com.francelabs.datafari.solraccessors;

import com.francelabs.datafari.config.CollectionPathConfig;
import com.francelabs.datafari.config.JobConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generic Accessor of Solr documents. Provide common functionalities like setting configuration parameters to select or
 * update documents, creating a Solr Client to connect to Solr...
 */
public abstract class AbstractDocuments implements AutoCloseable{
  protected SolrClient solrClient;
  protected String solrCollection;
  protected int maxDocsPerQuery;

  protected JobConfig jobConfig;

  /**
   * Set all configuration attributes for this Accessor of Solr documents.
   *
   * @param jobConfig the configuration {@link JobConfig} object that contains all parameters related to the job using
   *                  this Accessor of Documents.
   * @throws IOException if this configuration replace a previous configuration, this try to close the existing Solr Client
   * if close fail, this exception is returned.
   */
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

  /**
   * @return the Solr Collection config associated with this Accessor of Solr documents.
   */
  protected abstract CollectionPathConfig getCollectionPath();

  /**
   * Create the SolrJ Client object used to connect to Solr and send queries. Use the base Solr server URL to create
   * the client object. The collection is not specified at this point. The collection intended to receive Solr queries
   * will be specified later in each request.
   *
   * @param solrBaseUrls the URL(s) of Solr or Zookeeper server(s), separated by comma if several are specified.
   *                     Can be:
   *                     <ul>
   *                       <li>Solr url like: http://localhost:8983/solr, http://myDomain:8983/solr... </li>
   *                       <li>Zookeeper url like: localhost:2181, myDomain:2181... With Zookeeper URL, it is not the
   *                       http protocol, so do not add this prefix to the URL. In Datafari Solr data is stored in the root of Zookeeper</li>
   *                     </ul>
   * @return The {@link SolrClient} created
   */
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

  /**
   * Close the Solr client object.
   * @throws Exception
   */
  @Override
  public void close() throws Exception {
    solrClient.close();
  }

}
