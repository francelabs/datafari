package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.agent.CfPAgent;
import com.francelabs.datafari.ai.agentic.agent.DatafariAgent;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.data.document.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class AgenticService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(AgenticService.class.getName());

    public static ApiContent agentic(AiRequest params, HttpServletRequest request,
                                     ChatStream stream, SourcesAccumulator sourcesAcc) {

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();

        // Is AGENTIC RAG enabled ?
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_AGENTIC_RAG))
            return error(stream, "422", "ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.", null);

        ApiContent response = new ApiContent();
        try {

            // Retrieve query from request params
            String query = params.query;
            LOGGER.debug("RagService - RAG - RAG query : {}", query);
            if (query == null || query.isEmpty()) {
                return error(stream, "422", "ragBadRequest",
                        "Sorry, it appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.",
                        "'id' must not be null");
            }

            stream.phase("agent:preparation");


            DatafariAgent agent;
            if (params.agent == null) params.agent = "";
            switch (params.agent) {
                case "cfp":
                    agent = new DatafariAgent(request, stream, sourcesAcc);
                    break;
                    // TODO : implement multi agent
                case "rag":
                default:
                    LOGGER.info("AiPowered - RAG - Using RAG Agent");
                    agent = new DatafariAgent(request, stream, sourcesAcc);

            }

            stream.phase("agent:start");
            String answer = agent.stream(query);

            // Final & full response
            stream.finalMessage(answer);
            response.message = answer;

            // Final sources
            JSONArray merged = sourcesAcc.toJsonArray();

            stream.phase("agent:done");

        } catch (Exception e) {
            return error(stream, "500", "ragTechnicalError",
                    "Sorry, the agent met an unexpected error. Please try again later, and if the problem remains, contact an administrator.", e.getLocalizedMessage());
        }
        return response;
    }

}
