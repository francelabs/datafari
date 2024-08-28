package com.francelabs.datafari.rag;

import com.francelabs.datafari.utils.rag.PromptUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
    public String invoke(List<String> prompts, RagConfiguration config, HttpServletRequest request) throws IOException {

        StringBuilder concatenatedResponses = new StringBuilder();
        String message;

        // The first calls returns a concatenated responses from each chunk
        for (String prompt : prompts) {
            String body = generateRequestBody(prompt);
            concatenatedResponses.append(generate(body, config));
        }

        // If the is only one prompt to send, we get the answer from the response
        // Otherwise, we concatenate all the responses, and generate a new response to summarize the results
        if (prompts.size() == 1) {
            message = concatenatedResponses.toString();
        } else if (prompts.size() > 1) {
            String body = PromptUtils.createPrompt(config, "```" + concatenatedResponses + "```", request);
            body = generateRequestBody(body);
            message = generate(body, config);
        } else {
            throw new RuntimeException("Could not find data to send to the LLM");
        }

        return message;

    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String generate(String prompts, RagConfiguration config) {

        try {


            // Mock the webservices (to delete for prod, for test and dev purposes)
            if (!config.isEnabled()) return "RAG feature is currently disabled. This is a placeholder message. \\n Enable the feature by editing rag.properties file.";

            RestTemplate template = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!apiKey.isEmpty()) headers.setBearerAuth(apiKey);
            HttpEntity<String> requestEntity = new HttpEntity<>(prompts, headers);
            String resp = "";
            try{
                DatafariRagResponse response = template.postForObject(url, requestEntity,  DatafariRagResponse.class);
                resp = response.getOutput();
                return resp;
            }
            catch(NullPointerException e){
                throw new RuntimeException("An error occurred while calling external webservices.", e);
            }


        } catch (Exception e) {
            throw new RuntimeException("An error occurred while calling external webservices.", e);
        }
    }

    /**
     * Generate the body attached to the request sent to the LLM
     * @param prompt A single String prompt. Each prompt contains instructions for the model, document content and the user query
     * @return A JSON String
     */
    public String generateRequestBody(String prompt) {

        JSONObject queryBody = new JSONObject();
        JSONObject input = new JSONObject();
        JSONArray queries = new JSONArray();
        if (!temperature.isEmpty()) input.put("temperature", temperature);
        if (!maxToken.isEmpty()) input.put("max_tokens", maxToken);
        if (!model.isEmpty()) input.put("model", model);

        JSONObject query = new JSONObject();
        query.put("content", prompt);
        queries.add(query);

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

class DatafariRagResponse {

    String output;
    public String getOutput() {
        return output;
    }
}