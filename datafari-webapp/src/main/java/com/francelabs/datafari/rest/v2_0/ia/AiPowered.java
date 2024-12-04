package com.francelabs.datafari.rest.v2_0.ia;

import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.service.indexer.IndexerResponseDocument;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import dev.langchain4j.data.document.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class AiPowered {

    private static final Logger LOGGER = LogManager.getLogger(AiPowered.class.getName());
    private static final String LLM_SUMMARY_FIELD = "llm_summary";
    public static final String EXACT_CONTENT = "exactContent";

    @GetMapping("/rest/v2.0/ia/summarize")
    public String summarizeDocument(final HttpServletRequest request) {

        String id = request.getParameter("id"); // The ID of the target document
        IndexerResponseDocument doc; // The target document
        String summary; // The summary of the document
        final JSONObject jsonResponse = new JSONObject();

        try {

            // Retrieve document
            IndexerServer solr = IndexerServerManager.getIndexerServer(IndexerServerManager.Core.FILESHARE);
            doc = solr.getDocById(id);
            if (doc == null)
                return generateErrorJson(422, "The document cannot be retrieved.", null);

            summary = (String) doc.getFieldValue(LLM_SUMMARY_FIELD);
            String content = (String) doc.getFieldValue(EXACT_CONTENT);

            if ((summary == null || summary.isEmpty()) && (content != null && !content.isEmpty())) {
                // Summary not found in Solr Document, but existing content
                LOGGER.debug("No summary found for file {}", id);
                Metadata metadata = getMetadataFromSolrDoc(doc);
                summary = RagAPI.summarize(request, content, metadata);
                jsonResponse.put("status", "OK");
                jsonResponse.put("content", summary);

            } else if (content == null || content.isEmpty()) {
                // Error : No content, no summary
                LOGGER.error("Could not retrieve summary or content from file {}.", id);
                return generateErrorJson(422, "Unable to generate a content, since the file has no content.", null);
            }

            //  TODO: This feature can be improved by implementing Atomic Update.
            //  TODO: When a summary is generated in the method, the Solr Document from FILESHARE should be updated.


        } catch (final Exception e) {
            return generateErrorJson(500, "", e);
        }

        return jsonResponse.toJSONString();
    }

    private Metadata getMetadataFromSolrDoc(IndexerResponseDocument doc) {

        Metadata metadata = new Metadata();
        String title = doc.getFieldValues("title").toString();
        String id = doc.getFieldValues("id").toString();
        String url = doc.getFieldValues("url").toString();

        if (title != null) metadata.put("title", title);
        if (id != null) metadata.put("id", id);
        if (url != null) metadata.put("url", url);
        return metadata;
    }

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
