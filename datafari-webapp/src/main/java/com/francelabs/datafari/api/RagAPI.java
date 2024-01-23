package com.francelabs.datafari.api;

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

  public static JSONObject rag(final HttpServletRequest request) throws IOException {
    final String handler = getHandler(request);
    final String protocol = request.getScheme() + ":";
    final Map<String, String[]> parameterMap = new HashMap<String, String[]>(request.getParameterMap());
    if (request.getParameter("id") == null && request.getAttribute("id") != null && request.getAttribute("id") instanceof String) {
      final String idParam[] = { (String) request.getAttribute("id") };
      parameterMap.put("id", idParam);
    }
    // Todo : validate query
    String userQuery = request.getParameter("q");
    if (userQuery == null) {
      final JSONObject response = new JSONObject();
      return (JSONObject) response.put("error", writeJsonError(422, "No query provided."));
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
    JSONObject result = search(protocol, handler, request.getUserPrincipal(), parameterMap);
    // Todo : if callAiApi invalid, return error
    return callAiApi(userQuery, result);

  }

  private static JSONObject callAiApi(String prompt, JSONObject searchResult) throws IOException {
    JSONObject searchResponse = (JSONObject) searchResult.get("response");

    // The response that will be sent to user
    final JSONObject response = new JSONObject();
    List<String> documentsContent = extractDocumentsContent(searchResponse);

    if (documentsContent.isEmpty()) {
      response.put("error", writeJsonError(428, "The query cannot be answered because no associated documents were found."));
      return response;
    }

    String gptResponse = chatGPT(prompt, documentsContent);
    System.out.println(gptResponse);

    // Todo : check the validity of the response
    if (true) { // Todo : Valid response
      response.put("status", "OK");
      response.put("content", gptResponse);
      return response;
    } else {
      response.put("error", writeJsonError(428, "The query cannot be answered because no associated documents were found."));
      return response;
    }
  }

  private static JSONObject writeJsonError(int code, String message) {
    final JSONObject error = new JSONObject();
    error.put("status", "ERROR");
    error.put("code", code);
    error.put("message", message);
    return error;
  }

  private static List<String> extractDocumentsContent(JSONObject response) {
    try {
      int maxFiles = 3;
      List<String> documentsContent = new ArrayList<>();

      if (response != null && response.get("docs") != null) {
        JSONArray docs = (JSONArray) response.get("docs");
        if (docs.size() < maxFiles) maxFiles = docs.size(); // MaxFiles must not exceed the number of provided documents
        for (int i = 0; i < maxFiles; i++) {
          JSONArray exactContent = (JSONArray) ((JSONObject) docs.get(i)).get("preview_content"); // You can use exactContent to send the whole file content
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

  /**
   * Send a query to ChatGPT API
   * @param prompt : the user question
   * @param documents : A list of String documents to send with the user prompt
   * @return The extracted response from ChatGPT
   */
  public static String chatGPT(String prompt, List<String> documents) throws IOException {
    String url;
    String apiKey;
    String model;
    String temperature;
    String maxTokens;
    String instructions = getInstructions();

    // Set API configuration using
    Properties prop = new Properties();
    String fileName = "rag.config";
    try (InputStream fis = RagAPI.class.getClassLoader().getResourceAsStream(fileName)) {
      prop.load(fis);
      apiKey = prop.getProperty("rag.api.token");
      url = prop.getProperty("rag.api.endpoint");
      model = prop.getProperty("rag.model");
      temperature = prop.getProperty("rag.temperature");
      maxTokens = prop.getProperty("rag.maxTokens");

      URL obj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");

      // The request body
      String body = "{\"model\": \"" + model + "\",\"temperature\": " + temperature + ",\"max_tokens\": " + maxTokens + ", \"messages\": [";
      body +=        "{\"role\": \"system\", \"content\": \"" + instructions.replace("\n", " ").replace("\r", " ").replace("\t", " ") + "\"},";

      for (String doc : documents)
      {
        body += "{\"role\": \"user\", \"content\": \"Here is one of the documents: " + doc.replace("\n", " ").replace("\r", " ").replace("\t", " ").replace("\"", "`") + "\"},";
      }
      body += "{\"role\": \"user\", \"content\": \"The user question is: " + prompt + "\"}";
      body += "]}";


      connection.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from ChatGPT
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;

      StringBuffer response = new StringBuffer();

      while ((line = br.readLine()) != null) {
        response.append(line);
      }
      br.close();

      // calls the method to extract the message.
      return extractMessageFromJSONResponse(response.toString());

    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("An error occurred during the configuration. Configuration file not found.");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
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
  public static String extractMessageFromJSONResponse(String response) {
    response = response.replace("\\\"", "`");
    int start = response.indexOf("content")+ 11;
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



}
