package com.francelabs.datafari.ai.models.chatmodelbuilder;

import com.francelabs.datafari.ai.models.StreamingChatModelBuilder;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;
import java.util.Map;

public class OpenAiStreamingChatModelBuilder implements StreamingChatModelBuilder {

    @Override
    public StreamingChatModel build(Map<String, Object> props) {
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
