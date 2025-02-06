package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;
import java.time.Duration;

public class OpenAiLlmService extends LlmService implements ILlmService {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmService.class.getName());

    static final String DEFAULT_LLM_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    public OpenAiLlmService(LlmSpecification spec) {
        super(spec);

        if (spec.getLlm().isEmpty()) spec.setLlm(DEFAULT_LLM_MODEL);
        if (spec.getEmbeddingsModel().isEmpty()) spec.setEmbeddingsModel(DEFAULT_EMBEDDINGS_MODEL);
        if (spec.getLlmEndpoint().isEmpty()) spec.setLlmEndpoint(DEFAULT_URL);
    }


    /**
     * Call the LLM API (OpenAI, Datafari AI Agent...) with a simple String prompt.
     * @param prompt A ready-to-use prompt for the LLM
     * @return The string LLM response
     */
    @Override
    public String invoke(String prompt) throws ManifoldCFException {

        ChatLanguageModel llm = OpenAiChatModel.builder()
                .apiKey(spec.getApiKey())
                .temperature(temperature)
                .maxTokens(spec.getMaxTokens())
                .modelName(spec.getLlm())
                .baseUrl(spec.getLlmEndpoint())
                .maxRetries(3)
                .timeout(Duration.ofSeconds(1200))
                .build();
        String response = "";


        try {
            response = llm.generate(prompt);

            // Wait for a few ms after the request to avoid exceeding rate limit
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (OpenAiHttpException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("An error occurred while calling LLM AI request.", e);

            if (isErrorFatal(e)) {
                throw new ManifoldCFException(e);
            }

            // Retrying after 2 seconds...
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex2) {
                Thread.currentThread().interrupt();
            }

            response = llm.generate(prompt);
        }
        return response;
    }

    private boolean isErrorFatal(Exception e) {
        String errormessage = e.getMessage();

        // TODO : Look for blocking errors (like missing token, unknown model...)
        return errormessage.contains("Invalid Authentication")
                || errormessage.contains("Incorrect API key provided");
    }
}