package com.francelabs.datafari.transformation.binary.model;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ConfigNode;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;

import java.util.*;

/**
 * This class contains specifications for the Binary Connector.
 */
public class BinarySpecification {

    private static final Logger LOGGER = LogManager.getLogger(BinarySpecification.class.getName());


    Map<String,Object> spec = new HashMap<>();

    public BinarySpecification() {
        // Empty connector
        // TODO : remove probably
        spec.put(BinaryConfig.NODE_ENABLE_BINARY_CONNECTOR, false);
        spec.put(BinaryConfig.NODE_TYPE_OF_SERVICE, "");
        spec.put(BinaryConfig.NODE_SERVICE_HOSTNAME, "");
        spec.put(BinaryConfig.NODE_SERVICE_ENDPOINT, "");
        spec.put(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN, "");
        spec.put(BinaryConfig.NODE_SERVICE_ADDITIONAL_PARAMETERS, new HashMap<String, String>());
        spec.put(BinaryConfig.NODE_FILTERS, new HashMap<String, String>());
        spec.put(BinaryConfig.NODE_EXTRACTED_METADATA, new HashMap<String, String>());
    }

    public BinarySpecification(Specification os, ConfigParams config) {
        spec.put(BinaryConfig.NODE_ENABLE_BINARY_CONNECTOR, false);
        spec.put(BinaryConfig.NODE_TYPE_OF_SERVICE, "");
        spec.put(BinaryConfig.NODE_SERVICE_HOSTNAME, "");
        spec.put(BinaryConfig.NODE_SERVICE_ENDPOINT, "");
        spec.put(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN, "");
        spec.put(BinaryConfig.NODE_SERVICE_ADDITIONAL_PARAMETERS, new HashMap<String, String>());
        spec.put(BinaryConfig.NODE_FILTERS, new HashMap<String, String>());
        spec.put(BinaryConfig.NODE_EXTRACTED_METADATA, new HashMap<String, String>());

        for (int i = 0; i < config.getChildCount(); i++) {
            final ConfigNode cn = config.getChild(i);
            String attributeName = cn.getAttributeValue("name");
            setProperty(attributeName, cn.getValue());
        }

        for (int i = 0; i < os.getChildCount(); i++) {
            final SpecificationNode sn = os.getChild(i);
            String attributeName = sn.getType();
            setProperty(attributeName, sn.getAttributeValue(BinaryConfig.ATTRIBUTE_VALUE));
        }
    }

    public Map<String, Object> getSpec() {
        return spec;
    }

    public void setSpec(Map<String, Object> spec) {
        this.spec = spec;
    }

    // Generic property setter
    public void setProperty(String attributeName, String value) {
        Object currentValue = this.spec.get(attributeName);

        if (currentValue == null) {
            // TODO : set to debug level
            LOGGER.warn("No default type registered for property '{}'. Ignoring set operation.", attributeName);
            return;
        }

        try {
            // Handle String
            if (currentValue instanceof String) {
                this.spec.put(attributeName, value);
            }

            // Handle Integer
            else if (currentValue instanceof Integer) {
                int parsedInt = Integer.parseInt(value.trim());
                this.spec.put(attributeName, parsedInt);
            }

            // Handle Boolean
            else if (currentValue instanceof Boolean) {
                String lower = value.trim().toLowerCase();
                boolean parsedBool = lower.equals("true") || lower.equals("1") || lower.equals("yes");
                this.spec.put(attributeName, parsedBool);
            }

            // Handle HashMap<String, String>
            else if (currentValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) currentValue;

                // Allow multiple key=value lines (split on newline)
                String[] lines = value.split("\\r?\\n");

                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    int equalPos = line.indexOf('=');
                    if (equalPos != -1) {
                        String key = line.substring(0, equalPos).trim();
                        String val = line.substring(equalPos + 1).trim();
                        map.put(key, val);
                    } else {
                        LOGGER.warn("Invalid map format for property '{}': '{}'. Expected 'key=value'.", attributeName, line);
                    }
                }
                this.spec.put(attributeName, map);
            }

            // Unknown or unsupported type
            else {
                LOGGER.error("Unsupported type for attribute '{}': {}", attributeName, currentValue.getClass().getName());
            }

        } catch (Exception e) {
            LOGGER.error("Error while setting property '{}': {}", attributeName, e.getMessage(), e);
        }
    }

    // BOOLEAN
    public Boolean getBooleanProperty(String attributeName) {
        return getBooleanProperty(attributeName, false);
    }

    public Boolean getBooleanProperty(String attributeName, Boolean defaultValue) {
        Object value = this.spec.get("attributeName");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        logTypeMismatch(attributeName, value, "Boolean");
        return defaultValue;
    }

    public void setBooleanProperty(String attributeName, boolean value) {
        this.spec.put(attributeName, value);
    }

    // STRING
    public String getStringProperty(String attributeName) {
        return getStringProperty(attributeName, "");
    }

    public String getStringProperty(String attributeName, String defaultValue) {
        Object value = this.spec.get(attributeName);
        if (value instanceof String) {
            return (String) value;
        }
        logTypeMismatch(attributeName, value, "String");
        return defaultValue;
    }

    public void setStringProperty(String attributeName, String value) {
        this.spec.put(attributeName, value);
    }

    // HashMap<String, String>
    @SuppressWarnings("unchecked")
    public Map<String, String> getMapProperty(String attributeName) {
        return getMapProperty(attributeName, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getMapProperty(String attributeName, Map<String, String> defaultValue) {
        Object value = this.spec.get(attributeName);
        if (value instanceof Map) {
            try {
                return (Map<String, String>) value;
            } catch (ClassCastException e) {
                LOGGER.error("Casting error on property {}: expected Map<String,String>, but got Map<?,?>", attributeName);
            }
        }
        logTypeMismatch(attributeName, value, "Map<String, String>");
        return defaultValue;
    }

    public void setMapProperty(String attributeName, Map<String, String> value) {
        this.spec.put(attributeName, value);
    }

    private void logTypeMismatch(String attributeName, Object value, String expectedType) {
        if (value != null) {
            LOGGER.error("Unexpected type for property '{}': expected {}, but got {}", attributeName, expectedType, value.getClass().getSimpleName());
        } else {
            LOGGER.warn("No value found for property '{}'", attributeName);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BinarySpecification other = (BinarySpecification) obj;
        return Objects.equals(this.spec, other.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec);
    }
}
