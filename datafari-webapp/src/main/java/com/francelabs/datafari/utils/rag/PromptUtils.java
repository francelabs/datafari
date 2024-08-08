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

import com.francelabs.datafari.rag.RagConfiguration;

/**
 * Prompt Utility class for RAG
 *
 * @author France Labs
 *
 */
public class PromptUtils {

    static Integer maxJsonLength = Integer.MAX_VALUE;

    /**
     * Create a full prompt usable by a LLM, user the user prompt, the provided content and the configuration
     * @param config : Global RAG configuration
     * @param prompt : User query
     * @param content : Chunked documents provided by the search
     * @return a prompt ready to be sent to the LLM service
     */
    public String createPrompt(RagConfiguration config, String prompt, String content) {
        String template = "You are a helpful assistant. Here are some documents. Use the documents to answer the user's question.\n" +
                "You must not use your knowledge or any external source of information to answer the question.\n" +
                "So do not provide any information that does not belong in documents.\n" +
                "If you can't find the answer in the documents, just say that you can't find the answer.\n{format} " +
                "Documents: {content}\n" +
                "Answer the question: {prompt} and name the source from which you have taken the answer.\n" +
                "Don't cite the word source in your answer.\n" +
                "Answer: ";

        template = template.replace("{format}", getResponseFormat(config));
        template = template.replace("{prompt}", prompt);
        template = template.replace("{content}", content);

        return cleanContext(template);
    }

    public String formatDocument(RagConfiguration config, String title, String content) {
        String template = "Title:```{title}```\n Content:```{chunk}```";
        return template.replace("{title}", title).replace("{chunk}", content);
    }

    public String getResponseFormat(RagConfiguration config) {

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
                .replace("\n", " ") // Todo : See if it works with \\n
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
	
}
