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

import com.francelabs.datafari.rag.DocumentForRag;
import com.francelabs.datafari.rag.RagConfiguration;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunking Utility class for RAG
 *
 * @author France Labs
 *
 */
public class ChunkUtils {

    private ChunkUtils() {
        // Constructor
    }

    /**
     *
     * @param config : RAG configuration
     * @param documentList : JSONArray containing a list of documents (ID, title, url and content)
     * @return The document list. Big documents are chunked into multiple documents.
     */
    public static List<DocumentForRag> chunkDocuments(RagConfiguration config, List<DocumentForRag> documentList) {
        List<DocumentForRag> chunkedDocumentList = new ArrayList<>();


        for(DocumentForRag document : documentList) {

            List<String> chunks = extractChunksFromDocument(document, config);

            // Each chunk is added to "chunkedDocumentList" as a document
            for (String chunk : chunks) {
                DocumentForRag docToAdd = new DocumentForRag();
                docToAdd.setTitle(document.getTitle());
                docToAdd.setId(document.getId());
                docToAdd.setUrl(document.getUrl());
                docToAdd.setContent(chunk);
                chunkedDocumentList.add(docToAdd);
            }
        }

        return chunkedDocumentList;
    }

    /**
     *
     * @param content : A string content to be chunked
     * @param metadata : Information about the original file (title, url, id...)
     * @param config : RAG configuration
     * @return A list of TextSegments, that contain metadata. Big documents are chunked into multiple documents.
     */
    public static List<TextSegment> chunkContent(String content, Metadata metadata, RagConfiguration config) {
        Document doc = new Document(content, metadata);
        DocumentSplitter splitter = new DocumentByParagraphSplitter(100, config.getIntegerProperty(RagConfiguration.LLM_MAX_TOKENS));
        return splitter.split(doc);
    }

    /**
     *
     * @param doc : a Document objet
     * @return a list of one or subdocuments
     */
    private static List<String> extractChunksFromDocument(DocumentForRag doc, RagConfiguration config) {
        return splitStringBySize(doc.getContent(), config.getIntegerProperty(RagConfiguration.CHUNK_SIZE));
    }

    private static List<String> splitStringBySize(String str, int size) {
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / size; i++) {
            split.add(str.substring(i * size, Math.min((i + 1) * size, str.length())));
        }
        return split;
    }
}