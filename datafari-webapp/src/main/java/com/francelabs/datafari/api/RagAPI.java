package com.francelabs.datafari.api;

import com.francelabs.datafari.rag.DatafariLlmConnector;
import com.francelabs.datafari.rag.LlmConnector;
import com.francelabs.datafari.rag.RagConfiguration;
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
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    final Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());

    // Get RAG specific configuration
    RagConfiguration config = getRagConf();

    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }

    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      return writeJsonError(422, "No query provided.");
    } else if (config.isLogsEnabled()) {
      LOGGER.info("Request processed by RagAPI : {}", userQuery);
    }

    String format = request.getParameter("format");
    if (format != null && !format.isEmpty() && FORMAT_VALUES.contains(format)) {
      config.setFormat(format);
      // Todo : Warning, this method might no longer work after the configuration reworking
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
      return writeJsonError(500, "Invalid value for rag.hl.fragsize property. Integer expected.");
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
    JSONObject result = search(protocol, handler, request.getUserPrincipal(), parameterMap);

    return callAiApi(userQuery, result, config);

  }

  private static JSONObject callAiApi(String prompt, JSONObject searchResult, RagConfiguration config) throws IOException {
    List<String> documentsContent = new ArrayList<>();
    JSONArray documentsList; // List of simplified documents (id, url, name)

    JSONObject searchResponse = (JSONObject) searchResult.get("response");

    // Retrieving list of documents : id, title, url
    documentsList = getDocumentList(searchResponse, documentsContent, config);

    // If we chose to send the highlighting to the webservice, then we need to use a specific method
    if (HIGHLIGHTING.equals(config.getSolrField())) {
      JSONObject searchHighlighting = (JSONObject) searchResult.get(HIGHLIGHTING);
      documentsContent = extractDocumentsContentFromHighlighting(searchHighlighting, config, documentsList);
    } else if (ALLOWED_FIELDS_VALUE.contains(config.getSolrField())) {
      documentsContent = extractDocumentsContentFromResponse(searchResponse, config);
    } else {
      // If rag.solrField is not one of the allowed fields (ALLOW_FIELDS_VALUE), an error is returned.
      return writeJsonError(500, "Invalid value for rag.solrField property. Valid values are \"highlighting\", \"preview_content\" and \"exactContent\".");
    }

    if (documentsContent.isEmpty()) {
      return writeJsonError(428, "The query cannot be answered because no associated documents were found.");
    }

    String llmStrResponse = getLlmResponse(prompt, documentsContent, config, documentsList);

    if (!llmStrResponse.isEmpty()) {
      final JSONObject response = new JSONObject();
      response.put("status", "OK");
      JSONObject content = new JSONObject();
      content.put("message", llmStrResponse);
      content.put("documents", documentsList);
      response.put("content", content);
      return response;
    } else {
      return writeJsonError(428, "The webservice could not provide a valid response.");
    }
  }

  private static JSONArray getDocumentList(JSONObject response, List<String> documentsContent, RagConfiguration config) {
    try {
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

          // If SolrField is not highlighting, the extract is added to documentsContent
          JSONArray content = (JSONArray) ((JSONObject) docs.get(i)).get(config.getSolrField()); // You can use exactContent to send the whole file content
          if (content != null && content.get(0) != null) {
            documentsContent.add(content.get(0).toString());
            document.put("content", cleanContext(content.get(0).toString()));
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
   * @param response : the object "response" obtained from search
   * @param config : the RAG configuration
   * @return A list of documents pieces extracted from response
   */
  private static List<String> extractDocumentsContentFromResponse(JSONObject response, RagConfiguration config) {
    try {
      int maxFiles = config.getMaxFiles();
      List<String> documentsContent = new ArrayList<>();

      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        if (docs.size() < maxFiles) maxFiles = docs.size(); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < maxFiles; i++) {
          JSONArray exactContent = (JSONArray) ((JSONObject) docs.get(i)).get(config.getSolrField()); // You can use exactContent to send the whole file content
          if (exactContent != null && exactContent.get(0) != null) {
            documentsContent.add(exactContent.get(0).toString());
          }
        }
        return documentsContent;
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while extracting content from webservices response.", e);
    }
    return Collections.emptyList();
  }


  /**
   * @param highlighting : the object "highlighting" obtained from search
   * @param documentList : a list of objects containing data about documents (id, url, title)
   * @param config : the RAG configuration
   * @return A list of documents pieces extracted from highlightings
   */
  private static List<String> extractDocumentsContentFromHighlighting(JSONObject highlighting, RagConfiguration config, JSONArray documentList) {
    try {
      int maxFiles = Math.min(config.getMaxFiles(), documentList.size());
      List<String> documentsContent = new ArrayList<>();

      for (int i = 0; i < maxFiles; i++) {
        String key = ((JSONObject) documentList.get(i)).get("id").toString();
        StringBuilder content = new StringBuilder();

        if (highlighting.get(key) instanceof JSONObject) {
          JSONObject highlightContent = (JSONObject) highlighting.get(key);
          for (String typeOfContent : HIGHLIGHTING_FIELDS) {
            // typeOfContent is one of the allowed fields in highlighting : content_fr, content_en or exactContent
            if (highlightContent.get(typeOfContent) != null) {
              String highlightedContent = ((JSONArray) highlightContent.get(typeOfContent)).get(0).toString();
              content.append(highlightedContent);
              documentsContent.add(highlightedContent);
            }
          }
          ((JSONObject) documentList.get(i)).put("content", cleanContext(content.toString()));
        }
      }

      return documentsContent;

    } catch (Exception e) {
      LOGGER.error("An error occurred while extracting highlightings from Datafari search results.", e);
    }
    return Collections.emptyList();
  }

  /**
   * Send a query to Datafari RAG API
   * @param prompt : the user question
   * @param documents : A list of String documents to send with the user prompt
   * @return The extracted response from the LLM API
   */
  public static String getLlmResponse(String prompt, List<String> documents, RagConfiguration config, JSONArray documentList) throws IOException {
    String url;
    String apiKey;
    String template;

    try {
      apiKey = config.getToken();
      url = config.getEndpoint();
      template = config.getTemplate();

      URL obj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
      connection.setRequestMethod("POST");
      if (!apiKey.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");

      // The request body
      String body;
      switch (template) {
        case "datafari":
          body = generateJsonBodyForDatafariRag(prompt, documents, config, documentList);
          break;
        case "openai":
        default:
          body = generateJsonBodyForOpenAI(prompt, documents, config);
          break;
      }

      // Todo : Bouchonner / débouchonner (to delete for prod)
      if (!config.isEnabled()) return "RAG feature is currently disabled. This is a placeholder message. \\n Enable the feature by editing rag.properties file.";

      connection.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from LLM API
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;

      StringBuilder response = new StringBuilder();

      while ((line = br.readLine()) != null) {
        response.append(line);
      }
      br.close();

      // calls the method to extract the message.
      switch (template) {
        case "datafari":
          return extractMessageFromDatafariJSONResponse(response.toString());
        case "openai":
        default:
          return extractMessageFromOpenAiJSONResponse(response.toString());
      }
      
    } catch (IOException e) {
      throw new RuntimeException("An error occurred while calling external webservices.", e);
    }
  }

  /**
   * @param prompt The user question
   * @param documents The list of documents content
   * @param config The global RAG configuration
   * @return The generated JSON body to attach to the HTTP POST request for an OpenAI API
   */
  private static String generateJsonBodyForOpenAI(String prompt, List<String> documents, RagConfiguration config) throws IOException {
    StringBuilder body = new StringBuilder("{\"model\": \"" + config.getModel() + "\",\"temperature\": " + config.getTemperature() + ",\"max_tokens\": " + config.getMaxTokens() + ", \"messages\": [");
    body.append("{\"role\": \"system\", \"content\": \"").append(getInstructions().replace("\n", " ").replace("\r", " ").replace("\t", " ")).append("\"},");

    for (String doc : documents)
    {
      body.append("{\"role\": \"user\", \"content\": \"Here is one of the documents: ```").append(cleanContext(doc)).append("```\"},");
    }
    body.append("{\"role\": \"user\", \"content\": \"The user question is: ").append(prompt).append("\"}");
    body.append("]}");
    return body.toString();
  }


  /**
   * @param prompt The user question
   * @param documents The list of documents content
   * @param config The global RAG configuration
   * @return The generated JSON body to attach to the HTTP POST request for a Datafari-RAG API
   */
  private static String generateJsonBodyForDatafariRag(String prompt, List<String> documents, RagConfiguration config, JSONArray documentList) throws IOException {
    // Todo : handle instruction to send to the Datafari External Webservice
    //StringBuilder context = new StringBuilder((config.getAddInstructions()) ? getInstructions() + "\\n\\r" : "");

    JSONObject queryBody = new JSONObject();
    JSONObject input = new JSONObject();
    if (!config.getTemperature().isEmpty()) input.put("temperature", config.getTemperature());
    if (!config.getMaxTokens().isEmpty()) input.put("max_tokens", config.getMaxTokens());
    if (!config.getModel().isEmpty()) input.put("model", config.getModel());
    if (config.getFormat() != null && !config.getFormat().isEmpty() && FORMAT_VALUES.contains(config.getFormat())) input.put("format", config.getFormat());
    input.put("question", cleanContext(prompt));
    input.put("documents", documentList);

    queryBody.put("input", input);
    return queryBody.toJSONString();
  }


  /**
   * @param context The context, containing documents content
   * @return A clean context, with no characters or element that could cause an error or a prompt injection
   */
  private static String cleanContext(String context) {
    context = context.replace("\\", "/")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("\b", "")
            .replace("\"", "`");
    if (context.length() > maxJsonLength -1000) {
      // Truncate the context if too long
      context = context.substring(0, maxJsonLength - 1000);
    }
    return context;
  }

  /**
   * @return The instructions prompts stored in rag-instructions.txt file
   */
  private static String getInstructions() throws IOException {
    return readFromInputStream(RagAPI.class.getClassLoader().getResourceAsStream("rag-instructions.txt"));
  }

  /**
   * Extract the AI message from the API JSON response
   * @param response The response sent by the API
   * @return a readable string message
   */
  public static String extractMessageFromOpenAiJSONResponse(String response) {
    response = response.replace("\\\"", "`");
    int start = response.indexOf("content")+ 11;
    int end = response.indexOf("\"", start);
    return response.substring(start, end).trim();
  }

  /**
   * Extract the AI message from the API JSON response
   * @param response The response sent by the API
   * @return a readable string message
   */
  public static String extractMessageFromDatafariJSONResponse(String response) {
    response = response.replace("\\\"", "`");
    int start = response.indexOf("output")+ 9;
    int end = response.indexOf("\"", start);
    return response.substring(start, end).trim();
  }

  /**
   * Transform InputStream into String
   * @param inputStream InputStream to transform
   * @return String
   * @throws IOException : the InputStream is null or could not be read
   */
  private static String readFromInputStream(InputStream inputStream)
          throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
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
