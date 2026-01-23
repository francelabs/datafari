package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiContent {
    public String message;           // Final response
    public String conversationId;    // Conversation ID
    public String memoryId;    // Conversation ID
    public JSONArray sources;
    public JSONArray docs;
    public ApiError error;

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("message", message);
        if (conversationId != null && !conversationId.isEmpty()) json.put("conversationId", conversationId);
        if (docs != null && !docs.isEmpty()) json.put("docs", docs); // For assistant search results
        json.put("sources", sources);
        if (error != null) json.put("error", error.toJson());
        return json;
    }
}
