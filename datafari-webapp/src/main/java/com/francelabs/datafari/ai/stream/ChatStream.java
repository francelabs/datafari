package com.francelabs.datafari.ai.stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

public interface ChatStream {
    void event(String type, Map<String, ?> payload); // Generic event

    default void token(String text) { event("message.delta", Map.of("text", text)); }
    default void phase(String phase) { event("phase", Map.of("name", phase)); }
    default void addSource(JSONObject src) { event("sources.add", Map.of("source", src)); }
    default void addSources(JSONArray items) { event("sources.final", Map.of("items", items)); }
    default void finalMessage(String text) { event("message.final", Map.of("text", text)); }
    default void thinking(String text) { event("thinking", Map.of("text", text)); }
    default void error(String code, String label, String message, String reason) {
        event("error", Map.of("code", code, "label", label, "message", message, "reason", reason));
    }
    default void completed() { event("stream.completed", Map.of()); }
}
