package com.francelabs.datafari.rest.v2_0.ai;

import com.francelabs.datafari.utils.AuthenticatedUserName;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class AiWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger LOGGER = LogManager.getLogger(AiWebSocketHandshakeInterceptor.class.getName());


    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        LOGGER.info("EBE - beforeHandshake = {}", attributes);

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

//            attributes.put("httpRequest", httpRequest);
            attributes.put("principal", httpRequest.getUserPrincipal());
            attributes.put("cookies", httpRequest.getCookies());
            attributes.put("session", httpRequest.getSession(false));
            attributes.put("contextPath", httpRequest.getContextPath());
            attributes.put("servletPath", httpRequest.getServletPath());
            attributes.put("requestURI", httpRequest.getRequestURI());
//            attributes.put("sessionId", httpRequest.getSession(false) != null
//                    ? httpRequest.getSession(false).getId()
//                    : null);

            // TODO : remove logs
            LOGGER.info("WS httpRequest principal = {}", httpRequest.getUserPrincipal());
            LOGGER.info("WS username = {}", AuthenticatedUserName.getName(httpRequest));
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
