package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;

import java.io.IOException;
import java.util.List;

public interface LlmService {
    /**
     *
     * @param content The document content
     * @return The string LLM response
     */
    String invoke(String content) throws IOException;

    /**
     * @param content The document content
     * @return The string LLM response
     */
    float[] embeddings(String content) throws IOException;

    /**
     * @param content The document content
     * @return The string LLM response
     */
    String summarize(String content, LlmSpecification spec) throws IOException;

    /**
     * @param content The document content
     * @return The string LLM response
     */
    String categorize(String content, LlmSpecification spec) throws IOException;

}
