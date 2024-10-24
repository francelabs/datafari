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
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
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
     * @param documentList : A list containing a list of documents (ID, title, url and content)
     * @return The document list. Big documents are chunked into multiple documents.
     */
    public static List<DocumentForRag> processVectorSearch(List<DocumentForRag> documentList, HttpServletRequest request) {

        List<Document> documents = new ArrayList<>();
        List<DocumentForRag> embeddedDocumentList = new ArrayList<>();

        // Create a list of Langchain4j Documents
        for (DocumentForRag document : documentList) {
            // Convert DocumentForRag to Lanchain4j Document
            Document l4jDoc = convertDocuments(document);
            documents.add(l4jDoc);
        }

        // Embedding the documents and store them into vector DB
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // Vector query
        List<Content> contents = EmbeddingStoreContentRetriever.from(embeddingStore).retrieve(Query.from(request.getParameter("q")));

        // The first calls returns a concatenated responses from each chunk
        for (Content content : contents) {
            String embeddedContent = content.textSegment().text();
            DocumentForRag docToInsert = new DocumentForRag();
            docToInsert.setContent(embeddedContent);
            docToInsert.setTitle(content.textSegment().metadata().getString("title"));
            docToInsert.setId(content.textSegment().metadata().getString("id"));
            docToInsert.setUrl(content.textSegment().metadata().getString("url"));
            embeddedDocumentList.add(docToInsert);
        }

        return embeddedDocumentList;
    }

    private static Document convertDocuments(DocumentForRag doc4rag) {
        Document lc4jDoc = new Document(doc4rag.getContent());
        lc4jDoc.metadata().put("title", doc4rag.getTitle())
                .put("id", doc4rag.getId())
                .put("url", doc4rag.getUrl());
        return lc4jDoc;
    }
}