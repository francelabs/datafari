package com.francelabs.datafari.transformation.llm.services;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This LlmService class is an extension of the OpenAI LLM Service.
 * It is optimized for Datafari AI Agent.
 */
public class DatafariAiAgentLlmService extends OpenAiLlmService implements ILlmService {

    private static final Logger LOGGER = LogManager.getLogger(DatafariAiAgentLlmService.class.getName());

    static final String DEFAULT_LLM_MODEL = "";

    public DatafariAiAgentLlmService(LlmSpecification spec) {
        super(spec);

        // API key in not always required for Datafari AI Agent
        // However, it must not be empty for the OpenAiChatModel
        if (spec.getApiKey().isEmpty()) spec.setApiKey("xxx");

        // Remove the default models inherited from OpenAiLlmService
        if (DatafariAiAgentLlmService.DEFAULT_LLM_MODEL.equals(spec.getLlm()) ) spec.setLlm(DEFAULT_LLM_MODEL);
    }
}