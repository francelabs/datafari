package com.francelabs.datafari.ai.models.chatmodels;

import java.util.List;

public class ChatModelRegistry {
    private String activeModel;

    public List<ChatModelConfig> getModels() {
        return models;
    }

    public void setModels(List<ChatModelConfig> models) {
        this.models = models;
    }

    public String getActiveModel() {
        return activeModel;
    }

    public void setActiveModel(String activeModel) {
        this.activeModel = activeModel;
    }

    private List<ChatModelConfig> models;
}
