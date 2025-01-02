package com.francelabs.datafari.utils.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.Objects;

public class AiDocument {

    @JsonProperty("id")
    String id;

    @JsonProperty("url")
    String url;

    @JsonProperty("title")
    String title;

    @JsonProperty("content")
    String content;

    public AiDocument() {
    }

    public AiDocument(String id, String url, String title, String content) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public JSONObject toJSONObject() {
        try {
            String jsonString = new ObjectMapper().writeValueAsString(this);
            final JSONParser parser = new JSONParser();
            final JSONObject jsonCategories = (JSONObject) parser.parse(jsonString);
            return jsonCategories;
        } catch (JsonProcessingException | ParseException e) {
            return new JSONObject();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AiDocument)
        {
            AiDocument doc = (AiDocument) obj;
            return this.id.equals(doc.id) && this.url.equals(doc.url) && this.title.equals(doc.title);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, title, content);
    }
}