package com.francelabs.datafari.api;

import com.francelabs.datafari.rag.DatafariLlmConnector;
import com.francelabs.datafari.rag.LlmConnector;
import com.francelabs.datafari.rag.OpenAiLlmConnector;
import com.francelabs.datafari.rag.RagConfiguration;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class RagAPI extends SearchAPI {

  private static final Logger LOGGER = LogManager.getLogger(RagAPI.class.getName());
  public static final List<String> FORMAT_VALUES = List.of("bulletpoint", "text", "stepbystep");
  public static final String HIGHLIGHTING = "highlighting";
  public static final String EXACT_CONTENT = "exactContent";
  public static final String PREVIEW_CONTENT = "preview_content";
  public static final List<String> ALLOWED_FIELDS_VALUE = List.of(HIGHLIGHTING, EXACT_CONTENT, PREVIEW_CONTENT);
  public static final List<String> HIGHLIGHTING_FIELDS = List.of("content_en", "content_fr", EXACT_CONTENT);
  private static int maxJsonLength = Integer.MAX_VALUE;


  public static JSONObject rag(final HttpServletRequest request) throws IOException {

    // Get RAG specific configuration
    RagConfiguration config = getRagConf();
    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }
    String format = request.getParameter("format");
    if (format != null && !format.isEmpty() && FORMAT_VALUES.contains(format)) {
      config.setFormat(format);
      // Todo : Warning, this method might no longer work after the configuration reworking
    }

    // Search
    JSONObject result = new JSONObject();
    try {
      result = processSearch(config, request);
    } catch (NumberFormatException e) {
      return writeJsonError(500, "Invalid value for rag.hl.fragsize property. Integer expected.");
    }

    // Extract document content and data
    JSONArray documentsList;
    try {
      documentsList = extractDocumentsList(result, config);
    } catch (InvalidPropertiesFormatException e) {
      return writeJsonError(500, "Invalid value for rag.solrField property. Valid values are \"highlighting\", \"preview_content\" and \"exactContent\".");
    } catch (FileNotFoundException e) {
      return writeJsonError(428, "The query cannot be answered because no associated documents were found.");
    }

    // Chunking
    documentsList = ChunkUtils.chunkDocuments(config, documentsList);

    // Prompt
    List<String> prompts = PromptUtils.documentsListToPrompts(config, documentsList, request);


    // Select an LLM Connector
    LlmConnector connector;
    String llmConnector = config.getTemplate();
    switch(llmConnector) {
      case "openai":
        connector = new OpenAiLlmConnector(config);
        break;
      case "datafari":
      default:
        connector = new DatafariLlmConnector(config);
    }

    String message = connector.invoke(prompts, config, request);


    LOGGER.debug("LLM response: {}", message);

    // Return final message
    if (!message.isEmpty()) {
      message = cleanLlmFinalMessage(message);
      final JSONObject response = new JSONObject();
      response.put("status", "OK");
      JSONObject content = new JSONObject();
      content.put("message", message);
      content.put("documents", documentsList);
      response.put("content", content);
      return response;
    } else {
      return writeJsonError(428, "The webservice could not provide a valid response.");
    }

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
  private static JSONArray extractDocumentsList(JSONObject result, RagConfiguration config) throws InvalidPropertiesFormatException, FileNotFoundException {
    // Handling search results
    JSONArray documentsList; // List of simplified documents (id, url, name)
    // Retrieving list of documents : id, title, url
    documentsList = getDocumentList(result, config);

    if (!ALLOWED_FIELDS_VALUE.contains(config.getSolrField())) {
      // If rag.solrField is not one of the allowed fields (ALLOW_FIELDS_VALUE), an error is returned.
      throw new InvalidPropertiesFormatException("Invalid value for rag.solrField property. Valid values are \"highlighting\", \"preview_content\" and \"exactContent\".");
    }

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
  private static JSONObject processSearch(RagConfiguration config, HttpServletRequest request) throws NumberFormatException {

    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";

    // Preparing query for Search process
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      return writeJsonError(422, "No query provided.");
    } else if (config.isLogsEnabled()) {
      LOGGER.info("Request processed by RagAPI : {}", userQuery);
    }

    // If the content is extracted from highlighting, then the user can configure the size of the extracts
    try {
      if (HIGHLIGHTING.equals(config.getSolrField()) && config.getHlFragsize() != null) {
        request.setAttribute("hl.fragsize", Integer.valueOf(config.getHlFragsize()));
        request.setAttribute("hl.tag.pre", "");
        request.setAttribute("hl.tag.post", "");
        if (!config.getOperator().isEmpty()) request.setAttribute("q.op", config.getOperator());
      }
    } catch (NumberFormatException e) {
      throw new NumberFormatException("Invalid value for rag.hl.fragsize property. Integer expected.");
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
    return search(protocol, handler, request.getUserPrincipal(), parameterMap);
  }


  /**
   * Get a JSONArray containing a list of documents (ID, url, title and content) returned by the Search
   * @param result : The result of the search, including Solr documents and highlighting
   * @param config RAG configuration
   * @return JSONArray
   */
  private static JSONArray getDocumentList(JSONObject result, RagConfiguration config) {
    try {

      JSONObject response = (JSONObject) result.get("response");
      JSONObject highlighting = (JSONObject) result.get("highlighting");
      JSONArray documentsList = new JSONArray();

      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        int maxFiles = Math.min(config.getMaxFiles(), docs.size()); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < maxFiles; i++) {

          JSONObject document = new JSONObject();
          String title = ((JSONArray) ((JSONObject) docs.get(i)).get("title")).get(0).toString();
          String id = (String) ((JSONObject) docs.get(i)).get("id");
          String url = (String) ((JSONObject) docs.get(i)).get("url");

          document.put("id", id);
          document.put("title", title);
          document.put("url", url);

          // Add the content to the processed document
          JSONArray content = (JSONArray) ((JSONObject) docs.get(i)).get(config.getSolrField());
          if (content != null && content.get(0) != null) {
            document.put("content", content.get(0).toString());
          } else if (HIGHLIGHTING.equals(config.getSolrField())) {
            String documentHighlighting = extractDocumentsHighlighting(highlighting, id);
            if (!documentHighlighting.isEmpty()) document.put("content", documentHighlighting);
          }

          documentsList.add(document);
        }
        return documentsList;
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while retrieving the list of documents.", e);
    }
    return new JSONArray();

  }

  private static JSONObject writeJsonError(int code, String message) {
    final JSONObject response = new JSONObject();
    final JSONObject error = new JSONObject();
    response.put("status", "ERROR");
    error.put("code", code);
    error.put("reason", message);
    response.put("content", error);
    return response;
  }


  /**
   * Extract the content of the highlighting specific to one document
   * @param highlighting : A JSONObject containing all the highlightings
   * @param id : The ID of the document
   * @return
   */
  private static String extractDocumentsHighlighting(JSONObject highlighting, String id) {

    try {
      JSONObject documentHighlighting = (JSONObject) highlighting.get(id);

        StringBuilder content = new StringBuilder();

        if (documentHighlighting != null) {
          for (String typeOfContent : HIGHLIGHTING_FIELDS) {
            // typeOfContent is one of the allowed fields in highlighting : content_fr, content_en or exactContent
            if (documentHighlighting.get(typeOfContent) != null) {
              String highlightedContent = ((JSONArray) documentHighlighting.get(typeOfContent)).get(0).toString();
              content.append(highlightedContent);
            }
          }
          return content.toString();
        }

    } catch (Exception e) {
      LOGGER.error("An error occurred while extracting highlightings from Datafari search results.", e);
    }
    return "";
  }


  /**
   * Read the rag.properties file to create a RagConfiguration object
   * @return RagConfiguration The configuration used to access the RAG API
   */
  private static RagConfiguration getRagConf() throws FileNotFoundException {
    try  {
      RagConfiguration config = new RagConfiguration();
      if (config.getMaxJsonLength() != 0) maxJsonLength = config.getMaxJsonLength();

      return config;

    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("An error occurred during the configuration. Configuration file not found.");
    } catch (NumberFormatException e) {
      throw new NumberFormatException("An error occurred during the configuration. Invalid value for rag.maxTokens or rag.hl.fragsize or rag.maxFiles or rag.maxJsonLength. Integers expected.");
    } catch (IOException e) {
      throw new RuntimeException("An error occurred during the configuration.");
    }
  }



}
