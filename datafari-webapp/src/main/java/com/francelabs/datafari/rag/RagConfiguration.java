package com.francelabs.datafari.rag;

import com.francelabs.datafari.api.RagAPI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class RagConfiguration {
    boolean enabled = false;
    String token;
    String endpoint;
    String temperature;
    String maxTokens;
    int maxFiles;
    String model;
    boolean addInstructions;
    String template;
    String format;
    int maxJsonLength;

    int chunkSize;

    String operator = "AND";
    boolean logsEnabled;

    // The field that contains the corpus to send to the webservice. This value can be : "highlighting", "exact_content", "preview_content".
    String solrField;

    // Only required when using "highlighting" as Solr field. Defines the size in characters of the document extract.
    String hlFragsize;

    public RagConfiguration() throws FileNotFoundException {
        Properties prop = new Properties();
        String fileName = "rag.properties";
        try (InputStream fis = RagAPI.class.getClassLoader().getResourceAsStream(fileName)) {
            prop.load(fis);

            this.setEnabled(prop.getProperty("rag.enabled"));
            this.setToken(prop.getProperty("rag.api.token"));
            this.setEndpoint(prop.getProperty("rag.api.endpoint"));
            this.setModel(prop.getProperty("rag.model"));
            this.setTemperature(prop.getProperty("rag.temperature"));
            this.setMaxTokens(prop.getProperty("rag.maxTokens"));
            this.setMaxFiles(prop.getProperty("rag.maxFiles"));
            this.setAddInstructions(prop.getProperty("rag.addInstructions"));
            this.setTemplate(prop.getProperty("rag.template"));
            this.setSolrField(prop.getProperty("rag.solrField"));
            this.setHlFragsize(prop.getProperty("rag.hl.fragsize"));
            this.setLogsEnabled(prop.getProperty("rag.enable.logs"));
            this.setOperator(prop.getProperty("rag.operator"));
            this.setMaxJsonLength(Integer.parseInt(prop.getProperty("rag.maxJsonLength")));
            this.setChunkSize(Integer.parseInt(prop.getProperty("rag.chunk.size")));


        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("An error occurred during the configuration. Configuration file not found.");
        } catch (NumberFormatException e) {
            throw new NumberFormatException("An error occurred during the configuration. Invalid value for rag.maxTokens or rag.hl.fragsize or rag.maxFiles or rag.maxJsonLength. Integers expected.");
        } catch (IOException e) {
            throw new RuntimeException("An error occurred during the configuration.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = "true".equals(enabled);
    }

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

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public void setMaxFiles(String maxFiles) {
        this.maxFiles = Integer.parseInt(maxFiles);
    }

    public boolean isLogsEnabled() {
        return logsEnabled;
    }

    public void setLogsEnabled(boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    public void setLogsEnabled(String logsEnabled) {
        this.logsEnabled = ("true".equals(logsEnabled));
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = (List.of("OR", "AND").contains(operator)) ? operator : "AND";
    }

    public int getMaxJsonLength() {
        return maxJsonLength;
    }

    public void setMaxJsonLength(int maxJsonLength) {
        this.maxJsonLength = maxJsonLength;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}