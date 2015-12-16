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
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.francelabs.datafari.alerts.Mail;


public class Alert {
	private String subject;
	private String address;
	private SolrClient solr; 
	private String keyword;
	private String frequency;
	private Mail mail;
	private String user;
	private final static Logger LOGGER = Logger.getLogger(Alert.class
			.getName());
	/**
	 * Initializes all the attributes
	 */
	public Alert(String subject, String address, SolrClient solr2,
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
			ResourceBundle res = ResourceBundle.getBundle("com.francelabs.i18n.text");
			SolrQuery query = new SolrQuery();
			switch(this.frequency){											//The fq parameter depends of the frequency of the alerts
			default :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
				break;
			case "Hourly" :
				query.setParam("fq", "last_modified:[NOW-1HOUR TO NOW]");
				break;
			case "Daily" :
				query.setParam("fq", "last_modified:[NOW-1DAY TO NOW]");
				break;
			case "Weekly" :
				query.setParam("fq", "last_modified:[NOW-7DAY TO NOW]");
				break;
			}
			query.setParam("rows", "10");										//Sets the maximum number of results that will be sent in the email
			query.setParam("q.op", "AND");												
			query.setParam("q", keyword);										//Sets the q parameters according to the attribute
			query.setParam("AuthenticatedUserName", user);
			QueryResponse queryResponse;
			try {
				queryResponse = solr.query(query);
			} catch (SolrServerException | NullPointerException e) {
				LOGGER.error("Error getting the results of the solr query in the Alert's run(), check the name of the core in the alert with the followind attributes, keyword : "+this.keyword+", frequency : "+this.frequency+", user : "+this.user+". Error 69044", e);
				return;
			}		
			String message = "";
			SolrDocumentList list = queryResponse.getResults();					//Gets the results
			if(list.getNumFound()!=0){											//If there are some results
				message += list.getNumFound()+" "+res.getString("alertsMessage")+" : "+query.get("q"); //First sentence of the mail
				if(Integer.parseInt(query.get("rows"))<list.size()){			//If there are more than 10 results(can be modified in the setParam("rows","X") line) only the first ten will be printed
					for(int i=0; i<Integer.parseInt(query.get("rows")); i++){	//For the ten first results puts the title in the mail
						message += "\n"+list.get(i).getFieldValue("title");
						message += "\n"+list.get(i).getFieldValue("url");
						message += "\n"+list.get(i).getFieldValue("last_modified")+"\n";
					}
				}
				else {
					for(int i=0; i<list.size(); i++){							//Else puts the title of all the results
						message += "\n"+list.get(i).getFieldValue("title");
						message += "\n"+list.get(i).getFieldValue("url");
						message += "\n"+list.get(i).getFieldValue("last_modified")+"\n";
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
