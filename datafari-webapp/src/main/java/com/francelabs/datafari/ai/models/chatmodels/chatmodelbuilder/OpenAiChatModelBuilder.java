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
        String modelName = (String) props.get("modelName");
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName(modelName)
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .maxRetries(getInt(props, "maxRetries", 3))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .returnThinking(false);

        // gpt-5 does not support customising topP and temperature (because it is a reasoning model)
        if (!modelName.toLowerCase().startsWith("gpt-5")) {
            builder.maxTokens(getInt(props, "maxTokens", 800))
                .temperature(getDouble(props, "temperature", 0));
        } else {
            // if GPT-5
            builder.maxCompletionTokens(getInt(props, "maxTokens", 800));
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        String modelName = (String) props.get("modelName");
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey((String) props.get("apiKey"))
                .baseUrl((String) props.get("baseUrl"))
                .modelName((String) props.get("modelName"))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .logRequests(getBoolean(props, "logRequests", false))
                .logResponses(getBoolean(props, "logResponses", false))
                .returnThinking(false);

        // gpt-5 does not support customising topP and temperature (because it is a reasoning model)
        if (!modelName.toLowerCase().startsWith("gpt-5")) {
            builder.maxTokens(getInt(props, "maxTokens", 800))
                .temperature(getDouble(props, "temperature", 0));
        } else {
            // if GPT-5
            builder.maxCompletionTokens(getInt(props, "maxTokens", 500));
        }
        return builder.build();
    }
}
