package com.francelabs.datafari.rag;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OpenAiLlmConnector implements LlmConnector {

    String url;
    double temperature;
    int maxToken;
    String model = "";
    String apiKey;
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
    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String invoke(List<String> prompts, RagConfiguration config, String userQuery) throws IOException {

        ChatLanguageModel llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(temperature)
                .maxTokens(maxToken)
                .modelName(model)
                .build();

        List<ChatMessage> chatMessages = generateChatMessagesList(prompts);
        AiMessage response = llm.generate(chatMessages).content();
        return response.text();
    }


    /**
     * Transform String prompts into a list a ChatMessages
     * @param prompts A String prompts
     * @return a list a ChatMessages
     */
    public List<ChatMessage> generateChatMessagesList(List<String> prompts){
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (String prompt : prompts) {
            ChatMessage message = UserMessage.from(prompt);
            chatMessages.add(message);
        }
        return chatMessages;
    }
}
