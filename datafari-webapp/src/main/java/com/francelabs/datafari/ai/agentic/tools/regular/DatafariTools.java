package com.francelabs.datafari.ai.agentic.tools.regular;

import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.services.RagService;
import com.francelabs.datafari.ai.services.SummarizationService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.stream.NdjsonChatStream;
import com.francelabs.datafari.ai.stream.ToolMeta;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.exception.AwaitUserInputException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Map;

public class DatafariTools {

    private static final Logger LOGGER = LogManager.getLogger(DatafariTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;
    private final SourcesAccumulator sourcesAcc;
    private final ChatStream stream;
    private final AiRequest params;

    public DatafariTools(HttpServletRequest request,AiRequest params, ChatStream stream, SourcesAccumulator sourcesAcc) {
        this.request = request;
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        this.params = params;
        config = RagConfiguration.getInstance();
    }

    @ToolMeta(label = "Exploring documents...",
            i18nKey = "tool.ragByDocument",
            icon = "search")
    @Tool("Process a RAG query in a single document.")
    String ragByDocument(
            @P("The RAG query for a single document") String query,
            @P("The exact ID of the document") String id
    ) {
        LOGGER.info("AGENTIC TOOLS - RAG by document - ID: {} - Query: {}", id, query);
        String result = "";

        AiRequest ragrequest = new AiRequest();
        ragrequest.query = query;
        ragrequest.id = id;

        try {
            ApiContent resp = RagService.rag(request, ragrequest, stream, sourcesAcc, true);
            result = returnMessageOrReason(resp, "RAG query failed");
        } catch (AgenticToolException ex) {
          throw ex;
        } catch (Exception e) {
            LOGGER.error("AGENTIC TOOLS - RAG by document - ERROR: {}", e.getLocalizedMessage());
        }
        LOGGER.debug("AGENTIC TOOLS - RAG by document - Result {}", result);
        return result;
    }

    // TODO : Fix or delete
//    @ToolMeta(label = "Asking for information...",
//        i18nKey = "tool.askUser",
//        icon = "search")
//    @Tool("Ask a question to the user and wait for the response.")
//    String askUser(
//        @P("Message") String message,
//        @P("schema") Map<String,Object> schema
//    ) throws AwaitUserInputException {
//        LOGGER.info("AGENTIC TOOLS - Ask user - Message: {}", message);
//
//        // If streaming is used, we send the question to the user.
//        // Then, we watch the session to catch a response.
//        String response = null;
//        if (stream instanceof NdjsonChatStream) {
//            // 1) Emit "ask" event
//            stream.ask(message, AiService.getMemoryId(stream, params));
//            stream.phase("agentic:information needed");
//
//            // Create an empty entry in the HTTP Session, using the memory ID as key.
//            HttpSession session = request.getSession();
//            session.setAttribute(params.memoryId, "");
//
//            // Each 200ms, check if the entry has been updated.
//            long delay = 300; // Delay in ms between each attempt
//            long timeout = 1000 * 60 * 3; // Max duration in ms (1000*60*3 = 3 minutes)
//            long timer = 0;
//            try {
//                while (timeout > timer) {
//                    Thread.sleep(delay); // Wait 300ms
//                    timer = timer + delay;
//
//                    if ( !((String) session.getAttribute(params.memoryId)).isEmpty() ) {
//                        // If the entry is no longer empty
//                        response = (String) session.getAttribute(params.memoryId);
//                        stream.phase("agentic:response received");
//                        LOGGER.info("User response detected !");
//                        break;
//                    }
//                }
//
//                if (response != null && !response.isEmpty()) {
//                    LOGGER.info("EBE - Response : {}", response);
//                    return response;
//                }
//
//            } catch (InterruptedException e) {
//                LOGGER.error(e);
//            } finally {
//                session.removeAttribute(params.memoryId);
//            }
//
//        }
//
//        if (config.getBooleanProperty(RagConfiguration.CHAT_MEMORY_ENABLED))  {
//            throw new AwaitUserInputException(CodesReturned.PROBLEMQUERY, message);
//        } else {
//            // If memory is disabled, return default message to llm
//            return "Sorry, the 'ask user' feature is disabled.";
//        }
//    }

//    @ToolMeta(label = "Checking chat history...",
//            i18nKey = "tool.readChatHistory",
//            icon = "brain")
//    @Tool("Read the conversation history, including user's messages and assistant's responses.")
//    String readChatHistory() {
//        LOGGER.debug("AGENTIC TOOLS - Reading chat history");
//
//        if (params.history == null) {
//            params.history = new ArrayList<>();
//        }
//
//        StringBuilder sb = new StringBuilder();
//        for (AiRequest.ChatMessage message : params.history) {
//            sb.append(message.role.name().toUpperCase())
//                    .append(": ")
//                    .append(message.message)
//                    .append("\n");
//        }
//        sb.append("USER: ").append(params.query);
//        return sb.toString();
//    }

    @ToolMeta(label = "Searching documents...",
            i18nKey = "tool.bm25Search",
            icon = "search")
    @Tool("""
        Execute a search query and return information (metadata) from retrieved documents. (BM25 search)
        The content of the retrieved document is not returned.
        However, the returned IDs can be used with another tool (document reading, entities extraction...).
        The returned data for each retrieved document are:
        title, id, author, url, creation_date, last_modified, crawl_date, extension, source, word_count,
        language, xmptpg_npages, original_file_size
    """)
    String bm25Search(@P("The search query") String query) {
        LOGGER.info("AGENTIC TOOLS - BM25 Search - Query: {}", query);
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", query);
        req.addParameter("fl", "title,docId,author,url,creation_date,last_modified,crawl_date,extension,source,word_count,language,xmptpg_npages,original_file_size");
        req.addParameter("q.op", "OR");
        req.addParameter("start", "0");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");
        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        addDocumentsToSource(docs);
        LOGGER.debug("AGENTIC TOOLS - BM25 Search - {}", docs.toJSONString());
        return docs.toJSONString();
    }


    @ToolMeta(label = "Reading a document...",
            i18nKey = "tool.readNextChunks",
            icon = "document")
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
        req.addParameter("fl", "title,parent_doc,docId,url,embedded_content");
        req.addParameter("fq", "{!term f=docId}" + id);
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

    @ToolMeta(label = "Searching content from a document...",
            i18nKey = "tool.searchChunksFromADocument",
            icon = "search")
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
        req.addParameter("fl", "title,docId,url,embedded_content");
        req.addParameter("fq", "{!term f=docId}" + id);
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

    @ToolMeta(label = "Generating a document summary...",
            i18nKey = "tool.summarize",
            icon = "search")
    @Tool("Retrieve a summary of a document. You must provide the exact ID of the document.")
    String summarize(
            @P("The exact ID of the document") String id
    ) {
        AiRequest summarizerequest = new AiRequest();
        summarizerequest.id = id;

        ApiContent resp = SummarizationService.summarize(summarizerequest, request, stream, sourcesAcc, true);
        String result = returnMessageOrReason(resp, "Unable to generate a summary for the document " + id);
        LOGGER.debug("AGENTIC TOOLS - Summarize - {}", result);
        return result;
    }

    @ToolMeta(label = "Extracting entities...",
            i18nKey = "tool.entityExtraction",
            icon = "document")
    @Tool("Extract entities from a given document.")
    String entityExtraction(
            @P("ID of the document") String docId,
            @P("Entities to extract, separated by a comma (ex: cities, phone number, date)") String entities
    ) {
        LOGGER.info("AGENTIC TOOLS - Extracting entities from [{}] : {}", docId, entities);
        String query = "Extract the following entities from the document: " + entities;

        String result;
        AiRequest ragrequest = new AiRequest();
        ragrequest.query = query;
        ragrequest.id = docId;

        try {
            ApiContent resp = RagService.rag(request, ragrequest, stream, sourcesAcc, true);
            result = returnMessageOrReason(resp, "Extraction failed");
        } catch (AgenticToolException ex) {
            throw ex; // If the Exception is a tool error, it must be thrown to the tool executor to stream the error
        } catch (Exception e) {
            LOGGER.error("AGENTIC TOOLS - Entity extraction failed - ERROR: {}", e.getLocalizedMessage());
            result = "Extraction failed.";
        }

        LOGGER.debug("AGENTIC TOOLS - Entity extraction failed - Result {}", result);
        return result;
    }

//    @ToolMeta(
//            label = "Searching documents...",
//            i18nKey = "tool.vectorSearch",
//            icon = "search"
//    )
//    @Tool("Execute a search query and return information and partial content from retrieved documents. (vector search)")
//    String vectorSearch(
//            @P("The search query") String query
//    ) {
//        LOGGER.debug("AGENTIC TOOLS - Vector Search - Query: {}", query);
//
//        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
//        int topK = config.getIntegerProperty(RagConfiguration.RRF_TOPK, 50);
//
//        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
//        String handler = "/vector";
//        req.addParameter("q", query);
//        req.addParameter("queryrag", query);
//        req.addParameter("fl", "title,id,parent_doc,exactContent,embedded_content,url");
//        req.addParameter("topK", String.valueOf(topK));
//        req.addParameter("start", "0");
//        req.addParameter("rows", String.valueOf(rows));
//        req.addParameter("wt", "json");
//
//        JSONObject root = SearchUtils.processSearch(req, handler);
//        JSONArray docs = SearchUtils.extractDocs(root);
//        LOGGER.debug("AGENTIC TOOLS - Vector Search - {}", docs.toJSONString());
//
//        // Add document to sources
//        addDocumentsToSource(docs);
//
//        return docs.toJSONString();
//    }

  /**
   * Runs an hybrid search, and return the String JSON response.
   * If the response returns nothing, runs a classic search (without the content)
   * @param query: Search query
   * @return a JSON String
   */
  @ToolMeta(label = "Searching documents...",
            i18nKey = "tool.hybridSearch",
            icon = "search")
    @Tool("Search and return information and partial content from documents (if available).")
    String hybridSearch(
            @P("The search query") String query
    ) {
        LOGGER.info("AGENTIC TOOLS - Hybrid Search - Query: {}", query);
        EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
        String handler = "/rrf";
        editableRequest.addParameter("q", query);
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("fl", "title,docId,parent_doc,embedded_content,url");
        editableRequest.addParameter("topK", "50");
        editableRequest.addParameter("start", "0");
        editableRequest.addParameter("rows", "10");
        editableRequest.addParameter("queryrag", query);
        editableRequest.addParameter("wt", "json");
        JSONObject root = SearchUtils.processSearch(editableRequest, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        LOGGER.debug("AGENTIC TOOLS - Hybrid Search - {}", docs.toJSONString());

        if (docs.isEmpty()) {
          LOGGER.debug("AGENTIC TOOLS - Hybrid Search - No result found with hydrid search. Running BM25 search instead.");
          return bm25Search(query);
        }

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

    @ToolMeta(label = "",
            i18nKey = "",
            icon = "")
    @Tool("If you don't have the tools you need to answer the request, use this one to describe precisely the tools you need, for future improvement.")
    String requestNewTool(
            @P("The description of the tool you would need") String description
    ) {
        LOGGER.warn("AGENTIC TOOLS - Requesting tool - {}", description);
        return "Note taken.";
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
            Document source = SearchUtils.jsonToDocument(doc);
            sourcesAcc.add(source);

        } catch (Exception e) {
            LOGGER.error("Could not add document to sources.", e);
        }
    }

    String returnMessageOrReason(ApiContent content, String errorMessage) {
        if (content.message != null && !content.message.isBlank()) {
            return content.message;
        } else if (content.error != null && content.error.reason != null) {
            return errorMessage + ": " + content.error.reason;
        } else {
            return errorMessage;
        }
    }

}
