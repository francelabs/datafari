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

    private static final String SEND_LOCK = "datafari.ws.sendLock";
    private final WebSocketSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebsocketChatStream(WebSocketSession session) {
        this.session = session;
        session.getAttributes().computeIfAbsent(SEND_LOCK, k -> new Object());
    }

    @Override
    public synchronized void event(String type, Map<String, ?> payload) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", type);
            msg.put("data", payload);
            msg.put("ts", System.currentTimeMillis());

            Object lock = session.getAttributes().get(SEND_LOCK);

            synchronized (lock) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
                }
            }

//            session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to send WebSocket event", e);
        }
    }
}
