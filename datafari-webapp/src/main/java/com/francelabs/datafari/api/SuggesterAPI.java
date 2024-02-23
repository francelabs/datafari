package com.francelabs.datafari.api;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.EntityAutocompleteConfiguration;
import com.francelabs.datafari.utils.ResponseTools;

public class SuggesterAPI {

  private static final Logger LOGGER = LogManager.getLogger(SuggesterAPI.class.getName());

  private static final Set<String> getAllowedCores() {
    final Set<String> allowedCores = new HashSet<String>();

    allowedCores.add(Core.FILESHARE.toString());

    // Entity autocomplete
    try {
      final JSONParser parser = new JSONParser();
      final JSONArray entityAutocomplete = (JSONArray) parser.parse(EntityAutocompleteConfiguration.getInstance().getProperty(EntityAutocompleteConfiguration.AUTOCOMPLETE_SUGGESTERS, "[]"));
      entityAutocomplete.forEach(entity -> {
        final JSONObject jsonEntity = (JSONObject) entity;
        final String core = jsonEntity.get("solrCore").toString();
        allowedCores.add(core);
      });
    } catch (final org.json.simple.parser.ParseException e) {
      LOGGER.error("Unable to parse the JSON of the AdvancedSearchConfiguration autocomplete fields", e);
    }

    return allowedCores;
  }

  private static Set<String> getAllowedHandlers() {
    // Only handlers for suggesters are allowed here:
    // -the standard /suggest
    // -the advanced autocomplete handlers
    // -any handler defined together with an entity suggester.
    final Set<String> allowedHandlers = new HashSet<>();
    allowedHandlers.add("/suggest");
    allowedHandlers.add("/proposals");

    try {
      final JSONParser parser = new JSONParser();
      // Advanced autocomplete
      final JSONObject advAutocompleteFields = (JSONObject) parser.parse(AdvancedSearchConfiguration.getInstance().getProperty(AdvancedSearchConfiguration.AUTOCOMPLETE_FIELDS));
      for (final Object key : advAutocompleteFields.keySet()) {
        final String handler = "/" + advAutocompleteFields.get(key).toString();
        allowedHandlers.add(handler);
      }

      // Entity autocomplete
      final JSONArray entityAutocomplete = (JSONArray) parser.parse(EntityAutocompleteConfiguration.getInstance().getProperty(EntityAutocompleteConfiguration.AUTOCOMPLETE_SUGGESTERS, "[]"));
      entityAutocomplete.forEach(entity -> {
        final JSONObject jsonEntity = (JSONObject) entity;
        final String handler = "/" + jsonEntity.get("servlet").toString();
        allowedHandlers.add(handler);
      });
    } catch (final org.json.simple.parser.ParseException e) {
      LOGGER.error("Unable to parse the JSON of the AdvancedSearchConfiguration autocomplete fields", e);
    }
    return allowedHandlers;
  }

  public static JSONObject suggest(final String protocol, final String handler, final Principal principal, final Map<String, String[]> parameterMap) {
    LOGGER.info("Suggester API called");

    final JSONObject response = new JSONObject();

    final Set<String> allowedHandlers = getAllowedHandlers();
    final Set<String> allowedCores = getAllowedCores();

    if (!allowedHandlers.contains(handler)) {
      final JSONObject error = new JSONObject();
      error.put("code", 401);
      error.put("message", "Unauthorized handler. Allowed suggester handlers are " + allowedHandlers.toString());
      response.put("error", error);
      return response;
    }

    IndexerServer solr;
    IndexerQueryResponse queryResponse = null;

    final IndexerQuery params = IndexerServerManager.createQuery();
    params.addParams(parameterMap);

    String requestingUser = AuthenticatedUserName.getName(principal);

    if (requestingUser == null) {
      requestingUser = "";
    }

    // If the user is the search-agregator, it is allowed to pass the user as
    // parameter so retrieve it
    // Otherwise keep the current user and format it
    if (requestingUser.toLowerCase().contentEquals("search-aggregator") || requestingUser.toLowerCase().contentEquals("service-account-search-aggregator")) {
      if (params.getParamValue("AuthenticatedUserName") != null) {
        requestingUser = params.getParamValue("AuthenticatedUserName");
      }
    }

    // Remove potential AuthenticatedUserName param, as this param should only be
    // set by the API
    params.removeParam("AuthenticatedUserName");

    try {
      String core = Core.FILESHARE.toString();
      if (parameterMap.get("core") != null && parameterMap.get("core").length > 0 && !parameterMap.get("core")[0].trim().equals("") && allowedCores.contains(parameterMap.get("core")[0])) {
        core = parameterMap.get("core")[0];
      }
      solr = IndexerServerManager.getIndexerServer(core);

      // Add AuthenticatedUserName param if user authenticated
      if (!requestingUser.isEmpty()) {
        params.setParam("AuthenticatedUserName", requestingUser);
      }

      final String queryParam = params.getParamValue("query");
      if (queryParam != null) {
        params.setParam("q", queryParam);
        params.removeParam("query");
      }

      // TODO: how to manage secondary collections for suggesters ?

      // perform query define the request handler which may change if a specific
      // source has been provided
      String requestHandler = handler;
      if (parameterMap.get("source") != null && parameterMap.get("source").length > 0 && !parameterMap.get("source")[0].isEmpty() && !parameterMap.get("source")[0].equalsIgnoreCase("all")) {
        requestHandler += "-" + parameterMap.get("source")[0];
      }
      params.removeParam("source");
      params.setRequestHandler(requestHandler);
      queryResponse = solr.executeQuery(params);

      return ResponseTools.writeSolrJResponse(handler, params, queryResponse, null, null, requestingUser);

    } catch (final Exception e) {
      // TODO fine handling of exception
      LOGGER.error("Unknown error " + e.getMessage());
      final JSONObject error = new JSONObject();
      error.put("code", 500);
      error.put("message", e.getMessage());
      response.put("error", error);
      return response;
    }

  }

  public static JSONObject suggest(final HttpServletRequest request) {
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    return suggest(protocol, handler, request.getUserPrincipal(), request.getParameterMap());
  }

  private static String getHandler(final HttpServletRequest servletRequest) {
    String pathInfo = servletRequest.getPathInfo();
    if (pathInfo == null) {
      pathInfo = servletRequest.getServletPath();
      if (pathInfo == null) {
        pathInfo = "/suggest";
      }
    }
    return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
  }

  public static JSONObject suggestAndSearch(HttpServletRequest request) {
    JSONObject response = new JSONObject();
    response.put("search", SearchAPI.search(request));
    response.put("suggest", SuggesterAPI.suggest(request));

    if (((JSONObject) response.get("search")).containsKey("error") || ((JSONObject) response.get("suggest")).containsKey("error")) {
      final JSONObject error = new JSONObject();
      error.put("code", 500);
      error.put("message", "And error was met while retrieving suggestions");
      response.put("error", error);
      return response;
    }
    return response;
  }
}
