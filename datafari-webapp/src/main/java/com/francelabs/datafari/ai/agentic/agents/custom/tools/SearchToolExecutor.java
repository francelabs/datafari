package com.francelabs.datafari.ai.agentic.agents.custom.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.CustomTool;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.service.tool.ToolExecutor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public final class SearchToolExecutor {

  public static ToolExecutor build(CustomTool tool, HttpServletRequest request, SourcesAccumulator sourcesAcc) {

    return (req, memoryId) -> {

      try {
        Map<String,Object> args = parseArgs(req.arguments());
        EditableHttpServletRequest r = new EditableHttpServletRequest(request);
        String handler = (tool.properties != null && tool.properties.handler != null) ? tool.properties.handler : "/select";
        if (tool.properties != null && tool.properties.defaults != null) tool.properties.defaults.forEach(r::addParameter);
        if (tool.properties != null && tool.properties.collection != null) r.addParameter("collection", tool.properties.collection);
        r.addParameter("wt", "json");

        Object q = args.get("q");
        if (q != null) r.addParameter("q", String.valueOf(q));

        if (tool.properties != null && tool.properties.fq != null) {
          for (String tpl : tool.properties.fq) {
            String fq = render(tpl, args); // replace "{var}"
            if (!fq.isBlank()) r.addParameter("fq", fq);
          }
        }

        JSONObject root = SearchUtils.processSearch(r, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        // Save and stream sources
        for (Object o : docs) {
          JSONObject doc = (JSONObject) o;
          Document document = SearchUtils.jsonToDocument(doc);
          if (document != null) sourcesAcc.addAll(List.of(document));
        }

        return docs.toJSONString();

      } catch (Throwable t) {
        throw t;
      }
    };
  }

  private static Map<String,Object> parseArgs(String json) {
    try { return (json==null||json.isBlank()) ? Map.of() : new ObjectMapper().readValue(json, Map.class); }
    catch (Exception e) { return Map.of(); }
  }
  private static String render(String tpl, Map<String,Object> args) {
    if (tpl == null) return "";
    for (var e : args.entrySet()) {
      tpl = tpl.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
    }
    return tpl;
  }
}
