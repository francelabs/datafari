package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class MistralAiChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
        return MistralAiChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 800))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();
    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return MistralAiStreamingChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 800))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();
    }
}
