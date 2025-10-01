package com.francelabs.datafari.ai.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class StreamToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;
    private final ChatStream stream;
    private final String toolName;
    private final String label;
    private final String icon;
    private final String i18nKey;

    public StreamToolExecutor(String toolName, ToolExecutor delegate, ChatStream stream,
                              String label, String icon, String i18nKey) {
        this.toolName = toolName;
        this.delegate = delegate;
        this.stream = stream;
        this.label = label;
        this.icon = icon;
        this.i18nKey = i18nKey;
    }

    @Override
    public String execute(ToolExecutionRequest req, Object memoryId) {
        String id = java.util.UUID.randomUUID().toString();
        long t0 = System.nanoTime();

        // Sending all available metadata
        Map<String,String> event = new LinkedHashMap<>();
        event.put("id", id);
        event.put("name", toolName);
        event.put("label", label);
        if (icon != null) event.put("icon", icon);
        if (i18nKey != null) event.put("i18nKey", i18nKey);
        event.put("args", Optional.ofNullable(req.arguments()).orElse("{}"));

        stream.event("tool.call", event);

        try {
            String result = delegate.execute(req, memoryId);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.event("tool.result", Map.of(
                    "id", id,
                    "durationMs", ms
            ));
            return result;
        } catch (Throwable t) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.event("tool.error", Map.of(
                    "id", id,
                    "durationMs", ms,
                    "error", String.valueOf(t.getMessage())
            ));
            throw t;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}