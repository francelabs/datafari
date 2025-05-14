/*******************************************************************************
 * Copyright 2015 France Labs
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.api;

import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rag.*;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.VectorUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.security.authentication.DisabledException;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;

public class RagAPI extends SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());
  public static final List<String> FORMAT_VALUES = List.of("bulletpoint", "text", "stepbystep");
  public static final String EXACT_CONTENT = "exactContent";
  public static final String EMBEDDED_CONTENT = "embedded_content";


  public static JSONObject rag(final HttpServletRequest request, JSONObject searchResults, boolean ragBydocument) throws IOException {


    // Get RAG specific configuration
    RagConfiguration config = RagConfiguration.getInstance();

    // Is RAG enabled ?
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_RAG))
      return writeJsonError(422, "ragErrorNotEnabled", "Sorry, it seems the feature is not enabled.", null);

    // Set LlmService
    LlmService service = getLlmService(config);

    // Search
    // If the search result has not been provided, process a search
    if (searchResults == null) {
      try {
        LOGGER.debug("RagAPI - No search results provided in the method parameters. Processing a new search using request parameters.");
        searchResults = processSearch(config, request);
      } catch (Exception e) {
        LOGGER.error("RagAPI - ERROR. An error occurred while retrieving data.", e);
        return writeJsonError(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
      }
    }

    // Extract document content and data
    List<Document> initialDocumentsList;
    List<Document> documentsList;
    try {
      LOGGER.debug("RagAPI - Extracting documents list from search result...");
      initialDocumentsList = extractDocumentsList(searchResults, config);
      LOGGER.debug("RagAPI - {} documents extracted.", initialDocumentsList.size());
    } catch (FileNotFoundException e) {
      LOGGER.warn("RagAPI -  WARNING. The query cannot be answered because no associated documents were found.");
      return writeJsonError(428, "ragNoFileFound", "Sorry, I couldn't find any relevant document to answer your request.", e);
    }


    // Local vector search using a vector storage
    if (config.getBooleanProperty(RagConfiguration.ENABLE_VECTOR_SEARCH)) {
      LOGGER.debug("RagAPI - Local vector search is enabled.");
      initialDocumentsList = VectorUtils.processVectorSearch(initialDocumentsList, request);
      LOGGER.debug("RagAPI - The 'FAISS' vector search returned {} chunk(s).", initialDocumentsList.size());
    } else {
      LOGGER.debug("RagAPI - Vector search was ignored due to configuration");
    }

    // Chunking
    documentsList = initialDocumentsList;
    if (config.getBooleanProperty(RagConfiguration.ENABLE_CHUNKING, true)) {
      LOGGER.debug("RagAPI - Chunking starting...");
      LOGGER.debug("RagAPI - Max size allowed for a single chunk in configuration is {} characters. ", config.getIntegerProperty(RagConfiguration.CHUNK_SIZE));
      documentsList = ChunkUtils.chunkDocuments(initialDocumentsList, config);
      LOGGER.debug("RagAPI - The chunking returned {} chunk(s).", documentsList.size());
    } else {
      LOGGER.debug("RagAPI - Chunking was ignored due to configuration");
    }

    // Prompting : Convert all documents chunks into prompt Messages
    List<Message> contents = PromptUtils.documentsListToMessages(documentsList);

    // Process the RAG query using the selected service, the contents list and the user query
    String message;
    try {
      message = processRagQuery(contents, service, config, request, ragBydocument);
    } catch (DatafariServerException e) {
      LOGGER.error("An error occurred while calling external LLM service.", e);
      return writeJsonError(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
    }catch (Exception e) {
      LOGGER.error("An unexpected error occurred while processing RAG query.", e);
      return writeJsonError(500, "ragTechnicalError", "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.", e);
    }

    LOGGER.debug("RagAPI - LLM response: {}", message);

    // Return final message
    if (!message.isEmpty()) {
      message = cleanLlmFinalMessage(message);
      return writeJsonResponse(message, documentsList);
    } else {
      return writeJsonError(428, "ragNoValidAnswer", "Sorry, I could not find an answer to your question.", null);
    }

  }


  /**
   * Start RAG process
   * content List of prompt Messages containing documents chunks
   * service the LlmService
   * ragByDocument Does the query focus on one single document ?
   * @return The string LLM response
   */
  public static String processRagQuery(List<Message> contents, LlmService service, RagConfiguration config,
                                       HttpServletRequest request, boolean ragBydocument) throws IOException, DatafariServerException {

    LOGGER.debug("RagAPI - Processing RAG query. {} chunk(s) received.", contents.size());
    LOGGER.debug("RagAPI - Processing RAG query : q={}.", request.getParameter("q"));
    if (ragBydocument) LOGGER.debug("RagAPI - RAG by Document ");

    // Retrieve and sanitize user query
    String userquery = PromptUtils.sanitizeInput(request.getParameter("q"));

    // Get history (if enabled)
    List<Message> chatHistory = PromptUtils.getChatHistoryToList(request, config);
    String chatHistoryStr = PromptUtils.getStringHistory(chatHistory);
    String conversationStr = PromptUtils.getStringHistoryLines(chatHistory);

    if ("mapreduce".equals(config.getProperty(RagConfiguration.PROMPT_CHUNKING_STRATEGY, "refine"))){

      // Map Reduce method
      LOGGER.debug("RagAPI - The sources associated to the RAG request will be processed using Map Reduce method.");

      // Send all chunks one by one
      List<Message> prompts;
      List<Message> responseMessages = new ArrayList<>();
      String template = PromptUtils.getInitialRagTemplateMapReduce(request)
              .replace(PromptUtils.USER_QUERY_TAG, userquery)
              .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
              .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
              .replace(PromptUtils.CONVERSATION_TAG, conversationStr);
      String filledTemplate;
      String generatedResponse = "";

      int rqCounter = 0;
      while (!contents.isEmpty()) {
        rqCounter++;
        filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
        prompts = new ArrayList<>();
        Message prompt = new Message("user", filledTemplate);
        prompts.add(prompt);

        generatedResponse = service.generate(prompts, request);
        LOGGER.debug("RagAPI - Map Reduce - Last response : {}", generatedResponse);

        // Add the new response to the list
        Message responseMessage = new Message("assistant", "* " + generatedResponse + "\n");
        responseMessages.add(responseMessage);
      }

      // If the LLM was called only once, no need to call it again
      if (rqCounter <= 1) return generatedResponse;

      // Then, merge all responses into one
      template = PromptUtils.getFinalRagTemplateMapReduce(request);
      filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, responseMessages, config);
      filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, userquery)
              .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
              .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
              .replace(PromptUtils.CONVERSATION_TAG, conversationStr);

      prompts = new ArrayList<>();

      Message prompt = new Message("user", filledTemplate);
      prompts.add(prompt);

      return service.generate(prompts, request);

    } else {

      // Iterative Refining method
      LOGGER.debug("RagAPI - The sources associated to the RAG request will be processed using Iterative Refining method.");

      /*
        Variables:
        - snippets (formatted list of TextSegments)
        - userquery (userquery)
        - lastresponse (the last generated response)
        - format (Sentence enforcing the response format : bulletpoint, stepbystep, text, default... optional)
        - history (chat history, including prompt and conversation lines)
        - conversation : String formatted lines (e.g. "- user: hello world\n- assistant: Hello user !"
       */

      // Initial call with a fist set of snippets
      List<Message> prompts = new ArrayList<>();
      String template = PromptUtils.getInitialRagTemplateRefining(request);
      String filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
      filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, PromptUtils.cleanContext(userquery))
              .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
              .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
              .replace(PromptUtils.CONVERSATION_TAG, conversationStr);

      Message prompt = new Message("user", filledTemplate);
      prompts.add(prompt);
      String lastresponse = service.generate(prompts, request);

      // Refining response with each snippet pack
      template = PromptUtils.getRefineRagTemplateRefining(request);
      while (!contents.isEmpty()) {
        filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
        filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, userquery)
                .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
                .replace(PromptUtils.LAST_RESPONSE_TAG, lastresponse)
                .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
                .replace(PromptUtils.CONVERSATION_TAG, conversationStr);
        prompts = new ArrayList<>();
        prompt = new Message("user", filledTemplate);
        prompts.add(prompt);
        lastresponse = service.generate(prompts, request);
        LOGGER.debug("RagAPI - Iterative Refining - Last generated response : {}", lastresponse);
      }
      return lastresponse;

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
      case "openai":
      default:
        service = new OpenAiLlmService(config);
    }
    return service;
  }


  /**
   * Convert a natural-language user query into a search query, using the LLM
   * @param userQuery : The initial user query
   * @param request HttpServletRequest
   * @param config RagConfiguration
   * @return A ready-to-user search query
   */
  public static String rewriteSearchQuery(String userQuery,  final HttpServletRequest request, RagConfiguration config) throws IOException {

    LOGGER.debug("RagAPI - Rewriting user query into a search query.");

    List<Message> chatHistory = PromptUtils.getChatHistoryToList(request, config);
    String chatHistoryStr = PromptUtils.getStringHistoryLines(chatHistory);


    // Select an LLM service
    LlmService service = getLlmService(config);

    String template = PromptUtils.getRewriteQueryTemplate(request)
            .replace("{userquery}", userQuery)
            .replace("{conversation}", chatHistoryStr);
    List<Message> prompts = new ArrayList<>();
    prompts.add(new Message("user", template)) ;

    try {
      String response = service.generate(prompts, request);
      return (response != null && !response.isEmpty()) ? response : userQuery;
    } catch (Exception e) {
      LOGGER.error("Query rewriting failed. Using initial user query for the search.", e);
      return userQuery;
    }


  }


  public static String summarize(final HttpServletRequest request, Document doc) throws IOException {

    LOGGER.debug("RagAPI - Summary for document {} requested.", doc.metadata().getString("id"));

    // Get RAG configuration
    RagConfiguration config = RagConfiguration.getInstance();
    // Select an LLM service
    LlmService service = getLlmService(config);

    // Check if summarization is enabled
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
      LOGGER.debug("AiPowered - Summarize - Summarization is disabled");
      throw new DisabledException("The summary generation feature is disabled.");
    }

    // Chunk document
    List<TextSegment> segments = ChunkUtils.chunkContent(doc, config);

    // Setup prompt
    Message initialPrompt = PromptUtils.createInitialPromptForSummarization(request);

    // Summarize all chunks
    List<Message> summaries = new ArrayList<>();
    for (TextSegment segment: segments) {
      List<Message> prompts = new ArrayList<>();
      prompts.add(initialPrompt);
      prompts.add(PromptUtils.textSegmentsToMessage(segment, "user"));

      String response = service.generate(prompts, request);
      Message responseMessage = new Message("assistant", response);
      summaries.add(responseMessage);
    }

    // Merge all summaries
    if (summaries.size() > 1) {
      // Merge All Summaries as prompts and add an extra instruction to generate a synthesis
      LOGGER.debug("RagAPI - Summarize - Multiple chunk summaries have been generated.");
      LOGGER.debug("RagAPI - Summarize - Merging summaries for document {}, with {} chunk(s).", doc.metadata().getString("id"), segments.size());
      Message mergeAllSummariesPrompt = PromptUtils.createPromptForMergeAllSummaries(request);
      summaries.add(mergeAllSummariesPrompt);
      return service.generate(summaries, request);

    } else if (summaries.size() == 1) {
      LOGGER.debug("RagAPI - Summarize - One single summary have been generated for document {}.", doc.metadata().getString("id"));
      return summaries.get(0).getContent();
    } else {
      LOGGER.debug("RagAPI - Summarize - Something happened while processing summarization for document {}. ", doc.metadata().getString("id"));
      throw new IOException("Could not generate any summary for this document");
    }

  }

  private static @NotNull JSONObject writeJsonResponse(String message, List<Document> documentsList) {
    final JSONObject response = new JSONObject();
    response.put("status", "OK");
    JSONObject content = new JSONObject();
    content.put("message", message);
    content.put("documents", mergeSimilarDocuments(documentsList, message));
    response.put("content", content);

    LOGGER.debug("");
    LOGGER.debug("##### RAG final JSON response #####");
    LOGGER.debug(response.toJSONString());
    LOGGER.debug("###################################");
    LOGGER.debug("");

    return response;
  }

  private static JSONObject writeJsonError(int code, String errorLabel, String message, Exception ex) {
    final JSONObject response = new JSONObject();
    final JSONObject error = new JSONObject();
    final JSONObject content = new JSONObject();
    response.put("status", "ERROR");
    error.put("code", code);
    error.put("label", errorLabel);
    if (ex != null) error.put("reason", ex.getLocalizedMessage());
    content.put("message", message);
    content.put("documents", new ArrayList<>());
    content.put("error", error);
    response.put("content", content);

    LOGGER.debug("RagAPI - ERROR. An error occurred while processing the query.");
    LOGGER.debug("");
    LOGGER.debug("##### RAG final JSON response #####");
    LOGGER.debug(response.toJSONString());
    LOGGER.debug("###################################");
    LOGGER.debug("");

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
   * @return A List of Documents
   * @throws FileNotFoundException if the provided results doesn't contain any document
   */
  private static List<Document> extractDocumentsList(JSONObject result, RagConfiguration config) throws FileNotFoundException {
    // Handling search results
    // Retrieving list of documents: id, title, url
    List<Document> documentsList;
    documentsList = getDocumentList(result);

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
  public static JSONObject processSearch(RagConfiguration config, HttpServletRequest request) throws InvalidParameterException {

    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";

    // Preparing query for Search process
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      LOGGER.error("RagAPI - ERROR. No query provided.");
      throw new InvalidParameterException("No query provided.");
    } else {
      LOGGER.debug("RagAPI - Processing search for request : q={}", userQuery);
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

    // Add Solr "/vector" handler is Solr Vector Search is enabled
    if (config.getBooleanProperty(RagConfiguration.SOLR_ENABLE_VECTOR_SEARCH)) {

      LOGGER.debug("RagAPI - Solr Vector Search is enabled.");
      handler = "/vector";
      String[] queryrag = { userQuery };
      parameterMap.put("queryrag", queryrag);
    } else {
      // If search is BM25, set the result limit
      String[] rows = { config.getProperty(RagConfiguration.MAX_FILES, "3") };
      parameterMap.put("rows", rows);
    }

    return search(protocol, handler, request.getUserPrincipal(), parameterMap);
  }


  /**
   * Get a JSONArray containing a list of documents (ID, url, title and content) returned by the Search
   * @param result : The result of the search, containing Solr documents
   * @return JSONArray
   */
  private static List<Document> getDocumentList(JSONObject result) {
    try {

      JSONObject response = (JSONObject) result.get("response");
      List<Document> documentsList = new ArrayList<>();


      LOGGER.debug("RagAPI - Converting search results into a List of Documents.");
      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        JSONObject jsonObject;
        int nbDocs = docs.size(); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < nbDocs; i++) {

          jsonObject = (JSONObject) docs.get(i);

          // If the document has content, we generate a Document object with its content and metadata
          JSONArray contentArray = (JSONArray) jsonObject.get(EXACT_CONTENT);
          String content;
          if (contentArray != null && contentArray.get(0) != null) {
            content = contentArray.get(0).toString();
          } else {
            // If exactContent is not provided, we try to retrieve embedded_content
            content = (String) jsonObject.get(EMBEDDED_CONTENT);
          }

          if (content != null && !content.isBlank()) {
            String title = ((JSONArray) jsonObject.get("title")).get(0).toString();
            String id = (String) jsonObject.get("id");
            String url = (String) jsonObject.get("url");

            Metadata metadata = new Metadata();
            metadata.put("title", title);
            metadata.put("id", id);
            metadata.put("url", url);

            Document document = new Document(content, metadata);
            documentsList.add(document);
          }

        }
        return documentsList;
      }
    } catch (Exception e) {
      LOGGER.error("RagAPI - An error occurred while retrieving the list of documents.", e);
    }
    return new ArrayList<>();

  }


  /**
   * The "documentList" containing sources of the answer might contain duplicates.
   * Some document might not be used in the LLM response.
   * This methode merge thoses duplicated entries, and remove useless ones.
   * @return List<Document> The final list
   */
  private static JSONArray mergeSimilarDocuments(List<Document> documentList, String response) {
    // Removing Duplicates
    Map<String, Document> map = new HashMap<>();
    JSONArray displayedDocuments = new JSONArray();
    for (Document doc : documentList) {
      String url = doc.metadata().getString("url");
      if (map.containsKey(url) ||
              !response.toUpperCase().replaceAll("\\s+","").contains( doc.metadata().getString("title").toUpperCase().replaceAll("\\s+","") )) {
        // Skip if the document has already been added
        LOGGER.debug("RagAPI - RAG - Document {} is not contained in the LLM response, or is already in the list to return to the user.", doc.metadata().getString("id"));
        continue;
      }
      LOGGER.debug("RagAPI - RAG - Document {} is mentioned in LLM response, and will returned to the user.", url);
      map.put(url, doc);
      // Add document to displayedDocuments if it isn't there yet and if it is mentioned by the LLM
      JSONObject jsonDoc = new JSONObject();
      jsonDoc.put("id", url);
      jsonDoc.put("title", doc.metadata().getString("title"));
      jsonDoc.put("url", url);
      jsonDoc.put("content", doc.text());
      displayedDocuments.add(jsonDoc);
    }

    return displayedDocuments;
  }

}
