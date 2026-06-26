package com.francelabs.datafari.rest.v2_0.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputService;
import com.francelabs.datafari.ai.dto.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
public class AiWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LogManager.getLogger(AiWebSocketHandler.class.getName());

    private static final String ERROR = "ERROR";
    private static final String OK = "OK";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool();

    public AiWebSocketHandler() {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatStream stream = new WebsocketChatStream(session);

        try {
            AiWebSocketMessage wsMessage = mapper.readValue(message.getPayload(), AiWebSocketMessage.class);
            LOGGER.info("EBE - handleTextMessage : ws message - interactionId={}, value={}", wsMessage.interactionId, wsMessage.value);

            if ("human.input.response".equals(wsMessage.type)) {
                LOGGER.info("EBE - human.input.response");
                handleHumanInputResponse(stream, wsMessage);
                return;
            }

            // Reject unsupported message types
            if (!"ai.request".equals(wsMessage.type)) {
                LOGGER.info("EBE - !ai.reques");
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

            // Start the service in an async thread
            aiExecutor.submit(() -> handleAiRequest(session, stream, wsMessage));

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

    private void handleAiRequest(
            WebSocketSession session,
            ChatStream stream,
            AiWebSocketMessage wsMessage
    ) {
        try {
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

            HttpServletRequest request = buildRequestFromWebSocketSession(session, params);

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

        WebSocketHttpServletRequest baseRequest = (WebSocketHttpServletRequest) session.getAttributes().get("webSocketHttpRequest");
        if (baseRequest == null) {
            throw new IllegalStateException("Missing webSocketHttpRequest in WebSocket session attributes");
        }
        WebSocketHttpServletRequest request = baseRequest.copy();

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

    private void handleHumanInputResponse(
            ChatStream stream,
            AiWebSocketMessage wsMessage
    ) {
        LOGGER.info("EBE - handleHumanInputResponse : handling message - interactionId={}, value={}", wsMessage.interactionId, wsMessage.value);
        if (wsMessage.interactionId == null || wsMessage.interactionId.isBlank()) {
            LOGGER.info("EBE - interactionId missing");
            stream.error(
                    "400",
                    "Bad request",
                    "Missing human input interactionId",
                    "interactionId is required"
            );
            return;
        }

        if (wsMessage.value == null) {
            stream.error(
                    "400",
                    "Bad request",
                    "Missing human input value",
                    "value is required"
            );
            return;
        }

        // Notify reception of the message
        stream.humanInputReceived(wsMessage.interactionId);

        boolean accepted = HumanInputService.answer(
                wsMessage.interactionId,
                wsMessage.value
        );

        // The human response is rejected:
        if (!accepted) {
            stream.error("404", "humanInputNotFound",
                    "Human input request not found or already completed",
                    wsMessage.interactionId);
            return;
        }
        LOGGER.info("EBE - all OK");
    }
}