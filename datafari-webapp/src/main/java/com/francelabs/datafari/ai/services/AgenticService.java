package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.agent.*;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v2_0.users.Assistant;
import com.francelabs.datafari.utils.rag.SearchUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.http.HttpServletRequest;

public class AgenticService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(AgenticService.class.getName());

    public static ApiContent agentic(AiRequest params, HttpServletRequest request,
                                   ChatStream stream, SourcesAccumulator sourcesAcc) {
        return agentic(params, request, stream, sourcesAcc, false);
    }

    public static ApiContent agentic(AiRequest params, HttpServletRequest request,
                                     ChatStream stream, SourcesAccumulator sourcesAcc,
                                     boolean isTool) {

        LOGGER.info("AiPowered - Agentic - Agentic request received.");

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();

        // Is AGENTIC RAG enabled ?
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_AGENTIC))
            return error(stream, "422",
                ApiError.AGENTIC_ERROR_NOT_ENABLED.getKey(),
                ApiError.AGENTIC_ERROR_NOT_ENABLED.getValue(),
                "Agentic service is disabled in configuration.", null, isTool);

        if (!isTool) {
            // Save message in Postgresql DB
            params.conversationId = saveUserMessage(request, params);
        }

        // Apply filters
        if (params.filters != null && !params.filters.isEmpty()) {
            request = SearchUtils.filtersParamToFq(request, params);
        }

        ApiContent response = new ApiContent();
        try {

            // Retrieve query from request params
            String query = params.query;
            LOGGER.debug("RagService - Agentic - Agentic query : {}", query);
            if (query == null || query.isEmpty()) {
                return error(stream, "422",
                    ApiError.AGENTIC_BAD_REQUEST.getKey(),
                    ApiError.AGENTIC_BAD_REQUEST.getValue(),
                    "'id' must not be null", params.conversationId, isTool);
            }

            stream.phase("agent:preparation");



            IAgent agent;
            if (params.agent == null) params.agent = "";
            switch (params.agent) {
                case "cfp":
                    LOGGER.debug("AgenticService - Using CFP Agent");
                    agent = new CfPAgent(request, params, stream, sourcesAcc);
                    break;
                case "custom":
                    LOGGER.debug("AgenticService - Using custom Agent");
                    agent = new CustomAgent(request, params, stream, sourcesAcc);
                    break;
                case "rag":
                default:
                    LOGGER.debug("AgenticService - Using RAG Agent");
                    agent = new RagAgent(request, params, stream, sourcesAcc);

            }

//            ChatModel chatModel = RagAPI.getChatModel(config);
//            HumanInTheLoop AskUserAgent = AiServices.builder(HumanInTheLoop.class)
//              .streamingChatModel(RagAPI.getStreamingChatModel(config))
//              .chatMemoryProvider(memory)
//              .build();
//            SupervisorAgent supervisor = AgenticServices
//                .supervisorBuilder()
//                .chatModel(chatModel)
//                .subAgents(agent, askAgent)
//                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
//                .build();

            stream.phase("agent:start");
            // TODO : try/catch controlled error
            String answer = agent.ask(query);

            // Final & full response
//            stream.finalMessage(answer);
            response.message = answer;

            // Final sources
            response.sources = sourcesAcc.toJsonArray();

            stream.phase("agent:done");

        } catch (Exception e) {
            return error(stream, "500",
                    ApiError.AGENTIC_TECHNICAL_ERROR.getKey(),
                    ApiError.AGENTIC_TECHNICAL_ERROR.getValue(),
                    e.getLocalizedMessage(), params.conversationId, isTool);
        }

        // Save message in Postgresql DB
        if (!isTool) {
            response.conversationId = params.conversationId;
            saveAssistantMessage(request, response);
        }

        return response;
    }

}
