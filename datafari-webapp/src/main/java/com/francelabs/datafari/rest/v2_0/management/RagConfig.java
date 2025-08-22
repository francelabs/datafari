package com.francelabs.datafari.rest.v2_0.management;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
public class RagConfig {

  // Set of editable properties
  Set<String> allowedKeys = Set.of(
          RagConfiguration.ENABLE_RAG,
          RagConfiguration.ENABLE_SUMMARIZATION,
          RagConfiguration.API_ENDPOINT,
          RagConfiguration.API_TOKEN,
          RagConfiguration.LLM_SERVICE,
          RagConfiguration.LLM_TEMPERATURE,
          RagConfiguration.LLM_MAX_TOKENS,
          RagConfiguration.LLM_MODEL,
          RagConfiguration.PROMPT_CHUNKING_STRATEGY,
          RagConfiguration.MAX_REQUEST_SIZE,
          RagConfiguration.MAX_FILES,
          RagConfiguration.CHUNK_SIZE,
          RagConfiguration.SEARCH_OPERATOR,
          RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_BM25,
          RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_VECTOR,
          RagConfiguration.CHAT_MEMORY_ENABLED,
          RagConfiguration.CHAT_MEMORY_HISTORY_SIZE,
          RagConfiguration.RETRIEVAL_METHOD,
          RagConfiguration.SOLR_TOPK,
          RagConfiguration.RRF_TOPK,
          RagConfiguration.RRF_RANK_CONSTANT
  );

  @RequestMapping("/rest/v2.0/management/ragConfig")
  public String ragConfigManagement(final HttpServletRequest request) {
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
    JSONObject response = new JSONObject();
    RagConfiguration config = RagConfiguration.getInstance();

    response.put("enableRag", config.getBooleanProperty(RagConfiguration.ENABLE_RAG));
    response.put("enableSummarization", config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION));

    response.put("apiEndpoint", config.getProperty(RagConfiguration.API_ENDPOINT));
    response.put("apiToken", config.getProperty(RagConfiguration.API_TOKEN));
    response.put("llmService", config.getProperty(RagConfiguration.LLM_SERVICE));
    response.put("llmModel", config.getProperty(RagConfiguration.LLM_MODEL));
    response.put("llmTemperature", config.getProperty(RagConfiguration.LLM_TEMPERATURE));
    response.put("llmMaxTokens", config.getProperty(RagConfiguration.LLM_MAX_TOKENS));

    response.put("chunkingStrategy", config.getProperty(RagConfiguration.PROMPT_CHUNKING_STRATEGY));
    response.put("maxRequestSize", config.getProperty(RagConfiguration.MAX_REQUEST_SIZE));

    response.put("chunkingMaxFiles", config.getProperty(RagConfiguration.MAX_FILES));
    response.put("chunkingChunkSize", config.getProperty(RagConfiguration.CHUNK_SIZE));
    response.put("ragOperator", config.getProperty(RagConfiguration.SEARCH_OPERATOR));

    response.put("inMemoryEnableVectorSearch", config.getBooleanProperty(RagConfiguration.ENABLE_VECTOR_SEARCH));
    response.put("inMemoryTopK", config.getProperty(RagConfiguration.IN_MEMORY_TOP_K));

    response.put("chatQueryRewritingEnabledBM25", config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_BM25));
    response.put("chatQueryRewritingEnabledVector", config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_VECTOR));
    response.put("chatMemoryEnabled", config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED));
    response.put("chatMemoryHistorySize", config.getProperty(RagConfiguration.CHAT_MEMORY_HISTORY_SIZE));

    response.put("retrievalMethod", config.getProperty(RagConfiguration.RETRIEVAL_METHOD));
    response.put("solrEmbeddingsModel", config.getProperty(RagConfiguration.SOLR_EMBEDDINGS_MODEL));
    response.put("solrVectorField", config.getProperty(RagConfiguration.SOLR_VECTOR_FIELD));
    response.put("solrTopK", config.getProperty(RagConfiguration.SOLR_TOPK));
    response.put("rrfTopK", config.getProperty(RagConfiguration.RRF_TOPK));
    response.put("rrfRankConstant", config.getProperty(RagConfiguration.RRF_RANK_CONSTANT));

    return response.toJSONString();
  }

  protected String doPost(final HttpServletRequest request) {
    JSONObject response = new JSONObject();
    RagConfiguration config = RagConfiguration.getInstance();

    try {
      JSONParser parser = new JSONParser();
      String requestBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
      JSONObject input = (JSONObject) parser.parse(requestBody);

      for (Object keyObj : input.keySet()) {
        String key = (String) keyObj;
        if (allowedKeys.contains(key)) {
          config.setProperty(key, input.get(key).toString());
        }
      }

      config.saveProperties();

      response.put("status", "updated");
    } catch (Exception e) {
      response.put("status", "error");
      response.put("error", e.getMessage());
    }

    return response.toJSONString();
  }

}
