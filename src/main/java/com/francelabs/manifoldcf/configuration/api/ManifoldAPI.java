package com.francelabs.manifoldcf.configuration.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ManifoldAPI {

	private final static Logger LOGGER = Logger.getLogger(ManifoldAPI.class.getName());

	public static class COMMANDS {

		static public String JOBSTATUSES = "jobstatuses";
		static public String OUTPUTCONNECTIONS = "outputconnections";
		static public String REPOSITORYCONNECTIONS = "repositoryconnections";
		static public String AUTHORITYCONNECTIONS = "authorityconnections";
		static public String MAPPINGCONNECTIONS = "mappingconnections";
		static public String TRANSFORMATIONCONNECTIONS = "transformationconnections";
		static public String AUTHORITYGROUPS = "authoritygroups";
		static public String JOBS = "jobs";
	}

	static private String urlManifoldCFAPI = "http://localhost:8080/datafari-mcf-api-service/json/";

	public JSONObject readConfiguration() {
		return null;
	}

	static private HttpClient client = null;

	static private HttpClient getClient() {
		if (client == null) {
			client = HttpClientBuilder.create().build();
		}
		return client;
	}

	static public void cleanAll() throws Exception {
		ManifoldAPI.cleanJobs();
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.AUTHORITYGROUPS);
	}

	static public void cleanJobs() throws Exception {
		String command = "jobs";
		JSONObject jobs = getInfo("jobs");
		String subCommands = "job";
		JSONObject job = jobs.optJSONObject(subCommands);
		if (job != null) {
			delete(command, job.getString("id"));
			waitJob(job.getString("id"));
		}
		JSONArray jobList = jobs.optJSONArray(subCommands);
		if (jobList != null) {
			for (int i = 0; i < jobList.length(); i++) {
				JSONObject singleConnector = jobList.getJSONObject(i);
				delete(command, singleConnector.getString("id"));
				waitJob(singleConnector.getString("id"));
			}
		}
	}

	private static void waitJob(String id) throws Exception {
		JSONObject result;
		do {
			Thread.sleep(1000);
			result = readConfig(COMMANDS.JOBSTATUSES, id);
			LOGGER.info(result.toString());
		} while (result.length() != 0);

	}

	static public void cleanConnectors(String command) throws Exception {
		LOGGER.info("Start cleaning " + command);
		JSONObject connectors = getInfo(command);
		String subCommands = command.substring(0, command.length() - 1);
		JSONObject connector = connectors.optJSONObject(subCommands);
		if (connector != null) {
			delete(command, connector.getString("name"));
		}
		JSONArray connectorList = connectors.optJSONArray(subCommands);
		if (connectorList != null) {
			for (int i = 0; i < connectorList.length(); i++) {
				JSONObject singleConnector = connectorList.getJSONObject(i);
				delete(command, singleConnector.getString("name"));
			}
		}
		LOGGER.info("Connectors " + command + " cleaned");
	}

	private static void delete(String command, String paramName) throws Exception {

		LOGGER.info("Deleting connector " + paramName);
		String url = urlManifoldCFAPI + command + "/" + paramName;
		JSONObject result = executeCommand(url, "DELETE", null);

		if (result.length() != 0)
			throw new Exception(result.toString());

		LOGGER.info("Connector " + paramName + " deleted");

	}

	static public JSONObject getInfo(String command) throws IOException {

		String url = urlManifoldCFAPI + command;
		return executeCommand(url, "GET", null);
	}

	static public JSONObject readConfig(String command, String paramName) throws IOException {

		String url = urlManifoldCFAPI + command + "/" + paramName;
		return executeCommand(url, "GET", null);
	}

	static private void createConnectorFile(String command, JSONObject subConnector,
			Map<String, JSONObject> connectorsMap) throws JSONException {
		JSONObject singleConnector = new JSONObject();
		if (command.equals(ManifoldAPI.COMMANDS.JOBS)) {
			connectorsMap.put(subConnector.getString("id"), singleConnector);
		} else {
			connectorsMap.put(subConnector.getString("name"), singleConnector);
			subConnector.put("isnew", true);
		}
		singleConnector.append(command.substring(0, command.length() - 1), subConnector);
	}

	static public Map<String, JSONObject> getConnections(String command) throws Exception {
		LOGGER.info("Get connectors " + command);
		Map<String, JSONObject> connectorsMap = new HashMap<String, JSONObject>();
		JSONObject connectors = getInfo(command);
		String subCommands = command.substring(0, command.length() - 1);
		JSONObject connector = connectors.optJSONObject(subCommands);
		if (connector != null) {
			createConnectorFile(command, connector, connectorsMap);
		}
		JSONArray connectorList = connectors.optJSONArray(subCommands);
		if (connectorList != null) {
			for (int i = 0; i < connectorList.length(); i++) {
				createConnectorFile(command, connectorList.getJSONObject(i), connectorsMap);
			}
		}

		return connectorsMap;
	}

	static public void putConfig(String command, String paramName, JSONObject configuration) throws Exception {

		LOGGER.info("Putting new config for " + paramName);
		String url = urlManifoldCFAPI + command + "/" + paramName;
		JSONObject result = executeCommand(url, "PUT", configuration);
		if (result.length() != 0)
			throw new Exception(result.toString());

		LOGGER.info("Config for new connector " + paramName + " set");
	}

	static public void deleteConfig(String command, String paramName) throws Exception {
		LOGGER.info("Delete config for " + paramName);
		String url = urlManifoldCFAPI + command + "/" + paramName;
		JSONObject result = executeCommand(url, "DELETE", null);
		if (result.length() != 0)
			throw new Exception(result.toString());

		LOGGER.info("Connector " + paramName + " deleted");
	}

	static public JSONObject getConfig(String command, String paramName) throws Exception {

		LOGGER.info("Getting configuration for " + paramName);
		String url = urlManifoldCFAPI + command + "/" + paramName;
		JSONObject result = executeCommand(url, "GET", null);
		if (result.length() != 0)
			throw new Exception(result.toString());

		return result;
	}

	static private JSONObject executeCommand(String url, String verb, JSONObject jsonObject) throws IOException {

		HttpClient client = getClient();
		HttpRequestBase request = null;
		JSONObject responseObject = null;

		try {

			if (verb.equals("PUT") || verb.equals("POST")) {
				StringEntity params = new StringEntity(jsonObject.toString());
				if (verb.equals("POST")) {
					request = new HttpPost(url);
					((HttpPost) request).setEntity(params);
				} else {
					request = new HttpPut(url);
					((HttpPut) request).setEntity(params);
				}
				request.addHeader("Content-Type", "application/json");
			} else {
				request = new HttpGet(url);
			}

			HttpResponse response = client.execute(request);

			// Get Response
			InputStream is;

			is = response.getEntity().getContent();

			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;

			StringBuffer responseText = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				responseText.append(line);
				responseText.append('\r');
			}
			rd.close();

			responseObject = new JSONObject(responseText.toString());

		} catch (Exception e) {
			LOGGER.warn("Error : ", e);
		}

		return responseObject;

	}

	public static void waitUntilManifoldIsStarted() throws InterruptedException {

		LOGGER.info("Wait until MCF is started");
		HttpClient client = getClient();

		boolean exception = true;

		do {

			try {


				Thread.sleep(1000);
				
				HttpGet request = new HttpGet(urlManifoldCFAPI + "jobstatuses");
				HttpResponse response = client.execute(request);

				// Get Response
				InputStream is = response.getEntity().getContent();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is));
				String line;

				StringBuffer responseText = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					responseText.append(line);
					responseText.append('\r');
				}
				rd.close();

				exception = false;

			} catch (Exception e) {
				LOGGER.info("Waiting for mcf-api-service start");
			}


		} while (exception);
	}

	public static void authenticate(String apiUsername, String apiPassword) throws IOException {
		JSONObject json = new JSONObject("{\"userID\":\"" + apiUsername + "\", \"password\":\"" + apiPassword + "\"}");
		executeCommand(urlManifoldCFAPI + "LOGIN", "POST", json);
	}

}
