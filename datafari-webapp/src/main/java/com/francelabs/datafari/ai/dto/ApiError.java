package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.simple.JSONObject;

import java.util.Map;

import static java.util.Map.entry;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    public String code;
    public String label;
    public String reason;
    public String message;

    public static final Map.Entry<String, String> AI_ERROR_NOT_ENABLED = entry("aiErrorNotEnabled", "Sorry, it seems the feature is not enabled.");
    public static final Map.Entry<String, String> AI_BAD_REQUEST = entry("aiBadRequest", "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> AI_TECHNICAL_ERROR = entry("aiTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> AGENTIC_ERROR_NOT_ENABLED = entry("agenticErrorNotEnabled", "Sorry, it seems the feature is not enabled.");
    public static final Map.Entry<String, String> AGENTIC_NO_FILE_FOUND = entry("agenticNoFileFound", "Sorry, I couldn't find any relevant document to answer your request.");
    public static final Map.Entry<String, String> AGENTIC_FILE_NOT_FOUND = entry("agenticFileNotFound", "Sorry, the requested file does not exist, or is not available.");
    public static final Map.Entry<String, String> AGENTIC_TECHNICAL_ERROR = entry("agenticTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> AGENTIC_NO_VALID_ANSWER = entry("agenticNoValidAnswer", "Sorry, I could not find an answer to your question.");
    public static final Map.Entry<String, String> AGENTIC_BAD_REQUEST = entry("agenticBadRequest", "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> RAG_ERROR_NOT_ENABLED = entry("ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.");
    public static final Map.Entry<String, String> RAG_NO_FILE_FOUND = entry("ragNoFileFound", "Sorry, I couldn't find any relevant document to answer your request.");
    public static final Map.Entry<String, String> RAG_FILE_NOT_FOUND = entry("ragFileNotFound", "Sorry, the requested file does not exist, or is not available.");
    public static final Map.Entry<String, String> RAG_TECHNICAL_ERROR = entry("ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> RAG_NO_VALID_ANSWER = entry("ragNoValidAnswer", "Sorry, I could not find an answer to your question.");
    public static final Map.Entry<String, String> RAG_BAD_REQUEST = entry("ragBadRequest", "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> SUMMARIZATION_NOT_ENABLED = entry("summarizationErrorNotEnabled", "Sorry, it seems the feature is not enabled.");
    public static final Map.Entry<String, String> SUMMARIZATION_NO_FILE_FOUND = entry("summarizationNoFileFound", "Sorry, I cannot find this document.");
    public static final Map.Entry<String, String> SUMMARIZATION_TECHNICAL_ERROR = entry("summarizationTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> SUMMARIZATION_BAD_REQUEST = entry("summarizationBadRequest", "Sorry, it appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> SUMMARIZATION_EMPTY_FILE = entry("summarizationEmptyFile", "Sorry, I am unable to generate a summary, since the file has no content.");
    public static final Map.Entry<String, String> SYNTHESIS_NOT_ENABLED = entry("synthesisErrorNotEnabled", "Sorry, it seems the feature is not enabled.");
    public static final Map.Entry<String, String> SYNTHESIS_NO_FILE_CONTENT = entry("synthesisNoFileContent", "Sorry, I am unable to generate a synthesis of those documents, since the files are missing or have no content.");
    public static final Map.Entry<String, String> SYNTHESIS_TECHNICAL_ERROR = entry("synthesisTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    public static final Map.Entry<String, String> TEST_ERROR = entry("testLabelForError", "This test endpoint returns an error if 'summarize' action is called");

    public ApiError() {}
    public ApiError(String code, String label, String message, String reason) {
        this.code = code;
        this.label = label;
        this.message = message;
        this.reason = reason;
    }

    public ApiError(String code, Map.Entry<String, String> type, String reason, Exception e) {
        this.code = code;
        this.label = type.getKey();
        this.message = type.getValue();
        this.reason = reason != null ? reason : e.getLocalizedMessage();
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("label", label);
        json.put("message", message);
        if (reason != null) json.put("reason", reason);
        return json;
    }
}
