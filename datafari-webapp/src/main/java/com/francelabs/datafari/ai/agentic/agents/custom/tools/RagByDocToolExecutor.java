package com.francelabs.datafari.ai.agentic.agents.custom.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.CustomTool;
import com.francelabs.datafari.ai.services.RagService;
import com.francelabs.datafari.ai.stream.ChatStream;
import dev.langchain4j.service.tool.ToolExecutor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class RagByDocToolExecutor {
  public static ToolExecutor build(
      CustomTool ct, HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {

    return (req, memoryId) -> {

      try {
        Map<String,Object> args = parseArgs(req.arguments());
        String docId = String.valueOf(args.getOrDefault("id", ""));
        if (docId.isBlank()) throw new IllegalArgumentException("id is required");

        String prompt = render(ct.properties != null ? ct.properties.template : "", args);

        var aiReq = new AiRequest();
        aiReq.query = prompt;
        aiReq.id = docId;

        var resp = RagService.rag(request, aiReq, stream, sourcesAcc, true);
        String message = (resp != null && resp.message != null && !resp.message.isBlank()) ? resp.message : null;
        if (message == null) {
          message = (resp != null && resp.error != null && resp.error.message != null && !resp.error.message.isBlank()) ? resp.error.message : "No content";
        }

        return message;

      } catch (Throwable t) {
        throw t;
      }
    };
  }

  /**
   * Parse JSON to Map<String,Object>
   */
  private static Map<String,Object> parseArgs(String json) {
    try {
      return (json==null||json.isBlank()) ? Map.of() : new ObjectMapper().readValue(json, Map.class);
    } catch (Exception e) { return Map.of(); }
  }

  /**
   * Replace the variable tags in query template
   * @param template The query template (e.g. "Extract the following entities: {{entities}}")
   * @param args The tool parameters (e.g. "entities" -> "Cities, Events, Dates")
   * @return The final query for "RAG by Document"
   */
  private static String render(String template, Map<String,Object> args) {
    if (template == null) return "";
    for (var e : args.entrySet()) template = template.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
    return template;
  }
}