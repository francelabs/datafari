package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class OpenAiChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
        return OpenAiChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 500))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .returnThinking(false)
                .build();
    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return OpenAiStreamingChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 500))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .returnThinking(false)
                .build();
    }
}
