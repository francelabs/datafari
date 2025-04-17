package com.francelabs.datafari.rest.v2_0.ai;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.Message;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;

@RestController
public class AiPowered {

    private static final Logger LOGGER = LogManager.getLogger(AiPowered.class.getName());
    private static final String ID_FIELD = "id";
    private static final String TITLE_FIELD = "title";
    private static final String URL_FIELD = "url";
    private static final String LLM_SUMMARY_FIELD = "llm_summary";
    public static final String EXACT_CONTENT_FIELD = "exactContent";
    private static final String QUERY_FIELD = "query";
    private static final String STATUS_FIELD = "status";

    @PostMapping(value = "/rest/v2.0/ai/summarize", produces = "application/json;charset=UTF-8")
    public String summarizeDocument(final HttpServletRequest request, @RequestBody JSONObject jsonDoc) {

        String id;
        String lang;
        String title;
        String content;
        String summary;
        String url;
        final JSONObject jsonResponse = new JSONObject();


        // Retrieve ID from JSON input
        if (jsonDoc.get(ID_FIELD) != null) {
            id = (String) jsonDoc.get(ID_FIELD);
        } else {
            return generateErrorJson(422, "summarizationBadRequest", "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.", null);
        }
        // Retrieve language
        if (jsonDoc.get("lang") != null) {
            lang = (String) jsonDoc.get("lang");
            request.setAttribute("lang", lang);
        }

        LOGGER.info("AiPowered - Summarize - Summary of the document {} requested.", id);

        // Retrieve document from Solr using Datafari API methods
        try {

            // Retrieve document
            JSONObject searchresult = performSearchById(request, id);
            JSONObject jsonAiDocument = (JSONObject) ((JSONArray) ((JSONObject) searchresult.get("response")).get("docs")).get(0);

            if (jsonAiDocument.get(ID_FIELD) != null && id.equals(jsonAiDocument.get(ID_FIELD))) {
                title = (String) ((JSONArray) jsonAiDocument.get(TITLE_FIELD)).get(0);
                url = (String) jsonAiDocument.get(URL_FIELD);
                summary = (String) jsonAiDocument.get(LLM_SUMMARY_FIELD);
                content = (String) ((JSONArray) jsonAiDocument.get(EXACT_CONTENT_FIELD)).get(0);
            } else {
                return generateErrorJson(422, "summarizationNoFileFound", "The document cannot be retrieved.", null);
            }

        } catch (final NullPointerException|IndexOutOfBoundsException e) {
            return generateErrorJson(422, "summarizationNoFileFound", "The document cannot be retrieved.", null);
        } catch (final Exception e) {
            return generateErrorJson(500, "summarizationTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
        }


        try {
            // Process the document using RagAPI methods
            return getDocumentSummary(request, summary, content, id, title, url, jsonResponse);

            //  TODO: This feature can be improved by implementing Atomic Update.
            //   When a summary is generated in the method, the Solr Document from FILESHARE should be updated.


        } catch (final Exception e) {
            return generateErrorJson(500, "summarizationTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
        }
    }

    @PostMapping(value = "/rest/v2.0/ai/rag", produces = "application/json;charset=UTF-8")
    public String rag(final HttpServletRequest request, @RequestBody JSONObject jsonDoc) {

        LOGGER.debug("AiPowered - RAG - RAG request received.");

        String query; // The user request
        String id;
        String lang;
        boolean ragBydocument = false;
        JSONObject searchResults;

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();

        // Is RAG enabled ?
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_RAG))
            return generateErrorJson(422, "ragErrorNotEnabled ", "Sorry, it seems the feature is not enabled.", null);

        // Retrieve query from JSON input
        if (jsonDoc.get(QUERY_FIELD) != null) {
            query = (String) jsonDoc.get(QUERY_FIELD);
            LOGGER.debug("AiPowered - RAG - RAG query : {}", query);
        } else {
            LOGGER.warn("AiPowered - RAG - Missing query parameter");
            return generateErrorJson(422, "ragBadRequest", "Sorry, It appears there is an issue with the request. Please try again later, and if the problem remains, contact an administrator.", null);
        }

        if (jsonDoc.containsKey("history")) {
            Object history = jsonDoc.get("history");
            if (history != null) {
                request.setAttribute("history", history);
                LOGGER.debug("AiPowered - RAG - Conversation history retrieved for request: {}", query);
            } else {
                LOGGER.warn("AiPowered - RAG - History found but is not a valid JSONArray. Ignored.");
            }
        }

        // Search using Datafari API methods
        try {
            // Retrieve ID from JSON input
            if (jsonDoc.get(ID_FIELD) != null && !((String) jsonDoc.get(ID_FIELD)).isEmpty()) {
                id = (String) jsonDoc.get(ID_FIELD);
                LOGGER.debug("AiPowered - RAG - Retrieving document {} for RAG by Document.", id);
                searchResults = performSearchById(request, id);
                ragBydocument = true;
            } else {
                LOGGER.debug("AiPowered - RAG - Performing search.");
                request.setAttribute("q.op", config.getProperty(RagConfiguration.SEARCH_OPERATOR));
                searchResults = performSearch(request, query, config.getBooleanProperty(RagConfiguration.SOLR_ENABLE_VECTOR_SEARCH));
            }
        } catch (IOException|ServletException e) {
            LOGGER.error("AiPowered - RAG - ERROR. An error occurred while retrieving documents.", e);
            return generateErrorJson(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
        }

        // Retrieve language
        if (jsonDoc.get("lang") != null) {
            lang = (String) jsonDoc.get("lang");
            request.setAttribute("lang", lang);
        }

        try {
            // Process the document(s) using RagAPI methods
            EditableHttpServletRequest editablerequest = new EditableHttpServletRequest(request);
            editablerequest.addParameter("q", query);
            return RagAPI.rag(editablerequest, searchResults, ragBydocument).toJSONString();
        } catch (final Exception e) {
            LOGGER.error("AiPowered - RAG - ERROR", e);
            return generateErrorJson(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
        }
    }

    /**
     * Generate a summary for a document, or returns it if it already exists
     * @param request HttpServletRequest
     * @param summary String The summary of the documents if it already exists
     * @param content String The exactContent of the document
     * @param id String
     * @param title String
     * @param url String
     * @param jsonResponse String
     * @return a response JSONObject
     */
    private String getDocumentSummary(HttpServletRequest request, String summary, String content, String id,
                                      String title, String url, JSONObject jsonResponse) throws IOException {
        if ((summary == null || summary.isEmpty()) && (content != null && !content.isEmpty())) {
            // If there is no existing summary, but content is found, use RagAPI service to generate a summary
            LOGGER.debug("AiPowered - Summarize - No summary found for document {}.", id);

            // Instantiate a Langchain4j Document
            Metadata metadata = new Metadata();
            metadata.put(ID_FIELD, id);
            if (title != null) metadata.put(TITLE_FIELD, title);
            if (url != null) metadata.put(URL_FIELD, url);
            Document doc = new Document(content, metadata);

            LOGGER.debug("AiPowered - Summarize - Generating a summary for document {}.", id);
            try {
                summary = RagAPI.summarize(request, doc);
            } catch (Exception e) {
                LOGGER.error("Summarization failed !");
                return generateErrorJson(500, "summarizationTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
            }
            JSONObject jsonContent = new JSONObject();
            jsonContent.put("message", summary);
            jsonResponse.put(STATUS_FIELD, "OK");
            jsonResponse.put("content", jsonContent);
            return jsonResponse.toJSONString();

        } else if (content == null || content.isEmpty()) {
            // Error : No content, no summary
            LOGGER.warn("AiPowered - Summarize - Could not retrieve summary or content from file {}.", id);
            return generateErrorJson(422, "summarizationEmptyFile", "Sorry, I am unable to generate a summary, since the file has no content.", null);

        } else {
            jsonResponse.put(STATUS_FIELD, "OK");
            jsonResponse.put("content", summary);
            return jsonResponse.toJSONString();
        }
    }

    /**
     * Find a documents by its ID from Solr, using Datafari API methods.
     * This method uses an editable version of the original HttpServletRequest.
     * The updated request object is updated in order to process.
     * @param originalRequest : The HttpServletRequest
     * @param id The ID of the document
     * @return a JSONObject containing search resultats, with the following fields :
     *      id, title, exactContent, url, llm_summary
     */
    private static JSONObject performSearchById(HttpServletRequest originalRequest, String id) throws ServletException, IOException {
        EditableHttpServletRequest request = new EditableHttpServletRequest(originalRequest);
        request.addParameter("q", "id:" + id);
        request.addParameter("hl", "false");
        request.addParameter("fl", "id,title,exactContent,url,llm_summary");
        request.setPathInfo("/select"); // TODO : vector search for chunking ?

        LOGGER.debug("AiPowered - Retrieving document {}.", id);
        return SearchAggregator.doGetSearch(request, null);
    }

    /**
     * Find a documents by its ID from Solr, using Datafari API methods.
     * This method uses an editable version of the original HttpServletRequest.
     * The updated request object is updated in order to process.
     * @param originalRequest : The HttpServletRequest
     * @param q The user query
     * @return a JSONObject containing search results, with the following fields :
     *      id, title, exactContent, url, llm_summary
     */
    private static JSONObject performSearch(HttpServletRequest originalRequest, String q, boolean vectorSearch) throws ServletException, IOException {
        EditableHttpServletRequest request = new EditableHttpServletRequest(originalRequest);
        request.addParameter("q", q);
        request.addParameter("hl", "false");
        request.addParameter("fl", "id,title,exactContent,url,llm_summary");

        String handler = vectorSearch ? "/vector" : "/select";
        request.setPathInfo(handler);

        LOGGER.debug("AiPowered - Performing search using {} handler. q={}", handler, q);
        return RagAPI.processSearch(RagConfiguration.getInstance(), request);
    }

    /**
     * @param code : the integer HTTP error code
     * @param message : The message of the error, displayed in response
     * @param ex : The Exception (if any), displayed in logs
     * @return a JSON error response
     */
    private static String generateErrorJson(int code, String errorLabel, String message, Exception ex) {
        final JSONObject response = new JSONObject();
        final JSONObject error = new JSONObject();
        final JSONObject content = new JSONObject();
        response.put("status", "ERROR");
        error.put("code", code);
        error.put("label", errorLabel);
        if (ex != null) error.put("reason", ex.getLocalizedMessage());
        content.put("message", message);
        content.put("documents", new ArrayList<>());
        content.put("error", error);
        response.put("content", content);

        LOGGER.debug("RagAPI - ERROR. An error occurred while processing the query.");
        LOGGER.debug("");
        LOGGER.debug("##### RAG final JSON response #####");
        LOGGER.debug(response.toJSONString());
        LOGGER.debug("###################################");
        LOGGER.debug("");

        return response.toJSONString();
    }

}
