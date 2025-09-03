package com.francelabs.datafari.utils.rag;

import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
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

    // TODO : Move here common search request preparation
}
