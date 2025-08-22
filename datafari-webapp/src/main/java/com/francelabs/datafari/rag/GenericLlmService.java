package com.francelabs.datafari.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.francelabs.datafari.api.RagAPI;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Deprecated(forRemoval = true)
public class GenericLlmService {

    private static final String CONFIG_FILE = "models.json"; // Next to prompt templates

    private Object activeModelInstance;
    private String activeModelName;

    public GenericLlmService() {
        loadModel();
    }

    private void loadModel() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(new File(CONFIG_FILE), new TypeReference<>() {});


            activeModelName = (String) json.get("activeModel");
            List<Map<String, Object>> models = (List<Map<String, Object>>) json.get("models");

            Optional<Map<String, Object>> match = models.stream()
                    .filter(m -> m.get("name").equals(activeModelName))
                    .findFirst();

            if (match.isEmpty()) {
                throw new IllegalStateException("No model found with name: " + activeModelName);
            }

            Map<String, Object> modelConfig = match.get();
            String className = (String) modelConfig.get("class");
            Map<String, Object> params = (Map<String, Object>) modelConfig.get("params");

            Class<?> classPath = Class.forName(className);

            // Search for a constructor that takes a Map or similar
            Constructor<?> constructor = findMatchingConstructor(classPath, params);

            if (constructor == null) {
                throw new IllegalStateException("No compatible constructor found for " + className);
            }

            activeModelInstance = constructor.newInstance(params);

        } catch (IOException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to load model configuration", e);
        }
    }

    /**
     * @return The instructions prompts stored in resources/prompts folder
     */
    private String loadModelsList() throws IOException {
        return readFromInputStream(RagAPI.class.getClassLoader().getResourceAsStream(CONFIG_FILE));
    }

    /**
     * Transform InputStream into String
     * @param inputStream InputStream to transform
     * @return String
     * @throws IOException : the InputStream is null or could not be read
     */
    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private Constructor<?> findMatchingConstructor(Class<?> classPath, Map<String, Object> params) {
        for (Constructor<?> constructor : classPath.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 1 && Map.class.isAssignableFrom(types[0])) {
                return constructor;
            }
        }
        return null;
    }

    // === Public Access ===

    public Optional<ChatLanguageModel> getChatModel() {
        if (activeModelInstance instanceof ChatLanguageModel) {
            return Optional.of((ChatLanguageModel) activeModelInstance);
        }
        return Optional.empty();
    }

    public Object getRawModel() {
        return activeModelInstance;
    }

    public String getActiveModelName() {
        return activeModelName;
    }
}
