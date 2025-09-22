package com.francelabs.datafari.ai.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

public final class SseToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;
    private final SseBridge sse;
    private final String toolName;

    public SseToolExecutor(String toolName, ToolExecutor delegate, SseBridge sse) {
        this.toolName = toolName;
        this.delegate = delegate;
        this.sse = sse;
    }

    @Override
    public String execute(ToolExecutionRequest req, Object memoryId) {
        long t0 = System.nanoTime();
        sse.send("tool_call", "{\"name\":\"" + escape(toolName) + "\",\"args\":" + safeArgs(req) + "}");
        try {
            String result = delegate.execute(req, memoryId);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            sse.send("tool_result",
                    "{\"name\":\"" + escape(toolName) + "\",\"durationMs\":" + ms + ",\"result\":" + jsonSnippet(result) + "}");
            return result;
        } catch (Throwable t) {
            sse.send("tool_error",
                    "{\"name\":\"" + escape(toolName) + "\",\"error\":\"" + escape(t.getMessage()) + "\"}");
            throw t;
        }
    }

    private static String safeArgs(ToolExecutionRequest req) {
        String a = req.arguments();
        if (a == null || a.isBlank()) return "{}";
        return a;
    }

    private static String jsonSnippet(String s) {
        // tronque pour éviter d’inonder le front; ajuste au besoin
        if (s == null) return "null";
        String trimmed = s.length() > 2000 ? s.substring(0, 2000) + "…" : s;
        return "\"" + escape(trimmed) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}