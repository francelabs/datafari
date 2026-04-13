package com.francelabs.datafari.api;

import java.io.StringReader;
import java.security.InvalidParameterException;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfig;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfigurationManager;
import com.francelabs.datafari.utils.*;
import com.francelabs.datafari.utils.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
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

  private static final String VECTOR_MAIN_COLLECTION = "VectorMain";

  private static final Set<String> ALLOWED_COLLECTIONS = Set.of(
      Core.FILESHARE.toString(),
      VECTOR_MAIN_COLLECTION
  );

  private static Set<String> getAllowedHandlers() {
    final Set<String> allowedHandlers = new HashSet<>();
    allowedHandlers.add("/select");
    allowedHandlers.add("/vector");
    allowedHandlers.add("/rrf");
    allowedHandlers.add("/hybrid");
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

  private static JSONObject buildErrorResponse(final int code, final String message) {
    final JSONObject response = new JSONObject();
    final JSONObject error = new JSONObject();
    error.put("code", code);
    error.put("message", message);
    response.put("error", error);
    return response;
  }

  private static String extractRequestedCollection(final Map<String, String[]> parameterMap) {
    final String[] collections = parameterMap.get("collection");
    if (collections == null || collections.length == 0 || collections[0] == null) {
      return null;
    }

    final String requestedCollection = collections[0].trim();
    if (requestedCollection.isEmpty()) {
      return null;
    }

    return requestedCollection;
  }

  private static String validateRequestedCollection(final String handler, final String requestedCollection) {
    if (requestedCollection == null) {
      return null;
    }

    if (requestedCollection.contains(",")) {
      throw new InvalidParameterException("Multiple collections are not allowed");
    }

    if (!ALLOWED_COLLECTIONS.contains(requestedCollection)) {
      throw new InvalidParameterException(
          "Unauthorized collection. Allowed collections are " + ALLOWED_COLLECTIONS);
    }

    if ("/vector".equals(handler) && !VECTOR_MAIN_COLLECTION.equals(requestedCollection)) {
      throw new InvalidParameterException(
          "Unauthorized collection for /vector. Only " + VECTOR_MAIN_COLLECTION + " is allowed.");
    }

    return requestedCollection;
  }

  public static JSONObject search(final String protocol, final String handler, final Principal principal,
      final Map<String, String[]> parameterMap) {
    return search(protocol, handler, principal, parameterMap, Core.FILESHARE.toString());
  }

  public static JSONObject search(final String protocol, final String handler, final Principal principal,
      final Map<String, String[]> parameterMap, String collection) {
    Timer timer = new Timer(SearchAPI.class.getName(), "search");

    final int querySizeLimit = 4000;

    // If the "fl" contains "embedded_content", it must be edited
    String[] fl = parameterMap.get("fl");
    if (fl != null && fl[0] != null && fl[0].contains("embedded_content") && !fl[0].contains("embedded_content:")) {
      fl[0] = fl[0].replace(
          "embedded_content",
          "embedded_content:content_fr,embedded_content:content_en,embedded_content:content_es,embedded_content:content_de");
    }

    // Check the handler
    final Set<String> allowedHandlers = getAllowedHandlers();
    if (!allowedHandlers.contains(handler)) {
      timer.stop();
      return buildErrorResponse(401, "Unauthorized handler. Allowed handlers are " + allowedHandlers.toString());
    }

    // The query must not exceed 4000 characters
    if (parameterMap.get("q") != null && parameterMap.get("q").length > 0
        && parameterMap.get("q")[0].length() > querySizeLimit) {
      timer.stop();
      return buildErrorResponse(413, "The query is too long");
    }

    // Validate client-provided collection, if any
    try {
      final String requestedCollection = extractRequestedCollection(parameterMap);
      final String validatedCollection = validateRequestedCollection(handler, requestedCollection);
      if (validatedCollection != null) {
        collection = validatedCollection;
      }
    } catch (InvalidParameterException e) {
      timer.stop();
      return buildErrorResponse(401, e.getMessage());
    }

    // Add missing vector search parameters if required
    if ("/vector".equals(handler)) {
      collection = VECTOR_MAIN_COLLECTION;
      parameterMap.put("collection", new String[] { VECTOR_MAIN_COLLECTION });

      RagConfiguration ragConfiguration = RagConfiguration.getInstance();
      if (!parameterMap.containsKey("q")) {
        parameterMap.put("q", new String[] { "" });
      } else if (!parameterMap.containsKey("queryrag")) {
        parameterMap.put("queryrag", parameterMap.get("q"));
      }
      if (!parameterMap.containsKey("topK")) {
        String[] topK = new String[] { ragConfiguration.getProperty(RagConfiguration.SOLR_TOPK, "50") };
        parameterMap.put("topK", topK);
      }
      if (!parameterMap.containsKey("filteredSearchThreshold")
          && ragConfiguration.getBooleanProperty(RagConfiguration.SOLR_ENABLE_ACORN, false)) {
        String[] filteredSearchThreshold = new String[] {
            ragConfiguration.getProperty(RagConfiguration.SOLR_FILTERED_SEARCH_THRESHOLD, "60")
        };
        parameterMap.put("filteredSearchThreshold", filteredSearchThreshold);
      }

      if (!ragConfiguration.getBooleanProperty(RagConfiguration.SOLR_ENABLE_LADR)) {
        String[] seedQuery = new String[] { "id:__no_such_doc__" };
        parameterMap.put("seedQuery", seedQuery);
      }

      EbdModelConfigurationManager manager = new EbdModelConfigurationManager();
      if (!parameterMap.containsKey("model")) {
        EbdModelConfig modelConfig = manager.getActiveModelConfig();
        String[] model = new String[] { modelConfig.getName() };
        parameterMap.put("model", model);

        String[] vectorField = new String[] { modelConfig.getVectorField() };
        parameterMap.put("vectorField", vectorField);

      } else if (!parameterMap.containsKey("vectorField")) {
        String modelName = parameterMap.get("model")[0];
        EbdModelConfig modelConfig = manager.getModelByName(modelName);

        String[] vectorField = new String[] { modelConfig.getVectorField() };
        parameterMap.put("vectorField", vectorField);
      }
    } else if (collection == null || collection.isEmpty()) {
      collection = Core.FILESHARE.toString();
    }

    timer.top("1");
    IndexerServer solr;
    IndexerServer promolinkCore = null;
    IndexerQueryResponse queryResponse = null;
    IndexerQueryResponse queryResponsePromolink = null;
    IndexerQuery queryPromolink = null;

    final IndexerQuery params = IndexerServerManager.createQuery();
    params.addParams(parameterMap);

    // Do not trust raw client collection param
    params.removeParam("collection");
    params.setParam("collection", collection);

    String requestingUser = AuthenticatedUserName.getName(principal);

    if (requestingUser == null) {
      requestingUser = "";
    }

    timer.top("2");

    if (requestingUser.toLowerCase().contentEquals("search-aggregator")
        || requestingUser.toLowerCase().contentEquals("service-account-search-aggregator")) {
      if (params.getParamValue("AuthenticatedUserName") != null) {
        requestingUser = params.getParamValue("AuthenticatedUserName");
      }
    }

    params.removeParam("AuthenticatedUserName");
    params.removeParam("qt");

    timer.top("3");
    try {
      solr = IndexerServerManager.getIndexerServer(collection);

      promolinkCore = IndexerServerManager.getIndexerServer(Core.PROMOLINK);
      queryPromolink = IndexerServerManager.createQuery();

      if (!requestingUser.isEmpty()) {
        params.setParam("AuthenticatedUserName", requestingUser);
      }

      final String queryParam = params.getParamValue("query");
      if (queryParam != null) {
        params.setParam("q", queryParam);
        params.removeParam("query");
      }

      timer.top("4");
      if (params.getParamValue("original_query") == null
          && params.getParamValue("entQ") != null
          && !params.getParamValue("entQ").trim().isEmpty()) {
        final String categories = EntityAutocompleteConfiguration.getInstance()
            .getProperty(EntityAutocompleteConfiguration.CATEGORIES);
        final JSONParser parser = new JSONParser();
        final JSONObject jsonCategories = (JSONObject) parser.parse(categories);

        final String xmlToextract = params.getParamValue("entQ");
        String finalQuery = xmlToextract;
        final Pattern pattern = Pattern.compile("<span((?!<\\/span>).)*<\\/span>");
        final Matcher matcher = pattern.matcher(xmlToextract);

        while (matcher.find()) {
          for (int i = 0; i < matcher.groupCount(); i++) {
            final String match = matcher.group(i);
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(match));
            final Document document = builder.parse(is);
            final Node node = document.getChildNodes().item(0);

            if (node.hasAttributes()) {
              if (node.getAttributes().getNamedItem("class") != null
                  && node.getAttributes().getNamedItem("class").getNodeValue().toLowerCase().contains("entity-hl")) {
                final Node entityIdN = node.getAttributes().getNamedItem("entity-id");
                if (entityIdN != null) {
                  final String entityId = entityIdN.getNodeValue();
                  String entityVal = node.getTextContent();

                  if (node.getAttributes().getNamedItem("entity-payload") != null) {
                    entityVal = node.getAttributes().getNamedItem("entity-payload").getNodeValue().trim();
                  }

                  final JSONObject selectedCategory = (JSONObject) jsonCategories.get(entityId);

                  if (selectedCategory.containsKey("queryPrefix")
                      && !selectedCategory.get("queryPrefix").toString().isEmpty()) {
                    entityVal = selectedCategory.get("queryPrefix").toString() + entityVal;
                  }
                  if (selectedCategory.containsKey("querySuffix")
                      && !selectedCategory.get("querySuffix").toString().isEmpty()) {
                    entityVal = entityVal + selectedCategory.get("querySuffix").toString();
                  }

                  finalQuery = finalQuery.replace(match, entityVal);
                }
              }
            }
          }
        }

        params.removeParam("q");
        params.setParam("q", finalQuery);
        params.removeParam("entQ");
      }

      timer.top("5");
      final DatafariMainConfiguration config = DatafariMainConfiguration.getInstance();
      final String ontologyStatus = config.getProperty(DatafariMainConfiguration.ONTOLOGY_ENABLED, "false");
      if (ontologyStatus.equals("true") && handler.equals("/select")) {
        final boolean languageSelection = Boolean.valueOf(
            config.getProperty(DatafariMainConfiguration.ONTOLOGY_LANGUAGE_SELECTION));
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

      timer.top("6");
      String requestHandler = handler;
      if (parameterMap.get("source") != null
          && parameterMap.get("source").length > 0
          && !parameterMap.get("source")[0].equalsIgnoreCase("all")) {
        requestHandler += "-" + parameterMap.get("source")[0];
      }
      params.removeParam("source");
      params.setRequestHandler(requestHandler);

      timer.top("7");
      queryResponse = solr.executeQuery(params);

      timer.top("8");
      if (promolinkCore != null && !params.getParamValue("q").toString().equals("*:*")) {
        queryPromolink.setQuery(params.getParamValue("q") + " \"" + params.getParamValue("q") + "\"");
        queryPromolink.setFilterQueries("-dateBeginning:[NOW/DAY+1DAY TO *]", "-dateEnd:[* TO NOW/DAY]");
        queryResponsePromolink = promolinkCore.executeQuery(queryPromolink);
      }

      if (handler.equals("/select")) {
        if (params.getParamValue("id") != null && !params.getParamValue("id").equals("")) {
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
      }

      timer.stop();
      if (promolinkCore != null) {
        return ResponseTools.writeSolrJResponse(
            handler, params, queryResponse, queryPromolink, queryResponsePromolink, requestingUser);
      } else {
        return ResponseTools.writeSolrJResponse(handler, params, queryResponse, null, null, requestingUser);
      }

    } catch (final Exception e) {
      LOGGER.error("Unknown error " + e.getMessage(), e);
      timer.stop();
      return buildErrorResponse(500, e.getMessage());
    }
  }

  public static JSONObject search(final HttpServletRequest request) {
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());

    if (request.getParameter("id") == null && request.getAttribute("id") != null
        && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }

    final Iterator<String> attributeNamesIt = request.getAttributeNames().asIterator();
    while (attributeNamesIt.hasNext()) {
      final String name = attributeNamesIt.next();
      if (request.getAttribute(name) != null && request.getAttribute(name) instanceof String) {
        final String value[] = { (String) request.getAttribute(name) };
        parameterMap.put(name, value);
      }
    }

    if ("/rrf".equals(handler) || "/hybrid".equals(handler)) {
      return hybridSearch(protocol, request.getUserPrincipal(), parameterMap);
    }

    return search(protocol, handler, request.getUserPrincipal(), parameterMap);
  }

  public static JSONObject hybridSearch(final String protocol, final Principal principal,
      final Map<String, String[]> parameterMap) {

    RagConfiguration config = RagConfiguration.getInstance();

    String defaultRows = config.getProperty(RagConfiguration.SOLR_TOPK, "10");
    String defaultTopK = config.getProperty(RagConfiguration.RRF_TOPK, "60");
    String[] rows = parameterMap.getOrDefault("rows", new String[] { defaultRows });
    String[] start = parameterMap.getOrDefault("start", new String[] { "0" });
    String[] topK = parameterMap.getOrDefault("topK", new String[] { defaultTopK });

    int startInt = 0;
    int rowsInt = 10;
    int topKInt = 60;
    try {
      startInt = Integer.parseInt(start[0]);
      rowsInt = Integer.parseInt(rows[0]);
      topKInt = Math.max(Integer.parseInt(topK[0]), startInt + rowsInt);
    } catch (NumberFormatException e) {
      // Keep default values
    }

    parameterMap.put("rows", new String[] { Integer.toString(topKInt) });
    parameterMap.put("start", new String[] { "0" });
    parameterMap.put("topK", new String[] { Integer.toString(topKInt) });

    if (!parameterMap.containsKey("q")) {
      if (parameterMap.containsKey("queryrag")) {
        parameterMap.put("q", parameterMap.get("queryrag"));
      } else {
        parameterMap.put("q", new String[] { "" });
        LOGGER.error("No 'q' or 'queryrag' param provided for hybrid search.");
        throw new InvalidParameterException("No 'q' or 'queryrag' param provided for hybrid search.");
      }
    } else if (!parameterMap.containsKey("queryrag")) {
      parameterMap.put("queryrag", parameterMap.get("q"));
    }

    EbdModelConfigurationManager manager = new EbdModelConfigurationManager();
    if (!parameterMap.containsKey("model")) {
      EbdModelConfig modelConfig = manager.getActiveModelConfig();
      String[] model = new String[] { modelConfig.getName() };
      String[] vectorField = new String[] { modelConfig.getVectorField() };
      parameterMap.put("model", model);
      parameterMap.put("vectorField", vectorField);

    } else if (!parameterMap.containsKey("vectorField")) {
      String modelName = parameterMap.get("model")[0];
      EbdModelConfig modelConfig = manager.getModelByName(modelName);
      String[] vectorField = new String[] { modelConfig.getVectorField() };
      parameterMap.put("vectorField", vectorField);
    }

    JSONObject bm25Result = search(protocol, "/select", principal, parameterMap, VECTOR_MAIN_COLLECTION);
    JSONObject vectorSearchResult = search(protocol, "/vector", principal, parameterMap, VECTOR_MAIN_COLLECTION);

    int k = config.getIntegerProperty(RagConfiguration.RRF_RANK_CONSTANT, 60);
    List<String> fusedDocIds = HybridSearchUtils.fuseResultsWithRRF(bm25Result, vectorSearchResult, k);

    Map<String, JSONObject> allDocs = new HashMap<>();

    JSONObject bm25Response = (JSONObject) bm25Result.get("response");
    JSONArray bm25Docs = (JSONArray) bm25Response.get("docs");
    if (bm25Docs != null) {
      for (Object obj : bm25Docs) {
        JSONObject doc = (JSONObject) obj;
        String id = (String) doc.get("docId");
        if (id == null) {
          id = (String) doc.get("id");
        }
        allDocs.put(id, doc);
      }
    }

    JSONObject vectorResponse = (JSONObject) vectorSearchResult.get("response");
    JSONArray vectorDocs = (JSONArray) vectorResponse.get("docs");
    if (vectorDocs != null) {
      for (Object obj : vectorDocs) {
        JSONObject doc = (JSONObject) obj;
        String id = (String) doc.get("docId");
        if (id == null) {
          id = (String) doc.get("id");
        }
        allDocs.putIfAbsent(id, doc);
      }
    }

    JSONArray fusedDocs = new JSONArray();
    for (String id : fusedDocIds) {
      JSONObject doc = allDocs.get(id);
      if (doc != null) {
        fusedDocs.add(doc);
      }
    }

    int endInt = Math.min(startInt + rowsInt, fusedDocs.size());

    JSONArray paginatedDocs = new JSONArray();
    for (int i = startInt; i < endInt; i++) {
      paginatedDocs.add(fusedDocs.get(i));
    }

    JSONObject response = new JSONObject();
    response.put("numFound", fusedDocs.size());
    response.put("start", startInt);
    response.put("docs", paginatedDocs);
    response.put("numFoundExact", true);

    JSONObject finalResult = new JSONObject();
    finalResult.put("response", response);

    JSONObject bm25Highlighting = (JSONObject) bm25Result.get("highlighting");
    JSONObject vectorHighlighting = (JSONObject) vectorSearchResult.get("highlighting");
    JSONObject mergedHighlighting = mergeHighlighting(bm25Highlighting, vectorHighlighting, paginatedDocs);

    if (!mergedHighlighting.isEmpty()) {
      finalResult.put("highlighting", mergedHighlighting);
    }

    JSONObject facets = (JSONObject) bm25Result.get("facet_counts");
    if (facets != null && !facets.isEmpty()) {
      finalResult.put("facet_counts", facets);
    }

    return finalResult;
  }

  public static JSONObject mergeHighlighting(JSONObject bm25Highlighting, JSONObject vectorHighlighting,
      JSONArray paginatedDocs) {
    JSONObject mergedHighlighting = new JSONObject();

    for (Object obj : paginatedDocs) {
      JSONObject doc = (JSONObject) obj;
      String id = (String) doc.get("id");

      JSONObject highlight = new JSONObject();

      if (bm25Highlighting != null && bm25Highlighting.containsKey(id)) {
        JSONObject bm25HL = (JSONObject) bm25Highlighting.get(id);
        highlight.putAll(bm25HL);
      }

      if (vectorHighlighting != null && vectorHighlighting.containsKey(id)) {
        JSONObject vectorHL = (JSONObject) vectorHighlighting.get(id);
        highlight.putAll(vectorHL);
      }

      if (!highlight.isEmpty()) {
        mergedHighlighting.put(id, highlight);
      }
    }

    return mergedHighlighting;
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