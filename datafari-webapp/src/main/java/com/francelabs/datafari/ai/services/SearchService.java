package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EmptyFileException;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

/**
 * The SearchService can be used to run a search from the chatbot.
 * Results are streamed in a "search.result" event.
 */
public class SearchService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(SearchService.class.getName());

    public static @NotNull ApiContent search(AiRequest params, HttpServletRequest request, ChatStream stream) {
        ApiContent response = new ApiContent();

        String query = params.query;

        stream.phase("search:start");

        // Run a search in Datafari
        JSONArray docs = runSearch(request, query, stream);

        // Format response
        JSONArray formattedDocs = new JSONArray();
        for (Object d : docs) {
            JSONObject doc = simplifyJsonDocs((JSONObject) d);
            formattedDocs.add(doc);
        }

        // Stream the results
        stream.searchResults(formattedDocs);

        return response;
    }

    private static @NonNull JSONObject simplifyJsonDocs(JSONObject d) {
        String docId = (String) d.get("docId");
        String url = (String) d.get(AiService.URL_FIELD);

        // Retrieve first title from multivalued field
        String title;
        Object t = d.get(AiService.TITLE_FIELD);
        if (t instanceof JSONArray ja && !ja.isEmpty()) {
            title = String.valueOf(ja.getFirst());
        } else {
            title = String.valueOf(t != null ? t : "");
        }

        // Retrieve content from multivalued field
        String content;
        Object embeddedContent = d.get("embedded_content");
        if (embeddedContent instanceof JSONArray ja && !ja.isEmpty()) {
            content = String.valueOf(ja.getFirst());
        } else {
            content = String.valueOf(embeddedContent != null ? embeddedContent : "");
        }

        // Truncate content if too long
        if (content.length() > 300) {
            content = content.substring(0, 297) + "...";
        }

        JSONObject doc = new JSONObject();
        doc.put("docId", docId);
        doc.put("title", title);
        doc.put("url", url);
        doc.put("content", content);
        return doc;
    }

    /**
     * Generate a summary for a document, or returns it if it already exists
     * @param request HttpServletRequest
     * @param query The String search query
     * @param stream The ChatStream
     * @return a response JSONObject
     */
    private static JSONArray runSearch(HttpServletRequest request, String query, ChatStream stream) {

        LOGGER.info("AGENTIC TOOLS - BM25 Search - Query: {}", query);
        int rows = 5;

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", query);
        req.addParameter("fl", "title,docId,url,embedded_content:content_fr,embedded_content:content_en,embedded_content:content_es,embedded_content:content_de");
        req.addParameter("q.op", "OR");
        req.addParameter("start", "0");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");
        JSONObject root = SearchUtils.processSearch(req, handler);
        return SearchUtils.extractDocs(root);
    }
}
