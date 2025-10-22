package com.francelabs.datafari.rest.v2_0.management;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ChunkingConfig {


  private static final String CORE_NAME = getMainCollection();
  private static final String SOLR_URL = getSolrUrl();

  @RequestMapping("/rest/v2.0/management/chunking")
  public String chunkingConfigManagement(final HttpServletRequest request) {
    if (request.getMethod().contentEquals("GET")) {
      return doGet(request);
    } else if (request.getMethod().contentEquals("POST")) {
      return doPost(request);
    } else {
      final JSONObject jsonResponse = new JSONObject();
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Unsupported request method");
      return jsonResponse.toJSONString();
    }
  }

  protected static String getSolrUrl() {
    SolrConfiguration solrConf = SolrConfiguration.getInstance();
    String solrserver = solrConf.getProperty(SolrConfiguration.SOLRHOST, "localhost");
    String solrport = solrConf.getProperty(SolrConfiguration.SOLRPORT, "8983");
    String protocol = solrConf.getProperty(SolrConfiguration.SOLRPROTOCOL, "http");
    return protocol + "://" + solrserver + ":" + solrport + "/solr";
  }

  protected static String getMainCollection() {
    return DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION, "FileShare");
  }


  protected String doGet(final HttpServletRequest request) {
    JSONObject jsonResponse = new JSONObject();
    try {
      // Retrieve Solr config for FileShare collection
      JSONObject solrConfig = getJsonFromSolr(SOLR_URL + "/" + CORE_NAME + "/config/overlay");
      JSONObject overlay = (JSONObject) solrConfig.get("overlay");
      JSONObject userProps = (JSONObject) overlay.get("userProps");

      if (userProps != null) {
        jsonResponse.put("minChunkLength", Integer.parseInt((userProps.getOrDefault("vector.filter.minchunklength", 1)).toString()));
        jsonResponse.put("minAlphaNumRatio", userProps.getOrDefault("vector.filter.minalphanumratio", 0.0));
        jsonResponse.put("maxoverlap", Integer.parseInt((userProps.getOrDefault("vector.maxoverlap", 0L)).toString()));
        jsonResponse.put("chunksize", Integer.parseInt((userProps.getOrDefault("vector.chunksize", 300L)).toString()));
        jsonResponse.put("splitter", (String) userProps.getOrDefault("vector.splitter", "recursiveSplitter"));
      }

      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "OK");

    } catch (Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error: " + e.getMessage());
    }

    return jsonResponse.toJSONString();
  }

  protected String doPost(final HttpServletRequest request) {
    JSONObject jsonResponse = new JSONObject();

    try {
      JSONParser parser = new JSONParser();
      String requestBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
      JSONObject input = (JSONObject) parser.parse(requestBody);

      Map<String, Object> properties = new LinkedHashMap<>(); // FileShare properties

      if (input.containsKey("minChunkLength")) {
        properties.put("vector.filter.minchunklength", input.get("minChunkLength").toString());
      }
      if (input.containsKey("minAlphaNumRatio")) {
        properties.put("vector.filter.minalphanumratio", input.get("minAlphaNumRatio").toString());
      }
      if (input.containsKey("maxoverlap")) {
        properties.put("vector.maxoverlap", input.get("maxoverlap").toString());
      }
      if (input.containsKey("chunksize")) {
        properties.put("vector.chunksize", input.get("chunksize").toString());
      }
      if (input.containsKey("splitter")) {
        properties.put("vector.splitter", input.get("splitter").toString());
      }


      // Update each updated property in FileShare
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        JSONObject inner = new JSONObject();
        inner.put(entry.getKey(), entry.getValue());

        JSONObject payload = new JSONObject();
        payload.put("set-user-property", inner);

        postToSolr(SOLR_URL + "/" + CORE_NAME + "/config", payload);
      }

      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Configuration updated successfully");

    } catch (Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error: " + e.getMessage());
    }

    return jsonResponse.toJSONString();
  }




  private void disableSSLCertificateChecking() throws Exception {
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    HostnameVerifier allHostsValid = (hostname, session) -> true;
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
}

  // Utils HTTP (GET)
  private JSONObject getJsonFromSolr(String url) throws IOException, ParseException {
    try {
      disableSSLCertificateChecking();
    } catch (Exception e) {
      throw new IOException("Erreur lors de la désactivation de la vérification SSL", e);
    }
    URLConnection conn = new URL(url).openConnection();
    try (InputStream is = conn.getInputStream();
         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(reader);
    }
  }

  private void postToSolr(String url, JSONObject payload) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json; utf-8");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    try (InputStream is = conn.getInputStream()) {
      IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }

}
