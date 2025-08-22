package com.francelabs.datafari.rag;

import com.francelabs.datafari.config.AbstractConfigClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO : Must be moved to com.francelabs.datafari.ai
public class RagConfiguration extends AbstractConfigClass {


    private static final String CONFIG_FILENAME = "rag.properties";
    private static RagConfiguration instance;
    public static final Logger LOGGER = LogManager.getLogger(RagConfiguration.class.getName());


    // GLOBAL RAG PROPERTIES
    public static final String ENABLE_RAG = "ai.enable.rag";
    public static final String ENABLE_SUMMARIZATION = "ai.enable.summarization";

    // WEB SERVICES PARAMETERS
    public static final String API_ENDPOINT = "ai.api.endpoint";
    public static final String API_TOKEN = "ai.api.token";
    public static final String LLM_SERVICE = "ai.llm.service";

    // LLM PARAMETERS
    public static final String LLM_TEMPERATURE = "llm.temperature";
    public static final String LLM_MAX_TOKENS = "llm.maxTokens";
    public static final String LLM_MODEL = "llm.model";

    // PROMPTING
    public static final String PROMPT_CHUNKING_STRATEGY = "prompt.chunking.strategy"; // mapreduce or refine
    public static final String MAX_REQUEST_SIZE = "prompt.max.request.size";

    // CHAT HISTORY
    public static final String CHAT_QUERY_REWRITING_ENABLED_BM25 = "chat.query.rewriting.enabled.bm25";
    public static final String CHAT_QUERY_REWRITING_ENABLED_VECTOR = "chat.query.rewriting.enabled.vector";
    public static final String CHAT_MEMORY_ENABLED = "chat.memory.enabled";
    public static final String CHAT_MEMORY_HISTORY_SIZE = "chat.memory.history.size";

    // RETRIEVAL
    public static final String RETRIEVAL_METHOD = "retrieval.method";

    // BM25 SEARCH
    public static final String MAX_FILES = "chunking.maxFiles";
    public static final String CHUNK_SIZE = "chunking.chunk.size";
    public static final String SEARCH_OPERATOR = "rag.operator";

    // IN MEMORY VECTOR SEARCH
    public static final String ENABLE_VECTOR_SEARCH = "inMemory.enable.vector.search"; // Deprecated, see SOLR_ENABLE_VECTOR_SEARCH
    public static final String IN_MEMORY_TOP_K = "inMemory.topK";

    // BM25 SEARCH
    public static final String RRF_TOPK = "rrf.topK";
    public static final String RRF_RANK_CONSTANT = "rrf.rank.constant";

    // SOLR VECTOR SEARCH
    public static final String SOLR_ENABLE_VECTOR_SEARCH = "solr.enable.vector.search"; // TODO : Deprecate
    public static final String SOLR_EMBEDDINGS_MODEL = "solr.embeddings.model";
    public static final String SOLR_VECTOR_FIELD = "solr.vector.field";
    public static final String SOLR_TOPK = "solr.topK";


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

    public boolean getBooleanProperty(final String key, boolean defaultValue) {
        String parsedBoolean = (defaultValue) ? getProperty(key, "true") : getProperty(key, "false");
        return "true".equals(parsedBoolean);
    }

    private RagConfiguration() {
        super(CONFIG_FILENAME, LOGGER);
    }
}
