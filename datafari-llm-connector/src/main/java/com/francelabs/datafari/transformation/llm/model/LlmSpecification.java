package com.francelabs.datafari.transformation.llm.model;

import com.francelabs.datafari.transformation.llm.LlmConfig;
import org.apache.manifoldcf.core.interfaces.ConfigNode;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class contains specifications for the LLM Connector. Each object matches a line in the LLM specifications tab.
 */
public class LlmSpecification {

    String summariesLanguage = "en";
    boolean enableSummarize = false;
    boolean enableCategorize = false;
    String llmEndpoint = "";
    String apiKey = "";
    String llm = "";
    int maxTokens = 500;
    int maxChunkSizeInTokens = 10000;
    int maxChunkSizeInChar = 12000;
    int maxIterations = 0; // The max number of chunks processed by document
    String typeOfLlm = "";
    List<String> categories = new ArrayList<>();

    public LlmSpecification() {
        // Empty connector
    }

    public LlmSpecification(Specification os, ConfigParams config) {

        for (int i = 0; i < os.getChildCount(); i++) {
            final SpecificationNode sn = os.getChild(i);

            if (sn.getType().equals(LlmConfig.NODE_ENABLE_SUMMARIZE)) {
                this.enableSummarize = "true".equals(sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE));
            } else if (sn.getType().equals(LlmConfig.NODE_ENABLE_CATEGORIZE)) {
                this.enableCategorize = "true".equals(sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE));
            } else if (sn.getType().equals(LlmConfig.NODE_MAXTOKENS)) {
                this.maxTokens = Integer.parseInt(sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE));
            } else if (sn.getType().equals(LlmConfig.NODE_SUMMARIES_LANGUAGE)) {
                this.summariesLanguage = sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE);
            } else if (sn.getType().equals(LlmConfig.NODE_CATEGORIES)) {
                this.categories.add(sn.getAttributeValue(LlmConfig.ATTRIBUTE_VALUE));
            }
        }
        for (int i = 0; i < config.getChildCount(); i++) {
            final ConfigNode cn = config.getChild(i);

            if (cn.getAttributeValue("name").equals(LlmConfig.NODE_ENDPOINT)) {
                this.llmEndpoint = cn.getValue();
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_APIKEY)) {
                this.apiKey = cn.getValue();
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_LLM)) {
                this.llm = cn.getValue();
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_LLM_SERVICE)) {
                this.typeOfLlm = cn.getValue();
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_MAX_CHUNK_SIZE_IN_CHAR)) {
                this.maxChunkSizeInChar = Integer.parseInt(cn.getValue());
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_MAX_CHUNK_SIZE_IN_TOKENS)) {
                this.maxChunkSizeInTokens = Integer.parseInt(cn.getValue());
            } else if (cn.getAttributeValue("name").equals(LlmConfig.NODE_MAX_ITERATIONS)) {
                this.maxIterations = Integer.parseInt(cn.getValue());
            }
        }
    }

    public boolean getEnableSummarize() {
        return enableSummarize;
    }

    public void setEnableSummarize(Boolean enableSummarize) {
        this.enableSummarize = enableSummarize;
    }

    public boolean getEnableCategorize() {
        return enableCategorize;
    }

    public void setEnableCategorize(Boolean enableCategorize) {
        this.enableCategorize = enableCategorize;
    }

    public String getLlmEndpoint() {
        return llmEndpoint;
    }

    public void setLlmEndpoint(String llmEndpoint) {
        this.llmEndpoint = llmEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSummariesLanguage() {
        return summariesLanguage;
    }

    public void setSummariesLanguage(String summariesLanguage) {
        this.summariesLanguage = summariesLanguage;
    }

    public String getLlm() {
        return llm;
    }

    public void setLlm(String llm) {
        this.llm = llm;
    }

    public String getTypeOfLlm() {
        return typeOfLlm;
    }

    public void setTypeOfLlm(String typeOfLlm) {
        this.typeOfLlm = typeOfLlm;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMaxChunkSizeInChar() {
        return maxChunkSizeInChar;
    }

    public void setMaxChunkSizeInChar(int maxChunkSizeInChar) {
        this.maxChunkSizeInChar = maxChunkSizeInChar;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxChunkSizeInTokens() {
        return maxChunkSizeInTokens;
    }

    public void setMaxChunkSizeInTokens(int maxChunkSizeInTokens) {
        this.maxChunkSizeInTokens = maxChunkSizeInTokens;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmSpecification that = (LlmSpecification) o;
        return maxTokens == that.maxTokens && Objects.equals(summariesLanguage, that.summariesLanguage) && Objects.equals(enableSummarize, that.enableSummarize)
                && Objects.equals(enableCategorize, that.enableCategorize) && Objects.equals(llmEndpoint, that.llmEndpoint) && Objects.equals(apiKey, that.apiKey)
                && Objects.equals(llm, that.llm) && Objects.equals(typeOfLlm, that.typeOfLlm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summariesLanguage, enableSummarize, enableCategorize, llmEndpoint, apiKey, llm, maxTokens, typeOfLlm);
    }
}
