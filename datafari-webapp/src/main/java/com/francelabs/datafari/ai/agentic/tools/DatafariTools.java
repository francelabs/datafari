package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.ai.stream.SseBridge;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.rest.v2_0.ai.AiPowered;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class DatafariTools {

    private static final Logger LOGGER = LogManager.getLogger(DatafariTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;
    List<Document> sources;
    private final SourcesAccumulator sourcesAcc;
    private final SseBridge sse;

    public DatafariTools(HttpServletRequest request, List<Document> sources, SseBridge sse, SourcesAccumulator sourcesAcc) {
        this.request = request;
        this.sources = sources;
        this.sse = sse;
        this.sourcesAcc = sourcesAcc;
        config = RagConfiguration.getInstance();
    }

    @Tool("Process a RAG query in a single document.")
    String ragByDocument(
            @P("The RAG query for a single document") String query,
            @P("The exact ID of the document") String id
    ) {
        LOGGER.info("AGENTIC TOOLS - RAG by document - ID: {} - Query: {}", id, query);
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("query", query);
        jsonBody.put("id", id);
        String results = "";

        try {
            results = AiPowered.rag(request, jsonBody);
            extractSourcesFromRagResponse(results);
        } catch (Exception e) {
            LOGGER.error("AGENTIC TOOLS - RAG by document - ERROR: {}", e.getLocalizedMessage());
        }
        LOGGER.debug("AGENTIC TOOLS - RAG by document - Result {}", results);
        return results;
    }

    @Tool("""
        Execute a search query and return information (metadata) from retrieved documents. (BM25 search)
        The whole content of the retrieved document may be large, and therefor is not returned.
        However, you can use this tool to retrieve the ID of a document, and read the document later with another tool.
        The returned data returned for each retrieved document are:
        title, id, author, url, creation_date, last_modified, crawl_date, extension, source, word_count,
        language, xmptpg_npages, original_file_size
    """)
    String bm25Search(
            @P("The search query") String query
    ) {
        LOGGER.info("AGENTIC TOOLS - BM25 Search - Query: {}", query);
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", query);
        req.addParameter("fl", "title,id,author,url,creation_date,last_modified,crawl_date,extension,source,word_count,language,xmptpg_npages,original_file_size");
        req.addParameter("q.op", "OR");
        req.addParameter("start", "0");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");
        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - BM25 Search - {}", docs.toJSONString());
        return docs.toJSONString();
    }

    @Tool("""
            Read N chunks of a document from VectorMain, ordered, starting at the given page (0-based).
            If there in no more content to read from the document, 'No content' is returned.
            """)
    String readNextChunks(
            @P("The exact ID of the document") String id,
            @P("Page index (0-based)") int page
    ) {
        LOGGER.info("AGENTIC TOOLS - Reading page {} of document  '{}'", page, id);

        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
        int start = Math.max(0, page) * rows;


        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,embedded_content");
        req.addParameter("fq", "{!term f=parent_doc}" + id);
        req.addParameter("collection", "VectorMain");
        req.addParameter("start", String.valueOf(start));
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("sort", "chunk_index asc");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        String mergedChunkContents = SearchUtils.mergeChunks(docs);

        // Add document to sources
        if (!docs.isEmpty()) addDocumentToSource((JSONObject) docs.getFirst());

        if (mergedChunkContents.isEmpty()) return "No content";
        return "========== PAGE " + page + ": ==========\n\n" + mergedChunkContents + "\n\n========== END OF PAGE " + page + " ==========\n\n";
    }

    @Tool("""
            Process a search query within a specific document, and retrieve the most relevant chunks.
            You must provide the exact ID of the document.
            """)
    String searchChunksFromADocument(
            @P("The exact ID of the document") String id,
            @P("The search query") String query
    ) {
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
        LOGGER.debug("AGENTIC TOOLS - Search from document - Query: {} - Document: {}", query, id);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/rrf";
        req.addParameter("q", query);
        req.addParameter("queryrag", query);
        req.addParameter("fl", "title,id,url,embedded_content");
        req.addParameter("fq", "{!term f=parent_doc}" + id);
        req.addParameter("q.op", "OR");
        req.addParameter("wt", "json");
        req.addParameter("topK", "50");
        req.addParameter("start", "0");
        req.addParameter("rows", String.valueOf(rows));
        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.info("AGENTIC TOOLS - Search from document {}: {}", id, query);
        LOGGER.debug("AGENTIC TOOLS - Retrieved content: {}", docs.toJSONString());

        // Add document to sources
        if (!docs.isEmpty()) addDocumentToSource((JSONObject) docs.getFirst());

        return docs.toJSONString();
    }

    @Tool("Retrieve a summary of a document. You must provide the exact ID of the document.")
    String summarize(
            @P("The exact ID of the document") String id
    ) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("id", id);

        String results = AiPowered.summarizeDocument(request, jsonBody);
        LOGGER.debug("AGENTIC TOOLS - Summarize - {}", results);
        return results;
    }

    @Tool("Execute a search query and return information and chunks from retrieved documents. (vector search)")
    String vectorSearch(
            @P("The search query") String query
    ) {
        LOGGER.debug("AGENTIC TOOLS - Vector Search - Query: {}", query);

        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
        int topK = config.getIntegerProperty(RagConfiguration.RRF_TOPK, 50);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/vector";
        req.addParameter("q", query);
        req.addParameter("queryrag", query);
        req.addParameter("fl", "title,id,parent_doc,exactContent,embedded_content,url");
        req.addParameter("topK", String.valueOf(topK));
        req.addParameter("start", "0");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Vector Search - {}", docs.toJSONString());

        // Add document to sources
        addDocumentsToSource(docs);

        return docs.toJSONString();
    }

    @Tool("Search and return information and chunks (hybrid search).")
    String hybridSearch(
            @P("The search query") String query
    ) {
        LOGGER.debug("AGENTIC TOOLS - Hybrid Search - Query: {}", query);
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/rrf";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("fl", "title,id,parent_doc,exactContent,embedded_content,url");
        editableRequest.addParameter("topK", "50");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("wt", "json");
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Hybrid Search - {}", docs.toJSONString());

        // Add document to sources
        addDocumentsToSource(docs);

        return docs.toJSONString();
    }

//    // Experimental: Specific to CfP scenario
//    @Tool("Calls the agent specialised in CfP (Call for Proposals). Use it for queries about market, Call for Proposal...")
//    String callCFPAgent(
//            @P("The user query") String query
//    ) {
//        LOGGER.info("AGENTIC TOOLS - Calling subagent : CfPAgent");
//        CfPAgent agent = new CfPAgent(request);
//        return agent.ask(query);
//    }

    @Tool("If you don't have the tools you need to answer the request, use this one to describe precisely the tools you need, for future improvement.")
    String requestNewTool(
            @P("The description of the tool you would need") String description
    ) {
        LOGGER.warn("AGENTIC TOOLS - Requesting tool - {}", description);
        return "Note taken.";
    }

    /**
     * Extract the sources from the "RagByDocument" service and add them to the sources
     * @param results: A String JSON response from the RAG service
     */
    private void extractSourcesFromRagResponse(String results) {
        try {
            // Sources extraction
            JSONParser parser = new JSONParser();
            JSONObject content = (JSONObject) parser.parse(results);
            JSONArray documents = (JSONArray) content.get("documents");
            if (!documents.isEmpty()) addDocumentsToSource(documents);
        } catch (Exception e) {
            LOGGER.error("AGENTIC TOOLS - Extracting from CCTP - ERROR: {}", e.getLocalizedMessage());
        }
    }

    /**
     * Add retrieved documents (JSONArray) to the sources
     * @param docs: A JSONArray of documents (JSONObject, as returned by Datafari search)
     */
    private void addDocumentsToSource(JSONArray docs) {
        try {
            for (Object o : docs) {
                JSONObject doc = (JSONObject) o;
                addDocumentToSource(doc);
            }
        } catch (Exception e) {
            LOGGER.error("Could not add documents to sources.", e);
        }
    }


    /**
     * Add a retrieved document (JSONObject) to the sources
     * @param doc: A JSONObject document (as returned by Datafari search)
     */
    private void addDocumentToSource(JSONObject doc) {
        try {
            String id = (doc.get("parent_doc") != null) ? (String) doc.get("parent_doc") : (String) doc.get("id");
            String title = ((JSONArray) doc.get("title")).getFirst().toString();
            String url = (doc.get("click_url") != null) ? (String) doc.get("click_url") : (String) doc.get("url");

            String content;
            if (doc.get("content") != null) {
                content = (String) doc.get("content");
            } else if (doc.get("embedded_content") != null) {
                content = (String) doc.get("embedded_content");
            } else {
                content = "No content available.";
            }
            Document source = Document.document(content);
            source.metadata().put("id", id)
                    .put("title", title)
                    .put("url", url);
            this.sources.add(source); // Add the source for JSON response
            sourcesAcc.add(source); // Add the source for streaming response
            sse.send("source", SourcesAccumulator.toJson(source).toJSONString()); // Stream the source

        } catch (Exception e) {
            LOGGER.error("Could not add document to sources.", e);
        }
    }

}
