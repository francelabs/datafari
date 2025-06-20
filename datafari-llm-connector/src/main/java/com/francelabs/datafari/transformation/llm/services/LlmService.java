package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.PromptUtils;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import java.util.List;

/**
 *  This class should be inherited by all LlmService subclasses
 */
public abstract class LlmService {

    private static final Logger LOGGER = LogManager.getLogger(LlmService.class.getName());
    LlmSpecification spec;

    double temperature;
    int maxToken;
    static final String DEFAULT_LLM_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    protected LlmService(LlmSpecification spec) {
        this.temperature = 0;
        try {
            this.maxToken = spec.getMaxTokens();
        } catch (NumberFormatException e) {
            spec.setMaxTokens(200);
        }

        if (spec.getLlm().isEmpty()) spec.setLlm(DEFAULT_LLM_MODEL);
        if (spec.getLlmEndpoint().isEmpty()) spec.setLlmEndpoint(DEFAULT_URL);

        this.spec = spec;
    }

    /**
     * MUST BE OVERRIDDEN !
     * @param prompt A ready-to-use prompt for the LLM
     * @return The string LLM response
     */
    public String invoke(String prompt) throws ManifoldCFException {
        return null;
    }

    /**
     * Generate a summary from a document
     */
    public String summarize(TextSegment chunk, LlmSpecification spec) throws ManifoldCFException {
        String prompt = PromptUtils.promptForSummarization(chunk, spec);
        return invoke(prompt).trim();
    }

    /**
     * Generate a summary from a document
     */
    public String summarizeRecursively(List<TextSegment> chunks, LlmSpecification spec) throws ManifoldCFException {
        String lastSummary = summarize(chunks.get(0), spec);

        int maxIterations = spec.getMaxIterations();

        if (chunks.size() > 1) {
            // For each chunk, we create a new summary based on the chunk content and the previous summary.
            // The index should not exceed maxIterations
            for (int i = 1; i < chunks.size() && (maxIterations > i || maxIterations == 0); i++) {
                TextSegment segment = chunks.get(i);
                String prompt = PromptUtils.promptForRecursiveSummarization(segment, lastSummary, i, spec);
                LOGGER.debug("Processing segment {}", i+1);
                String response = invoke(prompt).trim();
                if (response.isEmpty()) {
                    lastSummary = response;
                } else {
                    break;
                }

            }
        }

        return lastSummary;
    }

    /**
     * Categorize a document into available categories
     */
    public String categorize(TextSegment chunk, LlmSpecification spec) throws ManifoldCFException {
        String prompt = PromptUtils.promptForCategorization(chunk, spec);
        // TODO : Chunking strategy
        return invoke(prompt).trim();
    }
}