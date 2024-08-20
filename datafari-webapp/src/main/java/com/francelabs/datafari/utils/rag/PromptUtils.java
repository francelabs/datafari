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
import com.francelabs.datafari.rag.RagConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt Utility class for RAG
 *
 * @author France Labs
 *
 */
public class PromptUtils {

    static Integer maxJsonLength = Integer.MAX_VALUE;

    private PromptUtils() {
        // Constructor
    }

    /**
     * Transform a JSONArray of documents (ID, title, url, content) into a list of prompts
     * @param config : Global RAG configuration
     * @param documentsList : A JSONArray list of documents
     * @return a prompt ready to be sent to the LLM service
     */
    public static List<String> documentsListToPrompts(RagConfiguration config, JSONArray documentsList, String userQuery) throws IOException {

        List<String> prompts = new ArrayList<>();
        for (Object document : documentsList) {
            JSONObject doc = (JSONObject) document;
            // format document
            String content = formatDocument(config, doc.get("title").toString(), doc.get("content").toString());
            // create prompt
            String prompt = createPrompt(config, userQuery, content);
            prompts.add(prompt);
        }

        return prompts;
    }


    /**
     * Create a full prompt usable by a LLM, user the user prompt, the provided content and the configuration
     * @param config : Global RAG configuration
     * @param prompt : User query
     * @param content : Chunked documents provided by the search
     * @return a prompt ready to be sent to the LLM service
     */
    public static String createPrompt(RagConfiguration config, String prompt, String content) throws IOException {
        // Retrieve the prompt template from instructions file.
        String template = getInstructions();

        template = template.replace("{format}", getResponseFormat(config));
        template = template.replace("{prompt}", prompt);
        template = template.replace("{content}", content);

        return cleanContext(template);
    }

    public static String formatDocument(RagConfiguration config, String title, String content) {
        String template = "Document title:```{title}```\nDocument content:```{chunk}```";
        return template.replace("{title}", title).replace("{chunk}", content);
    }

    public static String getResponseFormat(RagConfiguration config) {

        if("stepbystep".equals(config.getFormat()))
            return " If relevant, your response should take the form step-by-step instructions.\n";
        else if ("bulletpoint".equals(config.getFormat()))
            return " If relevant, your response should take the form of a bullet-point list.\n";
        else if ("text".equals(config.getFormat()))
            return " If relevant, your response should take the form of a text.\n";
        else return "";
    }

    /**
     * @param context The context, containing documents content
     * @return A clean context, with no characters or element that could cause an error or a prompt injection
     */
    private static String cleanContext(String context) {
        context = context.replace("\\", "/")
                .replace("\n", " ") // Todo : See if it works with "\\n"
                .replace("\r", " ")
                .replace("\t", " ")
                .replace("\b", "")
                .replace("\"", "`");
        if (context.length() > maxJsonLength -1000) { // Todo : Get maxJsonLenght from configuration
            // Truncate the context if too long
            context = context.substring(0, maxJsonLength - 1000);
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
	
}
