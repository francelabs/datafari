package com.francelabs.datafari.rag;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface LlmConnector {
    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String invoke(List<String> prompts, HttpServletRequest request) throws IOException;

}
