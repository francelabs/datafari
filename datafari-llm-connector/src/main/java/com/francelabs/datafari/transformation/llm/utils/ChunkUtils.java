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
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;

import java.util.List;

/**
 * Prompt Utility class for RAG
 *
 * @author France Labs
 *
 */
public class ChunkUtils {


    private ChunkUtils() {
        // Constructor
    }

	public static List<TextSegment> chunkString(String text, LlmSpecification spec) {
        Tokenizer tokenizer = new OpenAiTokenizer();
        int maxChunkSizeInTokens = spec.getMaxChunkSizeInTokens();
        DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSizeInTokens, 10, tokenizer);
        Document document = new Document(text);
        return splitter.split(document);
    }

    public static List<TextSegment> chunkRepositoryDocument(String content, RepositoryDocument repoDoc, LlmSpecification spec) {
        Tokenizer tokenizer = new OpenAiTokenizer();
        int maxChunkSizeInTokens = spec.getMaxChunkSizeInTokens();
        DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSizeInTokens, 10, tokenizer);
        Metadata metadata = new Metadata().put("filename", repoDoc.getFileName());
        Document document = new Document(content, metadata);
        return splitter.split(document);
    }
}
