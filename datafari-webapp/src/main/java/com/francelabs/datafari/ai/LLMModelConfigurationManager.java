package com.francelabs.datafari.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the loading, saving, and access to LLM model configurations defined in
 * the {@code models.json} file.
 * <p>
 * Provides utility methods to retrieve the active model, query specific models by name,
 * and modify the list of available configurations.
 * </p>
 */
public class LLMModelConfigurationManager {

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("catalina.base"), "conf", "models.json");
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private LLMModelRegistry registry;

    /**
     * Constructs a new configuration manager and immediately loads the model configurations
     * from the default JSON file ({@code conf/models.json}).
     *
     * @throws IOException if the file cannot be read or parsed.
     */
    public LLMModelConfigurationManager() throws IOException {
        load();
    }

    /**
     * Loads the model configurations from the {@code models.json} file.
     * If the file does not exist, initializes an empty registry.
     *
     * @throws IOException if the file exists but cannot be read or parsed.
     */
    public void load() throws IOException {
        if (Files.exists(CONFIG_PATH)) {
            this.registry = mapper.readValue(CONFIG_PATH.toFile(), LLMModelRegistry.class);
        } else {
            this.registry = new LLMModelRegistry();
            this.registry.setModels(new ArrayList<>());
        }
    }

    /**
     * Persists the current model registry to the {@code models.json} file.
     *
     * @throws IOException if the file cannot be written.
     */
    public void save() throws IOException {
        mapper.writeValue(CONFIG_PATH.toFile(), registry);
    }

    /**
     * Returns the configuration of the active model, as defined in the registry.
     *
     * @return The {@link LLMModelConfig} of the active model, or {@code null} if none is set.
     */
    public LLMModelConfig getActiveModelConfig() {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(registry.getActiveModel()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the active model by name and saves the updated registry.
     *
     * @param modelName The name of the model to mark as active.
     * @throws IOException if the updated registry cannot be saved.
     */
    public void setActiveModel(String modelName) throws IOException {
        registry.setActiveModel(modelName);
        save();
    }

    /**
     * Adds or updates a model configuration in the registry and persists the change.
     * If a model with the same name already exists, it will be replaced.
     *
     * @param config The new or updated model configuration.
     * @throws IOException if the registry cannot be saved.
     */
    public void addOrUpdateModel(LLMModelConfig config) throws IOException {
        registry.getModels().removeIf(m -> m.getName().equals(config.getName()));
        registry.getModels().add(config);
        save();
    }

    /**
     * Removes a model configuration by name.
     * If the removed model was the active model, the active model will be unset.
     *
     * @param modelName The name of the model to remove.
     * @throws IOException if the updated registry cannot be saved.
     */
    public void removeModel(String modelName) throws IOException {
        registry.getModels().removeIf(m -> m.getName().equals(modelName));
        if (modelName.equals(registry.getActiveModel())) {
            registry.setActiveModel(null);
        }
        save();
    }

    /**
     * Returns the list of all registered model configurations.
     *
     * @return A list of {@link LLMModelConfig} instances.
     */
    public List<LLMModelConfig> listModels() {
        return registry.getModels();
    }

    /**
     * Retrieves a model configuration by its name.
     *
     * @param name The name of the model to retrieve.
     * @return The corresponding {@link LLMModelConfig}, or {@code null} if not found.
     */
    public LLMModelConfig getModelByName(String name) {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
