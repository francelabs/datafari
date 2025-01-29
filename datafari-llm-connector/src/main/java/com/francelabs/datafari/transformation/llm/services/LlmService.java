package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.ChunkUtils;
import com.francelabs.datafari.transformation.llm.utils.PromptUtils;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 *  This class should be inherited by all LlmService subclasses
 */
public abstract class LlmService {

    private static final Logger LOGGER = LogManager.getLogger(LlmService.class.getName());
    LlmSpecification spec;

    double temperature;
    int maxToken;
    int dimensions = 124;
    static final String DEFAULT_LLM_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
    static final int DEFAULT_DIMENSION = 250;
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    public LlmService(LlmSpecification spec) {
        this.temperature = 0;
        try {
            this.maxToken = spec.getMaxTokens();
        } catch (NumberFormatException e) {
            spec.setMaxTokens(200);
        }
        try {
            this.dimensions = (spec.getVectorDimension() < 1) ? DEFAULT_DIMENSION : spec.getVectorDimension();
        } catch (NumberFormatException e) {
            spec.setVectorDimension(124);
        }

        if (spec.getLlm().isEmpty()) spec.setLlm(DEFAULT_LLM_MODEL);
        if (spec.getEmbeddingsModel().isEmpty()) spec.setEmbeddingsModel(DEFAULT_EMBEDDINGS_MODEL);
        if (spec.getLlmEndpoint().isEmpty()) spec.setLlmEndpoint(DEFAULT_URL);

        this.spec = spec;
    }

    /**
     * MUST BE OVERRIDDEN !
     * @param prompt A ready-to-use prompt for the LLM
     * @return The string LLM response
     */
    public String invoke(String prompt) throws IOException {
        return null;
    }

    /**
     * Generate a summary from a document
     */
    public String summarize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForSummarization(content, spec);
        return invoke(prompt).trim();
    }

    /**
     * Generate a summary from a document
     */
    public String summarizeRecursively(List<TextSegment> chunks, LlmSpecification spec) throws IOException {
        String lastSummary = summarize(chunks.get(0).text(), spec);

        int index = 1;

        if (chunks.size() > 1) {
            // For each chunk, we create a new summary based on the chunk content and the previous summary.
            for (int i = 1; i < chunks.size(); i++) {
                if (spec.getMaxIteration() <= i && spec.getMaxIteration() != 0) break;
                TextSegment segment = chunks.get(i);
                String prompt = PromptUtils.promptForRecursiveSummarization(segment, lastSummary, i, spec);
                LOGGER.debug("Processing segment {}", index);
                lastSummary = invoke(prompt).trim();
            }
        }

        // TODO : Chunking strategy
        return lastSummary;
    }

    /**
     * Categorize a document into available categories
     */
    public String categorize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForCategorization(content, spec);
        // TODO : Chunking strategy
        return invoke(prompt).trim();
    }
}