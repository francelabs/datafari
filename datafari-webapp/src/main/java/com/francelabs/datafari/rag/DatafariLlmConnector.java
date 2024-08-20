package com.francelabs.datafari.rag;

import com.francelabs.datafari.utils.rag.PromptUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DatafariLlmConnector implements LlmConnector {

    String url;
    String temperature;
    String maxToken;
    String model;
    String apiKey; // If not used in the future, this might be removed

    public DatafariLlmConnector(RagConfiguration config) {
        this.url = config.getEndpoint();
        this.temperature = config.getTemperature();
        this.maxToken = config.getMaxTokens();
        this.model = config.getModel();
        this.apiKey = config.getToken();
    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String invoke(List<String> prompts, RagConfiguration config, String userQuery) throws IOException {
        String body = generateRequestBody(prompts);

        // The first call returns a concatenated responses from each chunk
        String message = generate(body, config);

        message = PromptUtils.createPrompt(config, userQuery, message);
        prompts = new ArrayList<>();
        prompts.add("```" + message + "```");
        body = generateRequestBody(prompts);
        message = generate(body, config);
        return message;

    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String generate(String prompts, RagConfiguration config) {

        try {

            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            if (!apiKey.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // Mock the webservices (to delete for prod, for test and dev purposes)
            if (!config.isEnabled()) return "RAG feature is currently disabled. This is a placeholder message. \\n Enable the feature by editing rag.properties file.";

            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(prompts);
            writer.flush();
            writer.close();

            // Response from LLM API
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            String message = extractMessageFromResponse(response.toString());
            return message;

        } catch (IOException e) {
            throw new RuntimeException("An error occurred while calling external webservices.", e);
        }
    }


    /**
     * Generate the body attached to the request sent to the LLM
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return A JSON String
     */
    public String generateRequestBody(List<String> prompts) {

        JSONObject queryBody = new JSONObject();
        JSONObject input = new JSONObject();
        JSONArray queries = new JSONArray();
        if (!temperature.isEmpty()) input.put("temperature", temperature);
        if (!maxToken.isEmpty()) input.put("max_tokens", maxToken);
        if (!model.isEmpty()) input.put("model", model);

        for (String prompt : prompts) {
            JSONObject query = new JSONObject();
            if (!prompt.isEmpty()) {
                query.put("content", prompt);
                queries.add(query);
            }
        }

        input.put("queries", queries);
        queryBody.put("input", input);
        return queryBody.toJSONString();
    }

    /**
     * Extract message from LLM response
     * @param response A String JSON provided by the LLM
     * @return A simple String message
     */
    public String extractMessageFromResponse(String response){
        response = response.replace("\\\"", "`");
        int start = response.indexOf("output")+ 9;
        int end = response.indexOf("\"", start);
        return response.substring(start, end).trim();
    }
}
