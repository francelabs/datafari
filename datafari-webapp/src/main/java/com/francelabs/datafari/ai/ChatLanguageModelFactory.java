package com.francelabs.datafari.ai;

import com.francelabs.datafari.ai.models.*;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory responsible for creating {@link ChatLanguageModel} instances based on
 * model configurations defined in {@link LLMModelConfigurationManager}.
 * <p>
 * The factory uses a registry of {@link ChatLanguageModelBuilder} implementations
 * to dynamically build models for different interface types (e.g. OpenAI, AzureOpenAI, HuggingFace, etc.).
 * </p>
 *
 * <p>
 * New interface types can be added at runtime via the {@link #register(String, ChatLanguageModelBuilder)} method,
 * making the factory extensible without recompilation.
 * </p>
 */
public class ChatLanguageModelFactory {

    private final LLMModelConfigurationManager configManager;
    private final Map<String, ChatLanguageModelBuilder> builderRegistry = new HashMap<>();

    /**
     * Creates a new {@code ChatLanguageModelFactory} using the provided configuration manager.
     *
     * @param configManager The manager responsible for loading LLM model configurations.
     */
    public ChatLanguageModelFactory(LLMModelConfigurationManager configManager) {
        this.configManager = configManager;
        registerDefaults();
    }

    /**
     * Registers default {@link ChatLanguageModelBuilder} implementations for known interface types:
     * OpenAI, AzureOpenAI, HuggingFace, Ollama, GoogleAiGemini.
     */
    private void registerDefaults() {
        builderRegistry.put("OpenAI", new OpenAiChatLanguageModelBuilder());
        builderRegistry.put("AzureOpenAI", new AzureOpenAiChatLanguageModelBuilder());
        builderRegistry.put("HuggingFace", new HuggingFaceChatLanguageModelBuilder());
        builderRegistry.put("Ollama", new OllamaChatLanguageModelBuilder());
        builderRegistry.put("GoogleAiGemini", new GoogleAiGeminiChatLanguageModelBuilder());
    }

    /**
     * Registers a new {@link ChatLanguageModelBuilder} for a given interface type.
     * This allows the factory to support custom or third-party model types dynamically.
     *
     * @param interfaceType The name of the interface type (e.g. "MyCustomLLM").
     * @param builder       The builder responsible for constructing models of this type.
     */
    public void register(String interfaceType, ChatLanguageModelBuilder builder) {
        builderRegistry.put(interfaceType, builder);
    }

    /**
     * Creates a {@link ChatLanguageModel} using the active model configuration defined in
     * {@link LLMModelConfigurationManager}.
     *
     * @return A {@link ChatLanguageModel} instance corresponding to the active model.
     * @throws IllegalStateException    If no active model configuration is available.
     * @throws IllegalArgumentException If no builder is registered for the model's interface type.
     */
    public ChatLanguageModel createChatModel() {
        // TODO : Error management
        return createChatModel(null);
    }


    /**
     * Creates a {@link ChatLanguageModel} using the configuration for the given model name.
     * If {@code modelName} is {@code null}, the active model is used.
     *
     * @param modelName The name of the model to use, or {@code null} to use the active model.
     * @return A {@link ChatLanguageModel} instance configured accordingly.
     * @throws IllegalStateException    If no model configuration is found.
     * @throws IllegalArgumentException If no builder is registered for the model's interface type.
     */
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

        return builder.build(config.getParams());
    }
}