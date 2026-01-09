package com.francelabs.datafari.ai.stream;

import java.util.Map;

/**
 * This ChatStream is used to Stream events from Datafari to the UI in AI Powered processes.
 */
public class NdjsonChatStream implements ChatStream {
    private final NdjsonEmitter emitter;
    public NdjsonChatStream(NdjsonEmitter emitter) { this.emitter = emitter; }
    @Override public void event(String type, Map<String, ?> payload) {
        emitter.send(new java.util.LinkedHashMap<>() {{
            put("type", type);
            put("data", payload);
            put("ts", System.currentTimeMillis());
        }});
    }
}
