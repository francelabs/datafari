package com.francelabs.datafari.api;

import com.francelabs.datafari.rag.*;
import com.francelabs.datafari.rag.OpenAiLlmService;
import com.francelabs.datafari.utils.rag.AiDocument;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.VectorUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.security.authentication.DisabledException;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.InvalidParameterException;
import java.util.*;

public class RagAPI extends SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());
  public static final List<String> FORMAT_VALUES = List.of("bulletpoint", "text", "stepbystep");
  public static final String EXACT_CONTENT = "exactContent";


  public static JSONObject rag(final HttpServletRequest request) throws IOException {

    // Get RAG specific configuration
    RagConfiguration config = RagConfiguration.getInstance();

    // Todo : This code looks dead. Check if it can be deleted.
    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }

    // Is RAG enabled ?
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_RAG))
      return writeJsonError(422, "ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.");

    // Search
    JSONObject result;
    try {
      result = processSearch(config, request);
    } catch (Exception e) {
      LOGGER.error("RAG error. An error occurred while retrieving data.", e);
      return writeJsonError(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.");
    }

    // Extract document content and data
    List<AiDocument> initialDocumentsList;
    List<AiDocument> documentsList;
    try {
      initialDocumentsList = extractDocumentsList(result, config);
    } catch (FileNotFoundException e) {
      LOGGER.warn("RAG warning. The query cannot be answered because no associated documents were found.");
      return writeJsonError(428, "ragNoFileFound", "Sorry, I couldn't find any relevant document to answer your request.");
    }


    // Vector embedding
    if (config.getBooleanProperty(RagConfiguration.ENABLE_VECTOR_SEARCH)) {
      initialDocumentsList = VectorUtils.processVectorSearch(initialDocumentsList, request);
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

    // Select an LLM service
    LlmService service = getLlmService(config);

    message = service.invoke(prompts, request);

    LOGGER.debug("LLM response: {}", message);

    // Return final message
    if (!message.isEmpty()) {
      message = cleanLlmFinalMessage(message);
      return writeJsonResponse(message, documentsList);
    } else {
      return writeJsonError(428, "ragNoValidAnswer", "Sorry, I could not find an answer to your question.");
    }

  }

  /**
   * Select the proper LlmService class, based on RagConfiguration
   * @param config RagConfiguration
   * @return LlmService
   */
  private static @NotNull LlmService getLlmService(RagConfiguration config) {
    LlmService service;
    String llmService = config.getProperty(RagConfiguration.LLM_SERVICE);
    switch(llmService) {
      case "datafari":
        // Todo : Clean remove DatafariLlmService
        LOGGER.warn("The DatafariLlmService is deprecated, since the Datafari AI Agent is now OpenAI-compatible.");
        LOGGER.warn("Please use OpenAiLlmService instead.");
        service = new DatafariLlmService(config);
        break;
      case "openai":
      default:
        service = new OpenAiLlmService(config);
    }
    return service;
  }

  public static String summarize(final HttpServletRequest request, Document doc) throws IOException {
    // Get RAG configuration
    RagConfiguration config = RagConfiguration.getInstance();
    // Select an LLM service
    LlmService service = getLlmService(config);

    // Vector embedding
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
      throw new DisabledException("The summarization feature is disabled.");
    }

    // Chunk document
    List<TextSegment> segments = ChunkUtils.chunkContent(doc, config);

    // Setup prompt
    Message initialPrompt = PromptUtils.createInitialPromptForSummarization();

    // Summarize all chunks
    List<Message> summaries = new ArrayList<>();
    for (TextSegment segment: segments) {
      List<Message> prompts = new ArrayList<>();
      prompts.add(initialPrompt);
      prompts.add(PromptUtils.textSegmentsToMessage(segment, "user", config));

      String response = service.generate(prompts, request);
      Message responseMessage = new Message("assistant", response);
      summaries.add(responseMessage);
    }

    // Merge all summaries
    if (summaries.size() > 1) {
      // Merge All Summaries as prompts and add an extra instruction to generate a synthesis
      Message mergeAllSummariesPrompt = PromptUtils.createPromptForMergeAllSummaries();
      summaries.add(mergeAllSummariesPrompt);
      return service.generate(summaries, request);

    } else if (summaries.size() == 1) {
      return summaries.get(0).getContent();
    } else {
      throw new IOException("Could not generate any summary for this document");
    }

  }

  private static @NotNull JSONObject writeJsonResponse(String message, List<AiDocument> documentsList) {
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
   * @param result The raw Solr Search result
   * @param config RagConfiguration
   * @return A List of AiDocuments
   * @throws FileNotFoundException if the provided results doesn't contain any document
   */
  private static List<AiDocument> extractDocumentsList(JSONObject result, RagConfiguration config) throws FileNotFoundException {
    // Handling search results
    // Retrieving list of documents : id, title, url
    List<AiDocument> documentsList;
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
  private static JSONObject processSearch(RagConfiguration config, HttpServletRequest request) throws InvalidParameterException {

    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";

    // Preparing query for Search process
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      LOGGER.error("RAG error : No query provided.");
      throw new InvalidParameterException("No query provided.");
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
    return search(protocol, handler, request.getUserPrincipal(), parameterMap);
  }


  /**
   * Get a JSONArray containing a list of documents (ID, url, title and content) returned by the Search
   * @param result : The result of the search, containing Solr documents
   * @param config RAG configuration
   * @return JSONArray
   */
  private static List<AiDocument> getDocumentList(JSONObject result, RagConfiguration config) {
    try {

      JSONObject response = (JSONObject) result.get("response");
      List<AiDocument> documentsList = new ArrayList<>();

      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        int maxFiles = Math.min(config.getIntegerProperty(RagConfiguration.MAX_FILES), docs.size()); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < maxFiles; i++) {

          AiDocument document = new AiDocument();
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
   * @return List<AiDocument> The final list
   */
  private static List<AiDocument> mergeSimilarDocuments(List<AiDocument> documentList, String response) {
    // Removing Duplicates
    Map<String, AiDocument> map = new HashMap<>();
    for (AiDocument doc : documentList) {
      String id = doc.getId();
        if (map.containsKey(id) || !response.toUpperCase().contains(doc.getTitle().toUpperCase())) {
            continue;
        }
        map.put(id, doc);
    }
    return new ArrayList<>(map.values());
  }

}