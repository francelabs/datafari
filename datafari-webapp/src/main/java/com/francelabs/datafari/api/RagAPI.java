package com.francelabs.datafari.api;

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
  public static final List<String> HIGHLIGHTING_FIELDS = List.of("content_en", "content_fr", "exactContent");
  public static final String HIGHLIGHTING = "highlighting";
  public static final String EXACT_CONTENT = "exact_content";
  public static final String PREVIEW_CONTENT = "preview_content";
  public static final List<String> ALLOWED_FIELDS_VALUE = List.of(HIGHLIGHTING, EXACT_CONTENT, PREVIEW_CONTENT);

  public static JSONObject rag(final HttpServletRequest request) throws IOException {
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());

    // Get RAG specific configuration
    RagConfiguration config = getRagConf();

    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }

    // Todo : validate query
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      return writeJsonError(422, "No query provided.");
    }

    // If the content is extracted from highlighting, then the user can configure the size of the extracts
    try {
      if (HIGHLIGHTING.equals(config.getSolrField()) && config.getHlFragsize() != null) {
        request.setAttribute("hl.fragsize", Integer.valueOf(config.getHlFragsize()));
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
    List<String> documentsContent;

    // The response that will be sent to user
    final JSONObject response = new JSONObject();

    // If we chose to send the highlighting to the webservice, then we need to use a specific method
    if (HIGHLIGHTING.equals(config.getSolrField())) {
      JSONObject searchResponse = (JSONObject) searchResult.get(HIGHLIGHTING);
      documentsContent = extractDocumentsContentFromHighlighting(searchResponse, config);
    } else if (ALLOWED_FIELDS_VALUE.contains(config.getSolrField())) {
      JSONObject searchResponse = (JSONObject) searchResult.get("response");
      documentsContent = extractDocumentsContentFromResponse(searchResponse, config);
    } else {
      // If rag.solrField is not one of the allowed fields (ALLOW_FIELDS_VALUE), an error is returned.
      return writeJsonError(500, "Invalid value for rag.solrField property. Valid values are \"highlighting\", \"preview_content\" and \"exact_content\".");
    }

    if (documentsContent.isEmpty()) {
      return writeJsonError(428, "The query cannot be answered because no associated documents were found.");
    }

    String llmStrResponse = getLlmResponse(prompt, documentsContent, config);
    //String llmStrResponse = "Datafari connait certainement la réponse à votre requête, mais moi, je suis juste un bouchon.";

    // Todo : check the validity of the response
    if (!llmStrResponse.isEmpty()) {
      response.put("status", "OK");
      JSONObject content = new JSONObject();
      content.put("message", llmStrResponse);
      response.put("content", content);
      return response;
    } else {
      return writeJsonError(428, "The webservice could not provide a valid response.");
    }
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
      System.out.println(e);
    }
    return Collections.emptyList();
  }

  private static List<String> extractDocumentsContentFromHighlighting(JSONObject highlighting, RagConfiguration config) {
    try {
      int maxFiles = config.getMaxFiles();
      int fileCount = 0;
      List<String> documentsContent = new ArrayList<>();

      Iterator<String> keys = highlighting.keySet().iterator();

      while(keys.hasNext() && maxFiles > fileCount) {
        fileCount++;
        String key = keys.next();
        if (highlighting.get(key) instanceof JSONObject) {
          JSONObject highlightContent = (JSONObject) highlighting.get(key);
          for (String typeOfContent : HIGHLIGHTING_FIELDS) {
            // typeOfContent is one of the allowed fields in highlighting : content_fr, content_en or exactContent
            if (highlightContent.get(typeOfContent) != null) {
              String highlightedContent = ((JSONArray) highlightContent.get(typeOfContent)).get(0).toString();
              documentsContent.add(highlightedContent);
            }
          }
        }
      }

      return documentsContent;

    } catch (Exception e) {
      System.out.println(e);
    }
    return Collections.emptyList();
  }

  /**
   * Send a query to Datafari RAG API
   * @param prompt : the user question
   * @param documents : A list of String documents to send with the user prompt
   * @return The extracted response from the LLM API
   */
  public static String getLlmResponse(String prompt, List<String> documents, RagConfiguration config) throws IOException {
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
          body = generateJsonBodyForDatafariRag(prompt, documents, config);
          break;
        case "openai":
        default:
          body = generateJsonBodyForOpenAI(prompt, documents, config);
          break;
      }

      connection.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from LLM API
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;

      StringBuffer response = new StringBuffer();

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
      throw new RuntimeException(e);
    }
  }

  /**
   * @param prompt The user question
   * @param documents The list of documents content
   * @param config The global RAG configuration
   * @return The generated JSON body to attach to the HTTP POST request for an OpenAI API
   */
  private static String generateJsonBodyForOpenAI(String prompt, List<String> documents, RagConfiguration config) throws IOException {
    String body = "{\"model\": \"" + config.getModel() + "\",\"temperature\": " + config.getTemperature() + ",\"max_tokens\": " + config.getMaxTokens() + ", \"messages\": [";
    body +=        "{\"role\": \"system\", \"content\": \"" + getInstructions().replace("\n", " ").replace("\r", " ").replace("\t", " ") + "\"},";

    for (String doc : documents)
    {
      body += "{\"role\": \"user\", \"content\": \"Here is one of the documents: " + doc.replace("\n", " ").replace("\r", " ").replace("\t", " ").replace("\"", "`") + "\"},";
    }
    body += "{\"role\": \"user\", \"content\": \"The user question is: " + prompt + "\"}";
    body += "]}";
    return body;
  }


  /**
   * @param prompt The user question
   * @param documents The list of documents content
   * @param config The global RAG configuration
   * @return The generated JSON body to attach to the HTTP POST request for a Datafari-RAG API
   */
  private static String generateJsonBodyForDatafariRag(String prompt, List<String> documents, RagConfiguration config) throws IOException {
    String context = (config.getAddInstructions()) ? getInstructions() + "\\n\\r" : "";
    for (String doc : documents)
    {
      context += "\\n\\r Document "+ (documents.indexOf(doc) + 1) + " \\n\\r " + doc.replace("\n", " ").replace("\r", " ").replace("\t", " ").replace("\"", "`");
    }
      return "{\"input\":{\"context\": \"" + context + "\",\"temperature\": " + config.getTemperature() + ",\"max_tokens\": " + config.getMaxTokens()+ ",\"question\": \"" + prompt + "\"}}";
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
    return response.substring(start, end);
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
    return response.substring(start, end);
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
    Properties prop = new Properties();
    String fileName = "rag.properties";
    try (InputStream fis = RagAPI.class.getClassLoader().getResourceAsStream(fileName)) {
      prop.load(fis);

      RagConfiguration config = new RagConfiguration();
      config.setToken(prop.getProperty("rag.api.token"));
      config.setEndpoint(prop.getProperty("rag.api.endpoint"));
      config.setModel(prop.getProperty("rag.model"));
      config.setTemperature(prop.getProperty("rag.temperature"));
      config.setMaxTokens(prop.getProperty("rag.maxTokens"));
      config.setMaxFiles(prop.getProperty("rag.maxFiles"));
      config.setAddInstructions(prop.getProperty("rag.addInstructions"));
      config.setTemplate(prop.getProperty("rag.template"));
      config.setSolrField(prop.getProperty("rag.solrField"));
      config.setHlFragsize(prop.getProperty("rag.hl.fragsize"));

      return config;

    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("An error occurred during the configuration. Configuration file not found.");
    } catch (NumberFormatException e) {
      throw new FileNotFoundException("An error occurred during the configuration. Invalid value for rag.maxTokens or rag.hl.fragsize or rag.maxFiles");
    } catch (IOException e) {
      throw new RuntimeException("An error occurred during the configuration.");
    }
  }



}
