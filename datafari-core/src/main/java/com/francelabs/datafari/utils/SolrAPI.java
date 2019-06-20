/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.LockManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.ThreadContextFactory;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.utils.SolrConfiguration;

public class SolrAPI {

  private final static Logger LOGGER = LogManager.getLogger(SolrAPI.class.getName());



  private static String solrserver="localhost";
  private static String solrport="8983";
  private static String protocol="http";

  private static String config_api="/solr/FileShare/config/params";
  private static String overlay="/solr/FileShare/config/overlay";
  private static String overlayshort="/solr/FileShare/config";
  private static String schema_api="/solr/FileShare/schema";


  public JSONObject readConfiguration() {
    return null;
  }

  private static String getSolrUrl() throws IOException{
    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
    solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
    protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);

    return (protocol+"://"+solrserver+":"+solrport);
  }

  private static String getConfigAPI(String collection) throws IOException{

    config_api = "/solr/"+collection+"/config/params";

    return config_api;
  }

  private static String getSchemaAPI(String collection) throws IOException{

    config_api = "/solr/"+collection+"/schema";

    return schema_api;
  }

  private static String getOverlay(String collection) throws IOException{


    overlay = "/solr/"+collection+"/config/overlay";

    return overlay;
  }

  private static String getOverlayShort(String collection) throws IOException{

    overlayshort = "/solr/"+collection+"/config";

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
          LOGGER.debug("request"+request);
        }
      }

      final HttpResponse response = client.execute(request);
      LOGGER.debug("response"+response.toString());

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

  static private JSONObject executeCommand(final String url, final String verb, final JSONObject jsonObject) throws IOException,InterruptedException, ParseException {
    final HttpClient client = getClient();
    return executeCommand(url, verb, jsonObject, client);

  }

  static public JSONObject readConfig(String collection) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getConfigAPI(collection);
    LOGGER.debug(url+"url");
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readSchema(String collection,String path) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getSchemaAPI(collection)+"/"+path;
    LOGGER.debug(url+"url schema");
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readConfigOverlay(String collection) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getOverlay(collection);
    LOGGER.debug("url overlay"+url);
    return executeCommand(url, "GET", null);
  }

  static public long getHLcharacters(JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch"));
    long maxAnalyzedChars = (long) mySearch.get("hl.maxAnalyzedChars");
    return maxAnalyzedChars;
  }

  static public String getFieldsWeight(JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch"));
    String fieldsWeight = (String) mySearch.get("qf");
    return fieldsWeight;
  }

  static public String getPhraseFieldsWeight(JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject mySearch = (JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) object.get("response")).get("params")).get("mySearch"));
    String phraseFieldsWeight = (String) mySearch.get("pf");
    return phraseFieldsWeight;
  }

  static public JSONObject setHLcharacters(String collection, long nbCharactersHL) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject highlight = new JSONObject();

    highlight.put("hl.maxAnalyzedChars",nbCharactersHL);
    mysearch.put("mySearch", highlight);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);


  }

  static public JSONObject setFieldsWeight(String collection, String fieldsWeight) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject qf = new JSONObject();

    qf.put("qf",fieldsWeight);
    mysearch.put("mySearch", qf);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);


  }

  static public JSONObject setPhraseFieldsWeight(String collection, String fieldsWeight) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject pf = new JSONObject();

    pf.put("pf",fieldsWeight);
    mysearch.put("mySearch", pf);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);
  }


  static public String getUserProp(JSONObject object, String userProp) throws IOException, ManifoldCFException, InterruptedException, ParseException {
    LOGGER.debug("userProp:"+userProp);
    final JSONObject objectUserProps = (JSONObject) ((JSONObject)  ((JSONObject) object.get("overlay")).get("userProps"));
    String userPropValue = (String) objectUserProps.get(userProp);
    return userPropValue;
  }

  static public JSONObject setUserProp(String collection, String userProp, String userPropValue) throws IOException, InterruptedException, ParseException{

    LOGGER.debug("setUserProp:"+userPropValue);
    final String url = getSolrUrl() + getOverlayShort(collection);
    final JSONObject objet = new JSONObject();

    final JSONObject userPropObject = new JSONObject();
    userPropObject.put(userProp,userPropValue);
    objet.put("set-user-property", userPropObject);
    return executeCommand(url, "POST", objet);
  }

  static public JSONObject manageCopyField(String collection, String command, String sourceField, String targetField) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getSchemaAPI(collection);
    final JSONObject objet = new JSONObject();
    final JSONObject copy = new JSONObject();
    copy.put("dest",targetField);
    copy.put("source",sourceField);
    objet.put(command, copy);
    return executeCommand(url, "POST", objet);
  }

  static public boolean containsCopyFields(String collection) throws Exception {

    boolean copyFieldPresent = false ;

    JSONObject jsonObject = readSchema(collection,"copyfields");
    System.out.println(jsonObject);
    LOGGER.error(jsonObject.toJSONString());
    final JSONArray jsonarray = (JSONArray)  jsonObject.get("copyFields");
    System.out.println(jsonarray);
    for (int i = 0; i < jsonarray.size(); i++) {
      Object jsonobject = jsonarray.get(i);
      System.out.println(jsonobject);
      if (jsonobject.toString().equals("{\"source\":\"content_*\",\"dest\":\"entity_person\"}"))
        copyFieldPresent = true;
    }
    return copyFieldPresent ; 

  }



}