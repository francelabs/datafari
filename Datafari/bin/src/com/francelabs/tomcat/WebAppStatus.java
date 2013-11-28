package com.francelabs.tomcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.francelabs.datafari.script.ScriptConfiguration;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class WebAppStatus {

	public static String urlManifoldCFAPI = null;
	public static String urlSolrCore = null;


	private final static Logger LOGGER = Logger.getLogger(WebAppStatus.class .getName());
	
	public static void waitUntilManifoldCFIsStarted() throws InterruptedException, IOException {
		if (urlManifoldCFAPI == null){
			urlManifoldCFAPI = ScriptConfiguration.getProperty("URLManifoldCFAPI")+"/json/jobstatuses";
		}
		waitUntilApplicationIsStarted(urlManifoldCFAPI);
	}

	public static void waitUntilSolrIsStarted() throws InterruptedException, IOException {
		if (urlSolrCore == null){
			urlSolrCore = ScriptConfiguration.getProperty("URLSolrCore")+"/admin/ping?wt=json";
		}
		waitUntilApplicationIsStarted(urlSolrCore);
	}

	public static void main(String[] args) throws Exception {
		waitUntilManifoldCFIsStarted();
		waitUntilSolrIsStarted();
	}

	private static void waitUntilApplicationIsStarted(String url) throws InterruptedException {

		HttpURLConnection connection = null;
		URL commandURL;
		JSONObject responseObject;

		boolean exception = true;

		do {

			try {

				// Create connection
				commandURL = new URL(url);
				connection = (HttpURLConnection) commandURL.openConnection();

				connection.setConnectTimeout(1000);
				connection.setRequestMethod("GET");

				// Get Response
				InputStream is = connection.getInputStream();
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(is));
				String line;

				StringBuffer response = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();

				
				exception = false;

			} catch (Exception e) {
				LOGGER.info("Waiting for application start : "+url);
			} finally {

				if (connection != null) {
					connection.disconnect();
				}
			}
		
		Thread.sleep(1000);
			
		} while (exception);
	}

}
