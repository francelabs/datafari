package com.francelabs.datafari.transformation.binary.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonUtils {

    private static final Logger LOGGER = LogManager.getLogger(JsonUtils.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtils() {
        // Empty constructor
    }

    /**
     * Extract data from a provided JSON, based on a location key
     * @param jsonResponse A String JSON
     * @param dataLocation The location key (e.g.: content.results.entities[0].firstName)
     * @return
     */
    public static String extractResponse(String jsonResponse, String dataLocation) {
        try {
            JsonNode currentNode = mapper.readTree(jsonResponse);

            String[] parts = dataLocation.split("\\.");

            for (String part : parts) {
                if (part.contains("[")) {
                    // Ex: "choices[0]"
                    String field = part.substring(0, part.indexOf("["));
                    int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                    currentNode = currentNode.get(field).get(index);
                } else {
                    currentNode = currentNode.get(part);
                }

                if (currentNode == null) {
                    return null;
                }
            }

            if (currentNode.isValueNode()) {
                return currentNode.asText();
            } else {
                return currentNode.toString();
            }

        } catch (Exception e) {
            LOGGER.error("Invalid data location: {}. Could not extract data.", dataLocation, e);
            return null;
        }
    }
}
