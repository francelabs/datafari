package com.francelabs.datafari.rag;

import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class DatafariLlmService implements LlmService {

    String url;
    String temperature;
    String maxToken;
    String model;
    String apiKey; // If not used in the future, this might be removed
    RagConfiguration config;

    public DatafariLlmService(RagConfiguration config) {
        this.url = config.getProperty(RagConfiguration.API_ENDPOINT);
        this.temperature = config.getProperty(RagConfiguration.LLM_TEMPERATURE);
        this.maxToken = config.getProperty(RagConfiguration.LLM_MAX_TOKENS);
        this.model = config.getProperty(RagConfiguration.LLM_MODEL);
        this.apiKey = config.getProperty(RagConfiguration.API_TOKEN);
        this.config = config;
    }


    /**
     * Call the Datafari External LLM Webservice
     * @param prompts A list of prompts. Each prompt contains instructions for the model, document content and the user query
     * @return The string LLM response
     */
    public String invoke(List<String> prompts, HttpServletRequest request) throws IOException {

        StringBuilder concatenatedResponses = new StringBuilder();
        String message;

        // The first calls returns a concatenated responses from each chunk
        for (String prompt : prompts) {
            String body = generateRequestBody(prompt);
            concatenatedResponses.append(generate(body));
        }

        // If the is only one prompt to send, we get the answer from the response
        // Otherwise, we concatenate all the responses, and generate a new response to summarize the results
        if (prompts.size() == 1) {
            message = concatenatedResponses.toString();
        } else if (prompts.size() > 1) {
            String body = PromptUtils.createPrompt(config, "```" + concatenatedResponses + "```", request);
            body = generateRequestBody(body);
            message = generate(body);
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
    public String generate(String prompts) {

        try {

            RestTemplate template = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!apiKey.isEmpty()) headers.setBearerAuth(apiKey);
            HttpEntity<String> requestEntity = new HttpEntity<>(prompts, headers);
            String resp = "";
            String batchUrl = url + "/batch";
            try{
                DatafariRagResponse response = template.postForObject(batchUrl, requestEntity,  DatafariRagResponse.class);
                if (response == null) {
                    throw new RestClientException("An error occurred while calling external webservices. The response does not provide any information");
                } else if (response.getOutput().isEmpty() && !response.getError().isEmpty()) {
                    throw new RestClientException("An error occurred while calling external webservices: " + response.getError());
                } else if (response.getOutput().isEmpty() && response.getError().isEmpty()) {
                    throw new RestClientException("An error occurred while calling external webservices: the response output is empty.");
                } else {
                    resp = response.getOutput();
                }

                if (resp == null) throw new RestClientException("An error occurred while calling external webservices: " + response.getError());
                resp = response.getOutput();

                return resp;
            } catch(NullPointerException e){
                throw new RestClientException("An error occurred while calling external webservices.", e);
            }


        } catch (Exception e) {
            throw new RestClientException("An error occurred while calling external webservices.", e);
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
     * Generate the body attached to the request sent to the LLM
     * @param prompt A single String prompt. Each prompt contains instructions for the model, document content and the user query
     * @return A JSON String
     */
    public float[] embed(String prompt) {

        EmbeddingModel embdModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .modelName(model)
                .build();

        Response<Embedding> embeddings = embdModel.embed(prompt);
        return embeddings.content().vector();
    }
}

class DatafariRagResponse {

    String output;
    public String getOutput() {
        return output;
    }
    String error;
    public String getError() {
        return error;
    }
}