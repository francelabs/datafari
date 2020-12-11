package com.francelabs.datafari.rest.v1_0.utils;

import org.json.simple.JSONObject;

public class RestAPIUtils {

    public static String buildOKResponse(JSONObject content) {
        JSONObject response = new JSONObject();
        response.put("status", "OK");
        response.put("content", content);
        return response.toJSONString();
    }

    public static String buildErrorResponse(int code, String reason, JSONObject extra) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("status", "ERROR");
        JSONObject errorResponseContent = new JSONObject();
        errorResponseContent.put("code", code);
        errorResponseContent.put("reason", reason);
        if (extra != null) {
            errorResponseContent.put("extra", extra);
        } 
        errorResponse.put("content", errorResponseContent);
        return errorResponse.toJSONString();
    }
}
