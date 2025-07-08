package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.time.Duration;
import java.util.Map;

public class OllamaChatLanguageModelBuilder implements ChatLanguageModelBuilder {

    @Override
    public ChatLanguageModel build(Map<String, Object> props) {
        return OllamaChatModel.builder()
                .baseUrl(getString(props, "baseUrl", "http://localhost:11434"))
                .modelName(getString(props, "modelName", null))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .build();
    }
}