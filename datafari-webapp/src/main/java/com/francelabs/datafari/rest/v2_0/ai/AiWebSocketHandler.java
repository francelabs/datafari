package com.francelabs.datafari.rest.v2_0.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.dto.*;
import com.francelabs.datafari.ai.services.AgenticService;
import com.francelabs.datafari.ai.services.AiRequestHandlerService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.WebsocketChatStream;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.WebSocketHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;


@Component
public class AiWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LogManager.getLogger(AiWebSocketHandler.class.getName());

    private static final String ERROR = "ERROR";
    private static final String OK = "OK";

    private final ObjectMapper mapper = new ObjectMapper();

    public AiWebSocketHandler() {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatStream stream = new WebsocketChatStream(session);

        // TODO : remove logs
        LOGGER.info("EBE - handleTextMessage");
        LOGGER.info("EBE - WS principal = {}", session.getPrincipal());
        LOGGER.info("EBE - WS username = {}", AuthenticatedUserName.getName(session.getPrincipal()));

        try {
            AiWebSocketMessage wsMessage =
                    mapper.readValue(message.getPayload(), AiWebSocketMessage.class);

            // TODO : check type
            if (!"ai.request".equals(wsMessage.type)) {
                stream.error("400", "Bad request", "Unsupported WebSocket message type", wsMessage.type);
                stream.completed(ERROR);
                return;
            }

            AiRequest params = wsMessage.data;
            if (params == null) {
                stream.error("400", "Bad request", "Missing AiRequest data", "data is required");
                stream.completed(ERROR);
                return;
            }

            stream.start();

            List<String> errors = params.validate();
            if (!errors.isEmpty()) {
                stream.error(
                        "400",
                        ApiError.AI_BAD_REQUEST.getKey(),
                        ApiError.AI_BAD_REQUEST.getValue(),
                        String.join("; ", errors)
                );
                stream.completed(ERROR);
                return;
            }

//            HttpServletRequest handshakeRequest = (HttpServletRequest) session.getAttributes().get("httpRequest");
//            HttpServletRequest request = new EditableHttpServletRequest(handshakeRequest);
            HttpServletRequest request = buildRequestFromWebSocketSession(session, params);

            // TODO : remove logs
            LOGGER.info("WS principal = {}", session.getPrincipal());
            LOGGER.info("WS httpRequest principal = {}", request.getUserPrincipal());
            LOGGER.info("WS username = {}", AuthenticatedUserName.getName(request));

            ApiContent content = AiRequestHandlerService.handle(params, request, stream);

            ApiResponse finalApi = new ApiResponse();
            finalApi.status = content.error == null ? OK : ERROR;
            finalApi.content = content;

            stream.completed(finalApi.status);

        } catch (Exception e) {
            stream.error(
                    "500",
                    ApiError.RAG_TECHNICAL_ERROR.getKey(),
                    ApiError.RAG_TECHNICAL_ERROR.getValue(),
                    e.getMessage()
            );
            stream.completed(ERROR);
        }
    }

    private HttpServletRequest buildRequestFromWebSocketSession(
            WebSocketSession session,
            AiRequest params
    ) {

        WebSocketHttpServletRequest request =
                (WebSocketHttpServletRequest) session.getAttributes().get("webSocketHttpRequest");

        if (params.lang != null) {
            request.setAttribute("lang", params.lang);
            request.addParameter("lang", params.lang);
        }

        if (params.query != null) {
            request.addParameter("q", params.query);
            request.addParameter("query", params.query);
        }

        if (params.id != null) {
            request.addParameter("id", params.id);
        }

        if (params.agent != null) {
            request.addParameter("agent", params.agent);
        }

        if (params.action != null) {
            request.addParameter("action", params.action.name());
        }

        if (params.filters != null && params.filters.get("id") != null) {
            request.addParameters(
                    "id",
                    params.filters.get("id").toArray(new String[0])
            );
        }

        request.setAttribute("params", params);

        return request;
    }
}