package com.francelabs.datafari.ai;

import com.francelabs.datafari.ai.models.*;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.HashMap;
import java.util.Map;

public class ChatLanguageModelFactory {

    private final LLMModelConfigurationManager configManager;
    private final Map<String, ChatLanguageModelBuilder> builderRegistry = new HashMap<>();

    public ChatLanguageModelFactory(LLMModelConfigurationManager configManager) {
        this.configManager = configManager;
        registerDefaults();
    }

    private void registerDefaults() {
        builderRegistry.put("OpenAI", new OpenAiChatLanguageModelBuilder());
        builderRegistry.put("AzureOpenAI", new AzureOpenAiChatLanguageModelBuilder());
        builderRegistry.put("HuggingFace", new HuggingFaceChatLanguageModelBuilder());
        builderRegistry.put("Ollama", new OllamaChatLanguageModelBuilder());
        // Pas de MistralAI ici tant que tu es en 0.35.0
    }

    public void register(String interfaceType, ChatLanguageModelBuilder builder) {
        builderRegistry.put(interfaceType, builder);
    }

    public ChatLanguageModel createChatModel() {
        return createChatModel(null);
    }

    public ChatLanguageModel createChatModel(String modelName) {
        LLMModelConfig config = (modelName != null)
                ? configManager.getModelByName(modelName)
                : configManager.getActiveModelConfig();

        if (config == null) {
            throw new IllegalStateException("No model configuration found.");
        }

        String interfaceType = config.getInterfaceType();
        ChatLanguageModelBuilder builder = builderRegistry.get(interfaceType);
        if (builder == null) {
            throw new IllegalArgumentException("No builder found for interface: " + interfaceType);
        }

        return builder.build(config.getProperties());
    }
}