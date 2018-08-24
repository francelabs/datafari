/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.search;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;

import com.francelabs.datafari.utils.ScriptConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.francelabs.datafari.alerts.AlertsManager;

public class SolrServers {

	private static String defaultURL = "localhost:2181";
	
	private final static Logger LOGGER = LogManager.getLogger(AlertsManager.class.getName());
	public enum Core {
		FILESHARE {
			public String toString() {
				return "FileShare";
			}
		},
		STATISTICS {
			public String toString() {
				return "Statistics";
			}
		},
		PROMOLINK {
			public String toString() {
				return "Promolink";
			}
		}
	}

	private static Map<Core, CloudSolrClient> solrClients = new HashMap<Core, CloudSolrClient>();

	public static SolrClient getSolrServer(Core core) throws Exception {
		// Zookeeper Hosts
		String solrHosts = ScriptConfiguration.getProperty("SOLRHOSTS");
		if (!solrClients.containsKey(core)) {
			try {
				// TODO : change for ZK ensemble
				CloudSolrClient solrClient = new CloudSolrClient(solrHosts);
				solrClient.setDefaultCollection(core.toString());
				solrClient.setZkClientTimeout(60000);
				SolrPing ping = new SolrPing();
				solrClient.request(ping);
				solrClients.put(core, solrClient);
			} catch (Exception e) {
				// test default param
				try {
					CloudSolrClient solrClient = new CloudSolrClient(defaultURL);
					solrClient.setDefaultCollection(core.toString());
					SolrPing ping = new SolrPing();
					solrClient.request(ping);
					solrClients.put(core, solrClient);
				} catch (Exception e2){
					LOGGER.error("Cannot instanciate Solr Client for core : " + core.toString(), e);
					throw new Exception("Cannot instanciate Solr Client for core : " + core.toString());
				}
			}
		}
		return solrClients.get(core);
	}

}
