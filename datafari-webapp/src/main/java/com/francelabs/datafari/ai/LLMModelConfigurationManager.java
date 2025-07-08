package com.francelabs.datafari.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LLMModelConfigurationManager {

    private static final Path CONFIG_PATH = Paths.get("models.json");
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private LLMModelRegistry registry;

    public LLMModelConfigurationManager() throws IOException {
        load();
    }

    public void load() throws IOException {
        if (Files.exists(CONFIG_PATH)) {
            this.registry = mapper.readValue(CONFIG_PATH.toFile(), LLMModelRegistry.class);
        } else {
            this.registry = new LLMModelRegistry();
            this.registry.setModels(new ArrayList<>());
        }
    }

    public void save() throws IOException {
        mapper.writeValue(CONFIG_PATH.toFile(), registry);
    }

    public LLMModelConfig getActiveModelConfig() {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(registry.getActiveModel()))
                .findFirst()
                .orElse(null);
    }

    public void setActiveModel(String modelName) throws IOException {
        registry.setActiveModel(modelName);
        save();
    }

    public void addOrUpdateModel(LLMModelConfig config) throws IOException {
        registry.getModels().removeIf(m -> m.getName().equals(config.getName()));
        registry.getModels().add(config);
        save();
    }

    public void removeModel(String modelName) throws IOException {
        registry.getModels().removeIf(m -> m.getName().equals(modelName));
        if (modelName.equals(registry.getActiveModel())) {
            registry.setActiveModel(null);
        }
        save();
    }

    public List<LLMModelConfig> listModels() {
        return registry.getModels();
    }

    public LLMModelConfig getModelByName(String name) {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
