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
package com.francelabs.datafari.utils.rag;

import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rag.Message;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.user.Lang;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.*;

/**
 * Prompt Utility class for RAG
 *
 * @author France Labs
 *
 */
public class PromptUtils {

    private static final Logger LOGGER = LogManager.getLogger(PromptUtils.class.getName());
    public static final String SNIPPETS_TAG = "{snippets}";
    public static final String SYSTEM_ROLE = "system";
    public static final String USER_QUERY_TAG = "{userquery}";
    public static final String FORMAT_TAG = "{format}";

    private PromptUtils() {
        // Constructor
    }

    /**
     * Transform a list of Documents (content + metadata) into a list of prompts
     *
     * @param documentsList : A JSONArray list of documents
     * @return a prompt ready to be sent to the LLM service
     */
    public static List<Message> documentsListToMessages(List<Document> documentsList) throws IOException {

        List<Message> prompts = new ArrayList<>();
        for (Document document : documentsList) {
            // format document
            String content = formatDocument(document.metadata().getString("title"), document.text());
            // create prompt
            Message message = new Message(SYSTEM_ROLE, content);
            prompts.add(message);
        }

        return prompts;
    }

    /**
     * @return Retrieve the instructions used to summarize a document.
     */
    public static Message createInitialPromptForSummarization(HttpServletRequest request) throws IOException {
        String prompt = getInstructions("summarization/template-initialPromptForSummarization.txt")
                .replace("{language}", getUserLanguage(request));
        return new Message("system", prompt);
    }

    /**
     * @return Retrieve the instructions used to merge multiple summaries into one.
     */
    public static Message createPromptForMergeAllSummaries(HttpServletRequest request) throws IOException {
        String prompt =  getInstructions("summarization/template-summarization-mergeAll.txt")
                .replace("{language}", getUserLanguage(request));
        return new Message("user", prompt);
    }


    /**
     * @return Retrieve the instructions for the initial request of Refining method.
     */
    public static String getInitialRagTemplateRefining(HttpServletRequest request) throws IOException {
        return getInstructions("rag/template-refine-initial.txt")
                .replace("{language}", getUserLanguage(request));
    }


    /**
     * @return Retrieve the instructions for the initial request of Refining method.
     */
    public static String getRefineRagTemplateRefining(HttpServletRequest request) throws IOException {
        return getInstructions("rag/template-refine-refining.txt")
                .replace("{language}", getUserLanguage(request));
    }

    /**
     * @return Retrieve the instructions for the initial request of Map Reduce method.
     */
    public static String getInitialRagTemplateMapReduce(HttpServletRequest request) throws IOException {
        return getInstructions("rag/template-rag.txt")
                .replace("{language}", getUserLanguage(request));
    }

    /**
     * @return Retrieve the instructions for the initial request of Map Reduce method.
     */
    public static String getFinalRagTemplateMapReduce(HttpServletRequest request) throws IOException {
        return getInstructions("rag/template-mergeAllRag.txt")
                .replace("{language}", getUserLanguage(request));
    }


    public static String getRewriteQueryTemplate(HttpServletRequest request) throws IOException {
        return getInstructions("rag/template-rewriteSearchQuery.txt")
                .replace("{language}", getUserLanguage(request));
    }

    /**
     * Generate a string prompt containing a chunk of document and the document title
     * @param title Title of the document
     * @param content Chunk of document
     * @return String prompt for the LLM
     * @throws IOException
     */
    public static String formatDocument(String title, String content) throws IOException {
        String template =  getInstructions("rag/template-fromTextSegment.txt");
        return template.replace("{title}", title).replace("{content}", content);
    }

    public static String getResponseFormat(HttpServletRequest request) {

        String format = request.getParameter("format");
        if (format == null) return "";
        switch (format) {
            case "stepbystep":
                return " If relevant, your response should take the form step-by-step instructions.\n";
            case "bulletpoint":
                return " If relevant, your response should take the form of a bullet-point list.\n";
            case "text":
                return " If relevant, your response should take the form of a text.\n";
            default:
                return "";
        }
    }

    /**
     * Fill a provided prompt template with as many snippets as possible, without exceeding the limit set in
     * prompt.max.request.size (rag.properties). The snippet list replaces the require {snippets} tag.
     * @param template : A String prompt template, containing the {snippets} tag
     * @param contents : A list of formatted message, each containing one chunk/snippet
     * @param config : The RagConfiguration
     * @return : The original template, filled with as many snippets as possible.
     * @throws DatafariServerException : The template is missing the  {snippets} tag.
     */
    public static String stuffAsManySnippetsAsPossible(String template, List<Message> contents, RagConfiguration config, int historySize) throws DatafariServerException {

        if (!template.contains(SNIPPETS_TAG)) throw new DatafariServerException(CodesReturned.GENERALERROR, "Invalid prompt template: {snippets} tag is missing.");

        StringBuilder snippets = new StringBuilder();
        String prompt = template.replace(SNIPPETS_TAG, snippets.toString());
        List<Message> processedSnippets = new ArrayList<>();
        int i = 0;

        for (Message message : contents) {
            // Adding a snippet to the list, and check if the length is not exceeding the limit
            String snippet = message.getContent();
            snippets.append("\n").append(snippet);
            if (template.replace(SNIPPETS_TAG, snippets.toString()).length() < config.getIntegerProperty(RagConfiguration.MAX_REQUEST_SIZE)) {
                prompt = template.replace(SNIPPETS_TAG, snippets.toString());
                processedSnippets.add(message);
                i++;
            } else {
                break;
            }
        }

        // If the list is empty due to an excessive content size, the first chunk is stuffed in
        if (processedSnippets.isEmpty() && !contents.isEmpty()) {
            i = 0;
            snippets = new StringBuilder(contents.get(0).getContent());
            prompt = template.replace(SNIPPETS_TAG, snippets.toString());
            processedSnippets.add(contents.get(0));
        }

        // Remove the processed snippets from contents
        contents.removeAll(processedSnippets);

        LOGGER.debug("{} chunks processed in an LLM request. {} more to go.", i, contents.size());
        return prompt;
    }

    /**
     * @param context The context, containing documents content
     * @return A clean context, with no characters or element that could cause an error or a prompt injection
     */
    public static String cleanContext(String context) {
        context = context.replace("\\", "/")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("\b", "")
                .replace("\"", "`");
        return context;
    }

    /**
     * @param query The user query
     * @return A clean query, with no characters or element that could cause an error or a prompt injection
     */
    public static String sanitizeInput(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        // 1. Trim whitespace
        query = query.trim();

        // Normalize Unicode characters (é → e)
        query = Normalizer.normalize(query, Normalizer.Form.NFD);
        query = query.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove control characters (including newlines)
        query = query.replaceAll("\\p{Cntrl}", " ");

        // Escape or neutralize Lucene/Solr special characters
        // These characters have special meaning in Solr query parsers (e.g., edismax)
        // They may also have side effects with the LLM
        // Here, we replace them by space to avoid misparsing
        String[] specialChars = {
                "+", "&&", "||", "{", "}", "[", "]",
                "^", "~", "*", "\\", "<", ">", "=", "#"
        };
        for (String ch : specialChars) {
            query = query.replace(ch, " ");
        }
        // Replace multiple whitespace with single space
        query = query.replaceAll("\\s+", " ");
        // Length limit for user query arbitrarily set to 500 char
        int maxLength = 500;
        if (query.length() > maxLength) {
            query = query.substring(0, maxLength);
        }

        return query.trim();
    }

    /**
     * @return The instructions prompts stored in resources/prompts folder
     */
    private static String getInstructions(String filename) throws IOException {
        return readFromInputStream(RagAPI.class.getClassLoader().getResourceAsStream("prompts/" + filename));
    }

    /**
     *
     * @param segment A TextSegment to convert to a Message
     * @param role The role of the Message sender. Defaut is "user".
     * @return A list of TextSegments, that contain metadata. Big documents are chunked into multiple documents.
     */
    public static Message textSegmentsToMessage(TextSegment segment, String role, RagConfiguration config) throws IOException {
        String template = getInstructions("rag/template-fromTextSegment.txt");
        Metadata metadata = segment.metadata();
        String content = cleanContext(segment.text());
        template = template.replace("{content}", content);
        template = template.replace("{id}", metadata.getString("id"));
        template = template.replace("{title}", metadata.getString("title"));
        template = template.replace("{url}", metadata.getString("url"));
        return new Message(role, template);
    }

    /**
     * Transform InputStream into String
     * @param inputStream InputStream to transform
     * @return String
     * @throws IOException : the InputStream is null or could not be read
     */
    private static String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }


    /**
     * Return the preferred language or the user, or by defaut, his browser language
     * @param request : Http
     * @return a prompt ready to be sent to the LLM service
     */
    public static String getUserLanguage(HttpServletRequest request) {

        final String authenticatedUserName = AuthenticatedUserName.getName(request);
        try {
            // Retrieving user language from request parameters
            String lang = getDisplayedName(request.getParameter("lang"));

            // Retrieving user language from request attributes
            if (lang.isEmpty() && request.getAttribute("lang") != null)
                lang = getDisplayedName((String) request.getAttribute("lang"));

            // If no language is provided in the GET parameters, retrieving user language from Cassandra lang database
            if (lang.isEmpty()) lang = getDisplayedName(Lang.getLang(authenticatedUserName));
            if (lang.isEmpty()) throw new DatafariServerException(CodesReturned.ALLOK, "");
            return lang;
        } catch (final DatafariServerException e) {
            // If the language retrieving failed, use English as default
            Locale locale = request.getLocale();
            return locale.getDisplayLanguage(Locale.ENGLISH);
        }
    }

    public static String getDisplayedName(String lang) {
        if (lang == null) return "";
        switch (lang) {
            case "en":
                return "English";
            case "fr":
                return "French";
            case "it":
                return "Italian";
            case "pt":
            case "pt_br":
                return "Portuguese";
            case "de":
                return "German";
            case "es":
                return "Spanish";
            case "ru":
                return "Russian";
            default:
                return "";
        }
    }

    /**
     * Returns the total size (in character) of a list of prompts (List<Message>)
     * @return The total size of the final prompt (int)
     */
    public static int getTotalPromptSize(List<Message> messages) {
        int size = 0;
        for (Message message : messages) {
            size = size + message.getContent().length();
        }
        return size;
    }

    public static List<Message> getChatHistoryToList(HttpServletRequest request, RagConfiguration config) {

        List<Message> history = new ArrayList<>();

        if (!config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED)) return history;

        Object historyAttribute = request.getAttribute("history");
        if (historyAttribute instanceof ArrayList<?>) {
            ArrayList<?> historyList  = (ArrayList<?>) historyAttribute;
            try {
                // Get Message list from JSONArray
                history = new ArrayList<>();

                // LinkedHashmap to Message
                for (Object obj : historyList ) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (LinkedHashMap<String, String>) obj;
                        if (map.containsKey("role") && map.containsKey("content")) {
                            Message message = new Message(map.get("role"), map.get("content"));
                            history.add(message);
                        } else {
                            LOGGER.warn("A message from the chat history in invalid, and will be ignored.");
                        }
                    }
                }

                // The number of history message must not exceed the limit set in chat.memory.history.size (rag.properties)
                int historyMaxSize = config.getIntegerProperty(RagConfiguration.CHAT_MEMORY_HISTORY_SIZE, 6);
                if (history.size() > historyMaxSize) {
                    history = history.subList(history.size() - historyMaxSize, history.size());
                }

                return history;
            } catch (Exception e) {
                LOGGER.warn("The conversation history could not be read. Chat memory is ignored.", e);
            }
        } else {
            LOGGER.debug("PromptUtils - RAG - No valid chat history found in request. ");
        }
        return history;
    }

    public static int getHistorySize(List<Message> history, RagConfiguration config) {
        if (!config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED)) {
            return 0;
        } else {
            return getTotalPromptSize(history);
        }
    }
}
