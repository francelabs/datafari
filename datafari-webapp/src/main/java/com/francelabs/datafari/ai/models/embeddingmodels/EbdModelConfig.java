package com.francelabs.datafari.ai.models.embeddingmodels;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class EbdModelConfig {

    @JsonProperty
    private String name;
    @JsonProperty("class")
    @JsonAlias({"class", "interfaceType"})
    private String interfaceType;
    @JsonProperty
    private String vectorField;
    @JsonProperty
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVectorField() {
        return vectorField;
    }

    public void setVectorField(String vectorField) {
        this.vectorField = vectorField;
    }
}
