package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class CustomTools {

    private static final Logger LOGGER = LogManager.getLogger(CustomTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;

    public CustomTools(HttpServletRequest request) {
        this.request = request;
        config = RagConfiguration.getInstance();
    }


    @Tool("Retrieves 'Call for Providers' documents list by category.")
    JSONArray findDocumentsByCategory(
            @P("Number of documents to retrieve") int rows,
            @P("The category. Allowed categories are: Catering, Furniture") int category
    ) {
        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        if (rows < 1) rows = 30;


        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*");
        req.addParameter("fq", "{!term f=agentic_cfp_category}" + category);
        req.addParameter("start", "0");
        req.addParameter("sort", "creation_date desc");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.debug("AGENTIC TOOLS - Retrieving {} document from category '{}'", rows, category);
        return docs;
    }


    @Tool("Retrieves the CCTP (Cahier des Charges Techniques Particuliers) for the specified CfP.")
    JSONArray findCCTP(
            @P("CfP ID (stored in documents' agentic_cfp_id metadata or at the beginning of files name). IDs generally use the following format (X being digits): DCEXX") int cfpId
    ) {

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*");
        req.addParameter("fq", "{!term f=agentic_cfp_id}" + cfpId);
        req.addParameter("fq", "{!term f=agentic_cfp_doc_type}CCTP");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.debug("AGENTIC TOOLS - Retrieving CCTP for CfP {} ", cfpId);
        return docs;
    }


    @Tool("Retrieves the CCAP (Cahier des Clauses Administratives Particulières) for the specified CfP.")
    JSONArray findCCAP(
            @P("CfP ID (stored in documents' agentic_cfp_id metadata or at the beginning of files name). IDs generally use the following format (X being digits): DCEXX") int cfpId
    ) {

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*");
        req.addParameter("fq", "{!term f=agentic_cfp_id}" + cfpId);
        req.addParameter("fq", "{!term f=agentic_cfp_doc_type}CCTP");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.debug("AGENTIC TOOLS - Retrieving CCTP for CfP {} ", cfpId);
        return docs;
    }

    @Tool("Read one of a document, starting at the given page (0-based). If there in no more content to read, it returns 'No content'. You must provide the exact document ID (not the CfP ID).")
    String readNextChunks(
            @P("Document ID (found in 'id' or 'parent_doc' fields)") String id,
            @P("Page index (0-based)") int page
    ) {
        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        int rows = config.getIntegerProperty(RagConfiguration.SOLR_TOPK, 10);
        int start = Math.max(0, page) * rows;


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
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        String mergedChunkContents = SearchUtils.mergeChunks(docs);

        LOGGER.debug("AGENTIC TOOLS - Reading page {} of document  '{}'", page, id);
        if (mergedChunkContents.isEmpty()) return "No content";
        return "========== PAGE " + page + ": ==========\n\n" + mergedChunkContents + "\n\n========== END OF PAGE " + page + " ==========\n\n";
    }

    @Tool("If you don't have the tools you need to answer the request, use this one to describe precisely the tools you need, for future improvement.")
    String requestNewTool(
            @P("The description of the tool you would need") String description
    ) {
        LOGGER.warn("AGENTIC TOOLS - Requesting tool - {}", description);
        return "Note taken.";
    }
}
