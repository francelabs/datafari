package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.services.RagService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import com.francelabs.datafari.utils.rag.SearchUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class CfPTools {

    private static final Logger LOGGER = LogManager.getLogger(CfPTools.class.getName());
    HttpServletRequest request;
    RagConfiguration config;
    private final SourcesAccumulator sourcesAcc;
    private final ChatStream stream;

    public CfPTools(HttpServletRequest request, ChatStream stream, SourcesAccumulator sourcesAcc) {
        this.request = request;
        this.stream = stream;
        this.sourcesAcc = sourcesAcc;
        config = RagConfiguration.getInstance();
    }

/*
    @Tool("""
            Retrieve a list by CFP, filtered by category.
            The returned data are:
            title, parent_doc (the ID of the document),
            """)
    JSONArray findDocumentsByCategory(
            @P("Number of documents to retrieve") int rows,
            @P("The category to retrieve. Existing categories are: Catering, Furniture") String category
    ) {
        // "rows" is the number of chunks (from VectorMain) to show to the LLM at once.
        // Warning, should not be too high
        LOGGER.info("AGENTIC TOOLS - Retrieving {} document from category '{}'", rows, category);
        if (rows < 1) rows = 30;


        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,creation_date,agentic_*,llm_categories");
        if(!category.isBlank()) req.addParameter("fq", "{!term f=llm_categories}" + category);
        req.addParameter("start", "0");
        req.addParameter("sort", "creation_date desc");
        req.addParameter("rows", String.valueOf(rows));
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        return docs;
    }
*/
    @Tool("Retrieve a list of CFP IDs, filtered by category, sorted by creation_date desc.")
    String listCallsForProvidersByCategory(
            @P("Number of CFP to retrieve") int rows,
            @P("Category filter (Catering, Furniture, Carpentry or Maintenance).") String category
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
        if (ids.isEmpty()) {
            return "No result found in category " + category + ". Available categories are Catering, Furniture, Carpentry and Maintenance.";
        }

        rows = Math.min(ids.size(), rows);
        StringBuilder idsStr = new StringBuilder();
        for (Object item : ids) {
            String id = (String) item;
            idsStr.append(" ").append(id);
        }

        // Prepare response
        if (category != null && !category.isBlank()) {
            String response = "The ID of the latest {{rows}} CFP from category '{{category}}' are: {{ids}}";
            return response.replace("{{category}}", category)
                    .replace("{{rows}}", String.valueOf(rows))
                    .replace("{{ids}}", idsStr.toString());
        } else {
            // If category is blank
            String response = "The ID of the latest {{rows}} CFP are: {{ids}}";
            return response.replace("{{rows}}", String.valueOf(rows))
                    .replace("{{ids}}", idsStr.toString());
        }
    }

    @Tool("For a given CFP ID, returns information from CCTP (product type, delivery date, delivery requirements, guarantees duration).")
    String extractInfoFromCCTP(
            @P("ID of the CFP. IDs use the format (X being digits): DCEXX") String cfpId
    ) {
        LOGGER.info("AGENTIC TOOLS - Extracting data from CCTP: {}", cfpId);
        JSONArray docs = findCCTP(cfpId);
        if (docs.size() > 0) {
            try {
                JSONObject doc = (JSONObject) docs.get(0);
                String docId = (String) doc.get("id");

                String query = "Extract the following information from the document : Product type, Delivery date, Delivery requirements, Guaranties duration";

                String results = "";
                AiRequest ragrequest = new AiRequest();
                ragrequest.query = query;
                ragrequest.id = docId;

                try {
                    ApiContent resp = RagService.rag(request, ragrequest, stream, sourcesAcc);
                    if (resp.message != null && !resp.message.isBlank()) {
                        results = resp.message;
                    } else if (resp.error != null) {
                        results = "Extraction failed: " + resp.error.reason;
                    } else {
                        results = "Extraction failed.";
                    }
                } catch (Exception e) {
                    LOGGER.error("AGENTIC TOOLS - Extracting from CCAP - ERROR: {}", e.getLocalizedMessage());
                    results = "Extraction failed.";
                }

                LOGGER.debug("AGENTIC TOOLS - Extracting from CCTP - Result {}", results);
                return results;

            } catch (Exception e) {
                LOGGER.error("Cannot extract information from CCTP.");
            }
        }
        return "Cannot retrieve CCTP for CFP " + cfpId + ".";
    }


    @Tool("For a given CFP, returns information from CCAP (Minimum amount, Maximum amount).")
    String extractInfoFromCCAP(
            @P("ID of the CFP. IDs use the format (X being digits): DCEXX") String cfpId
    ) {
        LOGGER.info("AGENTIC TOOLS - Extracting data from CCAP: {}", cfpId);
        JSONArray docs = findCCAP(cfpId);
        if (!docs.isEmpty()) {
            try {
                JSONObject doc = (JSONObject) docs.getFirst();
                String docId = (String) doc.get("id");

                String query = "Extract the following information from the document : Minimum amount, Maximum amount";

                String results = "";
                AiRequest ragrequest = new AiRequest();
                ragrequest.query = query;
                ragrequest.id = docId;

                try {
                    ApiContent resp = RagService.rag(request, ragrequest, stream, sourcesAcc);
                    if (resp.message != null && !resp.message.isBlank()) {
                        results = resp.message;
                    } else if (resp.error != null) {
                        results = "Extraction failed: " + resp.error.reason;
                    } else {
                        results = "Extraction failed.";
                    }
                } catch (Exception e) {
                    LOGGER.error("AGENTIC TOOLS - Extracting from CCAP - ERROR: {}", e.getLocalizedMessage());
                    results = "Extraction failed.";
                }


                LOGGER.debug("AGENTIC TOOLS - Extracting from CCAP - Result {}", results);
                return results;

            } catch (Exception e) {
                LOGGER.error("Cannot extract information from CCAP.");
            }
        }
        return "Cannot extract information from CCAP.";
    }


    //    @Tool("Retrieves the CCTP (Cahier des Charges Techniques Particuliers) for the specified CFP.")
    JSONArray findCCTP(
            @P("ID of the CFP. IDs use the format (X being digits): DCEXX") String cfpId
    ) {
        LOGGER.info("AGENTIC TOOLS - Retrieving CCTP for CFP {} ", cfpId);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,creation_date,agentic_*");
        req.addParameter("fq", "({!term f=agentic_cfp_doc_type v='CCTP'} AND {!term f=agentic_cfp_id v='" + cfpId + "'})");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        addDocumentToSource((JSONObject) docs.getFirst());

        return docs;
    }


    //    @Tool("Retrieves the CCAP (Cahier des Clauses Administratives Particulières) for the specified CFP.")
    JSONArray findCCAP(
            @P("CFP ID. IDs generally use the following format (X being digits): DCEXX") String cfpId
    ) {
        LOGGER.info("AGENTIC TOOLS - Retrieving CCAP for CFP {} ", cfpId);

        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        String handler = "/select";
        req.addParameter("q", "*:*");
        req.addParameter("fl", "title,parent_doc,id,url,creation_date,agentic_*");
        req.addParameter("fq", "({!term f=agentic_cfp_doc_type v='CCAP'} AND {!term f=agentic_cfp_id v='" + cfpId + "'})");
        req.addParameter("start", "0");
        req.addParameter("rows", "1");
        req.addParameter("wt", "json");

        JSONObject root = SearchUtils.processSearch(req, handler);
        JSONArray docs = SearchUtils.extractDocs(root);

        addDocumentToSource((JSONObject) docs.getFirst());

        return docs;
    }

    @Tool("Read one page of the specified document. If the requested page does not exist for this document, it returns 'No content'. You must provide the exact document ID (not the CFP ID).")
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

        addDocumentToSource((JSONObject) docs.getFirst());

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
            String id = (doc.get("parent_doc") != null) ? (String) doc.get("parent_doc") : (String) doc.get(AiService.ID_FIELD);
            String title = ((JSONArray) doc.get(AiService.TITLE_FIELD)).getFirst().toString();
            String url = (String) doc.get(AiService.URL_FIELD);

            String content;
            if (doc.get(AiService.EXACT_CONTENT_FIELD) != null) {
                content = (String) ((JSONArray) doc.get(AiService.EXACT_CONTENT_FIELD)).get(0);
            } else if (doc.get("embedded_content") != null) {
                content = (String) doc.get("embedded_content");
            } else {
                content = "No content available.";
            }
            Document source = Document.document(content);
            source.metadata().put(AiService.ID_FIELD, id)
                    .put(AiService.TITLE_FIELD, title)
                    .put(AiService.URL_FIELD, url);
            sourcesAcc.add(source);

        } catch (Exception e) {
            LOGGER.error("Could not add document to sources.", e);
        }
    }
}
