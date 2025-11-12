package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.stream.ChatStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;


public abstract class AiService {

    private static final Logger LOGGER = LogManager.getLogger(AiService.class.getName());

    public static final String ID_FIELD = "id";
    public static final String TITLE_FIELD = "title";
    public static final String URL_FIELD = "url";
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
    public static ApiContent error(ChatStream stream, String code, String label, String message, @NotNull String reason, boolean isTool) {
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
    public static ApiContent error(ChatStream stream, String code, String label, String message, @NotNull String reason) {
        return error(stream, code, label, message, reason, false);
    }
}
