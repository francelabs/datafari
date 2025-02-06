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
import dev.langchain4j.data.segment.TextSegment;

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
     * Create a prompt for summarization
     *
     * @param chunk : The document content
     * @return a prompt ready to be sent to the LLM service
     */
    public static String promptForSummarization(TextSegment chunk, LlmSpecification spec) {
        String prompt;
        String language = "";
        if (!spec.getSummariesLanguage().isEmpty()) {
            Locale loc = new Locale(spec.getSummariesLanguage());
            if (!loc.getDisplayLanguage(new Locale("en")).isEmpty()) language = " in " +loc.getDisplayLanguage(Locale.ENGLISH);
        }
        prompt = "Summarize this document titled `{fileName}`{language}: \n\n\"\"\"{content}\"\"\""
                .replace("{fileName}", chunk.metadata().getString("filename"))
                .replace("{content}", chunk.text())
                .replace("{language}", language);
        return prompt;
    }

    /**
     * Create a prompt for summarization
     *
     * @param chunk : The document chunk
     * @return a prompt ready to be sent to the LLM service
     */
    public static String promptForRecursiveSummarization(TextSegment chunk, String previousSummary, int index, LlmSpecification spec) {
        String prompt;
        String language = "";
        String fileName = chunk.metadata().getString("filename");
        if (!spec.getSummariesLanguage().isEmpty()) {
            Locale loc = new Locale(spec.getSummariesLanguage());
            if (!loc.getDisplayLanguage(new Locale("en")).isEmpty()) language = " in " +loc.getDisplayLanguage(Locale.ENGLISH);
        }
        prompt = "Here is the a summary of the document `{fileName}` you wrote, based on {indexFork}:\n\n\"\"\"{previousSummary}\"\"\" \n\n"
                + "Here is the part {index} of the document:\n\n\"\"\"{content}\"\"\" \n\n"
                + "Write a global summary {language} of the document.";
        return prompt
                .replace("{fileName}", fileName)
                .replace("{indexFork}", getIndexFork(index))
                .replace("{previousSummary}", previousSummary)
                .replace("{index}", String.valueOf(index+1))
                .replace("{content}", chunk.text())
                .replace("{language}", language)
        ;
    }

    static String getIndexFork(int index) {
        if (index == 1) {
            return "part 1";
        }
        return "parts 1 to " + index;
    }

    /**
     * Create a prompt for summarization
     *
     * @param chunk : The first chunk of the document
     * @return a prompt ready to be sent to the LLM service
     */
    public static String promptForCategorization(TextSegment chunk, LlmSpecification spec) {
        String prompt;
        List<String> categories = spec.getCategories();
        StringBuilder sbCategories = new StringBuilder();
        for (String cat : categories) sbCategories.append(cat).append(", ");
        sbCategories.append("Others");

        prompt = "Categorize the following document titled `{filename}` in one (or more) of the following categories: {categories}. If you don't know, say \"Others\". \n\n\"\"\"{content}\"\"\""
                .replace("{categories}", sbCategories.toString())
                .replace("{filename}", chunk.metadata().getString("filename"))
                .replace("{content}", truncate(chunk.text(), spec.getMaxChunkSizeInChar()));
        return prompt;
    }

    static String truncate(String text, int length) {
        if (text.length() <= length && length > 0) {
            return text;
        } else {
            return text.substring(0, length);
        }
    }
	
}
