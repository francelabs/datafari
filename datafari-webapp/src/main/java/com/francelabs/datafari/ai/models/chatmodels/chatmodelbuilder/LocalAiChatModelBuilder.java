package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class LocalAiChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
        return LocalAiChatModel.builder()
                .baseUrl(getString(props, "baseUrl", "http://localhost:8082/v1"))
                .modelName(getString(props, "modelName", null))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .maxTokens(getInt(props, "maxTokens", 800))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();

    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return LocalAiStreamingChatModel.builder()
                .baseUrl(getString(props, "baseUrl", "http://localhost:8082/v1"))
                .modelName(getString(props, "modelName", null))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxTokens(getInt(props, "maxTokens", 800))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();
    }
}