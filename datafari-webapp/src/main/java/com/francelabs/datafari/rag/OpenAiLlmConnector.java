package com.francelabs.datafari.rag;

import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class OpenAiLlmConnector implements LlmConnector {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmConnector.class.getName());
    RagConfiguration config;
    String url;
    String model;
    String apiKey;
    double temperature;
    int maxToken;
    static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    public OpenAiLlmConnector(RagConfiguration config) {
        this.url = config.getEndpoint();
        try {
            this.temperature = Double.parseDouble(config.getTemperature());
            this.maxToken = Integer.parseInt(config.getMaxTokens());
        } catch (NumberFormatException e) {
            this.temperature = 0.0;
            this.maxToken = 200;
        }
        this.model = config.getModel().isEmpty() ? DEFAULT_MODEL : config.getModel();
        this.apiKey = config.getToken();
        this.config = config;
    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String invoke(List<String> prompts, HttpServletRequest request) throws IOException {

        ChatLanguageModel llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(temperature)
                .maxTokens(maxToken)
                .modelName(model)
                .build();

        StringBuilder concatenatedResponses = new StringBuilder();
        String message;

        // The first calls returns a concatenated responses from each chunk
        for (String prompt : prompts) {
            String response = llm.generate(prompt);
            concatenatedResponses.append(response);
        }

        // If the is only one prompt to send, we get the answer from the response
        // Otherwise, we concatenate all the responses, and generate a new response to summarize the results
        if (prompts.size() == 1) {
            message = concatenatedResponses.toString();
        } else if (prompts.size() > 1) {
            String body = PromptUtils.createPrompt(config, "```" + concatenatedResponses + "```", request);
            message = llm.generate(body);
        } else {
            throw new RuntimeException("Could not find data to send to the LLM");
        }

        return message;
    }
}