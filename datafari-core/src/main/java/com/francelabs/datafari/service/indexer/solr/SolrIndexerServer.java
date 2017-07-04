package com.francelabs.datafari.service.indexer.solr;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.QueryResponse;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.utils.ScriptConfiguration;

public class SolrIndexerServer implements IndexerServer {

  private static String defaultURL = "localhost:2181";
  private final Logger LOGGER = Logger.getLogger(SolrIndexerServer.class.getName());
  private CloudSolrClient client;

  public SolrIndexerServer(final Core core) throws Exception {
    // Zookeeper Hosts
    final String solrHosts = ScriptConfiguration.getProperty("SOLRHOSTS");

    try {
      // TODO : change for ZK ensemble
      client = new CloudSolrClient.Builder().withZkHost(solrHosts).build();
      client.setDefaultCollection(core.toString());
      final SolrPing ping = new SolrPing();
      client.request(ping);
    } catch (final Exception e) {
      // test default param
      try {
        client = new CloudSolrClient.Builder().withZkHost(defaultURL).build();
        client.setDefaultCollection(core.toString());
        final SolrPing ping = new SolrPing();
        client.request(ping);
      } catch (final Exception e2) {
        LOGGER.error("Cannot instanciate Solr Client for core : " + core.toString(), e);
        throw new Exception("Cannot instanciate Solr Client for core : " + core.toString());
      }
    }

  }

  @Override
  public IndexerQuery createQuery() {
    return new SolrIndexerQuery();
  }

  @Override
  public IndexerResponse executeQuery(final IndexerQuery query) throws Exception {
    try {
      final SolrQuery solrQuery = ((SolrIndexerQuery) query).prepareQuery();
      final QueryResponse response = client.query(solrQuery);
      final SolrIndexerResponse sir = new SolrIndexerResponse(solrQuery, response);
      return sir;
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
