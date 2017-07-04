package com.francelabs.datafari.service.indexer;

import java.util.HashMap;
import java.util.Map;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerServer;
import com.francelabs.datafari.service.search.SolrServers.Core;

public class IndexerServerManager {

  private static Map<Core, IndexerServer> serversList = new HashMap<Core, IndexerServer>();

  public static IndexerServer getIndexerServer(final Core core) throws Exception {
    if (serversList.containsKey(core)) {
      return serversList.get(core);
    } else {
      final IndexerServer newServer = new SolrIndexerServer(core);
      serversList.put(core, newServer);
      return newServer;
    }
  }

}
