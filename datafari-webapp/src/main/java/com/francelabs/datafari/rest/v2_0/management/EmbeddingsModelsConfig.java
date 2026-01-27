package com.francelabs.datafari.rest.v2_0.management;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfig;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfigurationManager;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelsPayload;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class EmbeddingsModelsConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @RequestMapping("/rest/v2.0/management/embeddings-models")
  public String embeddingsModelsConfig(final HttpServletRequest request) {
    if (request.getMethod().contentEquals("GET")) {
      return doGet(request);
    } else if (request.getMethod().contentEquals("POST")) {
      return doPost(request);
    } else if (request.getMethod().contentEquals("DELETE")) {
      return doDelete(request);
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

      // Retrieve models embeddings_models.json
      EbdModelConfigurationManager manager = new EbdModelConfigurationManager();
      List<EbdModelConfig> models = manager.listModels();
      String activeModel = (manager.getActiveModelConfig() != null) ?
              manager.getActiveModelConfig().getName() : null;

      JSONArray modelList = new JSONArray();

      for (EbdModelConfig m : models) {
          JSONObject model = (JSONObject) JSONValue.parse(MAPPER.writeValueAsString(m));
          modelList.add(model);
      }

      jsonResponse.put("models", modelList);
      jsonResponse.put("activeModel", activeModel); // Active model name

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
      EbdModelConfigurationManager configManager = new EbdModelConfigurationManager();

      JSONParser parser = new JSONParser();
      String requestBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
      JSONObject input = (JSONObject) parser.parse(requestBody);

      EbdModelsPayload payload = MAPPER.readValue(input.toJSONString(), EbdModelsPayload.class);
      String activeModel = payload.getActiveModel();
      List<EbdModelConfig> models = payload.getModels();

      // Update embeddings_models.json and Solr configuation
      configManager.addOrUpdateModels(models);
      configManager.setActiveModel(activeModel);

    } catch (Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error: " + e.getMessage());
      return jsonResponse.toJSONString();
    }

    return doGet(request);
  }

  protected String doDelete(final HttpServletRequest request) {
    JSONObject jsonResponse = new JSONObject();
    try {
      String modelName = request.getParameter("modelName");

      if (modelName == null || modelName.trim().isEmpty()) {
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
        jsonResponse.put(OutputConstants.STATUS, "Missing modelName parameter");
        return jsonResponse.toJSONString();
      }

      EbdModelConfigurationManager modelManager = new EbdModelConfigurationManager();
      modelManager.removeModel(modelName);

    } catch (Exception e) {
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      jsonResponse.put(OutputConstants.STATUS, "Error deleting model: " + e.getMessage());
      return jsonResponse.toJSONString();
    }

    return doGet(request);
  }

}
