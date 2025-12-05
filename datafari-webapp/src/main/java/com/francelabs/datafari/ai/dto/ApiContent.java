package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiContent {
    public String message;           // Final response
    public String memoryId;          // Memory ID
    public JSONArray sources;
    public ApiError error;

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("message", message);
        if (memoryId != null && !memoryId.isEmpty()) json.put("memoryId", memoryId);
        json.put("sources", sources);
        if (error != null) json.put("error", error.toJson());
        return json;
    }
}
