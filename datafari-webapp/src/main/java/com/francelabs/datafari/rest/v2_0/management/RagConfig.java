package com.francelabs.datafari.rest.v2_0.management;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
public class RagConfig {

  // Set of editable properties
  Set<String> allowedKeys = Set.of(
          RagConfiguration.ENABLE_ASSISTANT,
          RagConfiguration.ENABLE_CONVERSATION_STORAGE,
          RagConfiguration.ASSISTANT_RETRIEVAL_METHOD,
          RagConfiguration.ENABLE_RAG,
          RagConfiguration.ENABLE_AGENTIC,
          RagConfiguration.ENABLE_SUMMARIZATION,
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
          RagConfiguration.RAG_TOPK,
          RagConfiguration.RRF_TOPK,
          RagConfiguration.RRF_RANK_CONSTANT,
          RagConfiguration.AGENTIC_ENABLE_LOOP_CONTROL,
          RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON,
          RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE,
          RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON_SECONDARY,
          RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE_SECONDARY,
          RagConfiguration.SOLR_ENABLE_ACORN,
          RagConfiguration.SOLR_ENABLE_LADR,
          RagConfiguration.SOLR_FILTERED_SEARCH_THRESHOLD
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

    response.put("enableAssistant", config.getBooleanProperty(RagConfiguration.ENABLE_ASSISTANT));
    response.put("enableConversationStorage", config.getBooleanProperty(RagConfiguration.ENABLE_CONVERSATION_STORAGE));
    response.put("assistantRetrievalMethod", config.getProperty(RagConfiguration.ASSISTANT_RETRIEVAL_METHOD));

    response.put("enableRag", config.getBooleanProperty(RagConfiguration.ENABLE_RAG));
    response.put("enableAgentic", config.getBooleanProperty(RagConfiguration.ENABLE_AGENTIC));
    response.put("enableSummarization", config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION));
    response.put("enableSynthesis", config.getBooleanProperty(RagConfiguration.ENABLE_SYNTHESIS));

    response.put("enableLoopControl", config.getBooleanProperty(RagConfiguration.AGENTIC_ENABLE_LOOP_CONTROL));
    response.put("loopControlMaxIterations", config.getBooleanProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON));
    response.put("loopControlMinScore", config.getBooleanProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE));
    response.put("loopControlMaxIterationsBeforeSecondary", config.getBooleanProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON_SECONDARY));
    response.put("loopControlMinScoreSecondary", config.getBooleanProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE_SECONDARY));

    response.put("chunkingStrategy", config.getProperty(RagConfiguration.PROMPT_CHUNKING_STRATEGY));
    response.put("maxRequestSize", config.getProperty(RagConfiguration.MAX_REQUEST_SIZE));

    response.put("chunkingMaxFiles", config.getProperty(RagConfiguration.MAX_FILES));
    response.put("chunkingChunkSize", config.getProperty(RagConfiguration.CHUNK_SIZE));
    response.put("ragOperator", config.getProperty(RagConfiguration.SEARCH_OPERATOR));

    response.put("chatQueryRewritingEnabledBM25", config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_BM25));
    response.put("chatQueryRewritingEnabledVector", config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_VECTOR));
    response.put("chatMemoryEnabled", config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED));
    response.put("chatMemoryHistorySize", config.getProperty(RagConfiguration.CHAT_MEMORY_HISTORY_SIZE));

    response.put("retrievalMethod", config.getProperty(RagConfiguration.RETRIEVAL_METHOD));
    response.put("solrTopK", config.getProperty(RagConfiguration.SOLR_TOPK));
    response.put("ragTopK", config.getProperty(RagConfiguration.RAG_TOPK));
    response.put("enableAcorn", config.getBooleanProperty(RagConfiguration.SOLR_ENABLE_ACORN));
    response.put("enableLadr", config.getBooleanProperty(RagConfiguration.SOLR_ENABLE_LADR));
    response.put("solrFilteredSearchThreshold", config.getProperty(RagConfiguration.SOLR_FILTERED_SEARCH_THRESHOLD));
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
