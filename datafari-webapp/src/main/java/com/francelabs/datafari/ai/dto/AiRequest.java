package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiRequest {

    /** User query. Also accepts "q". */
    @JsonProperty("query")
    @JsonAlias({"q"})
    public String query;

    /** Required action=summarize ; optional otherwise. */
    @JsonProperty("id")
    public String id;

    /** Required action=summarize ; optional otherwise. */
    @JsonProperty("filters")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public Map<String, List<String>> filters;

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
    public Action action = Action.rag;  // "rag" | "agentic" | "summarize" | "search"

    /** Existing memory ID. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonProperty("memoryId")
    public String memoryId;

    /** Existing conversation ID. If missing, a new conversation is created. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonProperty("conversationId")
    public String conversationId;

    /** Validate requests depending on action. */
    @JsonIgnore
    public List<String> validate() {
        ArrayList<String> errors = new ArrayList<>();
        String act = action == null ? "rag" : action.name();
        switch (act) {
            case "summarize" -> { if (id == null || id.isBlank()) errors.add("id is required for summarize"); }
            case "rag", "agentic", "search" -> { if (query == null || query.isBlank()) errors.add("query is required for " + act); }
            default -> errors.add("unknown action: " + act);
        }
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("query:").append(query)
            .append("\naction:").append(action)
            .append("\nconversationId:").append(conversationId)
            .append("\nlang:").append(lang)
            .append("\nhistory:").append(history.size()).append(" mmessages")
            .append("\nagent:").append(agent)
            .append("\nfilters:").append(filters);
        return super.toString();
    }

    public enum Action { rag, agentic, summarize, search }

    public static class ChatMessage {
        public Role role;
        public String message;
        public enum Role { user, assistant, system }
    }

}


