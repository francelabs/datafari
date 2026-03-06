package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EmptyFileException;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SummarizationService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(SummarizationService.class.getName());

    public static @NotNull ApiContent summarize(AiRequest params, HttpServletRequest request, ChatStream stream,
                                                boolean isTool) {
        ApiContent response = new ApiContent();

        String id = params.id;
        String lang = params.lang;
        String title;
        String content;
        String summary;
        String url;

        stream.phase("summarize:start");

        // Retrieve language
        if (lang != null) {
            request.setAttribute("lang", lang);
        }

        if (!isTool) {
            // Save message in Postgresql DB
            if (params.query == null || params.query.isBlank()) params.query = "Summarize this document"; // TODO : translate label if possible
            params.conversationId = saveUserMessage(request, params);
        }

        LOGGER.info("AiPowered - Summarize - Summary of the document {} requested.", id);

        // Retrieve document from Solr using Datafari API methods
        try {

            // Retrieve document
            stream.phase("summarize:document retrieval");
            JSONObject searchresult = SearchUtils.findDocumentById(request, id);
            JSONObject jsonAiDocument = (JSONObject) ((JSONArray) ((JSONObject) searchresult.get("response")).get("docs")).get(0);

            if (jsonAiDocument.get("docId") != null) {
                title = (String) ((JSONArray) jsonAiDocument.get(AiService.TITLE_FIELD)).getFirst();
                url = (String) jsonAiDocument.get(AiService.URL_FIELD);
                summary = (String) jsonAiDocument.get(AiService.LLM_SUMMARY_FIELD);
                content = (String) ((JSONArray) jsonAiDocument.get(AiService.EXACT_CONTENT_FIELD)).getFirst();
            } else {
                return error(stream, "422", "summarizationNoFileFound",
                        "The document cannot be retrieved.",
                        "No document found for the provided ID.", params.conversationId, isTool);
            }

        } catch (final NullPointerException|IndexOutOfBoundsException e) {
            return error(stream, "422", "summarizationNoFileFound",
                    "The document cannot be retrieved.", e.getMessage(), params.conversationId, isTool);
        } catch (final Exception e) {
            return error(stream, "500", "summarizationTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                e.getLocalizedMessage(), params.conversationId, isTool);
        }


        try {
            // Retrieve an existing summary or generate a new one
            stream.phase("summarize:generation");
            response.message = getDocumentSummary(request, summary, content, id, title, url, stream);
        } catch (EmptyFileException e) {
            return error(stream, "422", "summarizationEmptyFile",
                    "Sorry, I am unable to generate a summary, since the file has no content.",
                "File is empty and can not be summarized.", params.conversationId, isTool);
        } catch (Exception e) {
            return error(stream, "500", "summarizationTechnicalError",
                "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                e.getLocalizedMessage(), params.conversationId, isTool);
        }

        if (!isTool) {
            // Save message in Postgresql DB
            response.conversationId = params.conversationId;
            saveAssistantMessage(request, response);
        }

        return response;
    }

    /**
     * Generate a summary for a document, or returns it if it already exists
     * @param request HttpServletRequest
     * @param summary String The summary of the documents if it already exists
     * @param content String The exactContent of the document
     * @param id String
     * @param title String
     * @param url String
     * @return a response JSONObject
     */
    private static String getDocumentSummary(HttpServletRequest request, String summary, String content, String id,
                                             String title, String url, ChatStream stream) throws IOException, DatafariServerException {
        if (summary != null && !summary.isEmpty()) {
            return summary;
        } else if (content != null && !content.isEmpty()) {
            // If there is no existing summary, but content is found, use RagAPI service to generate a summary
            LOGGER.debug("AiPowered - Summarize - No summary found for document {}.", id);

            // Instantiate a Langchain4j Document
            Metadata metadata = new Metadata();
            metadata.put(ID_FIELD, id);
            if (title != null) metadata.put(TITLE_FIELD, title);
            if (url != null) metadata.put(URL_FIELD, url);
            Document doc = Document.from(content, metadata);

            LOGGER.debug("AiPowered - Summarize - Generating a summary for document {}.", id);
            return RagAPI.summarize(request, doc, stream);
        } else {
            LOGGER.warn("AiPowered - Summarize - Could not retrieve summary or content from file {}.", id);
            // Error : No content, no summary
            throw new EmptyFileException();
        }
    }
}
