package com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.chat.ChatModel;
import java.time.Duration;

import java.util.Map;

public class HuggingFaceChatModelBuilder implements ChatModelBuilder {

    @Override
    public ChatModel build(Map<String, Object> props) {
        return HuggingFaceChatModel.builder()
                .accessToken(getString(props, "accessToken", null))
                .modelId(getString(props, "modelId", null))
                .temperature(getDouble(props, "temperature", 0))
                .maxNewTokens(getInt(props, "maxTokens", 200))
                .timeout(Duration.ofSeconds(getInt(props, "timeout", 60)))
                .build();
    }

    @Override
    public StreamingChatModel buildSCM(Map<String, Object> props) {
        return null;
    }
}