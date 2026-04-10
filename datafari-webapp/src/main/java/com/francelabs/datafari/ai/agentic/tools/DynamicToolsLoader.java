package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.ai.agentic.agents.custom.tools.RagByDocToolExecutor;
import com.francelabs.datafari.ai.agentic.agents.custom.tools.SearchToolExecutor;
import com.francelabs.datafari.ai.dto.CustomTool;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.StreamToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DynamicToolsLoader {

  private static final Logger LOGGER = LogManager.getLogger(DynamicToolsLoader.class.getName());


  public record DynamicTool(ToolSpecification spec, ToolExecutor exec) {}

  /**
   * Load custom tools from JSON
   * @param toolJson Path of the JSON.
   * @param request HttpServletRequest object (required for Datafari search & AI services)
   * @param stream The ChatStream object
   * @param sourcesAcc SourcesAccumulator
   * @return a list of DynamicTools
   */
  public static List<DynamicTool> load(Path toolJson,
                                       HttpServletRequest request,
                                       ChatStream stream,
                                       SourcesAccumulator sourcesAcc) throws Exception {

    List<DynamicTool> dynamicTools = new ArrayList<>();
    if (Files.exists(toolJson)) {
      try (InputStream is = new FileInputStream(toolJson.toFile())) {

        List<CustomTool> loadedTools = CustomToolsLoader.load(is);


        for (CustomTool tool : loadedTools) {
          // Extract tool specification
          ToolSpecification spec = ToolSpecFactory.toSpec(tool);
          // Extract tool executor
          ToolExecutor exec = switch (tool.type) {
            case "datafari-search" -> SearchToolExecutor.build(tool, request, sourcesAcc);
            case "rag-by-document-template" -> RagByDocToolExecutor.build(tool, request, stream, sourcesAcc);
            default -> null;
          };
          if (exec != null) {
            // wrap for streaming
            StreamToolExecutor wrapped = new StreamToolExecutor(tool.name, exec, stream, tool.label, tool.icon, tool.i18nKey);
            DynamicTool dyn = new DynamicTool(spec, wrapped);
            dynamicTools.add(dyn);
          }
        }
      }
    }

    // Return the list of DynamicTools
    return dynamicTools;
  }

}