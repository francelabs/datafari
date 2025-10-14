package com.francelabs.datafari.ai.models.embeddingmodels;

import java.util.List;

public class EbdModelRegistry {

    private List<EbdModelConfig> models;
    private String activeModel;

    public List<EbdModelConfig> getModels() {
        return models;
    }

    public void setModels(List<EbdModelConfig> models) {
        this.models = models;
    }

    public String getActiveModel() {
        return activeModel;
    }

    public void setActiveModel(String activeModel) {
        this.activeModel = activeModel;
    }
}
