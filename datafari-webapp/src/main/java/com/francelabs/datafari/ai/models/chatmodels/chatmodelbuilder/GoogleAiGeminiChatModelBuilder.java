package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class GoogleAiGeminiChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
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

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(getString(props, "apiKey", null))
                .modelName(getString(props, "modelName", "gemini-pro"))
                .temperature(getDouble(props, "temperature", 0))
                .topP(getDouble(props, "topP", 1.0))
                .topK(getInt(props, "topK", 40))
                .maxOutputTokens(getInt(props, "maxOutputTokens", 200))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .logRequestsAndResponses(getBoolean(props, "logRequestsAndResponses", false))
                .build();
    }
}