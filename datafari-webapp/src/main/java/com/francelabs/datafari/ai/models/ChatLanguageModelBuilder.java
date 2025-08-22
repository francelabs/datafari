package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.Map;

public interface ChatLanguageModelBuilder {
    /**
     * Must be overridden. Builds a ChatLanguageModel using the provided params.
     * @param params The properties map
     * @return ChatLanguageModel
     */
    ChatLanguageModel build(Map<String, Object> params);

    /**
     * Get a Double property from "params" by its name, or return default value.
     * @param params The properties map
     * @param key The name of the param
     * @param defaultValue Returned if the property is missing
     * @return A Double property
     */
    default double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Object val = params.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : defaultValue;
    }

    /**
     * Get an Interger property from "params" by its name, or return default value.
     * @param params The params map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return An Interger property
     */
    default int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        return val instanceof Number ? ((Number) val).intValue() : defaultValue;
    }

    /**
     * Get a String property from "params" by its name, or return default value.
     * @param params The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return A String property
     */
    default String getString(Map<String, Object> params, String key, String defaultValue) {
        Object val = params.get(key);
        return val instanceof String ? (String) val : defaultValue;
    }

    /**
     * Get a boolean property from "params" by its name, or return default value.
     * @param params The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return A boolean property
     */
    default boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object val = params.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return defaultValue;
    }
}