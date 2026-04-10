package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.models.chatmodels.ChatModelConfigurationManager;
import com.francelabs.datafari.ai.models.chatmodels.ChatModelFactory;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v2_0.users.Assistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public abstract class AiService {

    private static final Logger LOGGER = LogManager.getLogger(AiService.class.getName());

    public static final String ID_FIELD = "id";
    public static final String TITLE_FIELD = "title";
    public static final String URL_FIELD = "url";
    public static final String CLICK_URL_FIELD = "click_url";
    public static final String LLM_SUMMARY_FIELD = "llm_summary";
    public static final String EXACT_CONTENT_FIELD = "exactContent";
    public static final String QUERY_FIELD = "query";
    public static final String STATUS_FIELD = "status";

    protected AiService() {
        // Constructor
    }

    /**
     * Return an error from any AiService.
     * The error is streamed to the user (if streaming is used) and sent as JSON response to the user.
     * @param stream: ChatStream
     * @param code: Error code (500, 402...)
     * @param label: (Optional) A i18n key for localized message in the UI
     * @param message: A user-friendly message
     * @param reason: A technical description of the error
     * @return an ApiContent containing an error
     */
    public static ApiContent error(ChatStream stream, String code, String label, String message, @NotNull String reason, String conversationId, boolean isTool) {
        LOGGER.error("Error {} in AiService: {}", label, reason);

        if (isTool) {
            // When thrown, a "tool.error" event is emitted (Agentic only)
            throw new AgenticToolException(message);
        } else {
            if (stream != null) stream.error(code, label, message, reason);
            ApiContent content = new ApiContent();
            content.error = new ApiError(code, label, message, reason);
            return content;
        }
    }
    public static ApiContent error(ChatStream stream, String code, String label, String message, @NotNull String reason, String conversationId) {
        return error(stream, code, label, message, reason, conversationId, false);
    }

    public static String getMemoryId(ChatStream stream, AiRequest params) {
        if (params.memoryId == null || params.memoryId.isEmpty()) {
            params.memoryId = RandomStringUtils.randomAlphanumeric(15).toUpperCase();
            stream.memory(params.memoryId);
        }
        return params.memoryId;
    }

    /**
     * Saves a user message in postgresql Database
     * @param params AiRequest containing the message content and the conversation ID
     */
    public static String saveUserMessage(HttpServletRequest request, AiRequest params) {
        // TODO : if conversation storage is disabled, return null
        // Save message in Postresql DB
        Assistant assistant = new Assistant();
        try {
            return assistant.saveMessage(request, "user", params.query, params.conversationId); // TODO
        } catch (DatafariServerException e) {
            LOGGER.warn("Could not save message for request: \n{}", params);
            return null;
        }
    }

    /**
     * Saves an assistant message in postgresql Database
     * @param response The service response containing the conversationId and the message
     */
    public static void saveAssistantMessage(HttpServletRequest request, ApiContent response) {
        // TODO : if conversationId is null or if conversation storage is disabled, return null
        // Save message in Postresql DB
        Assistant assistant = new Assistant();
        try {
            assistant.saveMessage(request, "assistant", response.message, response.conversationId);
        } catch (DatafariServerException e) {
            LOGGER.warn("Could not save assistant message in conversation: {}", response.conversationId);
        }
    }

    /**
     * @param response The service response containing the conversationId, the search results (docs) and the message
     */
    public static void saveSearchResults(HttpServletRequest request, ApiContent response) {
        // TODO : if conversationId is null or if conversation storage is disabled, return null
        // Save message in Postresql DB
        Assistant assistant = new Assistant();
        try {
            assistant.saveMessage(request, "assistant", response.message, response.conversationId, response.docs.toJSONString());
        } catch (DatafariServerException e) {
            LOGGER.warn("Could not save search results message in conversation: {}", response.conversationId);
        }
    }

    /**
     * Returns the active chat language model as defined in the models.json configuration file.
     *
     * @param config The RAG configuration object (currently unused, included for future compatibility).
     * @return A {@link ChatModel} instance corresponding to the active model.
     * @throws IOException If an error occurs while reading or parsing the model configuration file.
     */
    public static ChatModel getChatModel(RagConfiguration config) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createChatModel(); // Return the active model
    }
    public static StreamingChatModel getStreamingChatModel(RagConfiguration config) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createStreamingChatModel(); // Return the active model
    }

    /**
     * Returns a specific chat language model by name, as defined in the models.json configuration file.
     *
     * @param modelName The name of the model to load (matching the "name" field in the configuration).
     * @return A {@link ChatModel} instance corresponding to the specified model name.
     * @throws IOException If an error occurs while reading or parsing the model configuration file.
     * @throws IllegalArgumentException If no model is found with the given name.
     */
    private static ChatModel getSpecificChatModel(String modelName) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createChatModel(modelName); // Return a specific model
    }
}
