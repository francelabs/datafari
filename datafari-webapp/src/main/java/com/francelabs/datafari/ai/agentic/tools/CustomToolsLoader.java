package com.francelabs.datafari.ai.agentic.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.dto.CustomTool;
import com.francelabs.datafari.ai.dto.CustomToolsConfig;

import java.io.InputStream;
import java.util.List;

/**
 * Read a JSON InputStream to load a list of CustomTool
 */
public final class CustomToolsLoader {
  private static final ObjectMapper M = new ObjectMapper();

  public static List<CustomTool> load(InputStream is) throws Exception {
    CustomToolsConfig cfg = M.readValue(is, CustomToolsConfig.class);
    return (cfg != null && cfg.tools != null) ? cfg.tools : List.of();
  }
}