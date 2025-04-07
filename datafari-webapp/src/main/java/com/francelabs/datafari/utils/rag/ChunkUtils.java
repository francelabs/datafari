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
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
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
     * @param documentList : A list of documents (content and metadata) to chunk
     * @param config       : RAG configuration
     * @return The document list. Big documents are chunked into multiple documents.
     */
    public static List<Document> chunkDocuments(List<Document> documentList, RagConfiguration config) {
        List<Document> chunkedDocumentList = new ArrayList<>();
        int maxFiles = config.getIntegerProperty(RagConfiguration.MAX_FILES); // MaxFiles must not exceed the number of provided documents
        int i = 0;
        for(Document document : documentList) {
            List<TextSegment> chunks = chunkContent(document, config);

            // Each chunk is added to "chunkedDocumentList" as a Document
            for (TextSegment chunk : chunks) {
                Document docToAdd = new Document(chunk.text(), chunk.metadata());
                chunkedDocumentList.add(docToAdd);
            }
            i++;
            if (maxFiles <= i) return chunkedDocumentList;
        }
        return chunkedDocumentList;
    }

    /**
     *
     * @param doc : A Document object containing content and metadata (title, url, id...)
     * @param config : RAG configuration
     * @return A list of TextSegments, that contain metadata. Big documents are chunked into multiple documents.
     */
    public static List<TextSegment> chunkContent(Document doc, RagConfiguration config) {
        DocumentSplitter splitter = DocumentSplitters.recursive(config.getIntegerProperty(RagConfiguration.CHUNK_SIZE), 0);
        return splitter.split(doc);
    }
}