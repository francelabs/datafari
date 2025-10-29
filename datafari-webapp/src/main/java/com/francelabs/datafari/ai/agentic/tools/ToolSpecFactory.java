package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.ai.dto.CustomTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

public class ToolSpecFactory {

  private ToolSpecFactory() {}

  /**
   * Generate a "ToolSpecification" object from a CustomTool
   * @param tool CustomTool
   * @return ToolSpecification
   */
  public static ToolSpecification toSpec(CustomTool tool) {
    JsonObjectSchema.Builder sb = JsonObjectSchema.builder();
    if (tool.parameters != null) {
      tool.parameters.forEach((name, p) -> {
        String desc = p.description == null ? "" : p.description;
        switch (String.valueOf(p.type).toLowerCase()) {
          case "boolean" -> sb.addBooleanProperty(name, desc);
          case "integer" -> sb.addIntegerProperty(name, desc);
          case "number"  -> sb.addNumberProperty(name,  desc);
          default        -> sb.addStringProperty(name, desc);
        }
        if (p.required) sb.required(name);
      });
    }
    return ToolSpecification.builder()
        .name(tool.name)
        .description(tool.description == null ? "" : tool.description)
        .parameters(sb.build())
        .build();
  }
}