package com.francelabs.datafari.ai;

import java.util.List;

public class LLMModelRegistry {
    private String activeModel;

    public List<LLMModelConfig> getModels() {
        return models;
    }

    public void setModels(List<LLMModelConfig> models) {
        this.models = models;
    }

    public String getActiveModel() {
        return activeModel;
    }

    public void setActiveModel(String activeModel) {
        this.activeModel = activeModel;
    }

    private List<LLMModelConfig> models;
}
