package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.agents.cfp.CfPAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.common.*;
import com.francelabs.datafari.ai.agentic.agents.custom.CustomAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.generic.GenericAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentBuilder;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IAgentService;
import com.francelabs.datafari.ai.agentic.agents.interfaces.IStreamingAgentService;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.stream.AgentStreamer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.TokenStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AgenticService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(AgenticService.class.getName());

    public static ApiContent agentic(AiRequest params, HttpServletRequest request,
                                   ChatStream stream, SourcesAccumulator sourcesAcc) {
        return agentic(params, request, stream, sourcesAcc, false);
    }

    public static ApiContent agentic(AiRequest params, HttpServletRequest request,
                                     ChatStream stream, SourcesAccumulator sourcesAcc,
                                     boolean isTool) {

        // TODO : add websocket support

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

            // Agent Builder instanciation
            IAgentBuilder builder;
            if (params.agent == null) params.agent = "";
            switch (params.agent) {
                case "cfp":
                    LOGGER.debug("AgenticService - Using custom Agent");
                    builder = new CfPAgentBuilder(request, stream, sourcesAcc);
                    break;
                case "custom":
                    LOGGER.debug("AgenticService - Using custom Agent");
                    builder = new CustomAgentBuilder(request, stream, sourcesAcc);
                    break;
                case "generic":
                case "rag":
                default:
                    LOGGER.debug("AgenticService - Using generic Agent");
                    builder = new GenericAgentBuilder(request, params, stream, sourcesAcc);
            }

            stream.phase("agent:start");

            // Models
            ChatModel chatModel = AiService.getChatModel(config);

            IStreamingAgentService streamingAgent = builder.buildStreamAgent();
            IAgentService agent = builder.buildAgent();

            String answer;
            if (config.getBooleanProperty(RagConfiguration.AGENTIC_ENABLE_LOOP_CONTROL)) {
                // No streaming with control loop

                // Evaluator instantiation
                ResponseScorer evaluator = AgenticServices
                    .agentBuilder(ResponseScorer.class)
                    .chatModel(chatModel)
                    .outputKey("score")
                    .build();

                // loop control
                // The response must get a score of 0.8/1, and can only attempt 3 iterations
                int maxIterations = config.getIntegerProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON, 3);
                double minScore = config.getDoubleProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE, 0.8);

                // Secondary iterations : starting on the 3rd iteration, the minScore is lowered to the secondary threshold (0.6).
                // If maxIterationsBeforeSecondary is set to 0, we don't want to apply secondary iteration
                int maxIterationsBeforeSecondary = config.getIntegerProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON_SECONDARY, 3) == 0 ?
                    config.getIntegerProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MAX_ITERATON_SECONDARY, 3) : maxIterations + 1;
                double minScoreSecondary = config.getDoubleProperty(RagConfiguration.AGENTIC_LOOP_CONTROL_MIN_SCORE_SECONDARY, 0.6);

                UntypedAgent responseReviewLoop = AgenticServices
                    .loopBuilder()
                    .subAgents(agent, evaluator)
                    .maxIterations(maxIterations)
                    .testExitAtLoopEnd(true)
                    .exitCondition( (agenticScope, loopCounter) -> {
                        double score = agenticScope.readState("score", 0.0);
                        LOGGER.info("Loop Control: Attempt {} - score {}", loopCounter, score);
                        return loopCounter <= maxIterationsBeforeSecondary ? score >= minScore : score >= minScoreSecondary;
                    })
                    .build();

                IAgentService supervisor = AgenticServices
                    .sequenceBuilder(AgenticOrchestrator.class)
                    .subAgents(agent, responseReviewLoop)
                    .outputKey("response")
                    .build();
                answer = ask(params, stream, supervisor, request);
            } else {
                // Streaming, no control loop
                answer = stream(params, stream, streamingAgent, request);
            }

            // Final & full response
            response.message = answer;

            // Final sources
            response.sources = sourcesAcc.toJsonArray();

            stream.phase("agent:done");

        } catch (Exception e) {
            LOGGER.error(e);
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

    static public String ask(AiRequest params, ChatStream stream, IAgentService supervisor, HttpServletRequest request) {
        String memoryId = AiService.getMemoryId(stream, params);
        String history = readChatHistory(params);
        String basket = readDocsBasket(params, request);
        return supervisor.ask(memoryId, params.query, PromptUtils.getUserLanguage(request), history, basket); // TODO : ADD DOCSBASKET
    }

    static public String stream(AiRequest params, ChatStream stream, IStreamingAgentService agent, HttpServletRequest request) {
        String memoryId = AiService.getMemoryId(stream, params);
        AgentStreamer streamer = new AgentStreamer();
        String history = readChatHistory(params);
        String basket = readDocsBasket(params, request);
        TokenStream ts = agent.stream(memoryId, params.query, PromptUtils.getUserLanguage(request), history, basket);
        return streamer.stream(ts, stream::event);
    }


    /**
     * @return a String containing the chat history (if any), with assistant and user messages
     */
    static String readChatHistory(AiRequest params) {

        RagConfiguration config = RagConfiguration.getInstance();
        if (params.history == null || !config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED)) {
            return "";
        }

        final int size = params.history.size();
        final int maxSize = config.getIntegerProperty(RagConfiguration.CHAT_MEMORY_HISTORY_SIZE);
        final int from = Math.max(0, size - maxSize);
        StringBuilder sb = new StringBuilder();

        for (int i = from; i < size; i++) {

            AiRequest.ChatMessage message = params.history.get(i);
            String role = (message != null && message.role != null) ? message.role.name().toUpperCase() : "USER";
            String text = (message != null && message.message != null) ? message.message : "";

            // Avoid multiline in compact history
            text = text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
            text = text.replace('\n', ' ').trim();

            sb.append(role).append(": ").append(text).append("\n");
        }

        return sb.toString();
    }


    /**
     * @return a String prompt containing the user's docsbasket, including documents title and ID
     */
    static String readDocsBasket(AiRequest params, HttpServletRequest request) {

        if (params.filters == null || params.filters.get("id") == null || params.filters.get("id").isEmpty()) {
            return "";
        }

        List<String> ids = params.filters.get("id");
        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        req.addParameter("q", "*:*");
        req.addParameter("fl", "id,docId,title");
        req.addParameter("rows", String.valueOf(ids.size()));
        req.addParameter("fq", buildIdsFilter(ids));

        // Search documents from Datafari search to retrieve titles
        JSONObject response = SearchUtils.processSearch(req, "/select");

        Map<String, String> titlesById = extractTitlesById(response);

        StringBuilder docsList = new StringBuilder();
        for (String id : ids) {
            String title = titlesById.getOrDefault(id, "untitled");
            docsList.append("- ")
                    .append(title)
                    .append(" (ID: ")
                    .append(id)
                    .append(")\n");
        }

        // "You have access to a set of documents selected by the user:"
        String template =  PromptUtils.getStringBasket();
        return template + docsList.toString().trim();
    }

    private static String buildIdsFilter(List<String> ids) {
        return "docId:(" + ids.stream()
                .map(AgenticService::escapeSolrQueryChars)
                .collect(Collectors.joining(" OR ")) + ")";
    }

    private static Map<String, String> extractTitlesById(JSONObject response) {

        Map<String, String> titlesById = new HashMap<>();

        if (response == null) {
            return titlesById;
        }

        JSONObject solrResponse = (JSONObject) response.get("response");
        if (solrResponse == null) {
            return titlesById;
        }

        JSONArray docs = (JSONArray) solrResponse.get("docs");
        if (docs == null) {
            return titlesById;
        }

        for (Object obj : docs) {
            if (!(obj instanceof JSONObject doc)) {
                continue;
            }

            String id = (String) doc.get("docId");
            if (id == null) {
                continue;
            }

            String title = readStringOrFirstArrayValue(doc, "title");

            if (title == null || title.isBlank()) {
                title = "untitled";
            }

            titlesById.put(id, title);
        }

        return titlesById;
    }

    private static String readStringOrFirstArrayValue(JSONObject doc, String field) {
        Object value = doc.get(field);

        switch (value) {
            case null -> {
                return null;
            }
            case String s -> {
                return s;
            }
            case JSONArray array -> {
                if (!array.isEmpty()) {
                    Object first = array.getFirst();
                    return first != null ? first.toString() : null;
                }
                return null;
            }
            default -> {
            }
        }

        return value.toString();
    }

    private static String escapeSolrQueryChars(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("!", "\\!")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace(":", "\\:")
                .replace("^", "\\^")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("\"", "\\\"")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\~")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace("|", "\\|")
                .replace("&", "\\&")
                .replace("/", "\\/");
    }

}
