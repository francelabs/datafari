package com.francelabs.datafari.utils.rag;

import com.francelabs.datafari.aggregator.servlet.SearchAggregator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import dev.langchain4j.data.document.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.*;

public class SearchUtils {

    private static final Logger LOGGER = LogManager.getLogger(SearchUtils.class.getName());


    /**
     * Extract documents from a Solr search response
     * @param root: A raw JSONObject Solr response
     * @return a JSONArray containing documents
     */
    public static JSONArray extractDocs(JSONObject root) {
        if (root == null) return new JSONArray();
        Object response = root.get("response");
        if (response instanceof JSONObject resp) {
            Object docs = resp.get("docs");
            if (docs instanceof JSONArray ja) return dedupeByEmbeddedContent(ja);
        }
        // alternative path (depending on handler)
        Object results = root.get("results");
        if (results instanceof JSONObject res) {
            Object docs = res.get("docs");
            if (docs instanceof JSONArray ja) return dedupeByEmbeddedContent(ja);
        }
        return new JSONArray();
    }

    /**
     * Deduplicating documents based on normalized embedded_content.
     * */
    @SuppressWarnings("unchecked")
    public static JSONArray dedupeByEmbeddedContent(JSONArray docs) {
        JSONArray out = new JSONArray();
        if (docs == null || docs.isEmpty()) return out;
        Set<String> seen = new HashSet<>();  // stocking hash to limit memory

        for (Object o : docs) {
            if (!(o instanceof JSONObject d)) continue;

            String text = extractEmbeddedText(d);
            if (text == null || text.isEmpty()) {
                // No basis for deduplicating, keeping the document
                out.add(d);
            } else {
                String norm = normalize(text);
                String sig  = sha256(norm);  // stable sha256 signature
                if (seen.add(sig)) {
                    out.add(d);  // First occurrence -> kept
                }
            }
        }
        return out;
    }

    /**
     * Process a search in Datafari using the user query
     * @param request: the original user request (can be an @EditableRequest)
     *                The request must contain the following parameters:
     *               - q
     *               - queryrag, if vector or hybrid search (if missing, "q" is used instead)
     *               - Any needed Solr parameter for the search
     * @param handler:  The search handler ("/select", "/vector", "/rrf"...)
     * @return A JSONObject containing the search results
     */
    public static JSONObject processSearch(HttpServletRequest request, String handler) throws InvalidParameterException {

        final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
        final String protocol = request.getScheme() + ":";
        final RagConfiguration config = RagConfiguration.getInstance();

        // Preparing query for Search process
        String userQuery = request.getParameter("q");

        if (userQuery == null) {
            LOGGER.error("SearchUtils - ERROR. No query provided.");
            throw new InvalidParameterException("No query provided.");
        } else {
            LOGGER.debug("SearchUtils - Processing search for request : q={}", userQuery);
        }

        String queryrag = request.getParameter("queryrag");
        // If queryrag is missing, set it to userQuery
        if (queryrag == null || queryrag.isEmpty()) {
            String[] queryragParam = { userQuery };
            parameterMap.put("queryrag", queryragParam);
        }

        if (!config.getProperty(RagConfiguration.SEARCH_OPERATOR).isEmpty()) {
            String[] op = {config.getProperty(RagConfiguration.SEARCH_OPERATOR)};
            parameterMap.put("q.op", op);
        }

        // Override parameters with request attributes (set by the code and not from the client, so
        // they prevail over what has been given as a parameter)
        final Iterator<String> attributeNamesIt = request.getAttributeNames().asIterator();
        while (attributeNamesIt.hasNext()) {
            final String name = attributeNamesIt.next();
            if (request.getAttribute(name) instanceof String) {
                final String[] value = { (String) request.getAttribute(name) };
                parameterMap.put(name, value);
            }
        }

        switch (handler) {
            case "/rrf":
                return SearchAPI.hybridSearch(protocol, request.getUserPrincipal(), parameterMap);
            case "/vector":
                return SearchAPI.search(protocol, handler, request.getUserPrincipal(), parameterMap);
            case "/select":
            default:
                handler = "/select";
                return SearchAPI.search(protocol, handler, request.getUserPrincipal(), parameterMap);
        }
    }


    /**
     * Process a search in Datafari using the rewritten queries
     * This method uses an editable version of the original HttpServletRequest.
     * The request object is updated in order to process a custom search.
     * @param originalRequest : The HttpServletRequest
     * @param q The search query for BM25
     * @param queryrag The search query for Vector Search
     * @return a JSONObject containing search results, with the following fields:
     *      docId, title, exactContent, url, llm_summary
     */
    public static JSONObject performCustomSearch(HttpServletRequest originalRequest, String q, String queryrag, String retrievalMethod, RagConfiguration config) {
        EditableHttpServletRequest request = new EditableHttpServletRequest(originalRequest);
        request.addParameter("q", q);
        request.addParameter("queryrag", queryrag); // The rewritten query is used only for Vector Search
        request.addParameter("hl", "false");
        request.addParameter("fl", "docId,title,exactContent,embedded_content,url,llm_summary");

        String handler;
        switch(retrievalMethod) {
            case "vector":
                handler = "/vector";
                break;
            case "rrf":
                handler = "/rrf";
                break;
            case "bm25":
            default:
                // If BM25, the number of results is limited to MAX_FILES
                request.addParameter("rows", config.getProperty(RagConfiguration.MAX_FILES, "3"));
                handler = "/select";
        }
        request.setPathInfo(handler);

        // Any additional RAG-related search options can be added here

        LOGGER.debug("AiPowered - Performing search using {} handler. q={}", handler, q);
        return processSearch(request, handler);
    }

    /** Retrieve embedded_content  */
    private static String extractEmbeddedText(JSONObject d) {
        Object val = d.get("embedded_content");
        if (val == null) return null;
        String s = String.valueOf(val);
        return s.isEmpty() ? null : s;
    }

    public static String mergeChunks(JSONArray docs) {
        String separator = "\n\n";
        if (docs == null || docs.isEmpty()) return "Empty content";
        StringBuilder sb = new StringBuilder();

        for (Object o : docs) {
            if (!(o instanceof JSONObject doc)) continue;

            // Retrieve embedded_content
            Object val = doc.get("embedded_content");
            if (val == null) continue;

            String chunk = normalize(toStringSafe(val));
            if (!chunk.isEmpty()) sb.append(chunk).append(separator);
        }
        return sb.toString();
    }

    /** Convert to String without NPE. */
    private static String toStringSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Soft Normalisation: trim + compacting multiple spaces */
    private static String normalize(String s) {
        if (s == null) return "";
        // Replace multiple spaces by one space
        String compact = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
        return compact;
    }



    /** Hash SHA-256 en hex (pour éviter de stocker de gros textes dans le Set). */
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return s;
        }
    }

    /**
     * Find a documents by its ID from Solr, using Datafari API methods.
     * This method uses an editable version of the original HttpServletRequest.
     * The updated request object is updated in order to process.
     * @param originalRequest : The HttpServletRequest
     * @param docId The docId of the document
     * @return a JSONObject containing search results, with the following fields :
     *      docId, title, exactContent, url, llm_summary
     */
    public static JSONObject findDocumentById(HttpServletRequest originalRequest, String docId) throws ServletException, IOException {
        EditableHttpServletRequest request = new EditableHttpServletRequest(originalRequest);
        request.addParameter("q", "*:*");
        request.addParameter("fq", "(docId:\"" + docId + "\" OR id:\"" + docId + "\")"); // Filter by id OR docId
        request.addParameter("hl", "false");
        request.addParameter("rows", "1");
        request.addParameter("fl", "docId,title,exactContent,url,llm_summary");
        request.setPathInfo("/select");

        LOGGER.debug("AiPowered - Retrieving document {}.", docId);
        return SearchAggregator.doGetSearch(request, null);
    }


  /**
   * Convert a JSONObject (from Datafari search) to a Langchain4j Document)
   * @param doc JSONObject
   * @return Document
   */
  public static Document jsonToDocument(JSONObject doc){
    try {
      String docId = (String) doc.get("docId");
      String url = (String) doc.get(AiService.URL_FIELD);
      String title;
      Object t = doc.get(AiService.TITLE_FIELD);
      if (t instanceof JSONArray ja) title = String.valueOf(ja.getFirst());
      else title = String.valueOf(t);

      String content;
      if (doc.get(AiService.EXACT_CONTENT_FIELD) != null) {
        content = String.valueOf(((JSONArray)doc.get(AiService.EXACT_CONTENT_FIELD)).getFirst());
      } else if (doc.get("embedded_content") != null) {
        content = String.valueOf(doc.get("embedded_content"));
      } else content = "No content available.";

      Document source = Document.document(content);
      source.metadata().put(AiService.ID_FIELD, docId)
          .put(AiService.TITLE_FIELD, title)
          .put(AiService.URL_FIELD, url);
      return source;
    } catch (Exception e) { return null; }
  }

  /**
   * When "filters" field is provided in AiPowered API, its content is converted in Solr parameter
   * and stored in the "request" object. Filters will then apply to any search ran by AiServices.
   * @param request : The original HttpServletRequest object to be updated
   * @param params : The AiRequest object containing the filters
   * @return a transformed EditableHttpServletRequest
   */
  public static EditableHttpServletRequest filtersParamToFq(HttpServletRequest request, AiRequest params) {
      EditableHttpServletRequest editableRequest = new EditableHttpServletRequest(request);
      Map<String, List<String>> filters = params.filters;
      for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
          System.out.println(entry.getKey() + "/" + entry.getValue());

          // If entry key is "id", we add a "fq" parameter
          if ("id".equals(entry.getKey())) {
              List<String> ids = entry.getValue();
              StringBuilder sb = new StringBuilder(); // The generated String must look like "({{id3}} OR {{id3}} OR {{id3}})"
              sb.append("(");

              for (int i = 0; i < ids.size(); i++) {

                  sb.append(ids.get(i));
                  // Add "OR", unless it is last element
                  if (i < ids.size() - 1) {
                      sb.append(" OR ");
                  }
              }
              sb.append(")");
              String strIds = sb.toString();
              String fq = "id:" + strIds + " OR docId:" + strIds;
              editableRequest.addParameter("fq", fq);
          } else {
              List<String> param =  entry.getValue();
              String[] array = param.toArray(new String[0]);
              editableRequest.addParameters(entry.getKey(), array);
          }

      }

      return editableRequest;
  }

    // TODO : Move here common search request preparation
}
