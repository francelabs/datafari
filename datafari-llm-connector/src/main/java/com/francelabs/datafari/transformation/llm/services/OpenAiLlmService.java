package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public String invoke(String prompt) throws IOException {

        ChatLanguageModel llm = OpenAiChatModel.builder()
                .apiKey(spec.getApiKey())
                .temperature(temperature)
                .maxTokens(spec.getMaxTokens())
                .modelName(spec.getLlm())
                .baseUrl(spec.getLlmEndpoint())
                .maxRetries(3)
                .timeout(Duration.ofSeconds(1200))
                .build();
        String response;

        // TODO : Delay between attempts. See https://github.com/langchain4j/langchain4j/issues/814

        try {
            response = llm.generate(prompt);
        } catch (OpenAiHttpException exception) {
            LOGGER.error(exception);
            LOGGER.error("EBE - REQUEST FAILED");
            LOGGER.warn("Waiting 2 seconds before trying again.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("EBE - COULDN'T SLEEP.");
                throw new RuntimeException(e);
            }
            LOGGER.error("EBE - ATTEMPTING AGAIN");
            return llm.generate(prompt);
        }
        return response;
    }
}