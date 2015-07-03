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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.francelabs.datafari.alerts.Mail;

public class Alert {
	private String subject;
	private String address;
	private SolrServer solr; 
	private String keyword;
	private String frequency;
	
	/**
	 * Initializes all the attributes
	 */
	public Alert(String subject, String address, SolrServer solr,
			String keyword, String frequency ) {
		super();
		this.subject = subject;
		this.address = address;
		this.solr = solr;
		this.keyword = keyword;
		this.frequency = frequency;
	}

	/**
	 * Creates a solr query
	 * Creates a mail with the title of each result
	 * Send the mail
	 */
	public void run() {
		try{
			SolrQuery query = new SolrQuery();
			switch(this.frequency){											//The fq parameter depends of the frequency of the alerts
			case "Hourly" :
				query.setParam("fq", "last_modified:[NOW-1HOUR TO NOW]");
			case "Daily" :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
			case "Weekly" :
				query.setParam("fq", "last_modified:[NOW-7DAY TO NOW]");
			default :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
			}
			query.setParam("rows", "10");										//Sets the maximum number of results that will be sent in the email
			query.setParam("q.op", "AND");												
			query.setParam("q", keyword);										//Sets the q parameters according to the attribute
			QueryResponse queryResponse = solr.query(query);	
			Mail mail = new Mail();
			String message = "";
			SolrDocumentList list = queryResponse.getResults();					//Gets the results
			if(list.getNumFound()!=0){											//If there are some results
				message += list.getNumFound()+" new or modified document(s) has/have been found for the key "+query.get("q"); //First sentence of the mail
				if(Integer.parseInt(query.get("rows"))<list.size()){			//If there are more than 10 results(can be modified in the setParam("rows","X") line) only the first ten will be printed
					for(int i=0; i<Integer.parseInt(query.get("rows")); i++){	//For the ten first results puts the title in the mail
						message += "\n"+list.get(i).getFieldValue("title");
					}
				}
				else {
					for(int i=0; i<list.size(); i++){							//Else puts the title of all the results
						message += "\n"+list.get(i).getFieldValue("title");
					}
				}
				mail.sendMessage(subject, message, address, "");			//Sends the mail (the last parameter is "" because other destinations are not necessary but you can add a String)
			}
		}catch(Exception e){
			e.printStackTrace();
		}

	}
}
