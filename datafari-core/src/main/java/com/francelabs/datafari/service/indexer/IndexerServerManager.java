package com.francelabs.datafari.service.indexer;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerInputDocument;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerQuery;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerServer;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerUpdateRequest;
import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class IndexerServerManager {

  static String mainCollection = "FileShare";
  private static final Logger LOGGER = LogManager.getLogger(IndexerServerManager.class.getName());

  public enum Core {
    FILESHARE {
      @Override
      public String toString() {

        if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION) != null) {
          mainCollection = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION);
        }
        return mainCollection;
      }
    },
    VECTORMAIN {
      @Override
      public String toString() {
        return "VectorMain";
      }
    },
    PROMOLINK {
      @Override
      public String toString() {
        return "Promolink";
      }
    },
    DUPLICATES {
      @Override
      public String toString() {
        return "Duplicates";
      }
    },
    ENTITIES {
      @Override
      public String toString() {
        return "Entities";
      }
    }
  }

  private static Map<String, IndexerServer> serversList = new HashMap<>();

  public static IndexerServer getIndexerServer(final Core core) throws Exception {
    return getIndexerServer(core.toString());
  }

  public synchronized static IndexerServer getIndexerServer(final String core) throws Exception {
    if (serversList.containsKey(core)) {
      return serversList.get(core);
    } else {
      final IndexerServer newServer = new SolrIndexerServer(core);
      serversList.put(core, newServer);
      return newServer;
    }
  }

  public synchronized static void closeAllIndexerServers() {
    serversList.forEach((core, server) -> {
      try {
        server.close();
      } catch (final Exception e) {
        LOGGER.error("Unable to shutdown indexer server of core " + core, e);
      }
    });
  }

  public static IndexerQuery createQuery() {
    return new SolrIndexerQuery();
  }

  public static IndexerUpdateRequest createUpdateRequest(final String updateHandler) {
    return new SolrIndexerUpdateRequest(updateHandler);
  }

  public static IndexerInputDocument createDocument() {
    return new SolrIndexerInputDocument();
  }

}
