package com.francelabs.datafari.api;

import java.io.StringReader;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.francelabs.datafari.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.statistics.StatsPusher;

public class SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(SearchAPI.class.getName());

  private static Set<String> getAllowedHandlers() {
    final Set<String> allowedHandlers = new HashSet<>();
    allowedHandlers.add("/select");
    allowedHandlers.add("/stats");
    allowedHandlers.add("/statsQuery");
    allowedHandlers.add("/");

    final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
    if (!config.getProperty(DatafariMainConfiguration.USER_ALLOWED_HANDLERS).equals("")) {
      final String userAllowedHandlers = config.getProperty(DatafariMainConfiguration.USER_ALLOWED_HANDLERS);
      final List<String> userAllowedHandlersList = Arrays.asList(userAllowedHandlers.split(","));
      for (int i = 0; i < userAllowedHandlersList.size(); i++) {
        allowedHandlers.add(userAllowedHandlersList.get(i));
      }
    }
    return allowedHandlers;
  }

  public static JSONObject search(final String protocol, final String handler, final Principal principal, final Map<String, String[]> parameterMap) {
    Timer timer = new Timer(SearchAPI.class.getName(), "search");

    // This value was set arbitrarily
    final int querySizeLimit = 4000;

    final JSONObject response = new JSONObject();

    final Set<String> allowedHandlers = getAllowedHandlers();

    if (!allowedHandlers.contains(handler)) {
      final JSONObject error = new JSONObject();
      error.put("code", 401);
      error.put("message", "Unauthorized handler. Allowed handlers are " + allowedHandlers.toString());
      response.put("error", error);
      timer.stop();
      return response;
    }

    // The query must not exceed 4000 characters
    if (parameterMap.get("q") != null && parameterMap.get("q")[0].length() > querySizeLimit) {
      final JSONObject error = new JSONObject();
      error.put("code", 413);
      error.put("message", "The query is too long");
      response.put("error", error);
      timer.stop();
      return response;
    }

    timer.top("1");
    IndexerServer solr;
    IndexerServer promolinkCore = null;
    IndexerQueryResponse queryResponse = null;
    IndexerQueryResponse queryResponsePromolink = null;
    IndexerQuery queryPromolink = null;

    final IndexerQuery params = IndexerServerManager.createQuery();
    params.addParams(parameterMap);
    String requestingUser = AuthenticatedUserName.getName(principal);

    if (requestingUser == null) {
      requestingUser = "";
    }

    timer.top("2");

    // If the user is the search-agregator, it is allowed to pass the user as parameter so retrieve it
    // Otherwise keep the current user and format it
    if (requestingUser.toLowerCase().contentEquals("search-aggregator") || requestingUser.toLowerCase().contentEquals("service-account-search-aggregator")) {
      if (params.getParamValue("AuthenticatedUserName") != null) {
        requestingUser = params.getParamValue("AuthenticatedUserName");
      }
    }

    // Remove potential AuthenticatedUserName param, as this param should only be set by the API
    params.removeParam("AuthenticatedUserName");

    timer.top("3");
    try {

      switch (handler) {
      case "/stats":
      case "/statsQuery":
        solr = IndexerServerManager.getIndexerServer(Core.STATISTICS);
        break;
      default:
        solr = IndexerServerManager.getIndexerServer(Core.FILESHARE);
        promolinkCore = IndexerServerManager.getIndexerServer(Core.PROMOLINK);
        queryPromolink = IndexerServerManager.createQuery();

        // Add AuthenticatedUserName param if user authenticated
        if (!requestingUser.isEmpty()) {
          params.setParam("AuthenticatedUserName", requestingUser);
        }

        final String queryParam = params.getParamValue("query");
        if (queryParam != null) {
          params.setParam("q", queryParam);
          params.removeParam("query");
        }

        break;
      }

      timer.top("4");
      // If entities are present in the query, need to modify the query in order to
      // take them into account. Don't rely on entQ if we are performing a spellcheked request
      // (i.e. original_query is defined)
      if (params.getParamValue("original_query") == null && params.getParamValue("entQ") != null && !params.getParamValue("entQ").trim().isEmpty()) {
        // Get the entities configuration
        final String categories = EntityAutocompleteConfiguration.getInstance().getProperty(EntityAutocompleteConfiguration.CATEGORIES);
        final JSONParser parser = new JSONParser();
        final JSONObject jsonCategories = (JSONObject) parser.parse(categories);

        // In the entQ param, the entitties term are encapsulated in a span tag, so need
        // to extract them
        final String xmlToextract = params.getParamValue("entQ");
        String finalQuery = xmlToextract;
        // Regex to extract any span tag
        final Pattern pattern = Pattern.compile("<span((?!<\\/span>).)*<\\/span>");
        final Matcher matcher = pattern.matcher(xmlToextract);
        // For each matched span tag
        while (matcher.find()) {
          for (int i = 0; i < matcher.groupCount(); i++) {
            final String match = matcher.group(i);
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(match));
            final Document document = builder.parse(is);
            // There is only one node so get it
            final Node node = document.getChildNodes().item(0);
            if (node.hasAttributes()) {
              // If the current span tag contains a class named "entity-hl", it is an entity
              if (node.getAttributes().getNamedItem("class") != null && node.getAttributes().getNamedItem("class").getNodeValue().toLowerCase().contains("entity-hl")) {
                final Node entityIdN = node.getAttributes().getNamedItem("entity-id");
                if (entityIdN != null) {
                  // extract the entityId (which is a category id in the configuration file)
                  final String entityId = entityIdN.getNodeValue();
                  // extract the entity value
                  String entityVal = node.getTextContent();
                  // If the entity has a payload, the payload is used instead of the display name
                  // for
                  // searching in the index.
                  if (node.getAttributes().getNamedItem("entity-payload") != null) {
                    entityVal = node.getAttributes().getNamedItem("entity-payload").getNodeValue().trim();
                  }
                  // retrieve the category configuration thanks to the extracted id
                  final JSONObject selectedCategory = (JSONObject) jsonCategories.get(entityId);
                  // apply the prefix and suffix defined in the category conf to the entity value
                  if (selectedCategory.containsKey("queryPrefix") && !selectedCategory.get("queryPrefix").toString().isEmpty()) {
                    entityVal = selectedCategory.get("queryPrefix").toString() + entityVal;
                  }
                  if (selectedCategory.containsKey("querySuffix") && !selectedCategory.get("querySuffix").toString().isEmpty()) {
                    entityVal = entityVal + selectedCategory.get("querySuffix").toString();
                  }
                  // replace the whole span tag by the entity value (which is now encapsulated)
                  finalQuery = finalQuery.replace(match, entityVal);
                }
              }
            }
          }
        }
        // replace the original query by the new one with entities
        params.removeParam("q");
        params.setParam("q", finalQuery);
        params.removeParam("entQ");
      }

      timer.top("5");
      final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
      final String ontologyStatus = config.getProperty(DatafariMainConfiguration.ONTOLOGY_ENABLED, "false");
      if (ontologyStatus.equals("true") && handler.equals("/select")) {
        final boolean languageSelection = Boolean.valueOf(config.getProperty(DatafariMainConfiguration.ONTOLOGY_LANGUAGE_SELECTION));
        String parentsLabels = config.getProperty(DatafariMainConfiguration.ONTOLOGY_PARENTS_LABELS);
        String childrenLabels = config.getProperty(DatafariMainConfiguration.ONTOLOGY_CHILDREN_LABELS);
        if (languageSelection) {
          parentsLabels += "_fr";
          childrenLabels += "_fr";
        }
        final int facetFieldLength = params.getParamValues("facet.field").length;
        final String[] facetFields = Arrays.copyOf(params.getParamValues("facet.field"), facetFieldLength + 2);
        facetFields[facetFieldLength] = "{!ex=" + parentsLabels + "}" + parentsLabels;
        facetFields[facetFieldLength + 1] = "{!ex=" + childrenLabels + "}" + childrenLabels;
        params.setParam("facet.field", facetFields);
      }

      if (!config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS).equals("") && handler.equals("/select")) {
        params.setParam("collection", config.getProperty(DatafariMainConfiguration.SOLR_MAIN_COLLECTION) + "," + config.getProperty(DatafariMainConfiguration.SOLR_SECONDARY_COLLECTIONS));
      }

      timer.top("6");
      // perform query define the request handler which may change if a specific
      // source has been provided
      String requestHandler = handler;
      if (parameterMap.get("source") != null && parameterMap.get("source").length > 0 && !parameterMap.get("source")[0].equalsIgnoreCase("all")) {
        requestHandler += "-" + parameterMap.get("source")[0];
      }
      params.removeParam("source");
      params.setRequestHandler(requestHandler);
      timer.top("7");
      queryResponse = solr.executeQuery(params);
      timer.top("8");
      if (promolinkCore != null && !params.getParamValue("q").toString().equals("*:*")) {
        // launch a request in the promolink core only if it's a request onZ the
        // FileShare core

        queryPromolink.setQuery(params.getParamValue("q") + " \"" + params.getParamValue("q") + "\"");
        queryPromolink.setFilterQueries("-dateBeginning:[NOW/DAY+1DAY TO *]", "-dateEnd:[* TO NOW/DAY]");
        queryResponsePromolink = promolinkCore.executeQuery(queryPromolink);
      }
      switch (handler) {
        case "/select":
          // If there is no id there is no need to record stats
          if (params.getParamValue("id") != null && !params.getParamValue("id").equals("")) {
            // index
            final long numFound = queryResponse.getNumFound();
            final int QTime = queryResponse.getQTime();
            final IndexerQuery statsParams = IndexerServerManager.createQuery();
            statsParams.addParams(params.getParams());
            statsParams.setParam("numFound", Long.toString(numFound));
            if (numFound == 0) {
              statsParams.setParam("noHits", "No Hits");
            }
            statsParams.setParam("QTime", Integer.toString(QTime));

            StatsPusher.pushQuery(statsParams, protocol);
          }
          break;
        case "/stats":
          solr.processStatsResponse(queryResponse);
          break;
        default:
          break;
      }

      timer.stop();
      if (promolinkCore != null) {
        return ResponseTools.writeSolrJResponse(handler, params, queryResponse, queryPromolink, queryResponsePromolink, requestingUser);
      } else {
        return ResponseTools.writeSolrJResponse(handler, params, queryResponse, null, null, requestingUser);
      }

    } catch (final Exception e) {
      // TODO fine handling of exception
      LOGGER.error("Unknown error " + e.getMessage());
      final JSONObject error = new JSONObject();
      error.put("code", 500);
      error.put("message", e.getMessage());
      response.put("error", error);
      timer.stop();
      return response;
    }
  }

  public static JSONObject search(final HttpServletRequest request) {
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
    return search(protocol, handler, request.getUserPrincipal(), parameterMap);

  }

  static String getHandler(final HttpServletRequest servletRequest) {
    String pathInfo = servletRequest.getPathInfo();
    if (pathInfo == null) {
      pathInfo = servletRequest.getServletPath();
      if (pathInfo == null) {
        pathInfo = "/select";
      }
    }
    return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
  }

}
