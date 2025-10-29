package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class AzureOpenAiChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
        return AzureOpenAiChatModel.builder()
                .apiKey(getString(props, "apiKey", null))
                .endpoint(getString(props, "endpoint", null))
                .deploymentName(getString(props, "deploymentName", null))
                .serviceVersion(getString(props, "serviceVersion", "2023-05-15"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 200))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequestsAndResponses(getBoolean(props, "logRequestsAndResponses", false))
                .build();
    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(getString(props, "apiKey", null))
                .endpoint(getString(props, "endpoint", null))
                .deploymentName(getString(props, "deploymentName", null))
                .serviceVersion(getString(props, "serviceVersion", "2023-05-15"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 200))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequestsAndResponses(getBoolean(props, "logRequestsAndResponses", false))
                .build();
    }
}