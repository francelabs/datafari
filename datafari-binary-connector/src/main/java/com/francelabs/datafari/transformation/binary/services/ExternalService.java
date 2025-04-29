package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

/**
 *  This class should be inherited by all ExternalService subclasses
 */
public abstract class ExternalService {

    private static final Logger LOGGER = LogManager.getLogger(ExternalService.class.getName());
    BinarySpecification spec;

    URI url;

    protected ExternalService(BinarySpecification spec) {

        // Add here common constructor code for all ExternalServices
        String hostname = spec.getStringProperty(BinaryConfig.NODE_SERVICE_HOSTNAME);
        String endpoint = spec.getStringProperty(BinaryConfig.NODE_SERVICE_HOSTNAME);
        if (hostname != null && endpoint != null
                && !hostname.isBlank() && !endpoint.isBlank()) {
            this.url = URI.create(hostname + endpoint);
        }

        this.spec = spec;
    }
}