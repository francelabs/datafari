package com.francelabs.datafari.ai.stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ChatStream {

    void event(String type, Map<String, ?> payload); // Generic event

    /** Invoked at the beginning of an AI Powered process */
    default void start() { event("stream.started", Map.of()); }
    /** Update the "phase" indicator in the UI (ex: "rag.retrieval") */
    default void phase(String phase) { event("phase", Map.of("name", phase)); }
    /** Sends the agent memory ID to the UI */
    default void memory(String memoryId) { event("memory", Map.of("memoryId", memoryId)); }

    /** Sends information about the current conversation */
    // TODO : retrieve conversation title
    default void conversation(String conversationId, String title) {
        event("conversation", Map.of("conversationId", conversationId, "title", title));
    }

    // Sources
    /** Invoked by source accumulator when a source is added */
    default void addSource(JSONObject src) { event("sources.add", Map.of("source", src)); }
    /** Invoked by source accumulator when multiple sources are added */
    default void addSources(JSONArray items) { event("sources.final", Map.of("items", items)); }

    // Tools
    /** Invoked by the tool loader when a tool is called by the agent */
    default void toolCall(String id, String toolName, String label, String icon, String i18nKey) {
        Map<String,String> args = new HashMap<>(Map.of("id", id, "toolName", toolName, "label", label));
        if (icon != null) args.put("icon", icon);
        if (i18nKey != null) args.put("i18nKey", i18nKey);
        event("tool.call", args);
    }
    /** Invoked by the tool loader at the end of a tool execution. */
    default void toolEnd(String id, long durationMs) {
        event("tool.end", Map.of("id", id, "durationMs", durationMs));
    }
    /** Invoked from a tool to show results or parameters to the user */
    default void toolResult(String id, Map<String, String> args) {
        HashMap<String, String> params = new HashMap<>(args);
        params.put("id", id);
        event("tool.result", params);
    }
    /** Invoked by the tool loader if an exception is thrown by a tool */
    default void toolError(String id, long durationMs, String message) {
        event("tool.error", Map.of("id", id, "durationMs", durationMs, "error", message));
    }

    // Search from chatbot
    /** Invoked by the search service to send a list of search results */
    default void searchResults(JSONArray docs) {
        event("search.results", Map.of("docs", docs));
    }


    /** Invoked by StreamingChatModels, sends one single token of the response */
    default void token(String text) { event("message.delta", Map.of("text", text)); }
    /** Invoked at the end of the process, sends the full and final LLM response. */
    default void finalMessage(String text) { event("message.final", Map.of("text", text)); }
    /** Invoked by StreamingChatModels, sends the thinking process (optional). */
    default void thinking(String text) { event("thinking", Map.of("text", text)); }

    /** Invoked by agents to request information (human in the loop). */
    default void humanInputRequired(
            String interactionId,
            String kind,
            String inputType,
            String title,
            String message,
            List<String> options,
            Map<String, ?> payload
    ) {
        Map<String, Object> args = new HashMap<>();

        args.put("interactionId", interactionId);
        args.put("kind", kind);
        args.put("inputType", inputType);
        args.put("title", title != null ? title : "");
        args.put("message", message != null ? message : "");
        args.put("options", options != null ? options : List.of());
        args.put("arguments", payload != null ? payload : Map.of()); // TODO : check arguments

        event("human.input.required", args);
    }

    /** Notify the reception of a human's response (human in the loop). */
    default void humanInputReceived(String interactionId) {
        event("human.input.received", Map.of("interactionId", interactionId));
    }

    /** Notify a timeout while waiting for human's input (human in the loop). */
    default void humanInputTimeout(String interactionId) {
        event("human.input.timeout", Map.of("interactionId", interactionId));
    }

    /** Notify an error to the UI. */
    default void error(String code, String label, String message, String reason) {
        event("error", Map.of("code", code, "label", label, "message", message, "reason", reason));
    }
    /** Notify the end of the stream */
    default void completed(String status) { event("stream.completed", Map.of("status", status)); }
}
