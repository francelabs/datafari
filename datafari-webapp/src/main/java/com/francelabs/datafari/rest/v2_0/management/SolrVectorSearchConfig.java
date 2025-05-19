package com.francelabs.datafari.rest.v2_0.management;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SolrVectorSearchConfig {



  private static final String SOLR_URL = "http://localhost:8983/solr";
  private static final String CORE_NAME = "FileShare";

  @RequestMapping("/rest/v2.0/management/solrvectorsearch")
  public String solrVectorSearchConfigManagement(final HttpServletRequest request) {
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

  protected String doGet(final HttpServletRequest request) {
    JSONObject jsonResponse = new JSONObject();
    try {
      // Retrieve Solr config for FileShare collection
      JSONObject solrConfig = getJsonFromSolr(SOLR_URL + "/" + CORE_NAME + "/config/overlay");
      JSONObject overlay = (JSONObject) solrConfig.get("overlay");
      JSONObject userProps = (JSONObject) overlay.get("userProps");

      jsonResponse.put("enableVectorSearch", Boolean.parseBoolean((String) userProps.getOrDefault("vector.enabled", "false")));
      jsonResponse.put("minchunklength", Integer.parseInt(((Long) userProps.getOrDefault("vector.filter.minchunklength", 0)).toString()));
      jsonResponse.put("minalphanumratio", (Double) userProps.getOrDefault("vector.filter.minalphanumratio", 0.0));

      // Retrieve Solr config for VectorMain collection
      JSONObject solrConfigVectorMain = getJsonFromSolr(SOLR_URL + "/VectorMain/config/overlay");
      overlay = (JSONObject) solrConfigVectorMain.get("overlay");
      userProps = (JSONObject) overlay.get("userProps");
      jsonResponse.put("vectorField", (String) userProps.getOrDefault("texttovector.outputfield", ""));

      // Retrieve "default_model" from Solr
      JSONObject modelStore = getJsonFromSolr(SOLR_URL + "/VectorMain/schema/text-to-vector-model-store");
      JSONArray models = (JSONArray) modelStore.get("models");

      for (Object modelObj : models) {
        JSONObject model = (JSONObject) modelObj;
        if ("default_model".equals(model.get("name"))) {
          jsonResponse.put("modelTemplate", model.get("class"));
          jsonResponse.put("jsonModel", model.toJSONString());
          break;
        }
      }

      // List of Solr fields
      JSONObject fieldsResp = getJsonFromSolr(SOLR_URL + "/VectorMain/schema/fields");
      JSONArray fields = (JSONArray) fieldsResp.get("fields");
      JSONArray fieldNames = new JSONArray();

      for (Object fObj : fields) {
        JSONObject field = (JSONObject) fObj;
        String fieldType = (String) field.get("type");
        if (fieldType.contains("vector")) {
          fieldNames.add(field.get("name"));
        }
      }
      jsonResponse.put("availableFields", fieldNames);

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

      Map<String, Object> properties = new LinkedHashMap<>();
      if (input.containsKey("enableVectorSearch")) {
        properties.put("vector.enabled", input.get("enableVectorSearch").toString());
      }
      if (input.containsKey("minchunklength")) {
        properties.put("vector.filter.minchunklength", input.get("minchunklength").toString());
      }
      if (input.containsKey("minalphanumratio")) {
        properties.put("vector.filter.minalphanumratio", input.get("minalphanumratio").toString());
      }
      if (input.containsKey("vectorField")) {
        properties.put("texttovector.outputfield", input.get("vectorField").toString());
      }

      // Update each updated property
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        JSONObject inner = new JSONObject();
        inner.put(entry.getKey(), entry.getValue());

        JSONObject payload = new JSONObject();
        payload.put("set-user-property", inner);

        postToSolr(SOLR_URL + "/" + CORE_NAME + "/config", payload);
      }

      // Update model
      if (input.containsKey("jsonModel")) {
        String newModelJson = input.get("jsonModel").toString();

        // Retrieve existing model
        JSONObject currentModelStore = getJsonFromSolr(SOLR_URL + "/" + CORE_NAME + "/schema/text-to-vector-model-store");
        JSONArray models = (JSONArray) currentModelStore.get("models");

        boolean modelExists = false;
        String currentModelJson = null;
        for (Object obj : models) {
          JSONObject model = (JSONObject) obj;
          if ("default_model".equals(model.get("name"))) {
            currentModelJson = model.toJSONString();
            modelExists = true;
            break;
          }
        }

        // Compare JSON String to check if the model changed
        if (!newModelJson.equals(currentModelJson)) {
          // Delete existing one
          if (modelExists) {
            deleteFromSolr(SOLR_URL + "/" + CORE_NAME + "/schema/text-to-vector-model-store/default_model");
          }

          // Upload the new one
          putToSolr(SOLR_URL + "/" + CORE_NAME + "/schema/text-to-vector-model-store", newModelJson);
        }
      }

      jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Configuration updated successfully");

    } catch (Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error: " + e.getMessage());
    }

    return jsonResponse.toJSONString();
  }


  // Utils HTTP (GET)
  private JSONObject getJsonFromSolr(String url) throws IOException, ParseException {
    URLConnection conn = new URL(url).openConnection();
    try (InputStream is = conn.getInputStream();
         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(reader);
    }
  }

  // Utils HTTP (POST)
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
      // lecture réponse (même si ignorée)
      IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }

  private void deleteFromSolr(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("DELETE");
    conn.setRequestProperty("Accept", "application/json");
    conn.connect();

    try (InputStream is = conn.getInputStream()) {
      IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }

  private void putToSolr(String url, String jsonPayload) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
    }

    try (InputStream is = conn.getInputStream()) {
      IOUtils.toString(is, StandardCharsets.UTF_8);  // Lecture même si ignorée
    }
  }

}
