package com.francelabs.datafari.mcp.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class AgenticRequest {
    @NotBlank
    private String query;
    private String lang;
    private String agent = "generic";
    private Map<String, List<String>> filters = Map.of();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Map<String, List<String>> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, List<String>> filters) {
        this.filters = filters;
    }
}