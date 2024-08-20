package com.francelabs.datafari.rag;

import java.io.IOException;
import java.util.List;

public interface LlmConnector {
    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String invoke(List<String> prompts, RagConfiguration config, String userQuery) throws IOException;

}
