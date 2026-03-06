/*******************************************************************************
 * Copyright 2015 France Labs
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.api;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.models.chatmodels.ChatModelFactory;
import com.francelabs.datafari.ai.models.chatmodels.ChatModelConfigurationManager;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.security.authentication.DisabledException;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class RagAPI extends SearchAPI {

    public static final String EXACT_CONTENT = "exactContent";
    public static final String EMBEDDED_CONTENT = "embedded_content";
    private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());

    public static ApiContent rag(final HttpServletRequest request, JSONObject searchResults,
                                 boolean ragBydocument, ChatStream stream, SourcesAccumulator sourcesAcc,
                                 boolean isTool) throws IOException {


        // Get RAG specific configuration
        RagConfiguration config = RagConfiguration.getInstance();

        // Set LlmService
        ChatModel chatModel = getChatModel(config);

        // Extract document content and data
        List<Document> initialDocumentsList;
        List<Document> documentsList;
        try {
            LOGGER.debug("RagAPI - Extracting documents list from search result...");
            initialDocumentsList = extractDocumentsList(searchResults);
            LOGGER.debug("RagAPI - {} documents extracted.", initialDocumentsList.size());
        } catch (FileNotFoundException e) {
            LOGGER.warn("RagAPI -  WARNING. The query cannot be answered because no associated documents were found.");
            if (ragBydocument) {
                return AiService.error(stream, "428", "ragNoFileFound",
                    "Sorry, the requested file does not exist, or is not available.",
                    e.getLocalizedMessage(), null, isTool); // TODO : retrieve conversation ID to save error messages
            } else {
                return AiService.error(stream, "428", "ragNoFileFound",
                    "Sorry, I couldn't find any relevant document to answer your request.",
                    e.getLocalizedMessage(), null, isTool);
            }
        }

        // Add the sources to the accumulator
        sourcesAcc.addAll(initialDocumentsList);

        // Chunking
        documentsList = ChunkUtils.chunkDocuments(initialDocumentsList, config);
        LOGGER.debug("RagAPI - The chunking returned {} chunk(s).", documentsList.size());

        // Prompting : Convert all documents chunks into prompt ChatMessages
        List<String> contents = PromptUtils.documentsListToMessages(documentsList);

        // Process the RAG query using the selected service, the contents list and the user query
        String message;
        try {
            message = processRagQuery(contents, chatModel, config, request, ragBydocument);
        } catch (DatafariServerException e) {
            LOGGER.error("An error occurred while calling external LLM service.", e);
            return AiService.error(stream, "500", "ragTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                e.getLocalizedMessage(), null, isTool);
        }catch (Exception e) {
            LOGGER.error("An unexpected error occurred while processing RAG query.", e);
            return AiService.error(stream, "500", "ragTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                e.getLocalizedMessage(), null, isTool);
        }

        LOGGER.debug("RagAPI - LLM response: {}", message);

        // Return final message
        if (message != null && !message.isEmpty()) {
            message = cleanLlmFinalMessage(message);
            return writeResponse(message);
        } else {
            return AiService.error(stream, "428", "ragNoValidAnswer",
                "Sorry, I could not find an answer to your question.", "LLM returned empty response.",
                null, isTool);
        }

    }


    /**
     * Start RAG process
     * content List of prompt ChatMessages containing documents chunks
     * chatModel the ChatLanguageModel
     * ragByDocument Does the query focus on one single document ?
     * @return The string LLM response
     */
    public static String processRagQuery(List<String> contents, ChatModel chatModel, RagConfiguration config,
                                         HttpServletRequest request, boolean ragBydocument) throws IOException, DatafariServerException {

        LOGGER.debug("RagAPI - Processing RAG query. {} chunk(s) received.", contents.size());
        LOGGER.debug("RagAPI - Processing RAG query : q={}.", request.getParameter("q"));
        if (ragBydocument) LOGGER.debug("RagAPI - RAG by Document ");

        // Retrieve and sanitize user query
        String userquery = PromptUtils.sanitizeInput(request.getParameter("q"));

        // Get history (if enabled)
        List<ChatMessage> chatHistory = PromptUtils.getChatHistoryToList(request, config);
        String chatHistoryStr = PromptUtils.getStringHistory(chatHistory);
        String conversationStr = PromptUtils.getStringHistoryLines(chatHistory);

        if ("mapreduce".equals(config.getProperty(RagConfiguration.PROMPT_CHUNKING_STRATEGY, "refine"))){

            // Map Reduce method
            LOGGER.debug("RagAPI - The sources associated to the RAG request will be processed using Map Reduce method.");

            // Send all chunks one by one
            List<ChatMessage> prompts;
            List<String> responseMessages = new ArrayList<>();
            String template = PromptUtils.getInitialRagTemplateMapReduce(request)
                    .replace(PromptUtils.USER_QUERY_TAG, userquery)
                    .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
                    .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
                    .replace(PromptUtils.CONVERSATION_TAG, conversationStr);
            String filledTemplate;
            String generatedResponse = "";

            int rqCounter = 0;
            while (!contents.isEmpty()) {
                rqCounter++;
                filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
                prompts = new ArrayList<>();
                ChatMessage prompt = new UserMessage(filledTemplate);
                prompts.add(prompt);

                generatedResponse = chatModel.chat(prompts).aiMessage().text();
                LOGGER.debug("RagAPI - Map Reduce - Last response : {}", generatedResponse);

                // Add the new response to the list
                String responseMessage = "* " + generatedResponse + "\n";
                responseMessages.add(responseMessage);
            }

            // If the LLM was called only once, no need to call it again
            if (rqCounter <= 1) return generatedResponse;

            // Then, merge all responses into one
            template = PromptUtils.getFinalRagTemplateMapReduce(request);
            filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, responseMessages, config);
            filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, userquery)
                    .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
                    .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
                    .replace(PromptUtils.CONVERSATION_TAG, conversationStr);

            prompts = new ArrayList<>();

            ChatMessage prompt = new UserMessage(filledTemplate);
            prompts.add(prompt);

            return chatModel.chat(prompts).aiMessage().text();

        } else {

            // Iterative Refining method
            LOGGER.debug("RagAPI - The sources associated to the RAG request will be processed using Iterative Refining method.");

      /*
        Variables:
        - snippets (formatted list of TextSegments)
        - userquery (userquery)
        - lastresponse (the last generated response)
        - format (Sentence enforcing the response format : bulletpoint, stepbystep, text, default... optional)
        - history (chat history, including prompt and conversation lines)
        - conversation : String formatted lines (e.g. "- user: hello world\n- assistant: Hello user !"
       */

            // Initial call with a fist set of snippets
            List<ChatMessage> prompts = new ArrayList<>();
            String template = PromptUtils.getInitialRagTemplateRefining(request);
            String filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
            filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, PromptUtils.cleanContext(userquery))
                    .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
                    .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
                    .replace(PromptUtils.CONVERSATION_TAG, conversationStr);

            ChatMessage prompt = new UserMessage(filledTemplate);
            prompts.add(prompt);
            String lastresponse = chatModel.chat(prompts).aiMessage().text();

            // Refining response with each snippet pack
            template = PromptUtils.getRefineRagTemplateRefining(request);
            while (!contents.isEmpty()) {
                filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
                filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, userquery)
                        .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
                        .replace(PromptUtils.LAST_RESPONSE_TAG, lastresponse)
                        .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
                        .replace(PromptUtils.CONVERSATION_TAG, conversationStr);
                prompts = new ArrayList<>();
                prompt = new UserMessage(filledTemplate);
                prompts.add(prompt);
                lastresponse = chatModel.chat(prompts).aiMessage().text();
                LOGGER.debug("RagAPI - Iterative Refining - Last generated response : {}", lastresponse);
            }
            return lastresponse;

        }

    }

    /**
     * Returns the active chat language model as defined in the models.json configuration file.
     *
     * @param config The RAG configuration object (currently unused, included for future compatibility).
     * @return A {@link ChatModel} instance corresponding to the active model.
     * @throws IOException If an error occurs while reading or parsing the model configuration file.
     */
    public static ChatModel getChatModel(RagConfiguration config) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createChatModel(); // Return the active model
    }
    public static StreamingChatModel getStreamingChatModel(RagConfiguration config) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createStreamingChatModel(); // Return the active model
    }

    /**
     * Returns a specific chat language model by name, as defined in the models.json configuration file.
     *
     * @param modelName The name of the model to load (matching the "name" field in the configuration).
     * @return A {@link ChatModel} instance corresponding to the specified model name.
     * @throws IOException If an error occurs while reading or parsing the model configuration file.
     * @throws IllegalArgumentException If no model is found with the given name.
     */
    private static ChatModel getSpecificChatModel(String modelName) throws IOException {
        ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
        ChatModelFactory chatModelFactory = new ChatModelFactory(configManager);
        return chatModelFactory.createChatModel(modelName); // Return a specific model
    }


    /**
     * Convert a natural-language user query into a search query, using the LLM
     * @param userQuery : The initial user query
     * @param request HttpServletRequest
     * @param config RagConfiguration
     * @return A ready-to-user search query
     */
    public static String rewriteSearchQuery(String userQuery, String retrievalMethod, final HttpServletRequest request, RagConfiguration config) throws IOException {

        LOGGER.debug("RagAPI - Rewriting user query into a search query. Initial query: {}", userQuery);
        List<ChatMessage> chatHistory = PromptUtils.getChatHistoryToList(request, config);
        String chatHistoryStr = PromptUtils.getStringHistoryLines(chatHistory);

        // Get the LLM interface (ChatLanguageModel)
        ChatModel chatModel = getChatModel(config);

        String template = PromptUtils.getRewriteQueryTemplate(request, retrievalMethod)
                .replace("{userquery}", userQuery)
                .replace("{conversation}", chatHistoryStr);
        List<ChatMessage> prompts = new ArrayList<>();
        prompts.add(new UserMessage(template)) ;

        try {
            String response = chatModel.chat(prompts).aiMessage().text();
            LOGGER.debug("RagAPI - Rewritten query: {}", response);
            return (response != null && !response.isEmpty()) && !"0".equals(response.trim()) ? response : userQuery;
        } catch (Exception e) {
            LOGGER.error("Query rewriting failed. Using initial user query for the search.", e);
            return userQuery;
        }


    }


    public static String summarize(final HttpServletRequest request, Document doc, ChatStream stream) throws IOException, DatafariServerException {

        LOGGER.debug("RagAPI - Summary for document {} requested.", doc.metadata().getString("id"));

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();
        ChatModel chatModel = getChatModel(config);

        // Check if summarization is enabled
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
            LOGGER.debug("AiPowered - Summarize - Summarization is disabled");
            throw new DisabledException("The summary generation feature is disabled.");
        }

        // Chunk document
        stream.phase("summarize:chunking");
        List<TextSegment> segments = ChunkUtils.chunkContent(doc, config);

        if (segments.isEmpty()) {
            LOGGER.debug("RagAPI - Summarize - No text segments found for document {}.", doc.metadata().getString("id"));
            throw new IOException("No content available for summarization.");
        }

        ArrayList<String> chunks = segments
                .stream()
                .map(TextSegment::text)
                .collect(Collectors.toCollection(ArrayList::new));
        int chunksNb = segments.size();

        // Setup prompt
        String initialPromptTemplate = PromptUtils.createInitialPromptForSummarization(request);
        String iterativePromptTemplate = PromptUtils.createPromptForIterateSummaries(request);


        // Summarize first chunk
        stream.phase("summarize:summarization");
        initialPromptTemplate = PromptUtils.stuffAsManySnippetsAsPossible(initialPromptTemplate, chunks, config);
        AiMessage responseMessage =  chatModel.chat(UserMessage.from(initialPromptTemplate)).aiMessage();
        String lastGeneratedSummary = responseMessage.text();

        // Refine iteratively the summary with each chunk
        while (!chunks.isEmpty()) {
            String progression = (chunksNb - chunks.size()) + "/" + chunksNb;
            stream.phase("summarize:" + progression);

            // Refine the summary for each chunk
            String iterativePrompt = PromptUtils.stuffAsManySnippetsAsPossible(
                    iterativePromptTemplate.replace("{summary}", lastGeneratedSummary),
                    chunks, config);

            responseMessage =  chatModel.chat(UserMessage.from(iterativePrompt)).aiMessage();
            lastGeneratedSummary = responseMessage.text();
        }
        stream.phase("summarize:done");

        // Return the last generated summary
        if (lastGeneratedSummary.length() > 1) {
            return lastGeneratedSummary;
        } else {
            LOGGER.debug("RagAPI - Summarize - Could not generate a summary for document {}. ", doc.metadata().getString("id"));
            throw new IOException("Could not generate any summary for this document");
        }

    }

    /**
     * Create a synthesis of multiple document, based on individual pre-generated summaries
     * @param request
     * @param documents List<Properties> - The list of documents
     * @param stream
     * @return A String synthesis
     */
    public static String synthesize(final HttpServletRequest request, List<Properties> documents, ChatStream stream) throws IOException, DatafariServerException {

        LOGGER.debug("RagAPI - Summary for document {} requested.", documents.size());

        // Get RAG configuration
        RagConfiguration config = RagConfiguration.getInstance();
        ChatModel chatModel = getChatModel(config);

        // Check if summarization is enabled
        if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
            LOGGER.debug("AiPowered - Synthesize - Summarization is disabled");
            throw new DisabledException("The summary generation feature is disabled.");
        }

        // Convert Properties to formatted prompt
        List<String> snippets = new ArrayList<>();
        for (Properties document : documents) {
            String title = document.getProperty("title", "-");
            String url = document.getProperty("url", "-");
            String id = document.getProperty("id");
            String summary = document.getProperty("summary", "-");
            String snippet = PromptUtils.synthesisSnippet(title, summary, id, url);

            snippets.add(snippet);
        }

        if (snippets.isEmpty()) {
            LOGGER.debug("RagAPI - Synthesize - No content available for synthesis.");
            throw new IOException("No content available for synthesis.");
        }

        int chunksNb = snippets.size();

        // Setup prompt
        String initialPromptTemplate = PromptUtils.createInitialPromptForSynthesis(request);
        String iterativePromptTemplate = PromptUtils.createPromptForIterateSynthesis(request);


        // First synthesis iteration
        stream.phase("synthesize:generation");
        initialPromptTemplate = PromptUtils.stuffAsManySnippetsAsPossible(initialPromptTemplate, snippets, config);
        AiMessage responseMessage =  chatModel.chat(UserMessage.from(initialPromptTemplate)).aiMessage();
        String lastGeneratedSynthesis = responseMessage.text();

        // Refine iteratively the summary with each chunk
        while (!snippets.isEmpty()) {
            String progression = (chunksNb - snippets.size()) + "/" + chunksNb;
            stream.phase("synthesize: generation (" + progression + ")");

            // Refine the summary for each chunk
            String iterativePrompt = PromptUtils.stuffAsManySnippetsAsPossible(
                iterativePromptTemplate.replace("{synthesis}", lastGeneratedSynthesis),
                snippets, config);

            responseMessage =  chatModel.chat(UserMessage.from(iterativePrompt)).aiMessage();
            lastGeneratedSynthesis = responseMessage.text();
        }
        stream.phase("synthesize:done");

        // Return the last generated summary
        if (lastGeneratedSynthesis.length() > 1) {
            return lastGeneratedSynthesis;
        } else {
            LOGGER.debug("RagAPI - synthesize - Could not generate a synthesis for {} documents.", documents.size());
            throw new IOException("Could not generate synthesis");
        }

    }

    public static ApiContent writeResponse(String message) {
        final ApiContent content = new ApiContent();
        content.message = message;
        return content;
    }

    /**
     *
     * @param message : The LLM String response
     * @return a cleaner String
     */
    private static String cleanLlmFinalMessage(String message) {
        message = message.replace("\\n", "\n");
        message = message.replace("/n", "\n");
        message = message.replace("\n ", "\n");
        message = message.replace("•", "\n•");
        message = message.replace("\n+", "\n");
        message = message.trim();

        return message;
    }

    /**
     *
     * @param result The raw Solr Search result
     * @return A List of Documents
     * @throws FileNotFoundException if the provided results doesn't contain any document
     */
    private static List<Document> extractDocumentsList(JSONObject result) throws FileNotFoundException {
        // Handling search results
        // Retrieving list of documents: id (docId), title, url
        List<Document> documentsList;
        documentsList = getDocumentList(result);

        if (documentsList.isEmpty()) {
            throw new FileNotFoundException("The query cannot be answered because no associated documents were found.");
        }
        return documentsList;
    }


    /**
     * Get a JSONArray containing a list of documents (ID, url, title and content) returned by the Search
     * @param result : The result of the search, containing Solr documents
     * @return JSONArray
     */
    private static List<Document> getDocumentList(JSONObject result) {
        try {

            JSONObject response = (JSONObject) result.get("response");
            List<Document> documentsList = new ArrayList<>();


            LOGGER.debug("RagAPI - Converting search results into a List of Documents.");
            if (response != null && response.get("docs") != null) {
                JSONArray docs = (JSONArray) response.get("docs");

                // Deduplicate by embedded_content
                if (docs.size() > 1) docs = dedupeByEmbeddedContent(docs);

                JSONObject jsonObject;
                for (Object doc : docs) {
                    jsonObject = (JSONObject) doc;
                    // Convert JSONObject to Langchain4j Document
                    Document document = SearchUtils.jsonToDocument(jsonObject);
                    if (document != null) {
                      documentsList.add(document);
                    }

                }
                return documentsList;
            }
        } catch (Exception e) {
            LOGGER.error("RagAPI - An error occurred while retrieving the list of documents.", e);
        }
        return new ArrayList<>();
    }

    /**
     * Deduplicating documents based on normalized embedded_content.
     * */
    @SuppressWarnings("unchecked")
    public static JSONArray dedupeByEmbeddedContent(JSONArray docs) {
        JSONArray out = new JSONArray();
        if (docs == null || docs.isEmpty()) return out;
        Set<String> seen = new HashSet<>();  // stocking hash to limit memory

        for (Object o : docs) {
            if (!(o instanceof JSONObject doc)) {
                continue;
            }

            String text = extractEmbeddedText(doc);
            if (text == null || text.isEmpty()) {
                // No basis for deduplicating, keeping the document
                out.add(doc);
            } else {
                String norm = normalize(text);
                String sig  = sha256(norm);  // stable sha256 signature
                if (seen.add(sig)) {
                    out.add(doc);  // First occurrence -> kept
                }
            }
        }
        return out;
    }

    /** Soft Normalisation: trim + compacting multiple spaces */
    private static String normalize(String s) {
        if (s == null) return "";
        // Replace multiple spaces by one space
        return s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
    }


    /** Retrieve embedded_content  */
    private static String extractEmbeddedText(JSONObject d) {
        Object val = d.get("embedded_content");
        if (val == null) return null;
        String s = String.valueOf(val);
        return s.isEmpty() ? null : s;
    }

    /** Hash SHA-256 en hex (pour éviter de stocker de gros textes dans le Set). */
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return s;
        }
    }

}
