package com.francelabs.datafari.api;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.EntityAutocompleteConfiguration;
import com.francelabs.datafari.utils.ResponseTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;

public class RagAPI extends SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());



  public static JSONObject rag(final HttpServletRequest request) {
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }
    // Override parameters with request attributes (set by the code and not from the client, so
    // they prevail over what has been given as a parameter)
    final Iterator<String> attributeNamesIt = request.getAttributeNames().asIterator();
    while (attributeNamesIt.hasNext()) {
      final String name = attributeNamesIt.next();
      if (request.getAttribute(name) != null && request.getAttribute(name) instanceof String) {
        final String value[] = { (String) request.getAttribute(name) };
        parameterMap.put(name, value);
      }
    }
    JSONObject result = search(protocol, handler, request.getUserPrincipal(), parameterMap);
    return result;

  }

  // Todo : Create a method to send document and prompt to user
  private JSONObject callAiApi(JSONObject result) {
    return result;
  }

}
