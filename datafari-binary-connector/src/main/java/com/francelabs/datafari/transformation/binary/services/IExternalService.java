package com.francelabs.datafari.transformation.binary.services;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;

/**
 * ExternalService classes are created to interact with an External API.
 * The should extend ExternalService.java, and implement IExternalService.
 */
public interface IExternalService {
    /**
     *
     * @param content The document content
     * @return The string response
     */
    //String invoke(String content) throws ManifoldCFException;

}
