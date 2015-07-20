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
package com.francelabs.datafari.solrj;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.francelabs.datafari.utils.ScriptConfiguration;

public class SolrServers {

	private static String host = "localhost";
	private static String solrWebapp = "datafari-solr";
	private static String solrPort = "8080";
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

	private static Map<Core, SolrServer> solrServers = new HashMap<Core, SolrServer>();

	public static SolrServer getSolrServer(Core core) throws IOException {
		if (!solrServers.containsKey(core)) {
			SolrServer server;
			if (ScriptConfiguration.getProperty("SOLRCLOUD").equals("true")) {
				server = new CloudSolrServer(host+":"+zookeeperPort);
				((CloudSolrServer)server).setDefaultCollection(core.toString());
			} else {
				server = new HttpSolrServer("http://" + host
						+ ":" + solrPort + "/" + solrWebapp + "/"+core.toString());
			}
			solrServers.put(core, server);
		}
		return solrServers.get(core);
	}

}
