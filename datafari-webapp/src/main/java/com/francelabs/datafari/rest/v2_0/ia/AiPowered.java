package com.francelabs.datafari.rest.v2_0.ia;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.api.RagAPI;
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

@RestController
public class AiPowered {

    private static final Logger LOGGER = LogManager.getLogger(AiPowered.class.getName());
    private static final String ID_FIELD = "id";
    private static final String TITLE_FIELD = "title";
    private static final String URL_FIELD = "url";
    private static final String LLM_SUMMARY_FIELD = "llm_summary";
    public static final String EXACT_CONTENT_FIELD = "exactContent";

    @PostMapping("/rest/v2.0/ia/summarize")
    public String summarizeDocument(final HttpServletRequest request, @RequestBody JSONObject jsonDoc) {

        String id;
        String title;
        String content;
        String summary;
        String url;
        final JSONObject jsonResponse = new JSONObject();

        // Retrieve ID from JSON input
        if (jsonDoc.get(ID_FIELD) != null) {
            id = (String) jsonDoc.get(ID_FIELD);
        } else {
            return generateErrorJson(422, "Please provide the ID of the document to summarize.", null);
        }

        LOGGER.info("Summary of the document {} requested.", id);

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
                return generateErrorJson(422, "The document cannot be retrieved.", null);
            }

        } catch (final NullPointerException e) {
            return generateErrorJson(422, "The document cannot be retrieved.", null);
        } catch (final Exception e) {
            return generateErrorJson(500, e.getMessage(), e);
        }


        try {
            // Process the document using RagAPI methods
            return getDocumentSummary(request, summary, content, id, title, url, jsonResponse);

            //  TODO: This feature can be improved by implementing Atomic Update.
            //  TODO: When a summary is generated in the method, the Solr Document from FILESHARE should be updated.


        } catch (final Exception e) {
            return generateErrorJson(500, e.getMessage(), e);
        }
    }

    /**
     * Generate a
     * @param request HttpServletRequest
     * @param summary String The summary of the documents if it already exists
     * @param content String The exactContent of the document
     * @param id String
     * @param title String
     * @param url String
     * @param jsonResponse String
     * @return a response JSONObject
     */
    private String getDocumentSummary(HttpServletRequest request, String summary, String content, String id, String title, String url, JSONObject jsonResponse) throws IOException {
        if ((summary == null || summary.isEmpty()) && (content != null && !content.isEmpty())) {
            // If there is no existing summary, but content is found, use RagAPI service to generate a summary
            LOGGER.debug("No summary found for file {}", id);

            // Instantiate a Langchain4j Document
            Metadata metadata = new Metadata();
            metadata.put(ID_FIELD, id);
            if (title != null) metadata.put(TITLE_FIELD, title);
            if (url != null) metadata.put(URL_FIELD, url);
            Document doc = new Document(content, metadata);

            summary = RagAPI.summarize(request, doc);
            jsonResponse.put("status", "OK");
            jsonResponse.put("content", summary);
            return jsonResponse.toJSONString();

        } else if (content == null || content.isEmpty()) {
            // Error : No content, no summary
            LOGGER.warn("Could not retrieve summary or content from file {}.", id);
            return generateErrorJson(422, "Unable to generate a summary, since the file has no content.", null);
        } else {
            jsonResponse.put("status", "OK");
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
        request.setPathInfo("/select");

        final JSONObject jsonResponse = SearchAggregator.doGetSearch(request, null);
        return jsonResponse;
    }

    /**
     * @param code : the integer HTTP error code
     * @param message : The message of the error, displayed in response
     * @param e : The Exception (if any), displayed in logs
     * @return a JSON error response
     */
    private String generateErrorJson(int code, String message, Exception e) {
        final JSONObject jsonResponse = new JSONObject();
        LOGGER.error("An error occurred: {}", message, e);
        final JSONObject error = new JSONObject();
        error.put("code", code);
        error.put("message", message);
        jsonResponse.put("error", error);
        return jsonResponse.toJSONString();
    }

}
