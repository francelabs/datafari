package com.francelabs.datafari.mcp.service;

import com.francelabs.datafari.mcp.dto.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatafariClient {

    private static final Logger LOGGER = LogManager.getLogger(DatafariClient.class.getName());
    private static final String DEFAULT_FL =
            "title,url,id,docId,extension,preview_content,last_modified,crawl_date,author,original_file_size,emptied,repo_source";

    private final RestClient restClient;

    public DatafariClient(@Value("${datafari.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }


    /**
     * Search documents from Datafari
     * @param request SearchRequest
     * @param authenticationHeaders HttpHeaders The authentication headers
     * @return SearchResponse
     */
    public SearchResponse search(SearchRequest request, HttpHeaders authenticationHeaders) {

        try {

            JsonNode root = restClient.get()
                .uri(uriBuilder -> {
                    URI uri = uriBuilder
                            .path("/Datafari/rest/v2.0/search/select")
                            .queryParam("q", request.getQuery())
                            .queryParam("fl", DEFAULT_FL)
                            .queryParam("sort", "score desc")
                            .queryParam("q.op", "AND")
                            .queryParam("rows", request.getRows())
                            .queryParam("start", 0)
                            .queryParam("aggregator", "")
                            .queryParam("wt", "json")
                            .build();

                    LOGGER.debug("Calling Datafari URL: " + uri);
                    return uri;
                })
                .headers(headers -> headers.addAll(authenticationHeaders))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            if (root == null) {
                LOGGER.error("ERROR ! JsonNode response is null ");
                return null;
            }

            LOGGER.debug("JsonNode response: {}", root);

            return mapSearchResponse(root);

        } catch (Exception e) {
            LOGGER.error(e);
            return null;
        }
    }

    /**
     * Search documents from Datafari
     * @param request Typed Request
     * @param authenticationHeaders HttpHeaders The authentication headers
     * @return SearchResponse
     */
    public AgenticResponse agentic(AgenticRequest request, HttpHeaders authenticationHeaders) {
        AiResponse aiResponse = callAiPoweredApi(buildAgenticPayload(request), authenticationHeaders);
        return new AgenticResponse(
                aiResponse.message(),
                aiResponse.sources()
        );
    }

    public SummarizeResponse summarize(SummarizeRequest request, HttpHeaders authenticationHeaders) {
        AiResponse aiResponse = callAiPoweredApi(buildSummarizePayload(request), authenticationHeaders);
        return new SummarizeResponse(
                aiResponse.message(),
                aiResponse.sources().getFirst()
        );
    }

    /**
     * Send a request to AI Powered API
     * @param payload Map<String, Object>
     * @param authenticationHeaders HttpHeaders The authentication headers
     * @return AiResponse
     */
    private AiResponse callAiPoweredApi(Map<String, Object> payload, HttpHeaders authenticationHeaders) {
        try {
            long start = System.currentTimeMillis();
            JsonNode root = restClient.post()
                    .uri("/Datafari/rest/v2.0/ai")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.addAll(authenticationHeaders))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            LOGGER.info("Datafari summarize done in " + (System.currentTimeMillis() - start) + " ms");

            if (root == null) {
                throw new IllegalStateException("Empty response from Datafari AI API");
            }

            String status = root.path("status").asString();
            JsonNode content = root.path("content");

            if (!"OK".equalsIgnoreCase(status)) {
                JsonNode error = content.path("error");
                throw new IllegalStateException(
                        "Datafari AI API error: "
                                + error.path("label").asString("")
                                + " - "
                                + error.path("message").asString("")
                                + " - "
                                + error.path("reason").asString("")
                );
            }

            return mapAiResponse(content);

        } catch (Exception e) {
            LOGGER.error("Server failed to call Datafari API", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a JSON into a SearchResponse Objet
     * @param root The returned JSON
     * @return SearchResponse
     */
    private SearchResponse mapSearchResponse(JsonNode root) {
        List<Source> results = new ArrayList<>();

        JsonNode docs = root.path("response").path("docs");
        if (docs.isArray()) {
            for (JsonNode doc : docs) {
                results.add(new Source(
                        doc.path("docId").asString(null),
                        firstText(doc.path("title")),
                        firstText(doc.path("preview_content")),
                        doc.path("url").asString(null),
                        doc.path("repo_source").asString(null),
                        null
                ));
            }
        }

        return new SearchResponse(results);
    }

    /**
     * Convert a JSON into a AiResponse Object
     * @param content The  JSON returned by AiPowered API
     * @return AiResponse
     */
    private AiResponse mapAiResponse(JsonNode content) {
        String message = content.path("message").asString(null);
//        String conversationId = content.path("conversationId").asString(null);

        List<Source> sources = new ArrayList<>();

        JsonNode sourceNodes = content.path("sources");
        if (sourceNodes.isArray()) {
            for (JsonNode sourceNode : sourceNodes) {
                sources.add(new Source(
                        sourceNode.path("id").asString(null),
                        sourceNode.path("title").asString(null),
                        sourceNode.path("content").asString(null),
                        sourceNode.path("url").asString(null),
                        null,
                        null
                ));
            }
        }

        return new AiResponse(message, sources, null);
    }

    /**
     * Build payload for Agentic
     */
    private Map<String, Object> buildAgenticPayload(AgenticRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", request.getQuery());
        payload.put("action", "agentic");
        payload.put("agent", request.getAgent());
        if (request.getLang() != null) payload.put("lang", request.getLang());
        payload.put("filters", request.getFilters());
        payload.put("history", List.of());
        return payload;
    }

    /**
     * Build payload for Summarization
     */
    private Map<String, Object> buildSummarizePayload(SummarizeRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "summarize");
        payload.put("id", request.getDocId());
        if (request.getLang() != null) payload.put("lang", request.getLang());
        payload.put("history", List.of());
        payload.put("filters", Map.of());
        return payload;
    }

    private String firstText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0).asString(null);
        }
        return node.asString(null);
    }
}