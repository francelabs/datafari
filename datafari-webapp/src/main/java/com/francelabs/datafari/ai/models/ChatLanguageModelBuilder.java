package com.francelabs.datafari.ai.models;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.Map;

public interface ChatLanguageModelBuilder {
    ChatLanguageModel build(Map<String, Object> properties);

    default double getDouble(Map<String, Object> props, String key, double defaultValue) {
        Object val = props.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : defaultValue;
    }

    default int getInt(Map<String, Object> props, String key, int defaultValue) {
        Object val = props.get(key);
        return val instanceof Number ? ((Number) val).intValue() : defaultValue;
    }

    default String getString(Map<String, Object> props, String key, String defaultValue) {
        Object val = props.get(key);
        return val instanceof String ? (String) val : defaultValue;
    }

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