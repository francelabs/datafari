package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Duration;
import java.util.Map;

public class GoogleAiGeminiChatLanguageModelBuilder implements ChatLanguageModelBuilder {

    @Override
    public ChatLanguageModel build(Map<String, Object> props) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(getString(props, "apiKey", null))
                .modelName(getString(props, "modelName", "gemini-pro"))
                .temperature(getDouble(props, "temperature", 0))
                .topP(getDouble(props, "topP", 1.0))
                .topK(getInt(props, "topK", 40))
                .maxOutputTokens(getInt(props, "maxOutputTokens", 200))
                .maxRetries(getInt(props, "maxRetries", 1))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .logRequestsAndResponses(getBoolean(props, "logRequestsAndResponses", false))
                .build();
    }
}