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
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Vector Search Utility class for RAG
 *
 * @author France Labs
 *
 */
public class VectorUtils {

    private VectorUtils() {
        // Constructor
    }

    /**
     *
     * @param request : The HttpServletRequest request
     * @param documentList : A list containing a list of documents (content and metadata)
     * @return The document list. Big documents are chunked into multiple documents.
     */
    public static List<Document> processVectorSearch(List<Document> documentList, HttpServletRequest request) {

        List<Document> embeddedDocumentList = new ArrayList<>();

        // Embedding the documents and store them into vector DB
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documentList, embeddingStore);

        // Vector query
        Query query = Query.from(request.getParameter("q"));

        int maxResult = RagConfiguration.getInstance().getIntegerProperty(RagConfiguration.IN_MEMORY_TOP_K, 10);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .maxResults(maxResult)
                .build();
        List<Content> contents = contentRetriever.retrieve(query);


        // The first calls returns a concatenated responses from each chunk
        for (Content content : contents) {
            Document docToInsert = new Document(content.textSegment().text(), content.textSegment().metadata());
            embeddedDocumentList.add(docToInsert);
        }

        return embeddedDocumentList;
    }
}
