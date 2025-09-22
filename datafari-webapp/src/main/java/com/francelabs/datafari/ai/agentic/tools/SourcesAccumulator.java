package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.api.RagAPI;
import dev.langchain4j.data.document.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SourcesAccumulator {

    // Key -> Document
    private final Map<String, Document> byKey = new LinkedHashMap<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    /** Détermine la clé de dédup : parent_doc > id */
    private static String keyOf(Document d) {
        if (d == null) return "";
        String parent = str(meta(d, "parent_doc"));
        if (!parent.isBlank()) return "id:" + parent;
        return "id:" + str(meta(d, "id"));
    }

    private static Object meta(Document d, String key) {
        return d.metadata().getString(key);
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    /** Ajoute un doc, renvoie true si c’est une nouvelle source (non vue) */
    public synchronized boolean add(Document d) {
        String k = keyOf(d);
        if (k.isBlank() || seen.contains(k)) return false;
        seen.add(k);
        byKey.put(k, d);
        return true;
    }

    /** Ajoute plusieurs docs, renvoie la liste de ceux qui sont “nouveaux” */
    public synchronized List<Document> addAll(Collection<Document> docs) {
        List<Document> newly = new ArrayList<>();
        if (docs == null) return newly;
        for (Document d : docs) {
            if (add(d)) newly.add(d);
        }
        return newly;
    }

    /** Build final JSONArray (id, title, url, content) */
    public synchronized JSONArray toJsonArray() {
        JSONArray arr = new JSONArray();
        for (Document d : byKey.values()) {
            JSONObject o = new JSONObject();
            o.put("id", str(meta(d, "id")));
            o.put("title", str(meta(d, "title")));
            o.put("url", str(meta(d, "url")));
            // Truncate content
            String content = (d.text() == null) ? "" : d.text();
            if (content.length() > 200) content = content.substring(0, 200) + "…";
            o.put("content", content);
            arr.add(o);
        }
        return arr;
    }

    /** Convert a single Document to JSON */
    public static JSONObject toJson(Document d) {
        JSONObject o = new JSONObject();
        o.put("id", str(meta(d, "id")));
        o.put("title", str(meta(d, "title")));
        o.put("url", str(meta(d, "url")));
        String content = d.text() == null ? "" : d.text();
        if (content.length() > 400) content = content.substring(0, 400) + "…";
        o.put("content", content);
        return o;
    }
}