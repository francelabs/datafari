package com.francelabs.datafari.ai.stream;

import org.json.simple.JSONObject;

import jakarta.servlet.AsyncContext;
import java.util.Map;

public class NdjsonEmitter {
    private final AsyncContext async;
    private final java.io.PrintWriter out;
    private long seq = 0L;

    public NdjsonEmitter(AsyncContext async) throws java.io.IOException {
        this.async = async;
        this.out = async.getResponse().getWriter();
    }

    public void send(Map<String, ?> obj) {
        out.write(new JSONObject(obj).toString());
        out.write("\n");
        out.flush();
    }

    public void close() { try { out.flush(); async.complete(); } catch (Exception ignored) {} }
}
