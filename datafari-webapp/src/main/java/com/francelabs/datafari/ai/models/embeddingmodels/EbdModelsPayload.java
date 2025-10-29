package com.francelabs.datafari.ai.models.embeddingmodels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EbdModelsPayload {
    private String activeModel;
    private List<EbdModelConfig> models;

    public String getActiveModel() { return activeModel; }
    public void setActiveModel(String activeModel) { this.activeModel = activeModel; }
    public List<EbdModelConfig> getModels() { return models; }
    public void setModels(List<EbdModelConfig> models) { this.models = models; }
}