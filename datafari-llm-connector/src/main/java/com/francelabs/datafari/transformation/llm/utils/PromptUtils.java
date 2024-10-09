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
package com.francelabs.datafari.transformation.llm.utils;


import com.francelabs.datafari.transformation.llm.model.LlmSpecification;

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
     * Create a prompt for summarization
     *
     * @param content : The document content
     * @return a prompt ready to be sent to the LLM service
     */
    public static String promptForSummarization(String content, LlmSpecification spec) {
        String prompt;
        String language = "";
        if (!spec.getSummariesLanguage().isEmpty()) {
            Locale loc = new Locale(spec.getSummariesLanguage());
            if (!loc.getDisplayLanguage(new Locale("en")).isEmpty()) language = " in " +loc.getDisplayLanguage(Locale.ENGLISH);
        }
        prompt = "\"\"\"Summarize this document " + language + ": \n\n" + content + "\"\"\"";
        return prompt;
    }

    /**
     * Create a prompt for summarization
     *
     * @param content : The document content
     * @return a prompt ready to be sent to the LLM service
     */
    public static String promptForCategorization(String content) {
        String prompt;
        prompt = "\"\"\"Categorize the following document in one of the following categories: Invoice, Call for Tenders, Request for Quotations, Technical paper, Presentation, Resumes, Others. If you don't know, say \"Others\". \n\n" + content.substring(0, 30000) + "\"\"\"";
        return prompt;
    }
	
}
