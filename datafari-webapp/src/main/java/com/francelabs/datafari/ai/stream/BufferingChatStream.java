package com.francelabs.datafari.ai.stream;

import com.francelabs.datafari.ai.dto.ApiError;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

public class BufferingChatStream implements ChatStream {
    public final StringBuilder message = new StringBuilder();
    public final JSONArray sources = new JSONArray();
    public ApiError error;

    // TODO : simply ignore ?
    @Override public void event(String type, Map<String, ?> data) {
        switch (type) {
            case "message.delta" -> message.append((String)data.get("text"));
            case "source.add" -> sources.add((JSONObject)data.get("source"));
            case "sources.add" -> {
                var items = (JSONArray)data.get("items");
                if (items != null) sources.addAll(items);
            }
            case "error" -> error = new ApiError(
                    (String)data.get("code"),
                    (String)data.get("label"),
                    (String)data.get("message"),
                    (String)data.get("reason")
            );
            case "message.final" -> {
                // If the model returns the final response, we override the concatenated one
                var t = (String)data.get("text");
                if (t != null) { message.setLength(0); message.append(t); }
            }
            default -> {} // ignore other stream events
        }
    }
}