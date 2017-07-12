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
package com.francelabs.datafari.alerts;
/**
 * 
 * This class makes the solr request and creates a mail
 * It is instantiated by the runnables in AlertsManager
 * All the attributes gets their values from the constructor
 * @author Alexis Karassev
 *
 */

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.francelabs.datafari.alerts.Mail;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryManager;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;


public class Alert {
	private String subject;
	private String address;
	private IndexerServer solr; 
	private String keyword;
	private String frequency;
	private Mail mail;
	private String user;
	private final static Logger LOGGER = Logger.getLogger(Alert.class
			.getName());
	/**
	 * Initializes all the attributes
	 */
	public Alert(String subject, String address, IndexerServer solr2,
			String keyword, String frequency, Mail mail, String user) {
		this.subject = subject;
		this.address = address;
		this.solr = solr2;
		this.keyword = keyword;
		this.frequency = frequency;
		this.mail = mail;
		this.user = user;
	}

	/**
	 * Creates a solr query
	 * Creates a mail with the title of each result
	 * Send the mail
	 */
	public void run() {
		try {
			IndexerQuery query = IndexerQueryManager.createQuery();
			switch(this.frequency){											//The fq parameter depends of the frequency of the alerts
			default :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
				break;
			case "hourly" :
				query.setParam("fq", "last_modified:[NOW-1HOUR TO NOW]");
				break;
			case "daily" :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
				break;
			case "weekly" :
				query.setParam("fq", "last_modified:[NOW-7DAY TO NOW]");
				break;
			}
			query.setParam("rows", "10");										//Sets the maximum number of results that will be sent in the email
			query.setParam("q.op", "AND");												
			query.setParam("q", keyword);										//Sets the q parameters according to the attribute
			query.setParam("AuthenticatedUserName", user);
			IndexerQueryResponse queryResponse;
			try {
				queryResponse = solr.executeQuery(query);
			} catch (SolrServerException | NullPointerException e) {
				LOGGER.error("Error getting the results of the solr query in the Alert's run(), check the name of the core in the alert with the followind attributes, keyword : "+this.keyword+", frequency : "+this.frequency+", user : "+this.user+". Error 69044", e);
				return;
			}		
			String message = "";
			JSONArray list = queryResponse.getResults();					//Gets the results
			if(queryResponse.getNumFound()!=0){											//If there are some results
				// TODO remove the language hardcode here (before ResourceBundle was used, now removed with Maven refacto)
				message += queryResponse.getNumFound()+" new or modified document(s) has/have been found for the key : "+query.getParamValue("q"); //First sentence of the mail
				if(Integer.parseInt(query.getParamValue("rows"))<list.length()){			//If there are more than 10 results(can be modified in the setParam("rows","X") line) only the first ten will be printed
					for(int i=0; i<Integer.parseInt(query.getParamValue("rows")); i++){	//For the ten first results puts the title in the mail
						JSONObject result = list.getJSONObject(i);
					  message += "\n"+result.getJSONArray("title").getString(0);
						message += "\n"+result.getString("url");
						message += "\n"+result.getString("last_modified")+"\n";
					}
				}
				else {
					for(int i=0; i<list.length(); i++){							//Else puts the title of all the results
					  JSONObject result = list.getJSONObject(i);
						message += "\n"+result.getJSONArray("title").getString(0);
						message += "\n"+result.getString("url");
						message += "\n"+result.getString("last_modified")+"\n";
					}
				}
				//Sends the mail (the last parameter is "" because other destinations are not necessary but you can add a String)														
				mail.sendMessage(subject, message, address, "");
			}
		}catch(Exception e){
			LOGGER.error("Unindentified error in Alert's run(). Error 69521", e);
			return;
		}
	}
}
