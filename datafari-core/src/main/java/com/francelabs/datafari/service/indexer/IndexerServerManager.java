package com.francelabs.datafari.service.indexer;

import java.util.HashMap;
import java.util.Map;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerInputDocument;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerQuery;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerServer;

public class IndexerServerManager {

  public enum Core {
    FILESHARE {
      @Override
      public String toString() {
        return "FileShare";
      }
    },
    STATISTICS {
      @Override
      public String toString() {
        return "Statistics";
      }
    },
    PROMOLINK {
      @Override
      public String toString() {
        return "Promolink";
      }
    }
  }

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

  public static IndexerQuery createQuery() {
    return new SolrIndexerQuery();
  }

  public static IndexerInputDocument createDocument() {
    return new SolrIndexerInputDocument();
  }

}
