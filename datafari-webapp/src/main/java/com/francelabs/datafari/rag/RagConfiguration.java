package com.francelabs.datafari.rag;

import com.francelabs.datafari.config.AbstractConfigClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RagConfiguration extends AbstractConfigClass {


    private static final String configFilename = "rag.properties";
    private static RagConfiguration instance;
    private final static Logger LOGGER = LogManager.getLogger(RagConfiguration.class.getName());


    // GLOBAL RAG PROPERTIES
    public final static String ENABLE_RAG = "rag.enabled";
    public final static String ENABLE_LOGS = "rag.enable.logs";
    public final static String ENABLE_CHUNKING = "rag.enable.chunking";
    public final static String ENABLE_VECTOR_SEARCH = "rag.enable.vector.search";

    // WEB SERVICES PARAMETERS
    public final static String API_ENDPOINT = "rag.api.endpoint";
    public final static String API_TOKEN = "rag.api.token";
    public final static String LLM_SERVICE = "rag.llm.service";

    // LLM PARAMETERS
    public final static String LLM_TEMPERATURE = "rag.temperature";
    public final static String LLM_MAX_TOKENS = "rag.maxTokens";
    public final static String LLM_MODEL = "rag.model";

    // DATAFARI RAG PRE-PROCESSING PROPERTIES
    public final static String MAX_FILES = "rag.maxFiles";
    public final static String CHUNK_SIZE = "rag.chunk.size";
    public final static String SEARCH_OPERATOR = "rag.operator";
    public final static String SOLR_FIELD = "rag.solrField"; // Todo : to remove
    public final static String SOLR_HL_FRAGSIZE = "rag.hl.fragsize"; // Todo : to remove


    /**
     *
     * Get the instance
     *
     */
    public static synchronized RagConfiguration getInstance() {
        if (null == instance) {
            instance = new RagConfiguration();
        }
        return instance;
    }
    /**
     * Set a property and save it the alerts.properties
     *
     * @param key   : the key that should be change
     * @param value : the new value of the key
     * @return : true if there's an error and false if not
     */
    @Override
    public void setProperty(final String key, String value) {
        lock.writeLock().lock();
        properties.setProperty(key, value);
        lock.writeLock().unlock();
    }

    public Integer getIntegerProperty(final String key) {
        return Integer.valueOf(getProperty(key));
    }

    public Boolean getBooleanProperty(final String key) {
        return "true".equals(getProperty(key));
    }


    private RagConfiguration() {
        super(configFilename, LOGGER);
    }
}