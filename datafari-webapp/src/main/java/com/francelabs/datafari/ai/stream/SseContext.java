package com.francelabs.datafari.ai.stream;

public final class SseContext {
    private static final ThreadLocal<SseBridge> TL = new ThreadLocal<>();
    public static void bind(SseBridge sse) { TL.set(sse); }
    public static void unbind() { TL.remove(); }
    public static void send(String event, String data) { SseBridge sse = TL.get(); if (sse != null) sse.send(event, data); }
}