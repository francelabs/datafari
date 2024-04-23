package com.francelabs.datafari.rag;

public class RagConfiguration {
    String token;
    String endpoint;
    String temperature;
    String maxTokens;
    String model;
    boolean addInstructions;
    String template;

    // The field that contains the corpus to send to the webservice. This value can be : "highlighting", "exact_content", "preview_content".
    String solrField;

    // Only required when using "highlighting" as Solr field. Defines the size in characters of the document extract.
    String hlFragsize;

    public RagConfiguration() {
    }

    /*public RagConfiguration(String token, String endpoint, String temperature, String maxTokens, String model, boolean addInstructions, String template, String solrField) {
        this.token = token;
        this.endpoint = endpoint;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.model = model;
        this.addInstructions = addInstructions;
        this.template = template;
        this.solrField = solrField;
    }*/

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean getAddInstructions() {
        return addInstructions;
    }

    public void setAddInstructions(String addInstructions) {
        this.addInstructions = ("true".equals(addInstructions));
    }

    public void setAddInstructions(boolean addInstructions) {
        this.addInstructions = addInstructions;
    }

    public String getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(String maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSolrField() {
        return solrField;
    }

    public void setSolrField(String solrField) {
        this.solrField = solrField;
    }

    public String getHlFragsize() {
        return hlFragsize;
    }

    public void setHlFragsize(String hlFragsize) {
        this.hlFragsize = hlFragsize;
    }
}