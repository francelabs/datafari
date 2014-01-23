package com.francelabs.datafari.solrj;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

public class SolrServers {
	

	private static String baseUrl = "http://localhost:8080/datafari-solr/";

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
		  }
	}
	
	private static Map<Core, HttpSolrServer> solrServers = new HashMap<Core, HttpSolrServer>();
	

	
	public static HttpSolrServer getSolrServer(Core core){
		if (!solrServers.containsKey(core)){
			solrServers.put(core, new HttpSolrServer(baseUrl+core));
		}
		return solrServers.get(core);
	}
	
	
	
	
}
