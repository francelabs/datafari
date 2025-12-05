package com.francelabs.datafari.ai.stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public interface ChatStream {
    void event(String type, Map<String, ?> payload); // Generic event

    default void start() { event("stream.started", Map.of()); }
    default void token(String text) { event("message.delta", Map.of("text", text)); }
    default void phase(String phase) { event("phase", Map.of("name", phase)); }
    default void memory(String memoryId) { event("memory", Map.of("memoryId", memoryId)); }

    // Sources
    default void addSource(JSONObject src) { event("sources.add", Map.of("source", src)); }
    default void addSources(JSONArray items) { event("sources.final", Map.of("items", items)); }

    // Tools
    default void toolCall(String id, String toolName, String label, String icon, String i18nKey) {
        Map<String,String> args = new HashMap<>(Map.of("id", id, "toolName", toolName, "label", label));
        if (icon != null) args.put("icon", icon);
        if (i18nKey != null) args.put("i18nKey", i18nKey);
        event("tool.call", args);
    }
    default void toolResult(String id, long durationMs) {
        event("tool.result", Map.of("id", id, "durationMs", durationMs));
    }
    default void toolError(String id, long durationMs, String message) {
        event("tool.error", Map.of("id", id, "durationMs", durationMs, "error", message));
    }


    default void finalMessage(String text) { event("message.final", Map.of("text", text)); }
    default void thinking(String text) { event("thinking", Map.of("text", text)); }

    // Human in the loop
    default void ask(String text, String memoryId) { event("ask", Map.of("text", text, "memoryId", memoryId)); }
    default void error(String code, String label, String message, String reason) {
        event("error", Map.of("code", code, "label", label, "message", message, "reason", reason));
    }
    default void completed(String status) { event("stream.completed", Map.of("status", status)); }
}
