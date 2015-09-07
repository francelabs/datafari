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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import org.apache.log4j.Logger;
import com.francelabs.datafari.alerts.AlertsManager;
import com.francelabs.datafari.utils.ScriptConfiguration;

public class SolrServers {

	private final static Logger LOGGER = Logger.getLogger(AlertsManager.class
			.getName());

	private static String host = "localhost";
	private static String solrWebapp = "solr";
	private static String solrPort = "8983";
	private static String zookeeperPort = "9080";

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

	private static Map<Core, SolrClient> solrClients = new HashMap<Core, SolrClient>();

	public static SolrClient getSolrServer(Core core) throws Exception {
		if (!solrClients.containsKey(core)) {
			try {
				SolrClient solrClient;
				if (ScriptConfiguration.getProperty("SOLRCLOUD").equals("true")) {
					solrClient = new CloudSolrClient(host + ":" + zookeeperPort);
					((CloudSolrClient) solrClient).setDefaultCollection(core
							.toString());
				} else {
					solrClient = new HttpSolrClient("http://" + host + ":"
							+ solrPort + "/" + solrWebapp + "/"
							+ core.toString());
				}
				solrClients.put(core, solrClient);
			} catch (Exception e) {
				LOGGER.error("Cannot instanciate Solr Client for core : "
						+ core.toString(), e);
				throw new Exception(
						"Cannot instanciate Solr Client for core : "
								+ core.toString());
			}
		}
		return solrClients.get(core);
	}

}
