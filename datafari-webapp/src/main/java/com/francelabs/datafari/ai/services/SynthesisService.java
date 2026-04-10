package com.francelabs.datafari.ai.services;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.services.summarization.Summarization;
import com.francelabs.datafari.ai.services.synthesis.Synthesis;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SynthesisService extends AiService {

    private static final Logger LOGGER = LogManager.getLogger(SynthesisService.class.getName());

    public static @NotNull ApiContent synthesize(AiRequest params, HttpServletRequest request, ChatStream stream,
                                                           boolean isTool) {
        RagConfiguration config = RagConfiguration.getInstance();

        stream.phase("synthesis:start");
        // Retrieve language
        if (params.lang != null) {
            request.setAttribute("lang", params.lang);
        }
        ApiContent response = new ApiContent();

        // Retrieve the IDs list and truncate it if needed
        List<String> ids = params.filters.get("id");
        if (ids.size() > config.getIntegerProperty(RagConfiguration.SYNTHESIS_MAX_FILES, 5)) {
            ids = ids.subList(0, config.getIntegerProperty(RagConfiguration.SYNTHESIS_MAX_FILES, 5));
        }
        int basketSize = ids.size();
        List<Properties> documents = new ArrayList<>();

        if (!isTool) {
            // Save message in Postgresql DB
            if (params.query == null || params.query.isBlank()) params.query = "Write a synthesis of these documents"; // TODO : translate label if possible
            params.conversationId = saveUserMessage(request, params);
        }

        LOGGER.info("AiPowered - Synthesize - synthesis of {} documents requested.", basketSize);
        // TODO: implement a max limit on the number of documents

        // Retrieve documents from Solr using Datafari API methods
        int index = 0;
        boolean hasContentToSynthesize = false;
        for (String id : ids) {
            try {
                // Retrieve document
                index++;
                stream.phase("synthesis:documents retrieval (" + index + "/" + basketSize + ")");
                JSONObject searchresult = SearchUtils.findDocumentById(request, id);
                JSONObject jsonAiDocument = (JSONObject) ((JSONArray) ((JSONObject) searchresult.get("response")).get("docs")).getFirst();

                Properties document = new Properties();
                if (jsonAiDocument.get("docId") != null && jsonAiDocument.get(AiService.EXACT_CONTENT_FIELD) != null) {
                    document.put("id", jsonAiDocument.get("docId"));
                    document.put("title", ((JSONArray) jsonAiDocument.get(AiService.TITLE_FIELD)).getFirst());
                    document.put("url", jsonAiDocument.get(AiService.URL_FIELD));
                    document.put("content", ((JSONArray) jsonAiDocument.get(AiService.EXACT_CONTENT_FIELD)).getFirst());
                    if (jsonAiDocument.get(AiService.LLM_SUMMARY_FIELD) != null) document.put("summary", jsonAiDocument.get(AiService.LLM_SUMMARY_FIELD));
                    hasContentToSynthesize = true;
                } else {
                    document.put("id", id);
                    document.put("summary", "Could not retrieve the document");
                }
                documents.add(document);

            } catch (final NullPointerException|IndexOutOfBoundsException e) {
                // Skip the entry
                LOGGER.warn("The document {} could not be retrieved.", id, e);
            } catch (final Exception e) {
                LOGGER.warn("The document {} could not be retrieved.", id, e);
            }

        }

        // If no doc retrieved
        if (documents.isEmpty() || !hasContentToSynthesize) {
            return error(stream, "422",
                ApiError.SYNTHESIS_NO_FILE_CONTENT.getKey(),
                ApiError.SYNTHESIS_NO_FILE_CONTENT.getValue(),
                "Files is empty or not found.", params.conversationId, isTool);
        }

        // Get individual summaries for each document
        index = 0;
        for (Properties document : documents) {
            index++;
            try {
                // Retrieve document
                stream.phase("synthesis:summary generation (" + index + "/" + documents.size() + ")");

                // Retrieve an existing summary or generate a new one, and update document object
                getDocumentSummary(request, document, stream);

            } catch (EmptyFileException e) {
                // non-blocking error
                LOGGER.warn("The document {} is empty and can not be summarized.", document.get("id"), e);
            } catch (Exception e) {
                LOGGER.warn("The document {} summarization failed. Ignoring it in the synthesis.", document.get("id"), e);
            }
        }


        // Generate a synthesis based on all summaries
        try {
            String synthesis = Synthesis.synthesize(request, documents, stream);

            if (synthesis != null && !synthesis.isBlank()) {
                response.message = synthesis;
            }

        } catch (DatafariServerException e) {
            return error(stream, "500",
                ApiError.SYNTHESIS_TECHNICAL_ERROR.getKey(),
                ApiError.SYNTHESIS_TECHNICAL_ERROR.getValue(),
                e.getLocalizedMessage(), params.conversationId, isTool);
        } catch (IOException e) {
            return error(stream, "500",
                ApiError.SYNTHESIS_TECHNICAL_ERROR.getKey(),
                ApiError.SYNTHESIS_TECHNICAL_ERROR.getValue(),
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
     * @param document Properties an object containing properties of the doc (title, url, summary, id...)
     * The "document" param is updated with the new summary
     */
    private static void getDocumentSummary(HttpServletRequest request, Properties document, ChatStream stream) throws IOException, DatafariServerException {
        String id = (document.get("id") != null) ? (String) document.get("content") : null;

        if (document.get("summary") != null && !((String) document.get("summary")).isEmpty()) {
            return;
        } else if (document.get("content") != null && !((String) document.get("content")).isEmpty()) {
            // If there is no existing summary, but content is found, use Summarization service to generate a summary
            LOGGER.debug("AiPowered - Synthesize - No summary found for document {}.", id);

            // Instantiate a Langchain4j Document
            Metadata metadata = new Metadata();
            metadata.put(ID_FIELD, id);
            if (document.get("title") != null) metadata.put(TITLE_FIELD, (String) document.get("title"));
            if (document.get("url") != null) metadata.put(URL_FIELD, (String) document.get("url"));
            Document doc = Document.from((String) document.get("content"), metadata);

            LOGGER.debug("AiPowered - Synthesize - Generating a summary for document {}.", id);

            // Run the summarization
            document.setProperty("summary", Summarization.summarize(request, doc, stream));

        } else {
            LOGGER.warn("AiPowered - Synthesize - Could not retrieve summary or content from file {}.", id);
            // No content, no summary
            document.setProperty("summary", "This file has no available context and cannot be summarize.");
        }
    }
}
