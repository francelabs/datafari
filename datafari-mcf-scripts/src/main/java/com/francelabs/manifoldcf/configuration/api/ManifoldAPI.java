package com.francelabs.manifoldcf.configuration.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.LockManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.ThreadContextFactory;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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

  static private HttpClient getClient() throws ManifoldCFException, InterruptedException, IOException, ParseException {

    if (client == null) {
      client = HttpClientBuilder.create().build();

      final IThreadContext tc = ThreadContextFactory.make();
      ManifoldCF.initializeEnvironment(tc);

      final String masterDatabaseUsername = LockManagerFactory.getStringProperty(tc, "org.apache.manifoldcf.apilogin.password.obfuscated", "");

      ManifoldAPI.waitUntilManifoldIsStarted(client);

      if (!"".equals(masterDatabaseUsername)) {
        final String apiPassword = ManifoldCF.deobfuscate(masterDatabaseUsername);
        LOGGER.info("Try to authenticate");
        ManifoldAPI.authenticate("", apiPassword, client);
      }

    }

    return client;
  }

  static public void useHttpsProtocol() {
    urlManifoldCFAPI = "https://localhost:8443/datafari-mcf-api-service/json/";
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
    final String command = "jobs";
    final JSONObject jobs = getInfo("jobs");
    final String subCommands = "job";

    final Object objGet = jobs.get(subCommands);
    if (objGet != null) {
      if (objGet instanceof JSONObject) {
        final JSONObject job = (JSONObject) objGet;
        delete(command, (String) job.get("id"));
        waitJob((String) job.get("id"));
      } else if (objGet instanceof JSONArray) {
        final JSONArray jobsList = (JSONArray) objGet;
        for (int i = 0; i < jobsList.size(); i++) {
          final JSONObject singleConnector = (JSONObject) jobsList.get(i);
          delete(command, (String) singleConnector.get("id"));
          waitJob((String) singleConnector.get("id"));
        }
      }
    }
  }

  private static void waitJob(final String id) throws Exception {
    JSONObject result;
    do {
      Thread.sleep(1000);
      result = readConfig(COMMANDS.JOBSTATUSES, id);
      LOGGER.info(result.toString());
    } while (result.size() != 0);

  }

  static public void cleanConnectors(final String command) throws Exception {
    LOGGER.info("Start cleaning " + command);
    final JSONObject connectors = getInfo(command);
    final String subCommands = command.substring(0, command.length() - 1);

    final Object objGet = connectors.get(subCommands);
    if (objGet != null) {
      if (objGet instanceof JSONObject) {
        final JSONObject connector = (JSONObject) objGet;
        delete(command, (String) connector.get("name"));
      } else if (objGet instanceof JSONArray) {
        final JSONArray connectorList = (JSONArray) connectors.get(subCommands);
        for (int i = 0; i < connectorList.size(); i++) {
          final JSONObject singleConnector = (JSONObject) connectorList.get(i);
          delete(command, (String) singleConnector.get("name"));
        }
      }
    }

    LOGGER.info("Connectors " + command + " cleaned");
  }

  private static void delete(final String command, final String paramName) throws Exception {

    LOGGER.info("Deleting connector " + paramName);
    final String url = urlManifoldCFAPI + command + "/" + paramName;
    final JSONObject result = executeCommand(url, "DELETE", null);

    if (result.size() != 0)
      throw new Exception(result.toString());

    LOGGER.info("Connector " + paramName + " deleted");

  }

  static public JSONObject getInfo(final String command) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = urlManifoldCFAPI + command;
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readConfig(final String command, final String paramName)
      throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = urlManifoldCFAPI + command + "/" + paramName;
    return executeCommand(url, "GET", null);
  }

  static private void createConnectorFile(final String command, final JSONObject subConnector, final Map<String, JSONObject> connectorsMap) {
    final JSONObject singleConnector = new JSONObject();
    if (command.equals(ManifoldAPI.COMMANDS.JOBS)) {
      connectorsMap.put((String) subConnector.get("id"), singleConnector);
    } else {
      connectorsMap.put((String) subConnector.get("name"), singleConnector);
      subConnector.put("isnew", true);
    }
    append(singleConnector, command.substring(0, command.length() - 1), subConnector);
  }

  static public Map<String, JSONObject> getConnections(final String command) throws Exception {
    LOGGER.info("Get connectors " + command);
    final Map<String, JSONObject> connectorsMap = new HashMap<>();
    final JSONObject connectors = getInfo(command);
    final String subCommands = command.substring(0, command.length() - 1);

    final Object objGet = connectors.get(subCommands);
    if (objGet != null) {
      if (objGet instanceof JSONObject) {
        final JSONObject connector = (JSONObject) objGet;
        createConnectorFile(command, connector, connectorsMap);
      } else if (objGet instanceof JSONArray) {
        final JSONArray connectorList = (JSONArray) connectors.get(subCommands);
        for (int i = 0; i < connectorList.size(); i++) {
          final JSONObject singleConnector = (JSONObject) connectorList.get(i);
          createConnectorFile(command, singleConnector, connectorsMap);
        }
      }
    }

    return connectorsMap;
  }

  static public void putConfig(final String command, final String paramName, final JSONObject configuration) throws Exception {

    LOGGER.info("Putting new config for " + paramName);
    final String url = urlManifoldCFAPI + command + "/" + paramName;
    final JSONObject result = executeCommand(url, "PUT", configuration);
    if (result.size() != 0)
      throw new Exception(result.toString());

    LOGGER.info("Config for new connector " + paramName + " set");
  }

  static public void deleteConfig(final String command, final String paramName) throws Exception {
    LOGGER.info("Delete config for " + paramName);
    final String url = urlManifoldCFAPI + command + "/" + paramName;
    final JSONObject result = executeCommand(url, "DELETE", null);
    if (result.size() != 0)
      throw new Exception(result.toString());

    LOGGER.info("Connector " + paramName + " deleted");
  }

  static public JSONObject getConfig(final String command, final String paramName) throws Exception {

    LOGGER.info("Getting configuration for " + paramName);
    final String url = urlManifoldCFAPI + command + "/" + paramName;
    final JSONObject result = executeCommand(url, "GET", null);
    if (result.size() != 0)
      throw new Exception(result.toString());

    return result;
  }

  static private JSONObject executeCommand(final String url, final String verb, final JSONObject jsonObject, final HttpClient client)
      throws IOException, ManifoldCFException, InterruptedException {

    HttpRequestBase request = null;
    JSONObject responseObject = null;

    try {

      if (verb.equals("PUT") || verb.equals("POST")) {
        final StringEntity params = new StringEntity(jsonObject.toString());
        if (verb.equals("POST")) {
          request = new HttpPost(url);
          ((HttpPost) request).setEntity(params);
        } else {
          request = new HttpPut(url);
          ((HttpPut) request).setEntity(params);
        }
        request.addHeader("Content-Type", "application/json");
      } else {
        if (verb.equals("DELETE")) {
          request = new HttpDelete(url);
        } else {
          request = new HttpGet(url);
        }
      }

      final HttpResponse response = client.execute(request);

      // Get Response
      InputStream is;

      is = response.getEntity().getContent();

      final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      String line;

      final StringBuffer responseText = new StringBuffer();
      while ((line = rd.readLine()) != null) {
        responseText.append(line);
        responseText.append('\r');
      }
      rd.close();

      final JSONParser parser = new JSONParser();
      responseObject = (JSONObject) parser.parse(responseText.toString());

    } catch (final Exception e) {
      LOGGER.warn("Error : ", e);
    }

    return responseObject;
  }

  static private JSONObject executeCommand(final String url, final String verb, final JSONObject jsonObject)
      throws IOException, ManifoldCFException, InterruptedException, ParseException {
    final HttpClient client = getClient();
    return executeCommand(url, verb, jsonObject, client);

  }

  private static void waitUntilManifoldIsStarted(final HttpClient client) throws InterruptedException, ManifoldCFException, IOException {
    LOGGER.info("Wait until MCF is started");

    boolean exception = true;

    do {

      try {

        Thread.sleep(1000);

        final HttpGet request = new HttpGet(urlManifoldCFAPI + "jobstatuses");
        final HttpResponse response = client.execute(request);

        // Get Response
        final InputStream is = response.getEntity().getContent();
        final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;

        final StringBuffer responseText = new StringBuffer();
        while ((line = rd.readLine()) != null) {
          responseText.append(line);
          responseText.append('\r');
        }
        rd.close();

        exception = false;

      } catch (final Exception e) {
        LOGGER.info("Waiting for mcf-api-service start");
      }

    } while (exception);
  }

  public static void authenticate(final String apiUsername, final String apiPassword, final HttpClient client)
      throws IOException, ManifoldCFException, InterruptedException, ParseException {
    final JSONParser parser = new JSONParser();
    final JSONObject json = (JSONObject) parser.parse("{\"userID\":\"" + apiUsername + "\", \"password\":\"" + apiPassword + "\"}");
    executeCommand(urlManifoldCFAPI + "LOGIN", "POST", json, client);
  }

  private static void append(final JSONObject json, final String key, final Object value) {
    JSONArray array;
    if (json.containsKey(key)) {
      array = (JSONArray) json.get(key);
    } else {
      array = new JSONArray();
      json.put(key, array);
    }
    array.add(value);
  }

}
