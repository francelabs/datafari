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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.rag.RagConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunking Utility class for RAG
 *
 * @author France Labs
 *
 */
public class ChunkUtils {

    /**
     *
     * @param config : RAG configuration
     * @param documentList : JSONArray containing a list of documents (ID, title, url and content)
     * @return The document list. Big documents are chunked into multiple documents.
     */
    public JSONArray chunkDocuments(RagConfiguration config, JSONArray documentList) {
        JSONArray chunkedDocumentList = new JSONArray();

        ObjectMapper mapper = new ObjectMapper();

        documentList.forEach(item -> {
            JSONObject jsonDoc = (JSONObject) item;
            DocumentForRag doc = null;
            try {
                doc = mapper.readValue(jsonDoc.toJSONString(), DocumentForRag.class);
                List<String> chunks = extractChunksFromDocument(doc, config);

                // Each chunk is added to "chunkedDocumentList" as a document
                for (String chunk : chunks) {
                    JSONObject docToAdd = new JSONObject();
                    docToAdd.put("title", doc.getTitle());
                    docToAdd.put("id", doc.getTitle());
                    docToAdd.put("url", doc.getTitle());
                    docToAdd.put("content", chunk);
                    chunkedDocumentList.add(docToAdd);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("An error occurred during chunking.");
            }
        });

        return chunkedDocumentList;
    }

    /**
     *
     * @param doc : a Document objet
     * @return a list of one or subdocuments
     */
    private List<String> extractChunksFromDocument(DocumentForRag doc, RagConfiguration config) {
        return splitStringBySize(doc.getContent(), config.getChunkSize());
    }

    private static List<String> splitStringBySize(String str, int size) {
        ArrayList<String> split = new ArrayList<>();
        for (int i = 0; i <= str.length() / size; i++) {
            split.add(str.substring(i * size, Math.min((i + 1) * size, str.length())));
        }
        return split;
    }
}

// Todo : delete ?
class DocumentForRag {
    String id;
    String url;
    String title;
    String content;

    public DocumentForRag(String id, String url, String title, String content) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}