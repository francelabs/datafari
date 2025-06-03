package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This ExternalService class is an extension of the OpenAI Binary Service.
 * It is optimized for Datafari AI Agent.
 */
public class DatafariAiAgentExternalService extends OpenAiExternalService implements IExternalService {

    private static final Logger LOGGER = LogManager.getLogger(DatafariAiAgentExternalService.class.getName());


    public DatafariAiAgentExternalService(BinarySpecification spec) {
        super(spec);
    }
}