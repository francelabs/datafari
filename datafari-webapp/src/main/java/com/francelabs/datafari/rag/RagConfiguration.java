package com.francelabs.datafari.rag;

import com.francelabs.datafari.config.AbstractConfigClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RagConfiguration extends AbstractConfigClass {


    private static final String CONFIG_FILENAME = "rag.properties";
    private static RagConfiguration instance;
    public static final Logger LOGGER = LogManager.getLogger(RagConfiguration.class.getName());


    // GLOBAL RAG PROPERTIES
    public static final String ENABLE_RAG = "rag.enabled";
    public static final String ENABLE_LOGS = "rag.enable.logs";
    public static final String ENABLE_CHUNKING = "rag.enable.chunking";
    public static final String ENABLE_VECTOR_SEARCH = "rag.enable.vector.search";
    public static final String ENABLE_SUMMARIZATION = "ai.summarization.enabled";

    // WEB SERVICES PARAMETERS
    public static final String API_ENDPOINT = "rag.api.endpoint";
    public static final String API_TOKEN = "rag.api.token";
    public static final String LLM_SERVICE = "rag.llm.service";

    // LLM PARAMETERS
    public static final String LLM_TEMPERATURE = "rag.temperature";
    public static final String LLM_MAX_TOKENS = "rag.maxTokens";
    public static final String LLM_MODEL = "rag.model";
    public static final String LLM_EMBEDDINGS_MODEL = "rag.embeddings.model";


    // DATAFARI RAG PRE-PROCESSING PROPERTIES
    public static final String MAX_FILES = "rag.maxFiles";
    public static final String MAX_CHUNKS = "rag.maxChunks";
    public static final String CHUNK_SIZE = "rag.chunk.size";
    public static final String SEARCH_OPERATOR = "rag.operator";


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

    public Integer getIntegerProperty(final String key, int defaultValue) {
        try {
            int value = Integer.parseInt(getProperty(key));
            if (value < 1) return defaultValue;
            return value;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(final String key) {
        return "true".equals(getProperty(key));
    }

    private RagConfiguration() {
        super(CONFIG_FILENAME, LOGGER);
    }
}