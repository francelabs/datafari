package com.francelabs.datafari.rag;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * This interface can be used to create new services able to call any LLM API.
 */
public interface LlmService {

    /**
     *
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    String generate(List<Message> prompts, HttpServletRequest request) throws IOException;

    float[] embed(String prompts) throws IOException;

}
