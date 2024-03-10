package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SolrAPI {

  private final static Logger LOGGER = LogManager.getLogger(SolrAPI.class.getName());

  private static String solrserver = "localhost";
  private static String solrport = "8983";
  private static String protocol = "http";

  public static String QUERY_QF = "qf";
  public static String QUERY_PF = "pf";
  public static String QUERY_BQ = "bq";



  public JSONObject readConfiguration() {
    return null;
  }

  private static String getSolrUrl() throws IOException {
    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
    solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
    protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);

    return protocol + "://" + solrserver + ":" + solrport;
  }

  private static String getConfigAPI(final String collection) throws IOException {

    String config_api = "/solr/" + collection + "/config/params";

    return config_api;
  }

  private static String getSchemaAPI(final String collection) throws IOException {

    String schema_api = "/solr/" + collection + "/schema";

    return schema_api;
  }

  private static String getOverlay(final String collection) throws IOException {

    String overlay = "/api/collections/" + collection + "/config/overlay";

    return overlay;
  }

  private static String getOverlayShort(final String collection) throws IOException {

    String overlayshort = "/api/collections/" + collection + "/config";

    return overlayshort;
  }

  static private HttpClient client = null;

  static private HttpClient getClient() throws InterruptedException, IOException, ParseException {

    client = HttpClientBuilder.create().build();

    return client;
  }

  static private JSONObject executeCommand(final String url, final String verb, final JSONObject jsonObject, final HttpClient client) throws IOException, InterruptedException {

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
          LOGGER.debug("request" + request);
        }
      }

      final HttpResponse response = client.execute(request);
      LOGGER.debug("response" + response.toString());

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

  static private JSONObject executeCommand(final String url, final String verb, final JSONObject jsonObject) throws IOException, InterruptedException, ParseException {
    final HttpClient client = getClient();
    return executeCommand(url, verb, jsonObject, client);

  }

  static public JSONObject readConfig(final String collection) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getConfigAPI(collection);
    LOGGER.debug(url + "url");
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readSchema(final String collection, final String path) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getSchemaAPI(collection) + "/" + path;
    LOGGER.debug(url + "url schema");
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readConfigOverlay(final String collection) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getOverlay(collection);
    LOGGER.debug("url overlay" + url);
    return executeCommand(url, "GET", null);
  }

  static public long getHLcharacters(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    final long maxAnalyzedChars = (long) mySearch.get("hl.maxAnalyzedChars");
    return maxAnalyzedChars;
  }

  static public String getFieldsWeight(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    final String fieldsWeight = (String) mySearch.get(QUERY_QF);
    return fieldsWeight;
  }

  static public String getPhraseFieldsWeight(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    final String phraseFieldsWeight = (String) mySearch.get(QUERY_PF);
    return phraseFieldsWeight;
  }

  static public String getBoost(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    return (String) mySearch.get("boost");
  }

  static public String getBoostQuery(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    return (String) mySearch.get("bq");
  }

  static public String getBoostFunction(final JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch");
    return (String) mySearch.get("bf");
  }

  static public JSONObject setHLcharacters(final String collection, final long nbCharactersHL) throws IOException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getConfigAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject highlight = new JSONObject();

    highlight.put("hl.maxAnalyzedChars", nbCharactersHL);
    mysearch.put("mySearch", highlight);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);

  }

  static public JSONObject setQueryParameter(final String qpFieldName, final String collection, final String qpFieldValue) throws IOException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getConfigAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject qpParamObj = new JSONObject();

    qpParamObj.put(qpFieldName, qpFieldValue);
    mysearch.put("mySearch", qpParamObj);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);

  }

  static public String getUserProp(final JSONObject object, final String userProp) throws IOException, ManifoldCFException, InterruptedException, ParseException {
    LOGGER.debug("userProp:" + userProp);
    final JSONObject objectUserProps = (JSONObject) ((JSONObject) object.get("overlay")).get("userProps");
    final String userPropValue = (String) objectUserProps.get(userProp);
    return userPropValue;
  }

  static public JSONObject setUserProp(final String collection, final String userProp, final String userPropValue) throws IOException, InterruptedException, ParseException {

    LOGGER.debug("setUserProp:" + userPropValue);
    final String url = getSolrUrl() + getOverlayShort(collection);
    final JSONObject objet = new JSONObject();

    final JSONObject userPropObject = new JSONObject();
    userPropObject.put(userProp, userPropValue);
    objet.put("set-user-property", userPropObject);
    return executeCommand(url, "POST", objet);
  }

  static public JSONObject setUserProp(final String collection, final Map<String, String> userProps) throws IOException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getOverlayShort(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject props = new JSONObject(userProps);

    objet.put("set-user-property", props);
    return executeCommand(url, "POST", objet);
  }

  static public JSONObject manageCopyField(final String collection, final String command, final String sourceField, final String targetField)
      throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getSchemaAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject copy = new JSONObject();
    copy.put("dest", targetField);
    copy.put("source", sourceField);
    objet.put(command, copy);
    return executeCommand(url, "POST", objet);
  }

  static public boolean containsCopyFields(final String collection) throws Exception {

    boolean copyFieldPresent = false;

    final JSONObject jsonObject = readSchema(collection, "copyfields");
    System.out.println(jsonObject);
    LOGGER.error(jsonObject.toJSONString());
    final JSONArray jsonarray = (JSONArray) jsonObject.get("copyFields");
    System.out.println(jsonarray);
    for (int i = 0; i < jsonarray.size(); i++) {
      final Object jsonobject = jsonarray.get(i);
      System.out.println(jsonobject);
      if (jsonobject.toString().equals("{\"source\":\"content_*\",\"dest\":\"entity_person\"}")) {
        copyFieldPresent = true;
      }
    }
    return copyFieldPresent;

  }

}