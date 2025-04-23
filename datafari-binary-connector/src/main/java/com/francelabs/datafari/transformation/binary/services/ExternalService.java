package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  This class should be inherited by all ExternalService subclasses
 */
public abstract class ExternalService {

    private static final Logger LOGGER = LogManager.getLogger(ExternalService.class.getName());
    BinarySpecification spec;

    protected ExternalService(BinarySpecification spec) {

        // Add here common constructor code for all ExternalServices

        this.spec = spec;
    }
}