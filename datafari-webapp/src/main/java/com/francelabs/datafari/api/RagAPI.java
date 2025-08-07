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

import com.francelabs.datafari.ai.ChatLanguageModelFactory;
import com.francelabs.datafari.ai.LLMModelConfigurationManager;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rag.*;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import com.francelabs.datafari.utils.rag.VectorUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.*;
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
    ChatLanguageModel chatModel = getChatModel(config);

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

    // Chunking
    LOGGER.debug("RagAPI - Chunking starting...");
    LOGGER.debug("RagAPI - Max size allowed for a single chunk in configuration is {} characters. ", config.getIntegerProperty(RagConfiguration.CHUNK_SIZE, 3000));
    documentsList = ChunkUtils.chunkDocuments(initialDocumentsList, config);
    LOGGER.debug("RagAPI - The chunking returned {} chunk(s).", documentsList.size());

    // Prompting : Convert all documents chunks into prompt ChatMessages
    List<ChatMessage> contents = PromptUtils.documentsListToMessages(documentsList);

    // Process the RAG query using the selected service, the contents list and the user query
    String message;
    try {
      message = processRagQuery(contents, chatModel, config, request, ragBydocument);
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
   * content List of prompt ChatMessages containing documents chunks
   * chatModel the ChatLanguageModel
   * ragByDocument Does the query focus on one single document ?
   * @return The string LLM response
   */
  public static String processRagQuery(List<ChatMessage> contents, ChatLanguageModel chatModel, RagConfiguration config,
                                       HttpServletRequest request, boolean ragBydocument) throws IOException, DatafariServerException {

    LOGGER.debug("RagAPI - Processing RAG query. {} chunk(s) received.", contents.size());
    LOGGER.debug("RagAPI - Processing RAG query : q={}.", request.getParameter("q"));
    if (ragBydocument) LOGGER.debug("RagAPI - RAG by Document ");

    // Retrieve and sanitize user query
    String userquery = PromptUtils.sanitizeInput(request.getParameter("q"));

    // Get history (if enabled)
    List<ChatMessage> chatHistory = PromptUtils.getChatHistoryToList(request, config);
    String chatHistoryStr = PromptUtils.getStringHistory(chatHistory);
    String conversationStr = PromptUtils.getStringHistoryLines(chatHistory);

    if ("mapreduce".equals(config.getProperty(RagConfiguration.PROMPT_CHUNKING_STRATEGY, "refine"))){

      // Map Reduce method
      LOGGER.debug("RagAPI - The sources associated to the RAG request will be processed using Map Reduce method.");

      // Send all chunks one by one
      List<ChatMessage> prompts;
      List<ChatMessage> responseMessages = new ArrayList<>();
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
        ChatMessage prompt = new UserMessage(filledTemplate);
        prompts.add(prompt);

        generatedResponse = chatModel.generate(prompts).content().text();
        LOGGER.debug("RagAPI - Map Reduce - Last response : {}", generatedResponse);

        // Add the new response to the list
        ChatMessage responseMessage = new AiMessage("* " + generatedResponse + "\n");
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

      ChatMessage prompt = new UserMessage(filledTemplate);
      prompts.add(prompt);

      return chatModel.generate(prompts).content().text();

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
      List<ChatMessage> prompts = new ArrayList<>();
      String template = PromptUtils.getInitialRagTemplateRefining(request);
      String filledTemplate = PromptUtils.stuffAsManySnippetsAsPossible(template, contents, config);
      filledTemplate = filledTemplate.replace(PromptUtils.USER_QUERY_TAG, PromptUtils.cleanContext(userquery))
              .replace(PromptUtils.FORMAT_TAG, PromptUtils.getResponseFormat(request))
              .replace(PromptUtils.HISTORY_TAG, chatHistoryStr)
              .replace(PromptUtils.CONVERSATION_TAG, conversationStr);

      ChatMessage prompt = new UserMessage(filledTemplate);
      prompts.add(prompt);
      String lastresponse = chatModel.generate(prompts).content().text();

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
        prompt = new UserMessage(filledTemplate);
        prompts.add(prompt);
        lastresponse = chatModel.generate(prompts).content().text();
        LOGGER.debug("RagAPI - Iterative Refining - Last generated response : {}", lastresponse);
      }
      return lastresponse;

    }

  }

  /**
   * Returns the active chat language model as defined in the models.json configuration file.
   *
   * @param config The RAG configuration object (currently unused, included for future compatibility).
   * @return A {@link ChatLanguageModel} instance corresponding to the active model.
   * @throws IOException If an error occurs while reading or parsing the model configuration file.
   */
  private static @NotNull ChatLanguageModel getChatModel(RagConfiguration config) throws IOException {
    LLMModelConfigurationManager configManager = new LLMModelConfigurationManager();
    ChatLanguageModelFactory chatModelFactory = new ChatLanguageModelFactory(configManager);
    return chatModelFactory.createChatModel(); // Return the activz model
  }

  /**
   * Returns a specific chat language model by name, as defined in the models.json configuration file.
   *
   * @param modelName The name of the model to load (matching the "name" field in the configuration).
   * @return A {@link ChatLanguageModel} instance corresponding to the specified model name.
   * @throws IOException If an error occurs while reading or parsing the model configuration file.
   * @throws IllegalArgumentException If no model is found with the given name.
   */
  private static @NotNull ChatLanguageModel getSpecificChatModel(String modelName) throws IOException {
    LLMModelConfigurationManager configManager = new LLMModelConfigurationManager();
    ChatLanguageModelFactory chatModelFactory = new ChatLanguageModelFactory(configManager);
    return chatModelFactory.createChatModel(modelName); // Return a specific model
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

    List<ChatMessage> chatHistory = PromptUtils.getChatHistoryToList(request, config);
    String chatHistoryStr = PromptUtils.getStringHistoryLines(chatHistory);


    // Get the LLM interface (ChatLanguageModel)
    ChatLanguageModel chatModel = getChatModel(config);

    String template = PromptUtils.getRewriteQueryTemplate(request)
            .replace("{userquery}", userQuery)
            .replace("{conversation}", chatHistoryStr);
    List<ChatMessage> prompts = new ArrayList<>();
    prompts.add(new UserMessage(template)) ;

    try {
      String response = chatModel.generate(prompts).content().text();
      return (response != null && !response.isEmpty()) && !"0".equals(response.trim()) ? response : userQuery;
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
    ChatLanguageModel chatModel = getChatModel(config);

    // Check if summarization is enabled
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
      LOGGER.debug("AiPowered - Summarize - Summarization is disabled");
      throw new DisabledException("The summary generation feature is disabled.");
    }

    // Chunk document
    List<TextSegment> segments = ChunkUtils.chunkContent(doc, config);

    // Setup prompt
    ChatMessage initialPrompt = PromptUtils.createInitialPromptForSummarization(request);

    // Summarize all chunks
    List<ChatMessage> summaries = new ArrayList<>();
    for (TextSegment segment: segments) {
      List<ChatMessage> prompts = new ArrayList<>();
      prompts.add(initialPrompt);
      prompts.add(PromptUtils.textSegmentsToMessage(segment, "user"));

      ChatMessage responseMessage =  chatModel.generate(prompts).content();
      summaries.add(responseMessage);
    }

    // Merge all summaries
    if (summaries.size() > 1) {
      // Merge All Summaries as prompts and add an extra instruction to generate a synthesis
      LOGGER.debug("RagAPI - Summarize - Multiple chunk summaries have been generated.");
      LOGGER.debug("RagAPI - Summarize - Merging summaries for document {}, with {} chunk(s).", doc.metadata().getString("id"), segments.size());
      ChatMessage mergeAllSummariesPrompt = PromptUtils.createPromptForMergeAllSummaries(request);
      summaries.add(mergeAllSummariesPrompt);
      return chatModel.generate(summaries).content().text();

    } else if (summaries.size() == 1) {
      LOGGER.debug("RagAPI - Summarize - One single summary have been generated for document {}.", doc.metadata().getString("id"));
      return summaries.get(0).text();
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
    final String protocol = request.getScheme() + ":";

    // Preparing query for Search process
    String userQuery = request.getParameter("q");

    if (userQuery == null) {
      LOGGER.error("RagAPI - ERROR. No query provided.");
      throw new InvalidParameterException("No query provided.");
    } else {
      LOGGER.debug("RagAPI - Processing search for request : q={}", userQuery);
    }

    String queryrag = request.getParameter("queryrag");
    // If queryrag is missing, set it to userQuery
    if (queryrag == null || queryrag.isEmpty()) {
      String[] queryragParam = { userQuery };
      parameterMap.put("queryrag", queryragParam);
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

    String retrievalMethod = config.getProperty(RagConfiguration.RETRIEVAL_METHOD, "bm25").toLowerCase();

    String handler;
    switch (retrievalMethod) {
      case "rrf":
        return hybridSearch(protocol, request.getUserPrincipal(), parameterMap);
      case "vector":
        handler = "/vector";
        return search(protocol, handler, request.getUserPrincipal(), parameterMap);
      case "bm25":
      default:
        handler = "/select";
        String[] rows = { config.getProperty(RagConfiguration.MAX_FILES, "3") };
        parameterMap.put("rows", rows);
        return search(protocol, handler, request.getUserPrincipal(), parameterMap);
    }
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
    RagConfiguration conf = RagConfiguration.getInstance();
    for (Document doc : documentList) {
      String url = doc.metadata().getString("url");
      String id = doc.metadata().getString("id");
      if (map.containsKey(url) &&
              "bm25".equals(conf.getProperty(RagConfiguration.RETRIEVAL_METHOD)) ) {
        // Keep only one chunk from a single document if BM25 is selected
        continue;
      }
      LOGGER.debug("RagAPI - RAG - Document {} is mentioned in LLM response, and will returned to the user.", url);
      map.put(url, doc);
      // Add document to displayedDocuments if it isn't there yet and if it is mentioned by the LLM
      JSONObject jsonDoc = new JSONObject();
      jsonDoc.put("id", id);
      jsonDoc.put("title", doc.metadata().getString("title"));
      // if (!"id".equals("url")) jsonDoc.put("chunk_id", id);
      if (!"id".equals("url")) jsonDoc.put("parent_id", url);
      jsonDoc.put("url", url);
      jsonDoc.put("content", doc.text());
      displayedDocuments.add(jsonDoc);
    }

    return displayedDocuments;
  }

}
