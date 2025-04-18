package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import com.francelabs.datafari.transformation.binary.utils.PromptUtils;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import java.util.List;

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