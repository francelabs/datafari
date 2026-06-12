package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v2_0.ai.AiPowered;
import com.francelabs.datafari.service.db.ConversationDataService;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AiRequestHandlerService {

    private static final Logger LOGGER = LogManager.getLogger(AiRequestHandlerService.class.getName());

    public static ApiContent handle(AiRequest params, HttpServletRequest request, ChatStream stream) {

        // If no action is provided, using "rag" by default
        AiRequest.Action action = params.action == null ? AiRequest.Action.agentic : params.action;
        if (params.lang != null) request.setAttribute("lang", params.lang);

        // Create a memory ID if not existing
        params.memoryId = AiService.getMemoryId(stream, params);

        stream.phase("service.started");

        SourcesAccumulator sourcesAcc = new SourcesAccumulator(stream);

        ApiContent result = new ApiContent();

        request.setAttribute("params", params);

        try {
            result = switch (action.name()) {
                case "rag" -> RagService.rag(request, params, stream, sourcesAcc, false);
                case "agentic" -> AgenticService.agentic(params, request, stream, sourcesAcc, false);
                case "summarize" -> SummarizationService.summarize(params, request, stream, false);
                case "synthesize" -> SynthesisService.synthesize(params, request, stream, false);
                case "search" -> SearchService.search(params, request, stream, sourcesAcc);
                default -> result;
            };

            // JSONize and stream the final sources
            result.sources = sourcesAcc.toJsonArray();

            if (result.message != null && !result.message.isBlank())
                stream.finalMessage(result.message);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in AIService.", e);
            return AiService.error(stream, "500",
                    ApiError.RAG_TECHNICAL_ERROR.getKey(),
                    ApiError.RAG_TECHNICAL_ERROR.getValue(),
                    e.getMessage(),
                    params.conversationId
            );
        }


        if (params.conversationId != null) {
            try {
                ConversationDataService service = ConversationDataService.getInstance();
                String conversationTitle = service.getConversationTitle(params.conversationId);
                result.conversationId = params.conversationId;

                // If needed, the conversation is renamed
                String username = AuthenticatedUserName.getName(request);
                if (params.query != null && !params.query.isBlank()
                        && "New conversation".equals(conversationTitle)
                        && username != null) {
                    service.updateConversationTitle(result.conversationId, AuthenticatedUserName.getName(request), params.query);
                }

                stream.conversation(result.conversationId, conversationTitle);

            } catch (DatafariServerException e) {
                LOGGER.warn("Unable to retrieve conversationId");
            }
        }

        stream.phase("service:done");

        return result;
    }
}
