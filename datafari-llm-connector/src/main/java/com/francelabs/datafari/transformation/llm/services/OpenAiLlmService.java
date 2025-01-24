package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.PromptUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class OpenAiLlmService implements LlmService {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmService.class.getName());
    LlmSpecification spec;

    double temperature;
    int maxToken;
    int dimensions = 124;
    static final String DEFAULT_LLM_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
    static final int DEFAULT_DIMENSION = 250;
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    public OpenAiLlmService(LlmSpecification spec) {
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
     * Call the Datafari External LLM Webservice
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
                .build();

        return llm.generate(prompt);
    }

    @Override
    public float[] embeddings(String content) throws IOException {

        EmbeddingModel llm = OpenAiEmbeddingModel.builder()
                .apiKey(spec.getApiKey())
                .modelName(spec.getEmbeddingsModel())
                .baseUrl(spec.getLlmEndpoint())
                .dimensions(spec.getVectorDimension())
                .build();

        Response<Embedding> embedding = llm.embed(content);
        LOGGER.info("Vector embedding : {}", embedding);
        return embedding.content().vector();
    }

    @Override
    public String summarize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForSummarization(content, spec);
        return invoke(prompt);
    }

    @Override
    public String categorize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForCategorization(content, spec);
        return invoke(prompt);
    }
}