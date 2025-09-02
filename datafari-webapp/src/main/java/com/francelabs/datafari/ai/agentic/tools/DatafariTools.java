package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.rest.v2_0.ai.AiPowered;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class DatafariTools {

    private static final Logger LOGGER = LogManager.getLogger(DatafariTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;
    int index = 0;

    public DatafariTools(HttpServletRequest request) {
        this.request = request;
        config = RagConfiguration.getInstance();
    }

//    @Tool("Process a simple RAG request in the search engine.")
//    String rag(
//            @P("The RAG query") String query
//    ) {
//        JSONObject jsonBody = new JSONObject();
//        jsonBody.put("query", query);
//        String results = "";
//
//        try {
//            results = AiPowered.rag(request, jsonBody);
//            RagAPI.rag(request, jsonBody, false);
//        } catch (IOException e) {
//            LOGGER.error("AGENTIC TOOLS - RAG - ERROR: {}", e.getLocalizedMessage());
//        }
//        LOGGER.debug("AGENTIC TOOLS - RAG - {}", results);
//        return results;
//    }

    @Tool("Process a simple RAG request in the search engine.")
    String ragByDocument(
            @P("The RAG query for a single document") String query,
            @P("The ID of the document") String id
    ) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("query", query);
        jsonBody.put("id", id);
        String results = "";

        try {
            results = AiPowered.rag(request, jsonBody);
            RagAPI.rag(request, jsonBody, true);
        } catch (IOException e) {
            LOGGER.error("AGENTIC TOOLS - RAG by document - ERROR: {}", e.getLocalizedMessage());
        }
        LOGGER.debug("AGENTIC TOOLS - RAG by document - {}", results);
        return results;
    }

    @Tool("Reformulate a search query")
    String queryRewriting(
            @P("The initial user query") String query
    ) {
        String vectorQuery = query;
        try {
            vectorQuery = RagAPI.rewriteSearchQuery(query, "vector", request, RagConfiguration.getInstance());
        } catch (IOException e) {
            LOGGER.error("Query rewriting failed ! Initial user query will be use for the search.", e);
        }
        LOGGER.debug("AGENTIC TOOLS - Query rewriting - {}", vectorQuery);
        return vectorQuery;
    }

    @Tool("Retrieves the metadata (ID, titles, URL) of the retrieved documents for the provided query.")
    String retrieveDocumentsInformation(
            @P("The search query") String query
    ) {
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/select";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("fl", "title,id,url,click_url,creation_date,last_modified,crawl_date,extension,source,word_count,language,xmptpg_npages,original_file_size");
        editableRequest.addParameter("q.op", "OR");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Retrieve documents information - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("Read the content of a specified document, one chunk at a time. The document ID is required. Each time this tool is used, a new chunk is read. If there in no more content to read, returns 'No content'")
    String readNextChunks(
            @P("The ID of the document") String id,
          //  @P("Page index (0-based)") int page
    ) {
        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
        int start = index*rows;


        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,embedded_content");
        // req.addParameter("fq", "parent_doc:\"" + id + "\"");
        req.addParameter("fq", "{!term f=parent_doc}" + id);
        req.addParameter("collection", "VectorMain");
        req.addParameter("start", String.valueOf(start));
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("sort", "chunk_index asc");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        String mergedChunkContents = SearchUtils.mergeChunks(docs);

        index = index + 1;
        LOGGER.debug("AGENTIC TOOLS - Reading chunk {} from '{}'", index, id);
        if (mergedChunkContents.isEmpty()) return "No content";
        return "========== CHUNK " + index + ": ==========\n\n" + mergedChunkContents + "\n\n========== END OF CHUNK " + index + " ==========\n\n";
    }

    @Tool("Search content into a specific document. The document ID is required.")
    String searchChunksFromADocument(
            @P("The ID of the document") String id,
            @P("The search query") String query
    ) {
        int chunksPerLot = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);

        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/rrf";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("fl", "title,id,url,click_url,embedded_content");
        editableRequest.addParameter("fq", "id:\""+id+"_"+index + "\"");
        editableRequest.addParameter("q.op", "OR");
        editableRequest.addParameter("topK", "50");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", String.valueOf(chunksPerLot));
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Retrieve documents information - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("Write a summary of a document. The ID of the document is required.")
    String summarize(
            @P("The ID of the document") String id
    ) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("id", id);

        String results = AiPowered.summarizeDocument(request, jsonBody);
        LOGGER.debug("AGENTIC TOOLS - Summarize - {}", results);
        return results;
    }

    @Tool("Returns the BM25 search results for the given query using Datafari. The content of the documents is not returned.")
    String bm25Search(
            @P("The search query") String query
    ) {
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/select";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("fl", "title,id,author,click_url,creation_date,last_modified,crawl_date,extension,source,word_count,language,xmptpg_npages,original_file_size");
        editableRequest.addParameter("q.op", "OR");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - BM25 Search - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("Returns the vector search results for the given keyword-based query using Datafari (including content snippets).")
    String vectorSearch(
            @P("The search query") String query
    ) {
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/vector";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("fl", "title,id,exactContent,embedded_content,click_url");
        editableRequest.addParameter("topK", "50");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        editableRequest.addParameter("queryrag", query);

        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Vector Search - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("Returns the hybrid search results for the given query using Datafari (including content snippets).")
    String hybridSearch(
            @P("The search query") String query
    ) {
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/rrf";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("fl", "title,id,exactContent,embedded_content,click_url");
        editableRequest.addParameter("topK", "50");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        editableRequest.addParameter("queryrag", query);
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Hybrid Search - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("If you don't have the tools you need to answer the request, use this one to describe precisely the tools you need, for future improvement.")
    String requestNewTool(
            @P("The description of the tool you would need") String description
    ) {
        LOGGER.warn("AGENTIC TOOLS - Requesting tool - {}", description);
        return "Note taken.";
    }

}
