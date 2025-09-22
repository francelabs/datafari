package com.francelabs.datafari.ai.stream;

import dev.langchain4j.service.TokenStream;

import java.util.concurrent.*;

public final class AgentStreamer {

    public String stream(TokenStream ts, SseBridge sse) {
        StringBuilder full = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        ts.onPartialResponse(token -> {
                    full.append(token);        // accumuler la réponse finale
                    sse.send("token", token);  // streamer au fil de l’eau
                })
                .onPartialThinking(thinking -> {
                    // If model emits "thinking"
                    if (thinking != null && thinking.text() != null) {
                        sse.send("thinking", thinking.text());
                    }
                })
                .onError(err -> {
                    sse.send("error", "{\"message\":\"" + escape(err.getMessage()) + "\"}");
                    done.countDown();
                })
                .onCompleteResponse(resp -> {
                    // End of stream
                    sse.send("done", "{\"finishReason\":\"" + resp.finishReason() + "\"}");
                    done.countDown();
                })
                .start(); // IMPORTANT : lance le flux)

        // BLOQUER jusqu’à la fin (ou timeout de sécurité)
        try {
            // adapte le timeout si nécessaire, ou enlève-le si tu veux bloquer indéfiniment
            done.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return full.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}