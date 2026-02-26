package com.francelabs.datafari.rest.v2_0.users;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.service.db.ConversationDataService;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.EditableHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class manages users preferences and configuration,
 * related to the Datafari Assistant (conversation history, docs basket...)
 */
@RestController
public class Assistant {

  private static final Logger logger = LogManager.getLogger(Assistant.class.getName());

  /**
   * Called when Datafari Assistant is loaded.
   * It returns:
   *   the list of user's conversation (can be empty)
   *   the list of messages from the latest conversation (can be empty)
   *   the list of documents in the basket from the latest conversation (can be empty)
   * <p>
   * If a "conversationId" is provided, this conversation is retrieved instead of the latest one.
   *
   * @param request HttpServletRequest
   * @return String json.
   */
  @GetMapping(value = "rest/v2.0/users/conversations", produces = "application/json;charset=UTF-8")
  protected String getUserConversations(final HttpServletRequest request) {
    final ConversationDataService service = ConversationDataService.getInstance();
    final String authenticatedUserName = AuthenticatedUserName.getName(request);

    if (authenticatedUserName == null) {
        return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
    }

    // If a conversationId is provided, we retrieve docsBasket/messages from the specified conversation,
    // instead of the latest one.
    String conversationId = request.getParameter("conversationId");
    String title = null;

    JSONArray messages = new JSONArray();
    JSONArray docsBasket = new JSONArray();
    JSONArray conversations;

    try {
        // List all conversations
        List<Properties> conversationsSet = service.getUserConversations(authenticatedUserName);
        conversations = listToJson(conversationsSet);

        // If no conversation specified, we retrieve the latest one.
        // We also check if the user owns the requested conversation
        if (conversationId == null || !isValidConversation(conversationsSet, conversationId)) {
            Properties conversation = service.getLatestConversation(authenticatedUserName);
            if (conversation != null) {
              conversationId = conversation.getProperty("id"); // Use the latest conversation
              title = conversation.getProperty("title"); // Use the latest conversation
            } else conversationId = null; // No existing conversation

        } else {
            // Valid conversationId provided
            // update specified conversation's last_refresh
            service.refreshConversation(conversationId, authenticatedUserName);
        }

        if (conversationId != null) {

            // Get conversation title
            if (title == null) {
                title = service.getConversationTitle(conversationId);
            }

            // get messages from latest conversation
            List<Properties> messagesSet = service.getMessagesByConversation(conversationId, authenticatedUserName);
            messages = listToJson(messagesSet);

            // get docsBasket from latest conversation
            List<Properties> docsBasketSet = service.getDocsBasketByConversation(conversationId, authenticatedUserName);
            docsBasket = listToJson(docsBasketSet);

        }

    } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
    }

    final JSONObject responseContent = new JSONObject();
    responseContent.put("conversationId", conversationId);
    responseContent.put("conversations", conversations);
    responseContent.put("docsBasket", docsBasket);
    responseContent.put("messages", messages);
    responseContent.put("title", title);
    return RestAPIUtils.buildOKResponse(responseContent);
  }

  /**
   * Delete an entry from docs_basket, as long as it belongs to the user.
   */
  @DeleteMapping(value = "rest/v2.0/users/docsbasket", produces = "application/json;charset=UTF-8")
  protected String removeDocFromUserDocsBasket(final HttpServletRequest request) {

      final String authenticatedUserName = AuthenticatedUserName.getName(request);

      if (authenticatedUserName == null) {
        return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
      }
      final ConversationDataService service = ConversationDataService.getInstance();
      final String docBasketId = request.getParameter("id");

      if (docBasketId == null) return RestAPIUtils.buildErrorResponse(403, "Missing 'id' parameter", null);

      try {
        service.removeDocFromBasketById(docBasketId, authenticatedUserName);
      } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
      }

      return getUserDocsBasket(request);
  }

  /**
   * Delete an entry from docs_basket, as long as it belongs to the user.
   */
  @GetMapping(value = "rest/v2.0/users/docsbasket", produces = "application/json;charset=UTF-8")
  protected String getUserDocsBasket(final HttpServletRequest request) {

    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    JSONObject responseContent = new JSONObject();
    JSONArray docsbasket = new JSONArray();

    if (authenticatedUserName == null) {
      return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
    }
    final ConversationDataService service = ConversationDataService.getInstance();


    try {
      Properties lastConversation = service.getLatestConversation(authenticatedUserName);
      if (lastConversation != null && lastConversation.getProperty("id") != null) {
        String conversationId = lastConversation.getProperty("id");
        List<Properties> docsbasketList = service.getDocsBasketByConversation(conversationId, authenticatedUserName);
        docsbasket = listToJson(docsbasketList);
      }
    } catch (DatafariServerException e) {
      return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
    }

    responseContent.put("docsbasket", docsbasket);
    return RestAPIUtils.buildOKResponse(responseContent);
  }

  /**
   * Delete a conversation, as long as it belongs to the user.
   */
  @DeleteMapping(value = "rest/v2.0/users/conversations", produces = "application/json;charset=UTF-8")
  protected String deleteConversation(final HttpServletRequest request) {
      final String authenticatedUserName = AuthenticatedUserName.getName(request);

      if (authenticatedUserName == null) {
        return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
      }
      final ConversationDataService service = ConversationDataService.getInstance();
      final String conversationId = request.getParameter("conversationId");

      try {
        service.deleteConversation(conversationId, authenticatedUserName);
      } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
      }
      return getUserConversations(request);
  }


  /**
   * Create an empty conversation.
   */
  @PostMapping(value = "rest/v2.0/users/conversations", produces = "application/json;charset=UTF-8")
  protected String createConversation(final HttpServletRequest request,
                                           @RequestBody final String jsonParam) {
    final String authenticatedUserName = AuthenticatedUserName.getName(request);

    try {
//      final JSONParser parser = new JSONParser();
//      final JSONObject body = (JSONObject) parser.parse(jsonParam);
//
//      final String conversationId = body.get("conversationId") != null ? body.get("conversationId").toString() : null;
//      final String title = body.get("title") != null ? body.get("title").toString() : null;

        if (authenticatedUserName == null) {
          return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
        }

        final ConversationDataService service = ConversationDataService.getInstance();

        // Create a new conversation in DB
        Properties convProperties = new Properties();
        convProperties.put("username", authenticatedUserName);
        convProperties.put("title", "New conversation");
        String conversationId = service.createConversation(convProperties);

        // If the option is enabled, migrate docsbasket from previous conversation
        // Parameters
        if (jsonParam != null) {
            final JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(jsonParam);
            final String sourceConversationId = (body.get("sourceConversationId") != null) ? (String) body.get("sourceConversationId") : null;
            try {
                if (sourceConversationId != null && !sourceConversationId.isBlank()) {
                    service.migrateDocsBasket(sourceConversationId, conversationId, authenticatedUserName);
                }
            } catch (DatafariServerException e) {
                logger.warn("Failed to migrate docsbasket from conversation '{}' to conversation '{}':", sourceConversationId, conversationId, e);
            }

        }

        // Return full conversation
        EditableHttpServletRequest req = new EditableHttpServletRequest(request);
        req.addParameter("conversationId", conversationId);
        return getUserConversations(req);

    } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(400, e.getMessage(), null);
    } catch (Exception e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
    }
  }


  /**
   * Rename a conversation, as long as it belongs to the user.
   */
  @PutMapping(value = "rest/v2.0/users/conversations", produces = "application/json;charset=UTF-8")
  protected String updateConversationTitle(final HttpServletRequest request,
                                           @RequestBody final String jsonParam) {
      final String authenticatedUserName = AuthenticatedUserName.getName(request);

      try {
          final JSONParser parser = new JSONParser();
          final JSONObject body = (JSONObject) parser.parse(jsonParam);

          final String conversationId = body.get("conversationId") != null ? body.get("conversationId").toString() : null;
          final String title = body.get("title") != null ? body.get("title").toString() : null;

          if (conversationId == null || conversationId.isBlank() || title == null || title.isBlank()) {
              return RestAPIUtils.buildErrorResponse(400, "Invalid request. Missing conversationId or title.", null);
          }

          final ConversationDataService service = ConversationDataService.getInstance();
          service.updateConversationTitle(conversationId, authenticatedUserName, title);

          final JSONObject response = new JSONObject();
          response.put("conversationId", conversationId);
          response.put("title", title);

          return RestAPIUtils.buildOKResponse(response);

      } catch (ParseException e) {
          return RestAPIUtils.buildErrorResponse(400, "Invalid JSON.", null);
      } catch (DatafariServerException e) {
          return RestAPIUtils.buildErrorResponse(400, e.getMessage(), null);
      } catch (Exception e) {
          return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
      }
  }

  /**
   * Add a document to docs_basket, associated to the specified conversation.
   * If no conversation is provided, we try to use the default one, or create a new one if needed.
   * @param request HttpServletRequest
   * @param jsonParam: A JSON containing docTitle, docId
   */
  @PostMapping(value = "rest/v2.0/users/docsbasket", produces = "application/json;charset=UTF-8")
  protected String addDocToUserDocsBasket(final HttpServletRequest request, @RequestBody final String jsonParam) {

    final ConversationDataService service = ConversationDataService.getInstance();
    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    String id; // The ID of the created entry

    if (authenticatedUserName == null) {
      return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
    }

    try {
        final JSONParser parser = new JSONParser();
        JSONObject body = (JSONObject) parser.parse(jsonParam);
        final String docId = (body.get("docId") != null) ? (String) body.get("docId") : null;
        final String docTitle = (body.get("docTitle") != null) ? (String) body.get("docTitle") : null;
        String conversationId = (body.get("conversationId") != null) ? (String) body.get("conversationId") : null;

        if (docId == null || docTitle == null || docId.isBlank() || docTitle.isBlank()) {
            return RestAPIUtils.buildErrorResponse(400, "Invalid request. Missing docTitle or docId.", null);
        }

        // Retrieve conversations
        List<Properties> conversationsSet = service.getUserConversations(authenticatedUserName);
        if (conversationId != null && !isValidConversation(conversationsSet, conversationId)) {
            // Invalid conversationId specified
            return RestAPIUtils.buildErrorResponse(403, "Invalid conversationId.", null);
        } else if (conversationsSet.isEmpty()) {
            // No existing conversation for this user: create a new conversation
            Properties convProperties = new Properties();
            convProperties.put("username", authenticatedUserName);
            conversationId = service.createConversation(convProperties);
        } else if (conversationId == null) {
            // No ID specified: retrieve the latest conversation
            conversationId = conversationsSet.getFirst().getProperty("id");
        }

        // Add document to basket
        Properties document = new Properties();
        document.put("docId", docId);
        document.put("docTitle", docTitle);
        document.put("conversationId", conversationId);
        id = service.addDocToBasket(document);
    } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
    } catch (ParseException e) {
        return RestAPIUtils.buildErrorResponse(400, "Invalid JSON.", null);
    }

    JSONObject response = new JSONObject();
    response.put("id", id);
    return RestAPIUtils.buildOKResponse(response);
  }

  /**
   * Add a document to docs_basket, associated to the specified conversation.
   * If no conversation is provided, we try to use the default one, or create a new one if needed.
   * @param request HttpServletRequest
   * @param jsonParam: A JSON containing docTitle, docId
   */
  @PostMapping(value = "rest/v2.0/users/messages", produces = "application/json;charset=UTF-8")
  protected String addMessage(final HttpServletRequest request, @RequestBody final String jsonParam) {

    final String authenticatedUserName = AuthenticatedUserName.getName(request);

    if (authenticatedUserName == null) {
      return RestAPIUtils.buildErrorResponse(407, "Authentication required", null);
    }

    String conversationId;
    try {
        // Parameters
        final JSONParser parser = new JSONParser();
        JSONObject body = (JSONObject) parser.parse(jsonParam);
        final String content = (body.get("content") != null) ? (String) body.get("content") : null;
        final String role = (body.get("role") != null) ? (String) body.get("role") : null;
        conversationId = (body.get("conversationId") != null) ? (String) body.get("conversationId") : null;

        if (content == null || role == null || content.isEmpty() || role.isBlank()) {
          return RestAPIUtils.buildErrorResponse(400, "Invalid request. Missing role or content.", null);
        }

        // Save the message in Postgresql
        conversationId = saveMessage(request, role, content, conversationId);

    } catch (DatafariServerException e) {
        return RestAPIUtils.buildErrorResponse(500, e.getMessage(), null);
    } catch (ParseException e) {
        return RestAPIUtils.buildErrorResponse(400, "Invalid JSON.", null);
    }

    JSONObject response = new JSONObject();
    response.put("conversationId", conversationId);
    return RestAPIUtils.buildOKResponse(response);
  }


  /**
   * Save a message in Postgresql database.
   * @param request HttpServletRequest
   * @param role "user" or "assistant"
   * @param content Text content of the message
   * @param conversationId Conversation ID. If missing, a new conversation is created.
   * @return the conversationId
   * @throws DatafariServerException if the save fails
   */
  public String saveMessage(final HttpServletRequest request, String role, String content, String conversationId, String searchResults) throws DatafariServerException {

     final ConversationDataService service = ConversationDataService.getInstance();
      final String authenticatedUserName = AuthenticatedUserName.getName(request);

      if (authenticatedUserName == null) {
          return null;
      }


      if (role == null || role.isBlank() || ((content == null || content.isEmpty()) && (searchResults == null) )) {
          throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Invalid request. Missing role or content.");
      }

      // Check if user is allowed to use this conversation
      if (conversationId != null && !service.userOwnsConversation(conversationId, authenticatedUserName)) {
          // Invalid conversationId specified
          throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Invalid conversationId.");
      } else if (conversationId == null) {

          // No conversationId specified: create a new conversation
          Properties convProperties = new Properties();
          convProperties.put("username", authenticatedUserName);
          convProperties.put("title", StringUtils.abbreviate(content, 50));
          conversationId = service.createConversation(convProperties);
      }

    // TODO : check if the conversation needs to be renamed
    //        final ConversationDataService service = ConversationDataService.getInstance();
    //        service.updateConversationTitle(conversationId, authenticatedUserName, title);

      // Add message to DB
      Properties document = new Properties();
      document.put("role", role);
      document.put("content", content);
      document.put("conversationId", conversationId);
      if (searchResults != null && !searchResults.isBlank()) document.put("searchResults", searchResults);
      service.addMessage(document);

      return conversationId;
  }

  public String saveMessage(final HttpServletRequest request, String role, String content, String conversationId) throws DatafariServerException {
      return saveMessage( request, role, content, conversationId, null);
  }

  /**
   * Check if a conversationId is present within a user's conversations set.
   * @return boolean
   */
  private static boolean isValidConversation(List<Properties> conversationsSet, String conversationId) {
    return conversationsSet.stream()
            .map(prop -> prop.getProperty("id"))
            .anyMatch(conversationId::equals);
  }

  /**
   * Convert a list of entries into a JSONArray
   * @param entries List<Properties> A list of entries (messages, docsBasket, conversations...)
   * @return a JSONArray
   */
  private JSONArray listToJson (List<Properties> entries) {
    JSONArray json = new JSONArray();
    for (Properties entry : entries) {
      json.add(propertiesToJson(entry));
    }
    return json;
  }

  /**
   * Convert an entry into a JSONObject
   * @param properties Properties An entry (message, docsBasket, conversation...)
   * @return a JSONArray
   */
  private JSONObject propertiesToJson (Properties properties) {
    JSONObject jsonMap = new JSONObject();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {

      // Handle search_results entries
      if (ConversationDataService.SEARCH_RESULTS_COLUMN.equals(entry.getKey())) {
        // Convert search results to JSONArray
        try {
          JSONParser parser = new JSONParser();
          jsonMap.put("docs", (JSONArray)parser.parse(entry.getValue().toString()));
        } catch (ParseException e) {
            logger.error("Unable to convert retrieved search_results to JSONArray");
        }

      } else {
        // All other fields are converted to Strings
        jsonMap.put(entry.getKey(), entry.getValue());
      }
    }
    return jsonMap;
  }

}
