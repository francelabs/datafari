package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;
import java.util.Map;

public class OpenAiChatLanguageModelBuilder implements ChatLanguageModelBuilder {

    @Override
    public ChatLanguageModel build(Map<String, Object> props) {
        return OpenAiChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .temperature(getDouble(props, "temperature", 0))
                .maxTokens(getInt(props, "maxTokens", 200))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();
    }
}
