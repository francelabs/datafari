package com.francelabs.datafari.utils;

import java.io.BufferedReader;
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


  public JSONObject readConfiguration() {
    return null;
  }

  private static String getSolrUrl() throws IOException{
    solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
    solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
    protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);

    return (protocol+"://"+solrserver+":"+solrport);
  }

  private static String getConfigAPI() throws IOException{

    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
      config_api = "/solr/"+DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)+"/config/params";

    return config_api;
  }
  
  private static String getOverlay() throws IOException{

    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
      overlay = "/solr/"+DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)+"/config/overlay";

    return overlay;
  }
  
  private static String getOverlayShort() throws IOException{

    if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)!= null)
      overlayshort = "/solr/"+DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION)+"/config";

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

  static public JSONObject readConfig() throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getConfigAPI();
    LOGGER.debug(url+"url");
    return executeCommand(url, "GET", null);
  }

  static public JSONObject readConfigOverlay() throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final String url = getSolrUrl() + getOverlay();
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

  static public JSONObject setHLcharacters(long nbCharactersHL) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI();
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject highlight = new JSONObject();

    highlight.put("hl.maxAnalyzedChars",nbCharactersHL);
    mysearch.put("mySearch", highlight);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);


  }

  static public JSONObject setFieldsWeight(String fieldsWeight) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI();
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject qf = new JSONObject();

    qf.put("qf",fieldsWeight);
    mysearch.put("mySearch", qf);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);


  }

  static public JSONObject setPhraseFieldsWeight(String fieldsWeight) throws IOException, InterruptedException, ParseException{

    final String url = getSolrUrl() + getConfigAPI();
    final JSONObject objet = new JSONObject();
    final JSONObject mysearch = new JSONObject();
    final JSONObject pf = new JSONObject();

    pf.put("pf",fieldsWeight);
    mysearch.put("mySearch", pf);
    objet.put("update", mysearch);

    return executeCommand(url, "POST", objet);


  }
  static public JSONObject setAutocompleteThreshold(double autocompleteThreshold) throws IOException, InterruptedException, ParseException{

    LOGGER.debug("setauto:"+autocompleteThreshold);
    final String url = getSolrUrl() + getOverlayShort();
    final JSONObject objet = new JSONObject();

    final JSONObject threshold = new JSONObject();

    threshold.put("autocomplete.threshold",autocompleteThreshold);
    objet.put("set-user-property", threshold);


    return executeCommand(url, "POST", objet);


  }

  static public double getAutocompleteThreshold(JSONObject object) throws IOException, ManifoldCFException, InterruptedException, ParseException {

    final JSONObject objectAutocomplete = (JSONObject) ((JSONObject)  ((JSONObject) object.get("overlay")).get("userProps"));
    double autoCompleteThreshold = (double) objectAutocomplete.get("autocomplete.threshold");
    return autoCompleteThreshold;
  }





}