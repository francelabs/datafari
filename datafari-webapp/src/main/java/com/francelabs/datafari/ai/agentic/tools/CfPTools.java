package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.api.RagAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class CfPTools {

    private static final Logger LOGGER = LogManager.getLogger(CfPTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;

    public CfPTools(HttpServletRequest request) {
        this.request = request;
        config = RagConfiguration.getInstance();
    }


    @Tool("Retrieves Call for Proposals documents list by category.")
    JSONArray findDocumentsByCategory(
            @P("Number of documents to retrieve") int rows,
            @P("The category. Allowed categories are: Catering, Furniture") String category
    ) {
        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        if (rows < 1) rows = 30;


        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*,llm_categories");
        req.addParameter("fq", "{!term f=llm_categories}" + category);
        req.addParameter("start", "0");
        req.addParameter("sort", "creation_date desc");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.info("AGENTIC TOOLS - Retrieving {} document from category '{}'", rows, category);
        return docs;
    }

    @Tool("Retrieves a list of Call for Proposals, filtered by category, sorted by creation_date desc.")
    JSONArray listCallsForProvidersByCategory(
            @P("Number of calls to retrieve (distinct CfP ID)") int rows,
            @P("Category filter (e.g., Catering, Furniture). Empty to ignore.") String category
    ) {
        if (rows < 1) rows = 30;

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        req.addParameter("q", "*:*");
        req.addParameter("wt", "json");
        req.addParameter("rows", "0"); // Only facets are needed

        if (category != null && !category.isBlank()) {
            req.addParameter("fq", "({!term f=llm_categories v='" + category + "'} AND agentic_cfp_id:[* TO *])");
        } else {
            req.addParameter("fq", "agentic_cfp_id:[* TO *]");
        }

        // JSON Facet: buckets of IDs sorted by date desc
        String jsonFacet = """
    {"cfp":{
        "type":"terms",
        "field":"agentic_cfp_id",
        "limit":%d,
        "sort":"maxDate desc",
        "facet":{"maxDate":"max(creation_date)"}
    }}""".formatted(rows);
        req.addParameter("json.facet", jsonFacet);

        JSONObject root = SearchUtils.processSearch(req, "/select");

        // Extraction: facets.cfp.buckets[].val -> JSONArray containing IDs ----
        JSONArray ids = new JSONArray();
        try {
            JSONObject facets = (JSONObject) root.get("facets");
            if (facets != null) {
                JSONObject cfp = (JSONObject) facets.get("cfp");
                if (cfp != null) {
                    JSONArray buckets = (JSONArray) cfp.get("buckets");
                    if (buckets != null) {
                        for (Object b : buckets) {
                            JSONObject bucket = (JSONObject) b;
                            Object val = bucket.get("val");
                            if (val != null) ids.add(String.valueOf(val));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CFP facet parsing error", e);
        }

        LOGGER.info("AGENTIC TOOLS - CFP IDs via JSON Facet: rows={} category='{}' -> {}", rows, category, ids.size());
        return ids;
    }

    @Tool("For a given CfP, returns information from CCTP (product type, delivery date, delivery requirements, guarantees duration).")
    String extractInfoFromCCTP(
            @P("CfP ID. IDs generally use the following format (X being digits): DCEXX") String cfpId
    ) {

        JSONArray docs = findCCTP(cfpId);
        if (docs.size() > 0) {
            try {
                JSONObject doc = (JSONObject) docs.get(0);
                String docId = (String) doc.get("id");
                String content = readPageFromDocument(docId, 0);

                // TODO : Use iterative refinement (Rag by document ?)

                ChatModel model = RagAPI.getChatModel(RagConfiguration.getInstance());
                String prompt = """
                        You are an assistant specialised in Call for Proposals (CfP).
                        You are provided a page from a CfP document. Extract the following information from the document above:
                        - Product type
                        - Delivery date
                        - Delivery requirements
                        - Guaranties duration
                        Document:
                        
                        
                        """;
                String response = model.chat(prompt + content);
                LOGGER.info("AGENTIC TOOLS - Extracting data from CCTP for CfP {} ", cfpId);
                LOGGER.debug("EBE - AGENTIC TOOLS - Extracted data: {}", response);
                return response;
            }catch (Exception e) {
                LOGGER.warn("Failed to extract information from CCTP: {}", cfpId);
                return "Failed to extract information from CCTP: '" + cfpId + "'";
            }

        } else {
            return "Can't find the CCTP document for Call for Proposals: '" + cfpId + "'";
        }
    }

    @Tool("For a given CfP, returns information from CCAP (min amount, max amount).")
    String extractInfoFromCCAP(
            @P("CfP ID . IDs generally use the following format (X being digits): DCEXX") String cfpId
    ) {

        JSONArray docs = findCCAP(cfpId);
        if (docs.size() > 0) {
            try {
                JSONObject doc = (JSONObject) docs.get(0);
                String docId = (String) doc.get("id");
                String content = readPageFromDocument(docId, 0);

                ChatModel model = RagAPI.getChatModel(RagConfiguration.getInstance());
                String prompt = """
                        You are an assistant specialised in Call for Proposals (CfP).
                        You are provided a page from a CfP document. Extract the following information from the document above:
                        - Minimum amount
                        - Maximum amount
                        
                        Document:
                        
                        """;
                String response = model.chat(prompt + content);
                LOGGER.info("AGENTIC TOOLS - Extracting data from CCAP for CfP {} ", cfpId);
                LOGGER.debug("AGENTIC TOOLS - Extracted data: {}", response);
                return response;
            }catch (Exception e) {
                LOGGER.warn("Failed to extract information from CCAP: {}", cfpId);
                return "Failed to extract information from CCAP: '" + cfpId + "'";
            }

        } else {
            return "Can't find the CCTP document for Call for Proposals: '" + cfpId + "'";
        }
    }



    @Tool("Retrieves the CCTP (Cahier des Charges Techniques Particuliers) for the specified CfP.")
    JSONArray findCCTP(
            @P("CfP ID (stored in documents' agentic_cfp_id metadata or at the beginning of files name). IDs generally use the following format (X being digits): DCEXX") String cfpId
    ) {

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*");
        req.addParameter("fq", "({!term f=agentic_cfp_doc_type v='CCTP'} AND {!term f=agentic_cfp_id v='" + cfpId + "'})");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.info("AGENTIC TOOLS - Retrieving CCTP for CfP {} ", cfpId);
        return docs;
    }


    @Tool("Retrieves the CCAP (Cahier des Clauses Administratives Particulières) for the specified CfP.")
    JSONArray findCCAP(
            @P("CfP ID. IDs generally use the following format (X being digits): DCEXX") String cfpId
    ) {

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,click_url,creation_date,agentic_*");
        req.addParameter("fq", "({!term f=agentic_cfp_doc_type v='CCAP'} AND {!term f=agentic_cfp_id v='" + cfpId + "'})");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        LOGGER.info("AGENTIC TOOLS - Retrieving CCAP for CfP {} ", cfpId);
        return docs;
    }

    @Tool("Read one page of the specified document, starting at the given page (0-based). If there in no more content to read for this document, it returns 'No content'. You must provide the exact document ID (not the CfP ID).")
    String readPageFromDocument(
            @P("Document ID (found in id or parent_doc fields)") String id,
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
        req.addParameter("fq", "{!term f=parent_doc}" + id);
        req.addParameter("collection", "VectorMain");
        req.addParameter("start", String.valueOf(start));
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("sort", "chunk_index asc");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);
        String mergedChunkContents = SearchUtils.mergeChunks(docs);

        LOGGER.info("AGENTIC TOOLS - Reading page {} of document  '{}'", page, id);
        if (mergedChunkContents.isEmpty()) {
            LOGGER.info("AGENTIC TOOLS - No content found in page {} of document  '{}'", page, id);
            return "No content";
        }
        LOGGER.info("AGENTIC TOOLS - Content found in page {} of document  '{}'. Length: {}", page, id, mergedChunkContents.length());
        return "========== PAGE " + page + ": ==========\n\n" + mergedChunkContents + "\n\n========== END OF PAGE " + page + " ==========\n\n";
    }

    @Tool("If you do not have the tools you need to answer the request, use this one to describe precisely the tools you need, for future improvement.")
    String requestNewTool(
            @P("The description of the tool you would need") String description
    ) {
        LOGGER.warn("AGENTIC TOOLS - Requesting tool - {}", description);
        return "Note taken.";
    }
}
