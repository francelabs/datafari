package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;

public class ConnectorCreator {

  private static ConnectorCreator instance = null;

  private final File tikaServerRmetaConnectorJSON;
  private final File spacyFastAPIConnectorJSON;

  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";

  private final static String transfoConnectionElement = "transformationconnection";
  private final static String nameElement = "name";
  private final static String configurationElement = "configuration";
  private final static String parameterElement = "_PARAMETER_";
  private final static String attributeName = "_attribute_name";
  private final static String tikaHostAttr = "tikaHostname";
  private final static String tikaPortAttr = "tikaPort";
  private final static String serverAddress = "serverAddress";

  private final static Logger logger = LogManager.getLogger(ConnectorCreator.class);

  private ConnectorCreator() {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }

    final String tikaServerRmetaConnectorJSONPath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "init" + File.separator + "transformationconnections" + File.separator + "TikaServerRmetaConnector.json";
    tikaServerRmetaConnectorJSON = new File(tikaServerRmetaConnectorJSONPath);

    final String spacyFastAPIConnectorJSONPath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "transformationconnections" + File.separator + "SpacyFastAPI.json";
    spacyFastAPIConnectorJSON = new File(spacyFastAPIConnectorJSONPath);

  }

  public static ConnectorCreator getInstance() {
    if (instance == null) {
      instance = new ConnectorCreator();
    }
    return instance;
  }

  public JSONObject createTikaRmetaConnector(final String tikaOCRName, final String tikaOCRHost, final String tikaOCRPort) throws Exception {

    // Prepare the Tika connector
    final JSONObject tikaServerJSONObj = JSONUtils.readJSON(tikaServerRmetaConnectorJSON);
    final JSONArray transfoConnection = (JSONArray) tikaServerJSONObj.get(transfoConnectionElement);
    final JSONObject transfoConnectionEl = (JSONObject) transfoConnection.get(0);

    // Set name
    transfoConnectionEl.put(nameElement, tikaOCRName);

    final JSONObject configurationEl = (JSONObject) transfoConnectionEl.get(configurationElement);
    final JSONArray parameterEl = (JSONArray) configurationEl.get(parameterElement);

    for (int i = 0; i < parameterEl.size(); i++) {
      final JSONObject parameter = (JSONObject) parameterEl.get(i);

      if (parameter.get(attributeName).equals(tikaHostAttr)) {
        // Set the tikaHost value
        parameter.put(value, tikaOCRHost);
      } else if (parameter.get(attributeName).equals(tikaPortAttr)) {
        // Set the tikaPort value
        parameter.put(value, tikaOCRPort);
      }

    }

    return tikaServerJSONObj;
  }

  public JSONObject createSpacyFastAPIConnector(final String connectorName, final String spacyServerAddress) throws Exception {

    final JSONObject spacyFastAPIJSONObj = JSONUtils.readJSON(spacyFastAPIConnectorJSON);
    final JSONArray transfoConnectionArr = (JSONArray) spacyFastAPIJSONObj.get(transfoConnectionElement);
    final JSONObject transfoConnection = (JSONObject) transfoConnectionArr.get(0);
    final JSONArray transfoConnectionChildrenEl = (JSONArray) transfoConnection.get(childrenElement);
    for (int i = 0; i < transfoConnectionChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) transfoConnectionChildrenEl.get(i);
      if (child.get(type).equals(nameElement)) {
        // Set the connector name
        child.replace(value, String.valueOf(connectorName));
      } else if (child.get(type).equals(configurationElement)) {
        final JSONArray parameters = (JSONArray) child.get(parameterElement);
        for (int j = 0; j < parameters.size(); j++) {
          final JSONObject parameter = (JSONObject) parameters.get(j);
          if (parameter.get(attributeName).equals(serverAddress)) {
            // Set the spacy server address
            parameter.replace(value, spacyServerAddress);
          }
        }
      }
    }

    return spacyFastAPIJSONObj;
  }

}
