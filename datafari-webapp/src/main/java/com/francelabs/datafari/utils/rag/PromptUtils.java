/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
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
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prompt Utility class for RAG
 *
 * @author France Labs
 *
 */
public class PromptUtils {

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
            Message message = new Message("system", content);
            prompts.add(message);
        }

        return prompts;
    }


    /**
     * Create a full prompt usable by a LLM, user the user prompt, the provided content and the configuration
     * @param config : Global RAG configuration
     * @param request : The request object
     * @return a prompt ready to be sent to the LLM service
     */
    public static Message createInitialRagPrompt(RagConfiguration config, HttpServletRequest request, boolean ragBydocument) throws IOException {

        // Retrieve the initial prompt template from instructions file.
        String template = ragBydocument ?
                getInstructions("template-ragByDocument.txt") : getInstructions("template-rag.txt");

        template = template.replace("{format}", getResponseFormat(request));
        template = template.replace("{language}", getUserLanguage(request));

        return new Message("system", cleanContext(template, config));
    }

    /**
     * Create a full prompt usable by a LLM, user the user prompt, the provided content and the configuration
     * @param config : Global RAG configuration
     * @param request : The request object
     * @return a prompt ready to be sent to the LLM service
     */
    public static Message createPromptForMergeAllRag(RagConfiguration config, HttpServletRequest request, boolean ragBydocument) throws IOException {

        // Retrieve the initial prompt template from instructions file.
        String template = ragBydocument ?
                getInstructions("template-mergeAllRagByDocument.txt") : getInstructions("template-mergeAllRag.txt");

        template = template.replace("{format}", getResponseFormat(request));
        template = template.replace("{language}", getUserLanguage(request));

        return new Message("system", cleanContext(template, config));
    }

    /**
     * @return Retrieve the instructions used to summarize a document.
     */
    public static Message createInitialPromptForSummarization(HttpServletRequest request) throws IOException {
        String prompt = getInstructions("template-initialPromptForSummarization.txt")
                .replace("{language}", getUserLanguage(request));
        return new Message("system", prompt);
    }

    /**
     * @return Retrieve the instructions used to merge multiple summaries into one.
     */
    public static Message createPromptForMergeAllSummaries(HttpServletRequest request) throws IOException {
        String prompt =  getInstructions("template-mergeAllSummaries.txt")
                .replace("{language}", getUserLanguage(request));
        return new Message("user", prompt);
    }

    /**
     * Generate a string prompt containing a chunk of document and the document title
     * @param title Title of the document
     * @param content Chunk of document
     * @return String prompt for the LLM
     * @throws IOException
     */
    public static String formatDocument(String title, String content) throws IOException {
        String template =  getInstructions("template-fromTextSegment.txt");
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
     * @param context The context, containing documents content
     * @return A clean context, with no characters or element that could cause an error or a prompt injection
     */
    private static String cleanContext(String context, RagConfiguration config) {
        context = context.replace("\\", "/")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("\b", "")
                .replace("\"", "`");
        if (context.length() > config.getIntegerProperty(RagConfiguration.CHUNK_SIZE) ) {
            // Truncate the context if too long
            context = context.substring(0, config.getIntegerProperty(RagConfiguration.CHUNK_SIZE));
        }
        return context;
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
        String template = getInstructions("template-fromTextSegment.txt");
        Metadata metadata = segment.metadata();
        String content = cleanContext(segment.text(), config);
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
}
