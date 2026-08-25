package com.francelabs.datafari.rest.v2_0.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AiWebSocketConfig implements WebSocketConfigurer {

    private final AiWebSocketHandler aiWebSocketHandler;

    public AiWebSocketConfig(AiWebSocketHandler aiWebSocketHandler) {
        this.aiWebSocketHandler = aiWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiWebSocketHandler, "/rest/v2.0/ai/ws")
                .addInterceptors(new AiWebSocketHandshakeInterceptor()) // Required to retrieve HttpServletRequest from Handshake HTTP request
                .setAllowedOrigins("*"); // TODO : check security/CORS configuration
    }
}
