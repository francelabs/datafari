package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.agent.CfPAgent;
import com.francelabs.datafari.ai.agentic.agent.DatafariAgent;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.rest.v2_0.ai.AiPowered;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.data.document.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RagService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(RagService.class.getName());

    public static ApiContent rag(HttpServletRequest request, AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        ApiContent response = new ApiContent();

        LOGGER.info("AiPowered - RAG - RAG request received.");

        String query; // The user request
        String id;
        boolean ragBydocument = false;
        JSONObject searchResults;

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();

        // Is RAG enabled ?
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_RAG))
            return error(stream, "422", "ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.", null);

        // Retrieve query from request params
        if (params.query != null) {
            query = params.query;
            LOGGER.debug("RagService - RAG - RAG query : {}", query);
        } else {
            LOGGER.warn("RagService - RAG - Missing query parameter");
            return error(stream, "422", "ragBadRequest",
                    "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.",
                    "'id' must not be null");
        }

        //
        // HISTORY
        // Retrieve, if enabled, the chat history
        //
        if (params.history != null && !params.history.isEmpty()) {
            stream.phase("rag:retrieving chat history");
            List<AiRequest.ChatMessage> history = params.history;
            request.setAttribute("history", history);
            // TODO : check the integrity of history
            LOGGER.debug("AiPowered - RAG - Conversation history retrieved for request: {}", query);
        }

        //
        // QUERY REWRITING FOR VECTOR SEARCH
        // If enabled, we use a LLM to reformulate the user query into a search query
        // Only applies to vector or hybrid search
        //
        String vectorQuery = query;
        if (config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_VECTOR)
                && List.of("rrf", "vector").contains(config.getProperty(RagConfiguration.RETRIEVAL_METHOD))
                && params.id == null
        ) {
            stream.phase("rag:query rewriting");
            try {
                vectorQuery = RagAPI.rewriteSearchQuery(query, "vector", request, config);
            } catch (IOException e) {
                LOGGER.error("Query rewriting failed ! Initial user query will be use for the search.", e);
            }
        }

        //
        // QUERY REWRITING FOR BM25 SEARCH
        // If enabled, we use a LLM to reformulate the user query into a search query
        // Only applies to BM25 or hybrid search
        //
        String bm25Query = query;
        if (config.getBooleanProperty(RagConfiguration.CHAT_QUERY_REWRITING_ENABLED_BM25)
                && List.of("rrf", "bm25").contains(config.getProperty(RagConfiguration.RETRIEVAL_METHOD))
                && params.id == null
        ) {
            stream.phase("rag:query rewriting");
            try {
                bm25Query = RagAPI.rewriteSearchQuery(query, "bm25", request, config);
            } catch (IOException e) {
                LOGGER.error("Query rewriting failed ! Initial user query will be use for the search.", e);
                vectorQuery = query;
            }
        }

        //
        // RETRIEVAL
        // Retrieve documents or snippets using Datafari search API
        //
        try {
            // Retrieve ID from JSON input
            stream.phase("rag:retrieval");
            if (params.id != null && !params.id.isEmpty()) {
                id = params.id;
                LOGGER.debug("AiPowered - RAG - Retrieving document {} for RAG by Document.", id);
                searchResults = SearchUtils.findDocumentById(request, id);
                ragBydocument = true;
            } else {
                LOGGER.debug("AiPowered - RAG - Performing search.");
                request.setAttribute("q.op", config.getProperty(RagConfiguration.SEARCH_OPERATOR));
                // rewritten query must not be used for BM25 search
                searchResults = SearchUtils.performCustomSearch(request, bm25Query, vectorQuery,
                        config.getProperty(RagConfiguration.RETRIEVAL_METHOD, "bm25"),
                        config);
            }
        } catch (IOException | ServletException e) {
            LOGGER.error("AiPowered - RAG - ERROR. An error occurred while retrieving documents.", e);
            return error(stream, "500",
                    "ragTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                    e.getLocalizedMessage());
        }

        // Retrieve language
        if (params.lang != null) {
            request.setAttribute("lang", params.lang);
        }


        //
        // RAG RESPONSE GENERATION
        // The request is processed using Datafari Rag API
        //
        try {
            // Process the document(s) using RagAPI methods
            stream.phase("rag:response generation");
            EditableHttpServletRequest editablerequest = new EditableHttpServletRequest(request);
            editablerequest.addParameter("q", query);
            return RagAPI.rag(editablerequest, searchResults, ragBydocument, stream, sourcesAcc);
        } catch (final Exception e) {
            LOGGER.error("AiPowered - RAG - ERROR", e);
            return error(stream, "500", "ragTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e.getLocalizedMessage());
        }
    }
}
