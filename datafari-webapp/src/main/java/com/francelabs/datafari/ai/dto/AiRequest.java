package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AiRequest {

    /** User query. Also accepts "q". */
    @JsonProperty("query")
    @JsonAlias({"q"})
    public String query;

    /** Required action=summarize ; optional otherwise. */
    @JsonProperty("id")
    public String id;

    /** Language ISO code (ex: "fr", "en"). Optional. */
    @JsonProperty("lang")
    public String lang;

    /** Multi turn chat history. Optional. */
    @JsonProperty("history")
    public List<ChatMessage> history = new ArrayList<>();

    /** Name of the agent. Optional (ex: "rag"). */
    @JsonProperty("agent")
    public String agent;

    /** Action requested. "rag", "summarize", "agentic"... */
    @JsonProperty("action")
    public Action action = Action.rag;  // "rag" | "agentic" | "summarize"

    /** Existing conversation ID. Optional */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonProperty("memoryId")
    public String memoryId;

    /** Validate requests depending on action. */
    @JsonIgnore
    public List<String> validate() {
        ArrayList<String> errors = new ArrayList<>();
        String act = action == null ? "rag" : action.name();
        switch (act) {
            case "summarize" -> { if (id == null || id.isBlank()) errors.add("id is required for summarize"); }
            case "rag", "agentic" -> { if (query == null || query.isBlank()) errors.add("query is required for " + act); }
            default -> errors.add("unknown action: " + act);
        }
        return errors;
    }

    public enum Action { rag, agentic, summarize }

    public static class ChatMessage {
        public Role role;
        public String message;
        public enum Role { user, assistant, system }
    }

}


