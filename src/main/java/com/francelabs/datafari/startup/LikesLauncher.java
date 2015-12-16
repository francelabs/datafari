/*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.startup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;

import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;
import com.francelabs.datafari.servlets.admin.StringsDatafariProperties;
import com.francelabs.datafari.utils.ScriptConfiguration;
import com.francelabs.datafari.utils.UpdateNbLikes;

public class LikesLauncher implements ServletContextListener {

	private static boolean islaunched = false;
	private static ScheduledExecutorService scheduler;
	private static Logger LOGGER = Logger.getLogger(LikesLauncher.class
			.getName());
	private static ScheduledFuture<?> handler;
	private static boolean doReload = false;
	private static boolean isThreadUpdateNbLikesStarted = false;

	@Override
	public void contextInitialized(ServletContextEvent arg0) {

		String isEnabled = null;
		try {
			isEnabled = ScriptConfiguration
					.getProperty(StringsDatafariProperties.LIKESANDFAVORTES);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		try {
			File externalFile = UpdateNbLikes.getInstance().getConfigFile();
			externalFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (isEnabled != null && isEnabled.equals("true")) {
			updateNbLikes();
			startScheduler();
		}
		LOGGER.debug("LikesLauncher Servlet Initialized successfully");
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		LikesLauncher.shutDown();
	}

	private void updateNbLikes() {
		if (!LikesLauncher.isThreadUpdateNbLikesStarted) {
			LikesLauncher.isThreadUpdateNbLikesStarted = true;
			new Thread(new Runnable() {
				public void run() {
					try {
						SolrClient solrClient = SolrServers
								.getSolrServer(Core.FILESHARE);
						SolrQuery refreshQuery = new SolrQuery();
						refreshQuery.set("qt", "reloadCache");
						solrClient.query(refreshQuery);
					} catch (Exception e) {
						LOGGER.error("Cannot send refresh request", e);
					}
					LOGGER.info("updateNbLikes finished its work");
				}

			}).start();
		}
	}

	public static void startScheduler() {
		if (!islaunched) {
			islaunched = true;
			scheduler = Executors.newScheduledThreadPool(1);
			handler = scheduler.scheduleAtFixedRate(reloadCache, 1, 10,
					TimeUnit.SECONDS);
		}
	}

	public static void saveChange() {
		LikesLauncher.doReload = true;
	}

	static Runnable reloadCache = new Runnable() {
		@Override
		public void run() {
			if (LikesLauncher.doReload) {
				try {
					SolrClient solrClient = SolrServers
							.getSolrServer(Core.FILESHARE);
					SolrQuery refreshQuery = new SolrQuery();
					refreshQuery.set("qt", "reloadCache");
					solrClient.query(refreshQuery);
					LikesLauncher.doReload = false;
				} catch (Exception e) {

					LOGGER.error("Cannot reload cache", e);
				}
			}
		}
	};

	public static void shutDown() {
		LOGGER.debug("-----------------Trying to ShutDown the scheduler---------------------");
		if (islaunched) {
			handler.cancel(true);
			scheduler.shutdown();
		}
		islaunched = false;
	}

}
