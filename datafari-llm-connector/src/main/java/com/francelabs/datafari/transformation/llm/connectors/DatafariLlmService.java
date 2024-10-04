package com.francelabs.datafari.transformation.llm.connectors;

import com.francelabs.datafari.transformation.llm.model.LlmSpecification;
import com.francelabs.datafari.transformation.llm.utils.PromptUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class DatafariLlmService implements LlmService {

    private static final Logger LOGGER = LogManager.getLogger(DatafariLlmService.class.getName());

    String url;
    String temperature;
    int maxToken;
    String model;
    String apiKey; // If not used in the future, this might be removed
    LlmSpecification spec;

    public DatafariLlmService(LlmSpecification spec) {
        this.url = spec.getLlmEndpoint();
        this.temperature = "0";
        this.maxToken = spec.getMaxTokens();
        this.model = spec.getLlm();
        this.apiKey = spec.getApiKey();
        this.spec = spec;
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
            try{
                DatafariRagResponse response = template.postForObject(url, requestEntity,  DatafariRagResponse.class);
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
        if (maxToken == 0) input.put("max_tokens", maxToken);
        if (!model.isEmpty()) input.put("model", model);

        JSONObject query = new JSONObject();
        query.put("content", prompt);
        queries.add(query);

        input.put("queries", queries);
        queryBody.put("input", input);
        return queryBody.toJSONString();
    }

    @Override
    public String invoke(String content) throws IOException {
        return generate(content);
    }

    @Override
    public float[] embeddings(String content) throws IOException {

        // Embedding the document
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Response<Embedding> embedding = embeddingModel.embed(content);

        LOGGER.info("Vector embedding : {}", embedding);
        return embedding.content().vector();
    }

    @Override
    public String summarize(String content, LlmSpecification spec) throws IOException {
        String prompt = PromptUtils.promptForSummarization(content, spec);
        return invoke(prompt);
    }

    @Override
    public String categorize(String content) throws IOException {
        String prompt = PromptUtils.promptForCategorization(content);
        return invoke(prompt);
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