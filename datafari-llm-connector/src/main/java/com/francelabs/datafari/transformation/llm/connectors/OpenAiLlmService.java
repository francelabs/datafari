package com.francelabs.datafari.transformation.llm.connectors;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.PromptUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class OpenAiLlmService implements LlmService {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmService.class.getName());
    LlmSpecification spec;
    String url;
    String model;
    String embeddingsModel;
    String apiKey;
    double temperature;
    int maxToken;
    static final String DEFAULT_LLM_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    public OpenAiLlmService(LlmSpecification spec) {
        try {
            this.temperature = 0;
            this.maxToken = spec.getMaxTokens();
        } catch (NumberFormatException e) {
            this.temperature = 0.0;
            this.maxToken = 200;
        }
        this.model = spec.getLlm().isEmpty() ? DEFAULT_LLM_MODEL : spec.getLlm();
        this.embeddingsModel = spec.getEmbeddingsModel().isEmpty() ? DEFAULT_EMBEDDINGS_MODEL : spec.getEmbeddingsModel();
        this.url = spec.getLlmEndpoint().isEmpty() ? DEFAULT_URL : spec.getLlmEndpoint();
        this.apiKey = spec.getApiKey();
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
                .apiKey(apiKey)
                .temperature(temperature)
                .maxTokens(maxToken)
                .modelName(model)
                .baseUrl(url)
                .build();

        return llm.generate(prompt);
    }

    @Override
    public float[] embeddings(String content) throws IOException {

        // Embedding the document
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Response<Embedding> embedding = embeddingModel.embed(content);

        LOGGER.info("Vector embedding : {}", embedding);
        return embedding.content().vector();
    }

    @Override
    public String summarize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForSummarization(content, spec);
        return invoke(prompt);
    }

    @Override
    public String categorize(String content) throws IOException {
        String prompt = PromptUtils.promptForCategorization(content);
        return invoke(prompt);
    }
}