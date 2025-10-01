package com.francelabs.datafari.ai.stream;

import java.util.Map;

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
