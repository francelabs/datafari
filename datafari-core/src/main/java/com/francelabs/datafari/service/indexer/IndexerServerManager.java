package com.francelabs.datafari.service.indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.francelabs.datafari.service.indexer.solr.SolrIndexerInputDocument;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerQuery;
import com.francelabs.datafari.service.indexer.solr.SolrIndexerServer;
import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class IndexerServerManager {

	static String mainCollection ="FileShare";

	public enum Core {
		FILESHARE {
			@Override
			public String toString() {
				try {
					if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
						mainCollection = DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return mainCollection;
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

	private static Map<String, IndexerServer> serversList = new HashMap<>();

	public static IndexerServer getIndexerServer(final Core core) throws Exception {
		return getIndexerServer(core.toString());
	}

	public static IndexerServer getIndexerServer(final String core) throws Exception {
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
