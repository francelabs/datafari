package com.francelabs.datafari.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.simple.JSONObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    public String status; // "OK" | "ERROR"
    public ApiContent content = new ApiContent();

    public ApiResponse() {
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("status", status);
        json.put("content", content.toJson());
        return json;
    }
}
