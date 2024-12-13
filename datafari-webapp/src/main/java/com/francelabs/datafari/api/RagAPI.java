package com.francelabs.datafari.api;

import com.francelabs.datafari.rag.*;
import com.francelabs.datafari.rag.OpenAiLlmService;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.VectorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.MapSolrParams;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;

public class RagAPI extends SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());
  public static final List<String> FORMAT_VALUES = List.of("bulletpoint", "text", "stepbystep");
  public static final String EXACT_CONTENT = "exactContent";

  // SOLR CLIENT
  private static final String DEFAULT_COLLECTION = "VectorMain";
  private static final String DEFAULT_HOST = "localhost:2181";
  private static final String COLLECTION = "collection";
  private static final String HOST = "host";


  public static JSONObject rag(final HttpServletRequest request) throws IOException {

    // Get RAG specific configuration
    RagConfiguration config = RagConfiguration.getInstance();
    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }

    // Is RAG enabled ?
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_RAG))
      return writeJsonError(422, "ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.");

    // Set LlmService
    LlmService service = getLlmService(config);

    // Search
    JSONObject result;
    JSONObject result2;
    try {
      result = processSearch(config, request);
      result2 = processVectorSearch(request.getParameter("q"), service);
      LOGGER.info(result2);
    } catch (Exception e) {
      LOGGER.error("RAG error. An error occurred while retrieving data.", e);
      return writeJsonError(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    }

    // Extract document content and data
    List<DocumentForRag> initialDocumentsList;
    List<DocumentForRag> documentsList;
    try {
      initialDocumentsList = extractDocumentsList(result, config);
    } catch (FileNotFoundException e) {
      LOGGER.warn("RAG warning. The query cannot be answered because no associated documents were found.");
      return writeJsonError(428, "ragNoFileFound", "Sorry, I couldn't find any relevant document to answer your request.");
    }


    // Vector embedding
    if (config.getBooleanProperty(RagConfiguration.ENABLE_VECTOR_SEARCH)) {
      initialDocumentsList = VectorUtils.processVectorSearch(initialDocumentsList, request, config);
    } else if (config.getBooleanProperty(RagConfiguration.ENABLE_LOGS)) {
      LOGGER.info("Vector search was ignored due to configuration");
    }

    // Chunking
    documentsList = initialDocumentsList;
    if (config.getBooleanProperty(RagConfiguration.ENABLE_CHUNKING)) {
      documentsList = ChunkUtils.chunkDocuments(config, initialDocumentsList);
    } else if (config.getBooleanProperty(RagConfiguration.ENABLE_LOGS)) {
      LOGGER.info("Chunking was ignored due to configuration");
    }

    // Prompting
    List<String> prompts;
    prompts = PromptUtils.documentsListToPrompts(config, documentsList, request);

    String message;

    message = service.invoke(prompts, request);

    LOGGER.debug("LLM response: {}", message);

    // Return final message
    if (!message.isEmpty()) {
      message = cleanLlmFinalMessage(message);
      final JSONObject response = writeJsonResponse(message, documentsList);
      return response;
    } else {
      return writeJsonError(428, "ragNoValidAnswer", "Sorry, I could not find an answer to your question.");
    }

  }

  private static @NotNull LlmService getLlmService(RagConfiguration config) {
    // Select an LLM service
    LlmService service;
    String llmService = config.getProperty(RagConfiguration.LLM_SERVICE);
    switch(llmService) {
      case "openai":
        service = new OpenAiLlmService(config);
        break;
      case "datafari":
      default:
        service = new DatafariLlmService(config);
    }
    return service;
  }

  private static @NotNull JSONObject writeJsonResponse(String message, List<DocumentForRag> documentsList) {
    final JSONObject response = new JSONObject();
    response.put("status", "OK");
    JSONObject content = new JSONObject();
    content.put("message", message);
    content.put("documents", mergeSimilarDocuments(documentsList, message));
    response.put("content", content);
    return response;
  }

  private static JSONObject writeJsonError(int code, String errorLabel, String message) {
    final JSONObject response = new JSONObject();
    final JSONObject error = new JSONObject();
    final JSONObject content = new JSONObject();
    response.put("status", "ERROR");
    error.put("code", code);
    error.put("label", errorLabel);
    response.put("message", message);
    response.put("documents", new ArrayList<>());
    response.put("error", error);
    response.put("content", content);
    return response;
  }

  /**
   *
   * @param message : The LLM String response
   * @return a cleaner String
   */
  private static String cleanLlmFinalMessage(String message) {
    message = message.replace("\\n", "\n");
    message = message.replace("/n", "\n");
    message = message.replace("\n ", "\n");
    message = message.replace("•", "\n•");
    message = message.replace("\n+", "\n");
    message = message.trim();

    return message;
  }

  /**
   *
   * @param result The Search result
   * @param config
   * @return
   * @throws InvalidPropertiesFormatException
   * @throws FileNotFoundException
   */
  private static List<DocumentForRag> extractDocumentsList(JSONObject result, RagConfiguration config) throws InvalidPropertiesFormatException, FileNotFoundException {
    // Handling search results
    // Retrieving list of documents : id, title, url
    List<DocumentForRag> documentsList;
    documentsList = getDocumentList(result, config);

    if (documentsList.isEmpty()) {
      throw new FileNotFoundException("The query cannot be answered because no associated documents were found.");
    }
    return documentsList;
  }

  /**
   * Process a classic search in Datafari using the user query
   * @param config RAG configuration
   * @param request the original user request
   * @return A JSONObject containing the search results
   */
  private static JSONObject processSearch(RagConfiguration config, HttpServletRequest request) throws Exception {

    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";

    // Preparing query for Search process
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      LOGGER.error("RAG error : No query provided.");
      throw new Exception("No query provided.");
    } else if (config.getBooleanProperty(RagConfiguration.ENABLE_LOGS)) {
      LOGGER.info("Request processed by RagAPI : {}", userQuery);
    }

    if (!config.getProperty(RagConfiguration.SEARCH_OPERATOR).isEmpty())
      request.setAttribute("q.op", config.getProperty(RagConfiguration.SEARCH_OPERATOR));

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

    // Add Solr vectorizer tag in the query, overwrite q param

    String model = config.getProperty(RagConfiguration.LLM_EMBEDDINGS_MODEL);
        // TODO Default value for embeddings model?
    String topK = config.getProperty(RagConfiguration.MAX_CHUNKS);
    userQuery = "{!text_to_vector model="+model+" f=vector topK="+topK+"}" + userQuery;
    final String[] value = { userQuery };
    parameterMap.put("q", value);

    return search(protocol, handler, request.getUserPrincipal(), parameterMap);
  }


  /**
   * Get a JSONArray containing a list of documents (ID, url, title and content) returned by the Search
   * @param result : The result of the search, containing Solr documents
   * @param config RAG configuration
   * @return JSONArray
   */
  private static List<DocumentForRag> getDocumentList(JSONObject result, RagConfiguration config) {
    try {

      JSONObject response = (JSONObject) result.get("response");
      List<DocumentForRag> documentsList = new ArrayList<>();

      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        int maxFiles = Math.min(config.getIntegerProperty(RagConfiguration.MAX_FILES), docs.size()); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < maxFiles; i++) {

          DocumentForRag document = new DocumentForRag();
          String title = ((JSONArray) ((JSONObject) docs.get(i)).get("title")).get(0).toString();
          String id = (String) ((JSONObject) docs.get(i)).get("id");
          String url = (String) ((JSONObject) docs.get(i)).get("url");

          document.setId(id);
          document.setTitle(title);
          document.setUrl(url);

          // Add the content to the processed document
          JSONArray content = (JSONArray) ((JSONObject) docs.get(i)).get(EXACT_CONTENT);
          if (content != null && content.get(0) != null) {
            document.setContent(content.get(0).toString());
          }

          documentsList.add(document);
        }
        return documentsList;
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while retrieving the list of documents.", e);
    }
    return new ArrayList<>();

  }


  /**
   * The "documentList" containing sources of the answer might contain duplicates.
   * Some document might not be used in the LLM response.
   * This methode merge thoses duplicated entries, and remove useless ones.
   * @return List<DocumentForRag> The final list
   */
  private static List<DocumentForRag> mergeSimilarDocuments(List<DocumentForRag> documentList, String response) {
    // Removing Duplicates
    Map<String, DocumentForRag> map = new HashMap<>();
    for (DocumentForRag doc : documentList) {
      String id = doc.getId();
        if (map.containsKey(id) || !response.toUpperCase().contains(doc.getTitle().toUpperCase())) {
            continue;
        }
        map.put(id, doc);
    }
    return new ArrayList<>(map.values());
  }

  private static JSONObject processVectorSearch(String q, LlmService service) throws SolrServerException, IOException {
    JSONObject jsonObject = new JSONObject();
    LOGGER.info("search q={}", q);


    // Process search
    try  {
        final QueryResponse response;
        try (CloudSolrClient client = initClient()) {
            // Query vector embedding
            float[] vector = service.embed(q);

            // ?q={!text_to_vector model=a-model f=vector topK=10}hello world query
            final Map<String, String> queryParamMap = new HashMap<>();
            queryParamMap.put("q", Arrays.toString(vector));
            queryParamMap.put("fl", "id, vector, parent_donc, content");
            MapSolrParams queryParams = new MapSolrParams(queryParamMap);

            response = client.query(queryParams);
        }
        final SolrDocumentList documents = response.getResults();
        jsonObject.put("documents", documents);
        return jsonObject;
    } catch (Exception e) {
      LOGGER.error("Error during vector search", e);
      return jsonObject;
    }

  }

  /**
   * Initiate the SolrClient
   * @return The SolrClient
   */
  private static CloudSolrClient initClient() throws SolrServerException, IOException {
    CloudSolrClient client;
    client = new CloudSolrClient.Builder(new ArrayList<>(Collections.singletonList(DEFAULT_HOST)), Optional.empty())
            .withDefaultCollection(COLLECTION)
            .build();
    final SolrPing ping = new SolrPing();
    client.request(ping);
    return client;
  }
}