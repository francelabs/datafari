package com.francelabs.datafari.ai.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This ChatStream is used to Stream events from Datafari to the UI in AI Powered processes.
 */
public class WebsocketChatStream implements ChatStream {

    private final WebSocketSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebsocketChatStream(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public synchronized void event(String type, Map<String, ?> payload) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", type);
            msg.put("data", payload);
            msg.put("ts", System.currentTimeMillis());

            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to send WebSocket event", e);
        }
    }
}
