package com.francelabs.datafari.rag;

import java.util.List;

public interface LlmConnector {
    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String invoke(List<String> prompts, RagConfiguration config);

    /**
     *
     * @param response A String JSON provided by the LLM
     * @return A simple String message
     */
    String extractMessageFromResponse(String response);
}
