package com.francelabs.manifoldcf.configuration.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ManifoldAPI {

	private final static Logger LOGGER = Logger.getLogger(ManifoldAPI.class
			.getName());

	public static class COMMANDS {

		static public String JOBSTATUSES = "jobstatuses";
		static public String OUTPUTCONNECTIONS = "outputconnections";
		static public String REPOSITORYCONNECTIONS = "repositoryconnections";
		static public String AUTHORITYCONNECTIONS = "authorityconnections";
		static public String JOBS = "jobs";
	}

	static private String urlManifoldCFAPI = "http://localhost:8080/datafari-mcf-api-service/json/";
	
	public JSONObject readConfiguration() {
		return null;
	}

	static public void cleanAll() throws Exception {
		ManifoldAPI.cleanJobs();
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);
		ManifoldAPI.cleanConnectors(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
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

	private static void delete(String command, String paramName)
			throws Exception {

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

	static public JSONObject readConfig(String command, String paramName)
			throws IOException {

		String url = urlManifoldCFAPI + command + "/" + paramName;
		return executeCommand(url, "GET", null);
	}

	static private void createConnectorFile(String command,
			JSONObject subConnector, Map<String, JSONObject> connectorsMap)
			throws JSONException {
		JSONObject singleConnector = new JSONObject();
		if (command.equals(ManifoldAPI.COMMANDS.JOBS)) {
			connectorsMap.put(subConnector.getString("id"), singleConnector);
		} else {
			connectorsMap.put(subConnector.getString("name"), singleConnector);
			subConnector.put("isnew", true);
		}
		singleConnector.append(command.substring(0, command.length() - 1),
				subConnector);
	}

	static public Map<String, JSONObject> getConnections(String command)
			throws Exception {
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
				createConnectorFile(command, connectorList.getJSONObject(i),
						connectorsMap);
			}
		}

		return connectorsMap;
	}

	static public void putConfig(String command, String paramName,
			JSONObject configuration) throws Exception {

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


	static public JSONObject getConfig(String command, String paramName)
			throws Exception {

		LOGGER.info("Getting configuration for " + paramName);
		String url = urlManifoldCFAPI + command + "/" + paramName;
		JSONObject result = executeCommand(url, "GET", null);
		if (result.length() != 0)
			throw new Exception(result.toString());

		return result;
	}

	static private JSONObject executeCommand(String url, String verb,
			JSONObject jsonObject) throws IOException {

		URL commandURL;
		HttpURLConnection connection = null;
		JSONObject responseObject = null;

		try {

			// Create connection
			commandURL = new URL(url);
			connection = (HttpURLConnection) commandURL.openConnection();

			connection.setRequestMethod(verb);

			if (verb.equals("PUT")) {
				connection.setRequestProperty("Content-Type",
						"application/json");
				connection.setUseCaches(false);
				connection.setDoInput(true);
				connection.setDoOutput(true);

				// Send request
				DataOutputStream wr = new DataOutputStream(
						connection.getOutputStream());

				wr.writeBytes(jsonObject.toString());

				wr.flush();
				wr.close();
			}

			// Get Response
			InputStream is;

			if (connection.getResponseCode() >= 400) {
				is = connection.getErrorStream();
			} else {
				is = connection.getInputStream();
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;

			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();

			responseObject = new JSONObject(response.toString());

		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}

		return responseObject;

	}
	

	public static void waitUntilManifoldIsStarted() throws InterruptedException {

		HttpURLConnection connection = null;
		URL commandURL;
		JSONObject responseObject;

		boolean exception = true;

		do {

			try {

				// Create connection
				commandURL = new URL(urlManifoldCFAPI+"jobstatuses");
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
				LOGGER.info("Waiting for mcf-api-service start");
			} finally {

				if (connection != null) {
					connection.disconnect();
				}
			}
		
		Thread.sleep(1000);
			
		} while (exception);
	}

}
