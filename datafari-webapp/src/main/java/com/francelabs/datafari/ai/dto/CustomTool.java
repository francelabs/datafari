package com.francelabs.datafari.ai.dto;

import java.util.List;
import java.util.Map;

public class CustomTool {

  public String type;        // "solr-search" | "rag-by-document-template"
  public String name;
  public String label;
  public String icon;
  public String i18nKey;
  public String description;
  public Map<String, ParamDef> parameters; // key -> definition
  public Properties properties;

  public static class ParamDef {
    public String type;       // "string" | "boolean" | "integer" | "number"
    public String description;
    public boolean required;
  }
  public static class Properties {
    // solr-search
    public String handler;         // ex: "/rrf"
    public String collection;      // ex: "VectorMain"
    public List<String> fq;        // ex: ["repo_source:${cfp}"]
    public Map<String,String> defaults; // fl, q.op, rows, start, sort, topK...
    // rag-by-document-template
    public String template;        // ex: "Extract ... {entities}"
  }
}