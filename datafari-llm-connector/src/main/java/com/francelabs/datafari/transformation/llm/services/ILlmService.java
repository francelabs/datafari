package com.francelabs.datafari.transformation.llm.services;

import java.io.IOException;

/**
 * LlmService classes are created to interact with an LLM API solution.
 * The should extend LlmService.java, and implement ILlmService.
 */
public interface ILlmService {
    /**
     *
     * @param content The document content
     * @return The string LLM response
     */
    String invoke(String content) throws IOException;

}
