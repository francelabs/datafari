package com.francelabs.datafari.utils.rag;

import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.rag.RagConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SearchUtils {

    private static final Logger LOGGER = LogManager.getLogger(SearchUtils.class.getName());


    public static JSONArray extractDocs(JSONObject root) {
        if (root == null) return new JSONArray();
        Object response = root.get("response");
        if (response instanceof JSONObject resp) {
            Object docs = resp.get("docs");
            if (docs instanceof JSONArray ja) return ja;
        }
        // alternative path (depending on handler)
        Object results = root.get("results");
        if (results instanceof JSONObject res) {
            Object docs = res.get("docs");
            if (docs instanceof JSONArray ja) return ja;
        }
        return new JSONArray();
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

        if (!config.getProperty(RagConfiguration.SEARCH_OPERATOR).isEmpty())
            request.setAttribute("q.op", config.getProperty(RagConfiguration.SEARCH_OPERATOR)); // TODO : does it work ?

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

    /** Convertit en String sans NPE. */
    private static String toStringSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Normalisation légère : trim + compaction des espaces multiples, préserve les retours ligne. */
    private static String normalize(String s) {
        if (s == null) return "";
        // Replace multiple spaces by one space
        String compact = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
        return compact;
    }

    // TODO : Move here common search request preparation
}
