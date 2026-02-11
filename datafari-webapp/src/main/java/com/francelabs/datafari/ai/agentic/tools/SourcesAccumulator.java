package com.francelabs.datafari.ai.agentic.tools;

import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import dev.langchain4j.data.document.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The SourcesAccumulator is used during AI Powered processes.
 * The added sources are:
 * - deduplicated (based on document ID)
 * - temporary stored in memory
 * - streamed to the user (if streaming is enabled)
 * - returned as sources in the final response (if streaming is disabled)
 */
public final class SourcesAccumulator {

    // Key -> Document
    private final Map<String, Document> byKey = new LinkedHashMap<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final ChatStream stream;

    public SourcesAccumulator(@Nullable ChatStream stream) {
        this.stream = stream;
    }

    /** Return the deduplication key : parent_doc > id */
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

    /** Add one doc, return true it is new. */
    public synchronized boolean add(Document d) {
        String k = keyOf(d);
        if (k.isBlank() || seen.contains(k)) return false;
        seen.add(k);
        byKey.put(k, d);
        streamSource(toJson(d));
        return true;
    }

    /** Add multiple docs, return the list of the "new ones" */
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
            o.put(AiService.ID_FIELD, str(meta(d, AiService.ID_FIELD)));
            o.put(AiService.TITLE_FIELD, str(meta(d, AiService.TITLE_FIELD)));
            o.put(AiService.URL_FIELD, str(meta(d, AiService.URL_FIELD)));
            // Truncate content
            String content = (d.text() == null) ? "" : d.text();
            if (content.length() > 200) content = content.substring(0, 200) + "…";
            o.put("content", content);
            arr.add(o);
        }
        streamSources(arr);
        return arr;
    }

    /** Convert a single Document to JSON */
    public static JSONObject toJson(Document d) {
        JSONObject o = new JSONObject();
        o.put(AiService.ID_FIELD, str(meta(d, AiService.ID_FIELD)));
        o.put(AiService.TITLE_FIELD, str(meta(d, AiService.TITLE_FIELD)));
        o.put(AiService.URL_FIELD, str(meta(d, AiService.URL_FIELD)));
        String content = d.text() == null ? "" : d.text();
        if (content.length() > 200) content = content.substring(0, 200) + "…";
        o.put("content", content);
        return o;
    }

    /** Stream one source if streaming is available */
    public void streamSource(JSONObject source) {
        if (stream != null) {
            stream.addSource(source);
        }
    }

    /** Stream all the sources if streaming is available */
    public void streamSources(JSONArray source) {
        if (stream != null) {
            stream.addSources(source);
        }
    }
}