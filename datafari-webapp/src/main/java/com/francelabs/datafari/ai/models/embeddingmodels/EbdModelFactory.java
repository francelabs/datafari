package com.francelabs.datafari.ai.models.embeddingmodels;

import com.francelabs.datafari.ai.models.chatmodels.ChatModelConfigurationManager;
import com.francelabs.datafari.ai.models.chatmodels.chatmodelbuilder.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory responsible for creating {@link EbdModelConfig} instances based on
 * model configurations defined in {@link ChatModelConfigurationManager}.
 * <p>
 * The factory uses a registry of {@link ChatModelBuilder} implementations
 * to dynamically build models for different interface types (e.g. OpenAI, MistralAI, AzureOpenAI, HuggingFace, etc.).
 * </p>
 */
public class EbdModelFactory {

    private final EbdModelConfigurationManager configManager;

    /**
     * Creates a new {@code ChatModelFactory} using the provided configuration manager.
     *
     * @param configManager The manager responsible for loading LLM model configurations.
     */
    public EbdModelFactory(EbdModelConfigurationManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Returns the active embedding model configuration defined in
     * {@link EbdModelConfigurationManager}.
     *
     * @return A {@link EbdModelConfig} instance corresponding to the active model.
     * @throws IllegalStateException    If no active model configuration is available.
     * @throws IllegalArgumentException If no builder is registered for the model's interface type.
     */
    public EbdModelConfig getEbdModel() {
        // TODO : call in vector search search
        return getEbdModel(null);
    }


    /**
     * Returns a {@link EbdModelConfig} using the configuration for the given model name.
     * If {@code modelName} is {@code null}, the active model is used.
     *
     * @param modelName The name of the model to use, or {@code null} to use the active model.
     * @return A {@link EbdModelConfig} instance configured accordingly.
     * @throws IllegalStateException    If no model configuration is found.
     * @throws IllegalArgumentException If no builder is registered for the model's interface type.
     */
    public EbdModelConfig getEbdModel(String modelName) {

        return (modelName != null)
                ? configManager.getModelByName(modelName)
                : configManager.getActiveModelConfig();
    }
}