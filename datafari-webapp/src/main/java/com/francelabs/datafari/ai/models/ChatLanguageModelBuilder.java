package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.Map;

public interface ChatLanguageModelBuilder {
    /**
     * Must be overridden. Builds a ChatLanguageModel using the provided properties.
     * @param properties The properties map
     * @return ChatLanguageModel
     */
    ChatLanguageModel build(Map<String, Object> properties);

    /**
     * Get a Double property from "props" by its name, or return default value.
     * @param props The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return A Double property
     */
    default double getDouble(Map<String, Object> props, String key, double defaultValue) {
        Object val = props.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : defaultValue;
    }

    /**
     * Get an Interger property from "props" by its name, or return default value.
     * @param props The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return An Interger property
     */
    default int getInt(Map<String, Object> props, String key, int defaultValue) {
        Object val = props.get(key);
        return val instanceof Number ? ((Number) val).intValue() : defaultValue;
    }

    /**
     * Get a String property from "props" by its name, or return default value.
     * @param props The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return A String property
     */
    default String getString(Map<String, Object> props, String key, String defaultValue) {
        Object val = props.get(key);
        return val instanceof String ? (String) val : defaultValue;
    }

    /**
     * Get a boolean property from "props" by its name, or return default value.
     * @param props The properties map
     * @param key The name of the property
     * @param defaultValue Returned if the property is missing
     * @return A boolean property
     */
    default boolean getBoolean(Map<String, Object> props, String key, boolean defaultValue) {
        Object val = props.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return defaultValue;
    }
}