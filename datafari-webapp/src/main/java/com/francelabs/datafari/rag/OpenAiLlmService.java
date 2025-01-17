package com.francelabs.datafari.rag;

import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenAiLlmService implements LlmService {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmService.class.getName());
    RagConfiguration config;
    String url;
    String model;
    String apiKey;
    double temperature;
    int maxToken;
    static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    static final String DEFAULT_URL = "https://api.openai.com/v1/";

    public OpenAiLlmService(RagConfiguration config) {
        this.url = config.getProperty(RagConfiguration.API_ENDPOINT);
        try {
            this.temperature = Double.parseDouble(config.getProperty(RagConfiguration.LLM_TEMPERATURE));
            this.maxToken = config.getIntegerProperty(RagConfiguration.LLM_MAX_TOKENS);
        } catch (NumberFormatException e) {
            this.temperature = 0.0;
            this.maxToken = 200;
        }
        this.model = config.getProperty(RagConfiguration.LLM_MODEL).isEmpty() ? DEFAULT_MODEL : config.getProperty(RagConfiguration.LLM_MODEL);
        this.url = config.getProperty(RagConfiguration.API_ENDPOINT).isEmpty() ? DEFAULT_URL : config.getProperty(RagConfiguration.API_ENDPOINT);
        this.apiKey = config.getProperty(RagConfiguration.API_TOKEN);
        this.config = config;
    }


    /**
     * Sends a list of Messages to a LLM.
     * @param prompts The list of Messages
     * @return A single string response
     */
    @Override
    public String generate(List<Message> prompts, HttpServletRequest request) throws IOException {
        ChatLanguageModel llm = getChatLanguageModel();
        return llm.generate(convertMessageList(prompts)).content().text();
    }

    /**
     * Sends a single Message to a LLM.
     * @param prompt The Message
     * @return A single string response
     */
    public String generate(Message prompt) {
        ChatLanguageModel llm = getChatLanguageModel();
        return llm.generate(convertMessage(prompt)).content().text();
    }

    private ChatLanguageModel getChatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(temperature)
                .maxTokens(maxToken)
                .modelName(model)
                .baseUrl(url)
                .build();
    }

    private ChatMessage convertMessage(Message message) {
        switch (message.getRole()) {
            case "assistant":
                return new AiMessage(message.content);
            case "system":
                return new SystemMessage(message.content);
            case "user":
            default:
                return new UserMessage(message.content);
        }
    }

    private List<ChatMessage> convertMessageList(List<Message> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Message message: messages) {
            chatMessages.add(convertMessage(message));
        }
        return chatMessages;
    }
}
