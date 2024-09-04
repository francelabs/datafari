package com.francelabs.datafari.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.utils.rag.DocumentForRag;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatafariLlmConnector implements LlmConnector {

    String url;
    String temperature;
    String maxToken;
    String model;
    String apiKey; // If not used in the future, this might be removed
    RagConfiguration config;

    public DatafariLlmConnector(RagConfiguration config) {
        this.url = config.getEndpoint();
        this.temperature = config.getTemperature();
        this.maxToken = config.getMaxTokens();
        this.model = config.getModel();
        this.apiKey = config.getToken();
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
     * @param documentList A list of documents.
     * @return The string LLM response
     */
    public String vectorRag(JSONArray documentList, HttpServletRequest request) throws IOException {

        List<Document> documents = new ArrayList<>();
        String message;
        StringBuilder concatenatedResponses = new StringBuilder();

        // Create a list of Langchain4j Documents
        ObjectMapper mapper = new ObjectMapper();
        documentList.forEach(item -> {
            JSONObject jsonDoc = (JSONObject) item;
            DocumentForRag doc = null;
            try {
                doc = mapper.readValue(jsonDoc.toJSONString(), DocumentForRag.class);
                Document s4jdoc = new Document(doc.getContent());
                s4jdoc.metadata().put("title", doc.getTitle());
                documents.add(s4jdoc);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("An error occurred during chunking.");
            }
        });

        // Embedding the documents and store them into vector DB
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // Vector query
        List<Content> contents = EmbeddingStoreContentRetriever.from(embeddingStore).retrieve(Query.from(request.getParameter("q")));


        // The first calls returns a concatenated responses from each chunk
        for (Content content : contents) {
            String body = generateRequestBody(content.textSegment().text());
            concatenatedResponses.append(generate(body));
        }

        // If the is only one prompt to send, we get the answer from the response
        // Otherwise, we concatenate all the responses, and generate a new response to summarize the results
        if (contents.size() == 1) {
            message = concatenatedResponses.toString();
        } else if (contents.size() > 1) {
            String body = PromptUtils.createPrompt(config, "```" + concatenatedResponses + "```", request);
            body = generateRequestBody(body);
            message = generate(body);
        } else {
            throw new RuntimeException("Could not find relevant data to generate an answer.");
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
    String error;
    public String getError() {
        return error;
    }
}