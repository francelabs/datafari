package com.francelabs.datafari.rag;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface LlmService {
    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String invoke(List<String> prompts, HttpServletRequest request) throws IOException;

    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String generate(List<Message> prompts, HttpServletRequest request) throws IOException;

    float[] embed(String prompts) throws IOException;

}
