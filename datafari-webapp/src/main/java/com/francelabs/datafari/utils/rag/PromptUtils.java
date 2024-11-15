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
import com.francelabs.datafari.audit.AuditLogUtil;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rag.DocumentForRag;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.users.Users;
import com.francelabs.datafari.user.Lang;
import com.francelabs.datafari.user.UiConfig;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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
     * Transform a list of documents (ID, title, url, content) into a list of prompts
     *
     * @param config        : Global RAG configuration
     * @param documentsList : A JSONArray list of documents
     * @param request : The HTTP request
     * @return a prompt ready to be sent to the LLM service
     */
    public static List<String> documentsListToPrompts(RagConfiguration config, List<DocumentForRag> documentsList, HttpServletRequest request) throws IOException {

        List<String> prompts = new ArrayList<>();
        String uniqueContent = formatDocuments(documentsList);
        if (uniqueContent.length() < config.getIntegerProperty(RagConfiguration.CHUNK_SIZE)) {
            // All documents are merged into one prompt
            String prompt = createPrompt(config, uniqueContent, request);
            prompts.add(prompt);
        } else {
            for (DocumentForRag document : documentsList) {
                // format document
                String content = formatDocument(document.getTitle(), document.getContent());
                // create prompt
                String prompt = createPrompt(config, content, request);
                prompts.add(prompt);
            }
        }

        return prompts;
    }


    /**
     * Create a full prompt usable by a LLM, user the user prompt, the provided content and the configuration
     * @param config : Global RAG configuration
     * @param content : Chunked documents provided by the search
     * @param request : The request object
     * @return a prompt ready to be sent to the LLM service
     */
    public static String createPrompt(RagConfiguration config, String content, HttpServletRequest request) throws IOException {
        // Retrieve the prompt template from instructions file.
        String template = getInstructions();
        String userQuery = request.getParameter("q");

        template = template.replace("{format}", getResponseFormat(request));
        template = template.replace("{prompt}", userQuery);
        template = template.replace("{content}", content);
        template = template.replace("{language}", getUserLanguage(request));

        return cleanContext(template, config);
    }

    public static String formatDocument(String title, String content) {
        String template = "Document title:```{title}```\nDocument content:```{chunk}```";
        return template.replace("{title}", title).replace("{chunk}", content);
    }

    public static String formatDocuments(List<DocumentForRag> documents) {
        String template = "Document title:```{title}```\nDocument content:```{chunk}```";
        StringBuilder prompt = new StringBuilder();
        for (DocumentForRag document : documents) {
            prompt.append(template.replace("{title}", document.getTitle()).replace("{chunk}", document.getContent()));
            prompt.append("\n\n");
        }

        return prompt.toString();
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
     * @return The instructions prompts stored in rag-instructions.txt file
     */
    private static String getInstructions() throws IOException {
        return readFromInputStream(RagAPI.class.getClassLoader().getResourceAsStream("rag-instructions.txt"));
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
            // Retrieving user language from query GET parameters
            String lang = getDisplayedName(request.getParameter("lang"));

            // If no language is provided in the GET parameters, retrieving user language from Cassandra lang database
            if (lang == null || lang.isEmpty()) lang = getDisplayedName(Lang.getLang(authenticatedUserName));
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
}
