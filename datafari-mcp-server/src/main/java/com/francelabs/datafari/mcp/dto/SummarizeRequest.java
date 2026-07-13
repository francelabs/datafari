package com.francelabs.datafari.mcp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class SummarizeRequest {
    private String lang = "en";

    @NotBlank
    @JsonAlias("id")
    private String docId;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}